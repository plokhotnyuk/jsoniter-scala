addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.1")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.1")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.14")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.5")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.8")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.2")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.8")
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")

libraryDependencies ++= Seq(
  "org.openjdk.jmh" % "jmh-core" % "1.36",
  "org.openjdk.jmh" % "jmh-generator-asm" % "1.36",
  "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.36",
  "org.openjdk.jmh" % "jmh-generator-reflection" % "1.36"
)