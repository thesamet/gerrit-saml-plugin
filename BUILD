load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "gerrit-saml-plugin",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: gerrit-saml-plugin",
    ],
    resources = glob(["src/main/**/*"]),
    deps = [
        "@cryptacular//jar",
        "@opensaml-core//jar",
        "@opensaml-messaging-api//jar",
        "@opensaml-messaging-impl//jar",
        "@opensaml-profile-api//jar",
        "@opensaml-profile-impl//jar",
        "@opensaml-saml-api//jar",
        "@opensaml-saml-impl//jar",
        "@opensaml-security-api//jar",
        "@opensaml-security-impl//jar",
        "@opensaml-soap-impl//jar",
        "@opensaml-storage-api//jar",
        "@opensaml-xmlsec-api//jar",
        "@opensaml-xmlsec-impl//jar",
        "@pac4j-core//jar",
        "@pac4j-saml//jar",
        "@santuario-xmlsec//jar",
        "@shibbolet-utilities//jar",
        "@shibbolet-xmlsectool//jar",
        "@spring-core//jar",
        "@stax2-api//jar",
        "@woodstox-core//jar",
    ],
)
