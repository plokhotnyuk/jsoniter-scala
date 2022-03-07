resolvers += Resolver.sonatypeRepo("staging")

val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.9.0")
val scalaNativeVersion =
  Option(System.getenv("SCALANATIVE_VERSION")).getOrElse("0.4.4")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.1.0")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.scala-native" % "sbt-scala-native" % scalaNativeVersion)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.12")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.0.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.2")
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")

libraryDependencies ++= Seq(
  "org.openjdk.jmh" % "jmh-core" % "1.34",
  "org.openjdk.jmh" % "jmh-generator-asm" % "1.34",
  "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.34",
  "org.openjdk.jmh" % "jmh-generator-reflection" % "1.34"
)