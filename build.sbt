import com.typesafe.tools.mima.core._
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
  scalaVersion := "2.13.4",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked",
    "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) => Seq(
      "-language:higherKinds",
      "-Ybackend:GenBCode",
      "-Ydelambdafy:inline"
    )
    case Some((2, 12)) => Seq(
      "-language:higherKinds"
    )
    case _ => Seq()
  }),
  compileOrder := CompileOrder.JavaThenScala,
  testOptions in Test += Tests.Argument("-oDF"),
  sonatypeProfileName := "com.github.plokhotnyuk",
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
  skip in publish := true,
  mimaPreviousArtifacts := Set()
)

lazy val publishSettings = Seq(
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
      val Array(newMajor, _, _) = version.value.split('.')
      val Array(oldMajor, _, _) = oldVersion.split('.')
      newMajor == oldMajor
    }

    if (isCheckingRequired) Set(organization.value %% moduleName.value % oldVersion)
    else Set()
  },
  mimaBinaryIssueFilters := Seq( // internal API to ignore
    ProblemFilters.exclude[DirectMissingMethodProblem]("com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.this")
  ),
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
    crossScalaVersions := Seq("2.13.4", "2.12.13", "2.11.12"),
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.expression-evaluator" %% "expression-evaluator" % "0.1.2" % "compile-internal",
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.4.0" % Test,
      "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.3.0" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.3" % Test
    )
  )

lazy val `jsoniter-scala-coreJVM` = `jsoniter-scala-core`.jvm

lazy val `jsoniter-scala-coreJS` = `jsoniter-scala-core`.js
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.1.0",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.1.0"
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
    crossScalaVersions := Seq("2.13.4", "2.12.13", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalatest" %%% "scalatest" % "3.2.3" % Test
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
    resolvers ++= Seq(
      "Rally Health" at "https://dl.bintray.com/rallyhealth/maven",
      "Playframework" at "https://dl.bintray.com/playframework/maven"
    ),
    crossScalaVersions := Seq("2.13.4", "2.12.13"),
    libraryDependencies ++= Seq(
      "com.rallyhealth" %% "weepickle-v1" % "1.4.0",
      "io.bullet" %%% "borer-derivation" % "1.6.3",
      "pl.iterators" %% "kebs-spray-json" % "1.8.0",
      "io.spray" %% "spray-json" % "1.3.6",
      "com.avsystem.commons" %%% "commons-core" % "2.0.0-M16",
      "com.lihaoyi" %%% "upickle" % "1.2.2",
      "com.dslplatform" %% "dsl-json-scala" % "1.9.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.1",
      "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.12.1",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.12.1",
      "io.circe" %%% "circe-generic-extras" % "0.13.0",
      "io.circe" %%% "circe-generic" % "0.13.0",
      "io.circe" %%% "circe-parser" % "0.13.0",
      "com.typesafe.play" %% "play-json" % "2.9.2",
      "org.julienrf" %% "play-json-derived-codecs" % "8.0.0",
      "ai.x" %% "play-json-extensions" % "0.42.0",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.0",
      "org.openjdk.jmh" % "jmh-core" % "1.27",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.27",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.27",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.27",
      "org.scalatest" %%% "scalatest" % "3.2.3" % Test
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
    mainClass in Compile := Some("com.github.plokhotnyuk.jsoniter_scala.benchmark.Main"),
    coverageEnabled := false // FIXME: No support for Scala.js 1.0 yet, see https://github.com/scoverage/scalac-scoverage-plugin/pull/287
  )
