# Gerrit SAML Plugin

This plugin allows you to authenticate to Gerrit using a SAML identity
provider.

## Installation

Gerrit looks for 3 attributes (which are configurable) in the AttributeStatement:

- **DisplayName:** the full name of the user.
- **EmailAddress:** email address of the user.
- **UserName:** username (used for ssh).

If any of these attributes is not found in the assertion, their value is
taken from the NameId field of the SAML assertion.

### Setting Gerrit in your IdP (Okta, Onelogin, ...)

- Create a new SAML 2.0 application.
- Set the following parameters:
  - Single sign on URL: http://gerrit.site.com/plugins/gerrit-saml-plugin/saml
  - Check "Use this for Recipient URL and Destination URL".
  - Audience URI (SP Entity Id): http://gerrit.site.com/plugins/gerrit-saml-plugin/saml
  - We need to set up the attributes in the assertion to send the right
    information. Here is how to do it with Okta:
    - Application username: "Okta username prefix"
    - Add attribute statement: Name: "DisplayName" with Value
      "user.displayName"
    - Add attribute statement: Name: "EmailAddress" with Value
      "user.email"
    - **IMPORTANT**: If you are not using Okta, you need to set up an attribute
      "UserName" with the value of the username (not email, without @). If you
      do not do so, the name will be taken from the NameId provided by
      the assertion.  This is why in Okta we set the application username to
      "Okta username prefix".
- Obtain your IdP metadata (either URL or a local XML file)

If you are using Active Directory Federation Services (ADFS), follow the below steps to configure Gerrit.
You can then [go here](doc/Setup_ADFS.md) for more details on howto make gerrit-saml-plugin work with ADFS.

### Download the plugin

Download [gerrit-saml-plugin](https://bintray.com/artifact/download/thesamet/maven/gerrit-saml-plugin-2.11.5-1.jar) and put it in $gerrit_site/lib/.

### Configure Gerrit to use the SAML filter:
In `$site_path/etc/gerrit.config` file, the `[httpd]` section should contain

```
[httpd]
    filterClass = com.thesamet.gerrit.plugins.saml.SamlWebFilter
```

### Configure HTTP authentication for Gerrit:

Please make sure you are using Gerrit 2.11.5 or later.

In `$site_path/etc/gerrit.config` file, the `[auth]` section should include
the following lines:

```
[auth]
	type = HTTP
    logoutUrl = https://mysso.example.com/logout
    httpHeader = X-SAML-UserName
    httpDisplaynameHeader = X-SAML-DisplayName
    httpEmailHeader = X-SAML-EmailHeader
    httpExternalIdHeader = X-SAML-ExternalId
```

The header names are used internally between the SAML plugin and Gerrit to
communicate the user's identity.  You can use other names (as long as it will
not conflict with any other HTTP header Gerrit might expect).

### Create a local keystore

In `$gerrit_site/etc` create a local keystore:

```
keytool -genkeypair -alias pac4j -keypass pac4j-demo-password \
  -keystore samlKeystore.jks \
  -storepass pac4j-demo-password -keyalg RSA -keysize 2048 -validity 3650
```

### Configure SAML

Add a new `[saml]` section to `$site_path/etc/gerrit.config`:

```
[saml]
    keystorePath = /path/to/samlKeystore.jks
    keystorePassword = pac4j-demo-password
    privateKeyPassword = pac4j-demo-password
    metadataPath = https://mycompany.okta.com/app/hashash/sso/saml/metadata
```

**saml.metadataPath**: Location of IdP Metadata from your SAML identity provider.
The value can be a URL, or a local file (prefix with `file://`)

**saml.keystorePath**: Path to the keystore created above. If not absolute,
the path is resolved relative to `$site_path`.

**saml.privateKeyPassword**: Password protecting the private key of the generated
key pair (needs to be the same as the password provided throguh the `keypass`
flag above.)

**saml.keystorePassword**: Password that is used to protect the integrity of the
keystore (needs to be the same as the password provided throguh the `keystore`
flag above.)

**saml.displayNameAttr**: Gerrit will look for an attribute with this name in
the assertion to find a display name for the user. If the attribute is not
found, the NameId from the SAML assertion is used instead.

Default is `DisplayName`

**saml.emailAddressAttr**: Gerrit will look for an attribute with this name in
the assertion to find a the email address of the user. If the attribute is not
found, the NameId from the SAML assertion is used instead.

Default is `EmailAddress`

**saml.userNameAttr**: Gerrit will look for an attribute with this name in the
assertion to find a the email address of the user. If the attribute is not
found, the NameId from the SAML assertion is used instead.

Default is `UserName`

## Development

- Clone this repository.
- Install sbt.
- Edit the code.
- Run 'sbt assembly' to build the jar.
- Copy target/out/gerrit-saml-plugin-$VERSION.jar into $site_path/lib/

