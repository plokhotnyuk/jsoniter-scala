addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.17.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.5")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.7")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.2.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.14")
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0")

libraryDependencies ++= Seq(
  "org.openjdk.jmh" % "jmh-core" % "1.37",
  "org.openjdk.jmh" % "jmh-generator-asm" % "1.37",
  "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.37",
  "org.openjdk.jmh" % "jmh-generator-reflection" % "1.37"
)
