// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.thesamet.gerrit.plugins.saml;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.restapi.Url;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.eclipse.jgit.lib.Config;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

@Singleton
class SamlWebFilter implements Filter {
    static final String GERRIT_LOGOUT = "/logout";
    static final String SAML_POSTBACK = "/plugins/gerrit-saml-plugin/saml";

    private static final String SESSION_ATTR_USER = "Gerrit-Saml-User";

    static final Logger log = LoggerFactory.getLogger(SamlWebFilter.class);
    private final Injector injector;
    private final SAML2Client saml2Client;
    private final SamlConfig samlConfig;
    private final String httpUserNameHeader;
    private final String httpDisplaynameHeader;
    private final String httpEmailHeader;
    private final String httpExternalIdHeader;
    private final HashSet<String> authHeaders;
    private final String logoutUrl;

    private String getHeaderFromConfig(Config gerritConfig, String name) {
        String s = gerritConfig.getString("auth", null, name);
        return s == null ? "" : s.toUpperCase();
    }

    @Inject
    SamlWebFilter(Injector injector, @GerritServerConfig Config gerritConfig, SamlConfig samlConfig) {
        this.injector = injector;
        this.samlConfig = samlConfig;
        saml2Client =
                new SAML2Client(new SAML2ClientConfiguration(
                        samlConfig.getKeystorePath(), samlConfig.getKeystorePassword(),
                        samlConfig.getPrivateKeyPassword(), samlConfig.getMetadataPath()));
        String callbackUrl = gerritConfig.getString("gerrit", null, "canonicalWebUrl") + "plugins/gerrit-saml-plugin/saml";
        httpUserNameHeader = getHeaderFromConfig(gerritConfig, "httpHeader");
        httpDisplaynameHeader = getHeaderFromConfig(gerritConfig, "httpDisplaynameHeader");
        httpEmailHeader = getHeaderFromConfig(gerritConfig, "httpEmailHeader");
        httpExternalIdHeader = getHeaderFromConfig(gerritConfig, "httpExternalIdHeader");
        authHeaders = Sets.newHashSet(
                httpUserNameHeader,
                httpDisplaynameHeader,
                httpEmailHeader,
                httpExternalIdHeader);
        if (authHeaders.contains("") || authHeaders.contains(null)) {
            throw new RuntimeException("All authentication headers must be set.");
        }
        if (authHeaders.size() != 4) {
            throw new RuntimeException("Unique values for httpUserNameHeader, " +
                    "httpDisplaynameHeader, httpEmailHeader and httpExternalIdHeader " +
                    "are required.");
        }
        logoutUrl = gerritConfig.getString("auth", null, "logoutUrl");

        saml2Client.setCallbackUrl(callbackUrl);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    private AuthenticatedUser userFromRequest(HttpServletRequest request) {
        HttpSession s = request.getSession();
        AuthenticatedUser user = (AuthenticatedUser) s.getAttribute(SESSION_ATTR_USER);
        if (user == null || user.getUsername() == null)
            return null;
        else return user;
    }

    private void signin(J2EContext context) throws RequiresHttpAction, IOException {
        SAML2Credentials credentials = saml2Client.getCredentials(context);
        SAML2Profile user = saml2Client.getUserProfile(credentials, context);
        if (user != null) {
            log.debug("Received SAML callback for userId={} with attributes: {}",
                    getUserName(user), user.getAttributes());
            HttpSession s = context.getRequest().getSession();
            s.setAttribute(SESSION_ATTR_USER, new AuthenticatedUser(
                    getUserName(user),
                    getDisplayName(user),
                    getEmailAddress(user),
                    "saml/" + user.getId()));

            String redirectUri = context.getRequest().getParameter("RelayState");
            log.debug("Got {}", redirectUri);
            if (null == redirectUri) {
                redirectUri = "/";
            }
            context.getResponse().sendRedirect(redirectUri);
        } else {
            signout(context.getRequest(), context.getResponse());
        }
    }

    private void signout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession();
        s.removeAttribute(SESSION_ATTR_USER);
        response.sendRedirect(logoutUrl);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        AuthenticatedUser user = userFromRequest(httpRequest);

        try {
            if (isSamlPostback(httpRequest)) {
                J2EContext context = new J2EContext(httpRequest, httpResponse);
                signin(context);
            } else if (isGerritLogout(httpRequest)) {
                signout(httpRequest, httpResponse);
            } else if (isAllowedWithoutAuth(httpRequest)) {
                // We allow URLs to continue without a user (and hence without the authentication
                // headers.  It is up for Gerrit to approve or deny these requests.  We do it
                // specifically for favicon.ico:  it could be that during a normal authentication
                // redirect, the browser will try to fetch /favicon.ico which will start a
                // parallel authentication process, but it will override the redirectUri and the
                // user will be redirected to /favicon.ico.  This would have been eliminated
                // if pac4j would allow obtaining RelayState...
                chain.doFilter(httpRequest, response);
            } else if (user == null) {
                J2EContext context = new J2EContext(httpRequest, httpResponse);
                redirectToIdentityProvider(context);
            } else {
                HttpServletRequest req = new AuthenticatedHttpRequest(httpRequest, user);
                chain.doFilter(req, response);
            }
        } catch (final RequiresHttpAction requiresHttpAction) {
            throw new TechnicalException("Unexpected HTTP action", requiresHttpAction);
        }
    }

    private boolean isAllowedWithoutAuth(HttpServletRequest httpRequest) {
        return (httpRequest.getRequestURI().equals("/favicon.ico"));
    }

    private void redirectToIdentityProvider(J2EContext context)
            throws RequiresHttpAction {
        String redirectUri = Url.decode(context
                .getRequest()
                .getRequestURI()
                .substring(
                        context.getRequest().getContextPath().length()));
        context.setSessionAttribute(SAML2Client.SAML_RELAY_STATE_ATTRIBUTE, redirectUri);
        log.debug("Setting redirectUri: {}", redirectUri);
        saml2Client.redirect(context, true);
    }

    private static boolean isGerritLogout(HttpServletRequest request) {
        return request.getRequestURI().indexOf(GERRIT_LOGOUT) >= 0;
    }

    private static boolean isSamlPostback(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && request.getRequestURI().indexOf(SAML_POSTBACK) >= 0;
    }

    private String getAttribute(SAML2Profile user, String attrName) {
        List<?> names = (List<?>) user.getAttribute(attrName);
        if (names != null && !names.isEmpty()) {
            return (String) names.get(0);
        }
        return null;
    }

    private String getAttributeOrElseId(SAML2Profile user, String attrName) {
        String value = getAttribute(user, attrName);
        if (value != null) {
            return value;
        }
        return user.getId();
    }

    private String getDisplayName(SAML2Profile user) {
        return getAttributeOrElseId(user, samlConfig.getDisplayNameAttr());
    }

    private String getEmailAddress(SAML2Profile user) {
        String emailAddress = getAttribute(user, samlConfig.getEmailAddressAttr());
        if (emailAddress != null) {
            return emailAddress;
        }
        String nameId = user.getId();
        if (!nameId.contains("@")) {
            log.debug(
                    "Email address attribute not found, NameId {} does not look like an email.",
                    nameId);
            return null;
        }
        return emailAddress;
    }

    private String getUserName(SAML2Profile user) {
        return getAttributeOrElseId(user, samlConfig.getUserNameAttr());
    }

    private class AuthenticatedHttpRequest extends HttpServletRequestWrapper {
        private AuthenticatedUser user;

        public AuthenticatedHttpRequest(HttpServletRequest request,
                                        AuthenticatedUser user) {
            super(request);
            this.user = user;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final Enumeration<String> wrappedHeaderNames = super.getHeaderNames();
            HashSet<String> headerNames = new HashSet<>(authHeaders);
            while (wrappedHeaderNames.hasMoreElements()) {
                headerNames.add(wrappedHeaderNames.nextElement());
            }
            return Iterators.asEnumeration(headerNames.iterator());
        }

        @Override
        public String getHeader(String name) {
            String nameUpperCase = name.toUpperCase();
            if (httpUserNameHeader.equals(nameUpperCase)) {
                return user.getUsername();
            } else if (httpDisplaynameHeader.equals(nameUpperCase)) {
                return user.getDisplayName();
            } else if (httpEmailHeader.equals(nameUpperCase)) {
                return user.getEmail();
            } else if (httpExternalIdHeader.equals(nameUpperCase)) {
                return user.getExternalId();
            } else {
                return super.getHeader(name);
            }
        }
    }
}
