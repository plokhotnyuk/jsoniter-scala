import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import sbt._
import scala.sys.process._

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

def mimaSettings: Seq[Setting[_]] = mimaDefaultSettings ++ Seq(
  mimaCheckDirection := {
    def isPatch: Boolean = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && newMinor == oldMinor
    }

    if (isPatch) "both" else "backward"
  },
  mimaPreviousArtifacts := {
    def isCheckingRequired: Boolean = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && (newMajor != "0" || newMinor == oldMinor)
    }

    if (isCheckingRequired) Set(organization.value %% moduleName.value % oldVersion)
    else Set()
  }
)

lazy val commonSettings = Seq(
  organization := "com.github.plokhotnyuk.jsoniter-scala",
  organizationHomepage := Some(url("https://github.com/plokhotnyuk")),
  homepage := Some(url("https://github.com/plokhotnyuk/jsoniter-scala")),
  licenses := Seq(("MIT License", url("https://opensource.org/licenses/mit-license.html"))),
  startYear := Some(2017),
  developers := List(
    Developer(
      id = "plokhotnyuk",
      name = "Andriy Plokhotnyuk",
      email = "plokhotnyuk@gmail.com",
      url = url("https://twitter.com/aplokhotnyuk")
    )
  ),
  resolvers += "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging",
  scalaVersion := "2.12.8",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Xfuture",
    "-Xlint",
    "-Xmacro-settings:print-codecs"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x >= 12 => Seq(
      "-opt:l:method"
    )
    case Some((2, x)) if x == 11 => Seq(
      "-Ybackend:GenBCode",
      "-Ydelambdafy:inline"
    )
    case _ => Seq()
  }),
  testOptions in Test += Tests.Argument("-oDF")
)

lazy val noPublishSettings = Seq(
  skip in publish := true,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)
)

lazy val publishSettings = Seq(
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  sonatypeProfileName := "com.github.plokhotnyuk",
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/plokhotnyuk/jsoniter-scala"),
      "scm:git@github.com:plokhotnyuk/jsoniter-scala.git"
    )
  ),
  publishConfiguration := {
    val javaVersion = System.getProperty("java.specification.version")
    if (javaVersion != "1.8") throw new IllegalStateException("Cancelling publish, please use JDK 1.8")
    publishConfiguration.value
  },
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false }
)

lazy val `jsoniter-scala` = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .aggregate(`jsoniter-scala-core`, `jsoniter-scala-macros`, `jsoniter-scala-benchmark`)

lazy val `jsoniter-scala-core` = project
  .settings(commonSettings: _*)
  .settings(mimaSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.13.0-M5", "2.12.8", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.7" % Test
    )
  )

lazy val `jsoniter-scala-macros` = project
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings: _*)
  .settings(mimaSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.13.0-M5", "2.12.8", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.7" % Test
    )
  )

lazy val `jsoniter-scala-benchmark` = project
  .enablePlugins(JmhPlugin)
  .dependsOn(`jsoniter-scala-macros`)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.avsystem.commons" %% "commons-core" % "1.34.16",
      "com.lihaoyi" %% "upickle" % "0.7.4",
      "com.dslplatform" %% "dsl-json-scala" % "1.9.1",
      "com.jsoniter" % "jsoniter" % "0.9.23",
      "org.javassist" % "javassist" % "3.24.1-GA", // required for Jsoniter Java
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8",
      "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.9.8",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.9.8",
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-generic-extras" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1",
      "io.circe" %% "circe-java8" % "0.11.1",
      "com.typesafe.play" %% "play-json" % "2.7.2",
      "org.julienrf" %% "play-json-derived-codecs" % "5.0.0",
      "ai.x" %% "play-json-extensions" % "0.30.1",
      "pl.project13.scala" % "sbt-jmh-extras" % "0.3.6",
      "org.scalatest" %% "scalatest" % "3.0.7" % Test
    )
  )
