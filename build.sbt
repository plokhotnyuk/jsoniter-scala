import org.scalajs.linker.interface.{CheckedBehavior, ESVersion}
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
  scalaVersion := "3.2.1",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq("-language:higherKinds")
    case Some((3, _)) => Seq("-Xcheck-macros")
    case _ => Seq()
  }),
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

lazy val jsSettings = Seq(
  libraryDependencies ++= Seq(
    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.5.0" % Test
  ),
  scalaJSLinkerConfig ~= {
    _.withSemantics({
      _.optimized
        .withProductionMode(true)
        .withAsInstanceOfs(CheckedBehavior.Unchecked)
        .withStringIndexOutOfBounds(CheckedBehavior.Unchecked)
        .withArrayIndexOutOfBounds(CheckedBehavior.Unchecked)
        .withArrayStores(CheckedBehavior.Unchecked)
        .withNegativeArraySizes(CheckedBehavior.Unchecked)
    }).withClosureCompiler(true)
      .withESFeatures(_.withESVersion(ESVersion.ES2015))
      .withModuleKind(ModuleKind.CommonJSModule)
  },
  coverageEnabled := false // FIXME: Unexpected crash of scalac
)

lazy val nativeSettings = Seq(
  libraryDependencies ++= Seq(
    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.5.0" % Test
  ),
  //nativeMode := "release-full", // Uncomment and test with these options
  //nativeLTO := "thin",
  coverageEnabled := false // FIXME: Unexpected linking error
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

    if (isPatch) "both"
    else "backward"
  },
  mimaPreviousArtifacts := {
    def isCheckingRequired: Boolean = {
      val Array(newMajor, _, _) = version.value.split('.')
      val Array(oldMajor, _, _) = oldVersion.split('.')
      newMajor == oldMajor
    }

    if (isCheckingRequired) Set(organization.value %%% moduleName.value % oldVersion)
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
    `jsoniter-scala-coreNative`,
    `jsoniter-scala-circeJVM`,
    `jsoniter-scala-circeJS`,
    `jsoniter-scala-circeNative`,
    `jsoniter-scala-macrosJVM`,
    `jsoniter-scala-macrosJS`,
    `jsoniter-scala-macrosNative`,
    `jsoniter-scala-benchmarkJVM`,
    `jsoniter-scala-benchmarkJS`
  )

lazy val `jsoniter-scala-core` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.2.1", "2.13.10", "2.12.17"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.9.0" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.15" % Test,
      "org.scalatestplus" %%% "scalacheck-1-17" % "3.2.15.0" % Test
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )

lazy val `jsoniter-scala-coreJVM` = `jsoniter-scala-core`.jvm

lazy val `jsoniter-scala-coreJS` = `jsoniter-scala-core`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-coreNative` = `jsoniter-scala-core`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-macros` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.2.1", "2.13.10", "2.12.17"),
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
      case _ => Seq()
    }) ++ Seq(
      "org.scalatest" %%% "scalatest" % "3.2.15" % Test,
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.9.0" % Test
    )
  )

lazy val `jsoniter-scala-macrosJVM` = `jsoniter-scala-macros`.jvm
  .settings(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "com.beachape" %%% "enumeratum" % "1.7.2" % Test
      )
      case _ => Seq()
    })
  )

lazy val `jsoniter-scala-macrosJS` = `jsoniter-scala-macros`.js
  .settings(jsSettings)
  .settings(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "com.beachape" %%% "enumeratum" % "1.7.2" % Test
      )
      case _ => Seq()
    })
  )

lazy val `jsoniter-scala-macrosNative` = `jsoniter-scala-macros`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-circe` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.2.1", "2.13.10", "2.12.17"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.3",
      "io.circe" %%% "circe-parser" % "0.14.3" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.15" % Test
    )
  )

lazy val `jsoniter-scala-circeJVM` = `jsoniter-scala-circe`.jvm

lazy val `jsoniter-scala-circeJS` = `jsoniter-scala-circe`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-circeNative` = `jsoniter-scala-circe`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-benchmark` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-circe`)
  .dependsOn(`jsoniter-scala-macros`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Seq("3.2.1", "2.13.10"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq("-Yretain-trees", "-Xmax-inlines:100")
      case _ => Seq()
    }),
    libraryDependencies ++= Seq(
      "io.spray" %% "spray-json" % "1.3.6",
      "com.typesafe.play" %%% "play-json" % "2.10.0-RC7",
      "dev.zio" %%% "zio-json" % "0.4.2",
      "com.evolutiongaming" %%% "play-json-jsoniter" % "0.10.3",
      "com.lihaoyi" %%% "upickle" % "2.0.0",
      "io.circe" %%% "circe-generic" % "0.14.3",
      "io.circe" %%% "circe-parser" % "0.14.3",
      "io.circe" %%% "circe-jawn" % "0.14.3",
      "com.disneystreaming.smithy4s" %%% "smithy4s-json" % "0.17.2",
      "org.json4s" %% "json4s-jackson" % "4.1.0-M2",
      "org.json4s" %% "json4s-native" % "4.1.0-M2",
      "com.rallyhealth" %% "weepickle-v1" % "1.8.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.14.1",
      "com.fasterxml.jackson.module" % "jackson-module-blackbird" % "2.14.1",
      "org.openjdk.jmh" % "jmh-core" % "1.36",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.36",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.36",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.36",
      "org.scalatest" %%% "scalatest" % "3.2.15" % Test
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "io.bullet" %%% "borer-derivation" % "1.8.0",
        "com.avsystem.commons" %%% "commons-core" % "2.8.0",
        "com.dslplatform" %% "dsl-json-scala" % "1.10.0"
      )
      case Some((3, _)) => Seq(
        "io.bullet" %%% "borer-derivation" % "1.10.1"
      )
      case _ => Seq()
    })
  )

lazy val `jsoniter-scala-benchmarkJVM` = `jsoniter-scala-benchmark`.jvm
  .enablePlugins(JmhPlugin)

lazy val assemblyJSBenchmarks = sys.props.get("assemblyJSBenchmarks").isDefined

lazy val `jsoniter-scala-benchmarkJS` = `jsoniter-scala-benchmark`.js
  .enablePlugins({
    if (assemblyJSBenchmarks) Seq(JSDependenciesPlugin)
    else Seq(JSDependenciesPlugin, ScalaJSBundlerPlugin)
  }:_*)
  .settings(jsSettings)
  .settings(
    libraryDependencies += "com.github.japgolly.scalajs-benchmark" %%% "benchmark" % "0.10.0",
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.benchmark.Main")
  )
  .settings({
    if (assemblyJSBenchmarks) Seq(scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.NoModule) }, Test / test := {})
    else Seq(Test / requireJsDomEnv := true)
  }:_*)