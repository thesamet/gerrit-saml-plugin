# Setting-up gerrit-saml-plugin for Active Directory Federation Services (ADFS)
Note: replace `fs.hc.sct` with the name of your ADFS, replace gerrit.hc.sct with the name of your Gerrit host.

## Setup on the Gerrit machine
Configure Gerrit as described in the [README.md](). Here is an example config
using SAML for Authentication, LDAP for authorization and running gerrit under the `gerrit` prefix.

    [gerrit]
            basePath = git
            canonicalWebUrl = https://gerrit.hc.sct/gerrit/
    ...
    [httpd]
        listenUrl = https://gerrit.hc.sct:8443/gerrit/
        filterClass = com.thesamet.gerrit.plugins.saml.SamlWebFilter
    [auth]
        type = HTTP_LDAP
        logoutUrl = https://fs.hc.sct/adfs/ls/?wa=wsignout1.0
        httpHeader = X-SAML-UserName
        httpDisplaynameHeader = X-SAML-DisplayName
        httpEmailHeader = X-SAML-EmailHeader
        httpExternalIdHeader = X-SAML-ExternalId
    [saml]
        keystorePath = /home/gerrit/samlKeystore.jks
        keystorePassword = pac4j-demo-password
        privateKeyPassword = pac4j-demo-password
        metadataPath = file:///home/gerrit/FederationMetadata.xml
    [ldap]
            server = ldap://fs.hc.sct
            username = CN=Administrator,CN=Users,DC=hc,DC=sct
            localUsernameToLowerCase = true
            sslVerify = false
            accountBase = DC=hc,DC=sct
            groupBase = DC=hc,DC=sct

You can download the IdP file FederationMeta.xml from your ADFS. You need to place it
in the location configured with saml.metadataPath (note that this is an URL and that file:// is required).

    wget https://fs.hc.sct/FederationMetadata/2007-06/FederationMetadata.xml


Export the certificate from the samlKeystore.jks you created during setup. You will need the certificate in your ADFS configuration (see below).

    keytool -exportcert  -keystore samlKeystore.jks -alias pac4j -rfc > pac4j-demo.cer

## Setup on ADFS

Open the Management console (mmc), make sure you have the AD FS Management snap-in. Add a Relying Party Trust.
![][screen16]

Go through the wizard. The properties at the end should look like indicated on the following screens.


Monitoring: unmodified

![][screen01]

Identifiers: The relying party identifier is: `https://gerrit.hc.sct/gerrit/plugins/gerrit-saml-plugin/saml`

![][screen02]

Encryption: unmodified

![][screen03]

Signature: In the signature tab you need to import the certificate you exported above.

![][screen04]

Accepted Claims: unmodified

![][screen05]

Organization: unmodified

![][screen06]

Endpoints: URL is `https://gerrit.hc.sct/gerrit/plugins/gerrit-saml-plugin/saml`, binding POST

![][screen07]

Proxy Endpoints: unmodified

![][screen08]

Notes: unmodfied

![][screen09]

Advanced: SHA-256

![][screen10]


Select the `Relying Party Truct` and click on `Edit Claim Rules...`.
You should expose the following LDAP attributes:

![][screen11]
![][screen12]

Allow all users to connect, or modify depending on your setup:

![][screen13]
![][screen14]

Delegation Authorization Rules: unmodified

![][screen15]



[screen01]: images/0.png    "screen 01"
[screen02]: images/1.png    "screen 02"
[screen03]: images/2.png    "screen 03"
[screen04]: images/3.png    "screen 04"
[screen05]: images/4.png    "screen 05"
[screen06]: images/5.png    "screen 06"
[screen07]: images/6.png    "screen 07"
[screen08]: images/7.png    "screen 08"
[screen09]: images/8.png    "screen 09"
[screen10]: images/9.png    "screen 10"
[screen11]: images/10.png   "screen 11"
[screen12]: images/11.png   "screen 12"
[screen13]: images/12.png   "screen 13"
[screen14]: images/13.png   "screen 14"
[screen15]: images/14.png   "screen 15"
[screen16]: images/15.png   "screen 16"









