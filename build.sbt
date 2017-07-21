name := "gerrit-saml-plugin"

val GerritVersion = "2.14"

version := GerritVersion + "-2"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

libraryDependencies += ("com.google.gerrit" % "gerrit-plugin-api" % GerritVersion % "provided")

libraryDependencies += "org.pac4j" % "pac4j-saml" % "2.0.0"

libraryDependencies ~= { _ map {
  case m => m
    .exclude("ch.qos.logback", "logback-classic")
    .exclude("ch.qos.logback", "logback-core")
    .exclude("com.google.guava", "guava")
    .exclude("commons-codec", "commons-codec")
    .exclude("commons-collections", "commons-collections")
    .exclude("commons-httpclient", "commons-httpclient")
    .exclude("commons-lang", "commons-lang")
    .exclude("commons-logging", "commons-logging")
    .exclude("commons-ssl", "commons-ssl")
    .exclude("javax.servlet", "servlet-api")
    .exclude("javax.xml", "*")
    .exclude("junit", "*")
    .exclude("org.apache.httpcomponents", "*")
    .exclude("org.apache.velocity", "*")
    .exclude("org.bouncycastle", "*")
    .exclude("org.slf4j", "*")
    .exclude("xalan", "xalan")
}}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "INDEX.LIST") => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", "NOTICE") => MergeStrategy.discard
  case PathList("META-INF", "NOTICE.txt") => MergeStrategy.discard
  case PathList("META-INF", "LICENSE") => MergeStrategy.concat
  case PathList("META-INF", "LICENSE.txt") => MergeStrategy.concat
  // Trick is here: get all the initializers concatenated...
  case PathList("META-INF", "services", "org.opensaml.core.config.Initializer") => MergeStrategy.concat
  case PathList("schema", v) if v.endsWith(".xsd") => MergeStrategy.first
  case PathList("credential-criteria-registry.properties") => MergeStrategy.first
  case PathList(xml) if xml.endsWith(".xml") => MergeStrategy.first
  case _ => MergeStrategy.deduplicate
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyOutputPath in assembly := target.value / "out" / (s"${name.value}-${version.value}.jar")

