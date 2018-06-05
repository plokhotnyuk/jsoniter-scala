import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import sbt.Keys.scalacOptions
import sbt.url
import scala.sys.process._

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

def mimaSettings = mimaDefaultSettings ++ Seq(
  mimaCheckDirection := {
    def isPatch = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && newMinor == oldMinor
    }

    if (isPatch) "both" else "backward"
  },
  mimaPreviousArtifacts := {
    def isCheckingRequired = {
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
  scalaVersion := "2.12.6",
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
    case Some((2, x)) if x >= 12 => Seq("-opt:l:method")
    case Some((2, x)) if x == 11 => Seq("-Ybackend:GenBCode", "-Ydelambdafy:inline")
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
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false }
)

lazy val `jsoniter-scala` = project.in(file("."))
  .aggregate(core, macros, benchmark)
  .settings(noPublishSettings: _*)

lazy val core = project
  .settings(commonSettings: _*)
  .settings(mimaSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.13.0-M4", "2.13.0-M3", "2.12.6", "2.11.12"),
    libraryDependencies ++= {
      val scalaV = scalaVersion.value
      CrossVersion.partialVersion(scalaV) match {
        case Some((2, v)) if v >= 13 && scalaV != "2.13.0-M3" =>
          // TODO: scalatest for Scala 2.13.0-M4
          Nil
        case _ =>
          Seq(
            "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
            "org.scalatest" %% "scalatest" % "3.0.5-M1" % Test
          )
      }
    }
  )

lazy val macros = project
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(mimaSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.13.0-M4", "2.13.0-M3", "2.12.6", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    libraryDependencies ++= {
      val scalaV = scalaVersion.value
      CrossVersion.partialVersion(scalaV) match {
        case Some((2, v)) if v >= 13 && scalaV != "2.13.0-M3" =>
          // TODO: scalatest for Scala 2.13.0-M4
          Nil
        case _ =>
          Seq(
            "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
            "org.scalatest" %% "scalatest" % "3.0.5-M1" % Test
          )
      }
    }
  )

lazy val benchmark = project
  .enablePlugins(JmhPlugin)
  .dependsOn(macros)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.12.6", "2.11.12"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "0.6.6",
      "com.dslplatform" %% "dsl-json-scala" % "1.7.4",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.5",
      "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.9.5",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.9.5",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.9.5",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.9.5",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.5",
      "io.circe" %% "circe-generic" % "0.10.0-M1",
      "io.circe" %% "circe-generic-extras" % "0.10.0-M1",
      "io.circe" %% "circe-parser" % "0.10.0-M1",
      "io.circe" %% "circe-java8" % "0.10.0-M1",
      "ai.x" %% "play-json-extensions" % "0.10.0",
      "com.typesafe.play" %% "play-json" % "2.6.9",
      "org.julienrf" %% "play-json-derived-codecs" % "4.0.0",
      "pl.project13.scala" % "sbt-jmh-extras" % "0.3.4",
      "org.scalatest" %% "scalatest" % "3.0.5-M1" % Test
    )
  )
