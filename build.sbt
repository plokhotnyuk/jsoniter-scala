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
  scalaVersion := "2.13.18",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq(
      "-language:higherKinds",
      "-opt:l:method")
    case Some((2, 13)) => Seq(
      "-opt:l:method"
    )
    case _ => Seq(
      "-Xcheck-macros",
      "-explain",
      "-Wconf:msg=Ignoring [this] qualifier.:s",
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s",
    )
  }),
  compileOrder := CompileOrder.JavaThenScala,
  Test / testOptions += Tests.Argument("-oDF"),
  sonatypeProfileName := "com.github.plokhotnyuk",
  versionScheme := Some("early-semver"),
  Compile / doc / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-jdk-api-doc-base", "https://docs.oracle.com/en/java/javase/11/docs/api")
    case _ => Seq()
  }),
  publishTo := localStaging.value,
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
      .withESFeatures(_.withESVersion(ESVersion.ES2021))
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
  },
  coverageEnabled := false // FIXME: Unexpected linking error
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  mimaPreviousArtifacts := Set(),
  Compile / doc / sources := Seq()
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
    ProblemFilters.exclude[Problem]("com.github.plokhotnyuk.jsoniter_scala.core.NativeMath*")
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
    `jsoniter-scala-next-testsJVM`,
    `jsoniter-scala-next-testsJS`,
    `jsoniter-scala-next-testsNative`,
    `jsoniter-scala-benchmarkJVM`,
    `jsoniter-scala-benchmarkJS`
  )

lazy val `jsoniter-scala-core` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.3.7", "2.13.18", "2.12.21"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq()
      case _ => Seq(
        "-source:3.3"
      )
    }),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.14.0" % Test,
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
    crossScalaVersions := Seq("3.3.7", "2.13.18", "2.12.21"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq()
      case _ => Seq(
        "-source:3.3",
        "-Ydebug"
      )
    }),
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "com.beachape" %%% "enumeratum" % "1.9.2" % Test
      )
      case _ => Seq()
    }) ++ Seq(
      "com.epam.deltix" % "dfp" % "1.0.10" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.14.0" % Test
    )
  )

lazy val `jsoniter-scala-macrosJVM` = `jsoniter-scala-macros`.jvm

lazy val `jsoniter-scala-macrosJS` = `jsoniter-scala-macros`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-macrosNative` = `jsoniter-scala-macros`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-next-tests` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-macros` % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Seq("3.7.4"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq()
      case _ => Seq(
        "-Yexplicit-nulls"
      )
    }),
    libraryDependencies ++= Seq(
      "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    )
  )

lazy val `jsoniter-scala-next-testsJVM` = `jsoniter-scala-next-tests`.jvm

lazy val `jsoniter-scala-next-testsJS` = `jsoniter-scala-next-tests`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-next-testsNative` = `jsoniter-scala-next-tests`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-circe` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.3.7", "2.13.18", "2.12.21"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq()
      case _ => Seq(
        "-source:3.3"
      )
    }),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.15",
      "io.circe" %%% "circe-parser" % "0.14.15" % Test,
      "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % Test,
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
    crossScalaVersions := Seq("3.7.4", "2.13.18"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "-Wopt",
        "-opt:l:inline",
        "-opt-inline-from:**:!java.**"
      )
      case _ => Seq(
        "-source:3.3",
        "-Xmax-inlines:100",
        "-preview"
      )
    }),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-json" % "0.18.45",
      "com.evolutiongaming" %%% "play-json-jsoniter" % "0.10.3" intransitive(),
      "org.playframework" %%% "play-json" % "3.0.6",
      "dev.zio" %%% "zio-blocks-schema" % "0.0.3",
      "dev.zio" %%% "zio-json" % "0.7.45",
      "dev.zio" %%% "zio-schema-json" % "1.7.5",
      "io.circe" %%% "circe-generic" % "0.14.15",
      "io.circe" %%% "circe-jawn" % "0.14.15",
      "com.lihaoyi" %%% "upickle" % "4.4.2",
      "com.rallyhealth" %% "weepickle-v1" % "1.9.1",
      "io.spray" %% "spray-json" % "1.3.6",
      "io.github.json4s" %% "json4s-ext" % "4.1.0",
      "io.github.json4s" %% "json4s-jackson" % "4.1.0",
      "io.github.json4s" %% "json4s-native" % "4.1.0",
      "tools.jackson.module" %% "jackson-module-scala" % "3.0.3",
      "org.openjdk.jmh" % "jmh-core" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.37",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.37",
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "io.bullet" %%% "borer-derivation" % "1.8.0",
        "com.avsystem.commons" %%% "commons-core" % "2.25.0",
        "com.dslplatform" %% "dsl-json-scala" % "2.0.2"
      )
      case _ => Seq(
        "org.scala-lang" %% "scala3-staging" % scalaVersion.value, // required by `OpenRTBReading.json4sNative` and `OpenRTBReading.json4sNative` benchmarks for Scala 3
        "io.bullet" %%% "borer-derivation" % "1.16.2"
      )
    })
  )

lazy val `jsoniter-scala-benchmarkJVM` = `jsoniter-scala-benchmark`.jvm
  .enablePlugins(JmhPlugin)
  .settings(
    assembly / assemblyJarName := "benchmarks.jar",
    assembly / mainClass := Some("org.openjdk.jmh.Main"),
    assembly / fullClasspath := (Jmh / fullClasspath).value,
    ThisBuild / assemblyMergeStrategy := {
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case x if x.contains("unroll") => MergeStrategy.last
      case PathList("deriving.conf") => MergeStrategy.concat
      case path => MergeStrategy.defaultMergeStrategy(path)
    }
  )

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
