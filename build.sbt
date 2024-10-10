import com.typesafe.tools.mima.core.*
import org.scalajs.linker.interface.{CheckedBehavior, ESVersion}
import sbt.*
import scala.scalanative.build.*
import scala.sys.process.*

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
  scalaVersion := "2.13.15",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq("-language:higherKinds", "-opt:l:method")
    case Some((2, 13)) => Seq("-opt:l:method")
    case _ => Seq("-Xcheck-macros", "-explain")
  }),
  compileOrder := CompileOrder.JavaThenScala,
  Test / testOptions += Tests.Argument("-oDF"),
  sonatypeProfileName := "com.github.plokhotnyuk",
  versionScheme := Some("early-semver"),
  Compile / doc / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-jdk-api-doc-base", "https://docs.oracle.com/en/java/javase/11/docs/api")
    case _ => Seq()
  }),
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
  scalacOptions ++= {
    val localSourcesPath = (LocalRootProject / baseDirectory).value.toURI
    val remoteSourcesPath = s"https://raw.githubusercontent.com/plokhotnyuk/jsoniter-scala/${git.gitHeadCommit.value.get}/"
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        s"-P:scalajs:mapSourceURI:$localSourcesPath->$remoteSourcesPath",
        "-P:scalajs:genStaticForwardersForNonTopLevelObjects"
      )
      case _ => Seq(
        s"-scalajs-mapSourceURI:$localSourcesPath->$remoteSourcesPath",
        "-scalajs-genStaticForwardersForNonTopLevelObjects"
      )
    }
  },
  libraryDependencies ++= Seq(
    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.6.0" % Test
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
        .withNullPointers(CheckedBehavior.Unchecked)
    }).withClosureCompiler(true)
      .withESFeatures(_.withESVersion(ESVersion.ES2015))
      .withModuleKind(ModuleKind.CommonJSModule)
  },
  coverageEnabled := false // FIXME: Unexpected crash of scalac
)

lazy val nativeSettings = Seq(
  scalacOptions ++= Seq("-P:scalanative:genStaticForwardersForNonTopLevelObjects"),
  libraryDependencies ++= Seq(
    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.6.0" % Test
  ),
  nativeConfig ~= {
    _.withMode(Mode.releaseFast) // TODO: Test with `Mode.releaseSize` and `Mode.releaseFull`
      .withLTO(LTO.none)
      .withGC(GC.boehm) // FIXME: Remove after fixing of https://github.com/scala-native/scala-native/issues/4032
  },
  coverageEnabled := false // FIXME: Unexpected linking error
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  mimaPreviousArtifacts := Set()
)

lazy val publishSettings = Seq(
  packageOptions += Package.ManifestAttributes("Automatic-Module-Name" ->
    moduleName.value.replace("jsoniter-scala", "com.github.plokhotnyuk.jsoniter_scala").replace('-', '.')),
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
  mimaReportSignatureProblems := true,
  mimaBinaryIssueFilters := Seq(
    ProblemFilters.exclude[MissingClassProblem]("com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodecWrapper")
  )
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
    crossScalaVersions := Seq("3.3.4", "2.13.15", "2.12.20"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.12.0" % Test,
      "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    )
  )

lazy val `jsoniter-scala-coreJVM` = `jsoniter-scala-core`.jvm

lazy val `jsoniter-scala-coreJS` = `jsoniter-scala-core`.js
  .settings(jsSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.6.0"
    )
  )

lazy val `jsoniter-scala-coreNative` = `jsoniter-scala-core`.native
  .settings(nativeSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.6.0"
    )
  )

lazy val `jsoniter-scala-macros` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.3.4", "2.13.15", "2.12.20"),
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "com.beachape" %%% "enumeratum" % "1.7.5" % Test
      )
      case _ => Seq()
    }) ++ Seq(
      "com.epam.deltix" % "dfp" % "1.0.3" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.12.0" % Test
    )
  )

lazy val `jsoniter-scala-macrosJVM` = `jsoniter-scala-macros`.jvm

lazy val `jsoniter-scala-macrosJS` = `jsoniter-scala-macros`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-macrosNative` = `jsoniter-scala-macros`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-circe` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.3.4", "2.13.15", "2.12.20"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.10",
      "io.circe" %%% "circe-parser" % "0.14.10" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    )
  )

lazy val `jsoniter-scala-circeJVM` = `jsoniter-scala-circe`.jvm

lazy val `jsoniter-scala-circeJS` = `jsoniter-scala-circe`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-circeNative` = `jsoniter-scala-circe`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-benchmark` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-macros`, `jsoniter-scala-circe`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Seq("3.5.2-RC2", "2.13.15"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq()
      case _ => Seq("-source:3.3", "-Xmax-inlines:100")
    }),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-json" % "0.18.25",
      "com.evolutiongaming" %%% "play-json-jsoniter" % "0.10.3",
      "org.playframework" %%% "play-json" % "3.0.4",
      "dev.zio" %%% "zio-json" % "0.7.3",
      "io.circe" %%% "circe-generic" % "0.14.10",
      "io.circe" %%% "circe-jawn" % "0.14.10",
      "com.lihaoyi" %%% "upickle" % "4.0.2",
      "com.rallyhealth" %% "weepickle-v1" % "1.9.1",
      "io.spray" %% "spray-json" % "1.3.6",
      "org.json4s" %% "json4s-ext" % "4.1.0-M7",
      "org.json4s" %% "json4s-jackson" % "4.1.0-M7",
      "org.json4s" %% "json4s-native" % "4.1.0-M7",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.18.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.18.0",
      "com.fasterxml.jackson.module" % "jackson-module-blackbird" % "2.18.0",
      "org.openjdk.jmh" % "jmh-core" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.37",
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "io.bullet" %%% "borer-derivation" % "1.8.0",
        "com.avsystem.commons" %%% "commons-core" % "2.20.0",
        "com.dslplatform" %% "dsl-json-scala" % "2.0.2"
      )
      case _ => Seq(
        "io.bullet" %%% "borer-derivation" % "1.14.1"
      )
    }),
    Compile / doc / sources := Seq()
  )

lazy val `jsoniter-scala-benchmarkJVM` = `jsoniter-scala-benchmark`.jvm
  .enablePlugins(JmhPlugin)

lazy val assemblyJSBenchmarks = sys.props.get("assemblyJSBenchmarks").isDefined

lazy val `jsoniter-scala-benchmarkJS` = `jsoniter-scala-benchmark`.js
  .enablePlugins({
    if (assemblyJSBenchmarks) Seq(JSDependenciesPlugin)
    else Seq(JSDependenciesPlugin, ScalaJSBundlerPlugin)
  }*)
  .settings(jsSettings)
  .settings(
    libraryDependencies += "com.github.japgolly.scalajs-benchmark" %%% "benchmark" % "0.10.0",
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.benchmark.Main")
  )
  .settings({
    if (assemblyJSBenchmarks) Seq(scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.NoModule)
    }, Test / test := {})
    else Seq(Test / requireJsDomEnv := true)
  }*)
