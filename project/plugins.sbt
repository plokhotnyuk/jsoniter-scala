resolvers += Resolver.sonatypeRepo("staging")
resolvers += Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")
resolvers += Resolver.bintrayIvyRepo("sbt", "sbt-plugin-releases")

val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.5.1")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.7")

libraryDependencies ++= Seq(
  "org.openjdk.jmh" % "jmh-core" % "1.29",
  "org.openjdk.jmh" % "jmh-generator-asm" % "1.29",
  "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.29",
  "org.openjdk.jmh" % "jmh-generator-reflection" % "1.29"
)