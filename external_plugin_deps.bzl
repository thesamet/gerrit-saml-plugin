load("//tools/bzl:maven_jar.bzl", "maven_jar")

SHIBBOLETH = "https://build.shibboleth.net/nexus/content/repositories/releases/"

def external_plugin_deps():
    # Transitive dependency of velocity
    maven_jar(
        name = "commons-collections",
        artifact = "commons-collections:commons-collections:3.2.2",
        sha1 = "8ad72fe39fa8c91eaaf12aadb21e0c3661fe26d5",
    )

    maven_jar(
        name = "cryptacular",
        artifact = "org.cryptacular:cryptacular:1.2.1",
        sha1 = "c470bac7309ac04b0b9529bd7dcb1e0b75954f11",
    )

    maven_jar(
        name = "joda-time",
        artifact = "joda-time:joda-time:2.9.9",
        sha1 = "f7b520c458572890807d143670c9b24f4de90897",
    )

    maven_jar(
        name = "opensaml-core",
        artifact = "org.opensaml:opensaml-core:3.4.0",
        sha1 = "a1a4607c800bebd921615ef6fdb7bcd1340452c3",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-saml-api",
        artifact = "org.opensaml:opensaml-saml-api:3.4.0",
        sha1 = "538e1e54e5e8160f2d284b08f1b8a7b93053e0da",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-storage-api",
        artifact = "org.opensaml:opensaml-storage-api:3.4.0",
        sha1 = "37beed2a755b031a57322a1c910eca8278371f4f",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-saml-impl",
        artifact = "org.opensaml:opensaml-saml-impl:3.4.0",
        sha1 = "06336645ec0b0fbd98a7a5e719b4c4c284a4d79f",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-soap-impl",
        artifact = "org.opensaml:opensaml-soap-impl:3.4.0",
        sha1 = "23f0b2732c87a34c0179584e71a52839eaf9c186",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-soap-api",
        artifact = "org.opensaml:opensaml-soap-api:3.4.0",
        sha1 = "830b14c47a7e3e21ed377be4c82e6f19ff5c6749",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-xmlsec-api",
        artifact = "org.opensaml:opensaml-xmlsec-api:3.4.0",
        sha1 = "6af095f2882bd73067860699c9b2d11f5e5d9fa2",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-xmlsec-impl",
        artifact = "org.opensaml:opensaml-xmlsec-impl:3.4.0",
        sha1 = "94ea339d9e63436cdf4a2247b5ef85867e66d302",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-security-api",
        artifact = "org.opensaml:opensaml-security-api:3.4.0",
        sha1 = "a39782c2c23abf09f9e0e6c51a33f7e658654342",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "stax2-api",
        artifact = "org.codehaus.woodstox:stax2-api:3.1.4",
        sha1 = "ac19014b1e6a7c08aad07fe114af792676b685b7",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-security-impl",
        artifact = "org.opensaml:opensaml-security-impl:3.4.0",
        sha1 = "ee6158d53b576d6a63d3c7a0cf063c8518e75126",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-profile-api",
        artifact = "org.opensaml:opensaml-profile-api:3.4.0",
        sha1 = "e881c110d608f9e515681315d1509cd4d0737fa7",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-profile-impl",
        artifact = "org.opensaml:opensaml-profile-impl:3.4.0",
        sha1 = "312eb6bf5dd7fb34c4305299e83b24ba10814f86",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-messaging-api",
        artifact = "org.opensaml:opensaml-messaging-api:3.4.0",
        sha1 = "e0377b92343e8aa8a293361537d2d89d88e2b5da",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "opensaml-messaging-impl",
        artifact = "org.opensaml:opensaml-messaging-impl:3.4.0",
        sha1 = "4ef49243790872163c13aee1607ccb2681b39c53",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "pac4j-saml",
        artifact = "org.pac4j:pac4j-saml:3.4.0",
        sha1 = "5d031bee53298e483ade21dd186f5f1c89c770e3",
    )

    maven_jar(
        name = "pac4j-core",
        artifact = "org.pac4j:pac4j-core:3.4.0",
        sha1 = "22066c85a65803ec249908e38fb3c9f7c42a568a",
    )

    maven_jar(
        name = "shibbolet-utilities",
        artifact = "net.shibboleth.utilities:java-support:7.4.0",
        sha1 = "e10c137cdb5045eea2c0ccf8ac5094052eaee36b",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "shibbolet-xmlsectool",
        artifact = "net.shibboleth.tool:xmlsectool:2.0.0",
        sha1 = "c57f887f522c0e930341c7d86eff4d8ec9b797a1",
        repository = SHIBBOLETH,
    )

    maven_jar(
        name = "santuario-xmlsec",
        artifact = "org.apache.santuario:xmlsec:2.0.10",
        sha1 = "57865d2fbaf65f27c6cb8e909e37842e5cb87960",
    )

    maven_jar(
        name = "spring-core",
        artifact = "org.springframework:spring-core:5.0.2.RELEASE",
        sha1 = "45b2958ab3fb022dd29f8b1c553ebf1c75a144aa",
    )

    maven_jar(
        name = "stax2-api",
        artifact = "org.codehaus.woodstox:stax2-api:3.1.4",
        sha1 = "ac19014b1e6a7c08aad07fe114af792676b685b7",
    )

    maven_jar(
        name = "velocity",
        artifact = "org.apache.velocity:velocity:1.7",
        sha1 = "2ceb567b8f3f21118ecdec129fe1271dbc09aa7a",
    )

    maven_jar(
        name = "woodstox-core",
        artifact = "com.fasterxml.woodstox:woodstox-core:5.0.3",
        sha1 = "10aa199207fda142eff01cd61c69244877d71770",
    )
