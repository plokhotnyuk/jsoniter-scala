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
  scalaVersion := "2.13.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked"
  ) ++ {
    val Some((major, minor)) = CrossVersion.partialVersion(scalaVersion.value)
    if (major == 2) {
      (minor match {
        case 12 => Seq(
          "-language:higherKinds"
        )
        case 13 => Seq()
      }) ++ Seq(
        "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
      )
    } else Seq(
      "-Xcheck-macros",
      //"-Ycheck:all",
      //"-Yprint-syms",
      //"-Ydebug-error", // many stack traces, really many stack traces.
      //"--explain"
    )
  },
  compileOrder := CompileOrder.JavaThenScala,
  Compile / unmanagedSourceDirectories ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toSeq.map(f => file(f.getPath + "-2"))
    case _ => Seq()
  }),
  Test / unmanagedSourceDirectories ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toSeq.map(f => file(f.getPath + "-2"))
    case _ => Seq()
  }),
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
  scalaJSLinkerConfig ~= {
    _.withSemantics({
      _.optimized
        .withProductionMode(true)
        .withAsInstanceOfs(CheckedBehavior.Unchecked)
        .withArrayIndexOutOfBounds(CheckedBehavior.Unchecked)
    }).withClosureCompiler(true)
      .withESFeatures(_.withESVersion(ESVersion.ES2015))
      .withModuleKind(ModuleKind.CommonJSModule)
  },
  coverageEnabled := false // FIXME: Too slow coverage test running
)

lazy val nativeSettings = Seq(
  coverageEnabled := false
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
    `jsoniter-scala-coreNative`,
    `jsoniter-scala-circeJVM`,
    `jsoniter-scala-circeJS`,
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
    crossScalaVersions := Seq("3.1.3", "2.13.8", "2.12.16"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.8.1" % Test,
      "org.scalatestplus" %%% "scalacheck-1-16" % "3.2.13.0" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.13" % Test
    )
  )

lazy val `jsoniter-scala-coreJVM` = `jsoniter-scala-core`.jvm

lazy val `jsoniter-scala-coreJS` = `jsoniter-scala-core`.js
  .settings(jsSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.4.0",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.4.0"
    )
  )

lazy val `jsoniter-scala-coreNative` = `jsoniter-scala-core`.native
  .settings(nativeSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.4.0",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.4.0"
    )
  )

lazy val `jsoniter-scala-macros` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.1.3", "2.13.8", "2.12.16"),
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
      case _ => Seq()
    }) ++ Seq(
      "org.scalatest" %%% "scalatest" % "3.2.13" % Test,
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.8.1" % Test
    )
  )

lazy val `jsoniter-scala-macrosJVM` = `jsoniter-scala-macros`.jvm
  .settings(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "com.beachape" %%% "enumeratum" % "1.6.1" % Test
      )
      case _ => Seq()
    })
  )

lazy val `jsoniter-scala-macrosJS` = `jsoniter-scala-macros`.js
  .settings(jsSettings)
  .settings(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "com.beachape" %%% "enumeratum" % "1.6.1" % Test
      )
      case _ => Seq()
    })
  )

lazy val `jsoniter-scala-macrosNative` = `jsoniter-scala-macros`.native
  .settings(nativeSettings)

lazy val `jsoniter-scala-circe` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.1.3", "2.13.8", "2.12.16"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.2",
      "io.circe" %%% "circe-parser" % "0.14.2" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.13" % Test
    )
  )

lazy val `jsoniter-scala-circeJVM` = `jsoniter-scala-circe`.jvm

lazy val `jsoniter-scala-circeJS` = `jsoniter-scala-circe`.js
  .settings(jsSettings)

lazy val `jsoniter-scala-benchmark` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`jsoniter-scala-circe`)
  .dependsOn(`jsoniter-scala-macros`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    crossScalaVersions := Seq("2.13.8"),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-json" % "0.15.1",
      "dev.zio" %%% "zio-json" % "0.3.0-RC11",
      "com.rallyhealth" %% "weepickle-v1" % "1.7.2",
      "io.bullet" %%% "borer-derivation" % "1.8.0",
      "pl.iterators" %% "kebs-spray-json" % "1.9.4",
      "io.spray" %% "spray-json" % "1.3.6",
      "com.avsystem.commons" %%% "commons-core" % "2.7.4",
      "com.lihaoyi" %%% "upickle" % "2.0.0",
      "com.dslplatform" %% "dsl-json-scala" % "1.9.9",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.14.0-SNAPSHOT" changing(),
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.0-SNAPSHOT" changing(),
      "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.14.0-SNAPSHOT" changing(),
      "io.circe" %%% "circe-generic-extras" % "0.14.2",
      "io.circe" %%% "circe-generic" % "0.15.0-M1",
      "io.circe" %%% "circe-parser" % "0.15.0-M1",
      "io.circe" %%% "circe-jawn" % "0.15.0-M1",
      "com.typesafe.play" %%% "play-json" % "2.10.0-RC6",
      "com.evolutiongaming" %%% "play-json-jsoniter" % "0.10.0",
      "org.julienrf" %%% "play-json-derived-codecs" % "10.1.0",
      "com.github.plokhotnyuk.play-json-extensions" %%% "play-json-extensions" % "0.43.1",
      "tk.nrktkt" %% "ninny" % "0.7.0",
      "org.openjdk.jmh" % "jmh-core" % "1.35",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.35",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.35",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.35",
      "org.scalatest" %%% "scalatest" % "3.2.13" % Test
    )
  )

lazy val `jsoniter-scala-benchmarkJVM` = `jsoniter-scala-benchmark`.jvm
  .enablePlugins(JmhPlugin)

lazy val `jsoniter-scala-benchmarkJS` = `jsoniter-scala-benchmark`.js
  .enablePlugins(JSDependenciesPlugin)
  .settings(jsSettings)
  .settings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.NoModule) },
    libraryDependencies += "com.github.japgolly.scalajs-benchmark" %%% "benchmark" % "0.10.0",
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.benchmark.Main"),
    Test / test := {} // FIXME: Add and enable `jsoniter-scala-benchmarkJS` tests
  )