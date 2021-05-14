import org.scalajs.linker.interface.Semantics
import sbt._

import scala.sys.process._

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

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
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("staging"),
    "scala-integration" at "https://scala-ci.typesafe.com/artifactory/scala-integration/"
  ),
  scalaVersion := "2.13.5",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked"
  ) ++ {
    val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
    if (major == 2) {
      (minor match {
        case 11 => Seq(
          "-Ybackend:GenBCode",
          "-Ydelambdafy:inline",
          "-language:higherKinds",
        )
        case 12 => Seq(
          "-language:higherKinds"
        )
        case 13 => Seq()
      }) ++ Seq(
        "-target:jvm-1.8",
        "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
      )
    } else Seq()
  },
  compileOrder := CompileOrder.JavaThenScala,
  Test / testOptions += Tests.Argument("-oDF"),
  sonatypeProfileName := "com.github.plokhotnyuk",
  versionScheme := Some("early-semver"),
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/plokhotnyuk/jsoniter-scala"),
      "scm:git@github.com:plokhotnyuk/jsoniter-scala.git"
    )
  )
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  mimaPreviousArtifacts := Set()
)

lazy val publishSettings = Seq(
    packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> moduleName.value),
    mimaCheckDirection := {
    def isPatch: Boolean = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && newMinor == oldMinor
    }

    if (isPatch) "both" else "backward"
  },
  mimaPreviousArtifacts := {
    val Some((scalaMajor, _)) = CrossVersion.partialVersion(scalaVersion.value)

    def isCheckingRequired: Boolean = {
      val Array(newMajor, _, _) = version.value.split('.')
      val Array(oldMajor, _, _) = oldVersion.split('.')
      newMajor == oldMajor && scalaMajor == 2
    }

    if (isCheckingRequired) Set(organization.value %% moduleName.value % oldVersion)
    else Set()
  },
  mimaReportSignatureProblems := true
)

lazy val `jsoniter-scala` = project.in(file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .aggregate(
    `jsoniter-scala-coreJVM`,
    `jsoniter-scala-coreJS`,
    `jsoniter-scala-macrosJVM`,
    `jsoniter-scala-macrosJS`,
    `jsoniter-scala-benchmarkJVM` // FIXME: Restore `jsoniter-scala-benchmarkJS` here
  )

lazy val `jsoniter-scala-core` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.0.0-RC3", "2.13.5", "2.12.13", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.4.4" % Test
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq(
        "org.scalatest" %%% "scalatest" % "3.2.4-M1" % Test,
        "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.4.0-M1" % Test
      )
      case _=> Seq(
        "org.scalatest" %%% "scalatest" % "3.2.8" % Test,
        "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.8.0" % Test
      )
    })
  )

lazy val `jsoniter-scala-coreJVM` = `jsoniter-scala-core`.jvm

lazy val `jsoniter-scala-coreJS` = `jsoniter-scala-core`.js
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.2.2",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.2.2"
    ),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule).withESFeatures(_.withUseECMAScript2015(false))),
    coverageEnabled := false // FIXME: No support for Scala.js 1.0 yet, see https://github.com/scoverage/scalac-scoverage-plugin/pull/287
  )

lazy val `jsoniter-scala-macros` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("2.13.5", "2.12.13", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalatest" %%% "scalatest" % "3.2.8" % Test
    )
  )

lazy val `jsoniter-scala-macrosJVM` = `jsoniter-scala-macros`.jvm

lazy val `jsoniter-scala-macrosJS` = `jsoniter-scala-macros`.js
  .settings(
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule).withESFeatures(_.withUseECMAScript2015(false))),
    coverageEnabled := false // FIXME: No support for Scala.js 1.0 yet, see https://github.com/scoverage/scalac-scoverage-plugin/pull/287
  )

lazy val `jsoniter-scala-benchmark` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-macros`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    crossScalaVersions := Seq("2.13.5"),
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % "0.1.4",
      "com.evolutiongaming" %% "play-json-jsoniter" % "0.9.1",
      "com.rallyhealth" %% "weepickle-v1" % "1.4.1",
      "io.bullet" %%% "borer-derivation" % "1.7.2",
      "pl.iterators" %% "kebs-spray-json" % "1.9.1",
      "io.spray" %% "spray-json" % "1.3.6",
      "com.avsystem.commons" %%% "commons-core" % "2.2.1",
      "com.lihaoyi" %%% "upickle" % "1.3.13",
      "com.dslplatform" %% "dsl-json-scala" % "1.9.8",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.12.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.3",
      "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.12.3",
      "io.circe" %%% "circe-generic-extras" % "0.13.0",
      "io.circe" %%% "circe-generic" % "0.13.0",
      "io.circe" %%% "circe-parser" % "0.13.0",
      "com.typesafe.play" %% "play-json" % "2.9.2",
      "org.julienrf" %% "play-json-derived-codecs" % "10.0.2",
      "ai.x" %% "play-json-extensions" % "0.42.0",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4",
      "org.openjdk.jmh" % "jmh-core" % "1.31",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.31",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.31",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.31",
      "org.scalatest" %%% "scalatest" % "3.2.8" % Test
    )
  )

lazy val `jsoniter-scala-benchmarkJVM` = `jsoniter-scala-benchmark`.jvm
  .enablePlugins(JmhPlugin)

lazy val `jsoniter-scala-benchmarkJS` = `jsoniter-scala-benchmark`.js
  .enablePlugins(JSDependenciesPlugin)
  .settings(
    libraryDependencies += "com.github.japgolly.scalajs-benchmark" %%% "benchmark" % "0.9.0",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withSemantics(Semantics.Defaults.withProductionMode(true)).withClosureCompiler(true).withESFeatures(_.withUseECMAScript2015(false))),
    Compile / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.benchmark.Main"),
    coverageEnabled := false // FIXME: No support for Scala.js 1.0 yet, see https://github.com/scoverage/scalac-scoverage-plugin/pull/287
  )
