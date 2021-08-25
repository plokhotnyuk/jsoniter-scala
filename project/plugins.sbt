resolvers += Resolver.sonatypeRepo("staging")

val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.7.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.10")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.0")
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")

libraryDependencies ++= Seq(
  "org.openjdk.jmh" % "jmh-core" % "1.32",
  "org.openjdk.jmh" % "jmh-generator-asm" % "1.32",
  "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.32",
  "org.openjdk.jmh" % "jmh-generator-reflection" % "1.32"
)