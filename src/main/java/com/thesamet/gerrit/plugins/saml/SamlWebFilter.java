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
import com.google.inject.Singleton;
import org.eclipse.jgit.lib.Config;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.profile.SAML2Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
    static final String GERRIT_LOGIN = "/login";
    static final String SAML_POSTBACK = "/plugins/gerrit-saml-plugin/saml";

    private static final String SESSION_ATTR_USER = "Gerrit-Saml-User";

    static final Logger log = LoggerFactory.getLogger(SamlWebFilter.class);
    private final SAML2Client saml2Client;
    private final SamlConfig samlConfig;
    private final String httpUserNameHeader;
    private final String httpDisplaynameHeader;
    private final String httpEmailHeader;
    private final String httpExternalIdHeader;
    private final HashSet<String> authHeaders;

    private String getHeaderFromConfig(Config gerritConfig, String name) {
        String s = gerritConfig.getString("auth", null, name);
        return s == null ? "" : s.toUpperCase();
    }

    @Inject
    SamlWebFilter(@GerritServerConfig Config gerritConfig, SamlConfig samlConfig) {
        this.samlConfig = samlConfig;
        log.info("Max Authentication Lifetime: "+ samlConfig.getMaxAuthLifetime());
        SAML2ClientConfiguration samlClientConfig = new SAML2ClientConfiguration(
                samlConfig.getKeystorePath(), samlConfig.getKeystorePassword(),
                samlConfig.getPrivateKeyPassword(), samlConfig.getMetadataPath());
        samlClientConfig.setMaximumAuthenticationLifetime(samlConfig.getMaxAuthLifetime());
        saml2Client =
                new SAML2Client(samlClientConfig);
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

    private void signin(J2EContext context) throws HttpAction, IOException {
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
            if (null == redirectUri || redirectUri.isEmpty()) {
                redirectUri = "/";
            }
            context.getResponse().sendRedirect(context.getRequest().getContextPath() + redirectUri);
        }
    }

    @Override
    public void doFilter(ServletRequest incomingRequest, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        /* The first thing we do is to wrap the request in an anonymous request, so in case
           a malicious user is trying to set the headers manually, they'll be discarded.
         */
        HttpServletRequest httpRequest = new AnonymousHttpRequest(
                (HttpServletRequest) incomingRequest);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        AuthenticatedUser user = userFromRequest(httpRequest);

        try {
            if (isSamlPostback(httpRequest)) {
                J2EContext context = new J2EContext(httpRequest, httpResponse);
                signin(context);
            } else if (isGerritLogin(httpRequest)) {
                if (user == null) {
                    J2EContext context = new J2EContext(httpRequest, httpResponse);
                    redirectToIdentityProvider(context);
                } else {
                    HttpServletRequest req = new AuthenticatedHttpRequest(httpRequest, user);
                    chain.doFilter(req, response);
                }
            } else if (isGerritLogout(httpRequest)) {
                httpRequest.getSession().removeAttribute(SESSION_ATTR_USER);
                chain.doFilter(httpRequest, httpResponse);
            } else {
                chain.doFilter(httpRequest, httpResponse);
            }
        } catch (final HttpAction requiresHttpAction) {
            throw new TechnicalException("Unexpected HTTP action", requiresHttpAction);
        }
    }

    private void redirectToIdentityProvider(J2EContext context)
            throws HttpAction {
        String redirectUri = Url.decode(context
                .getRequest()
                .getRequestURI()
                .substring(
                        context.getRequest().getContextPath().length()));
        context.setSessionAttribute(SAML2Client.SAML_RELAY_STATE_ATTRIBUTE, redirectUri);
        log.debug("Setting redirectUri: {}", redirectUri);
        saml2Client.redirect(context);
    }

    private static boolean isGerritLogin(HttpServletRequest request) {
        return request.getRequestURI().indexOf(GERRIT_LOGIN) >= 0;
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
                                        @Nonnull AuthenticatedUser user) {
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

    public static void logWarning(String message) {
        log.warn(message);
    }

    public static void logError(String message) {
        log.error(message);
    }

    private class AnonymousHttpRequest extends HttpServletRequestWrapper {
        public AnonymousHttpRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final Enumeration<String> wrappedHeaderNames = super.getHeaderNames();

            HashSet<String> headerNames = new HashSet<>();
            while (wrappedHeaderNames.hasMoreElements()) {
                String header = wrappedHeaderNames.nextElement();
                if (!authHeaders.contains(header.toUpperCase())) {
                    headerNames.add(wrappedHeaderNames.nextElement());
                }
            }
            return Iterators.asEnumeration(headerNames.iterator());
        }

        @Override
        public String getHeader(String name) {
            String nameUpperCase = name.toUpperCase();
            if (authHeaders.contains(nameUpperCase)) {
                return null;
            } else {
                return super.getHeader(name);
            }
        }
    }
}
