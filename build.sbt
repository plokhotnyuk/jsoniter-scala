import sbt.Keys.scalacOptions

lazy val `jsoniter-scala` = project.in(file("."))
  .settings(
    inThisBuild(Seq(
      organization := "com.github.plokhotnyuk.jsoniter-scala",
      scalaVersion := "2.12.4",
      crossScalaVersions := Seq("2.12.4", "2.11.11"),
      startYear := Some(2017),
      organizationHomepage := Some(url("https://github.com/plokhotnyuk")),
      homepage := Some(url("http://github.com/plokhotnyuk/jsoniter-scala")),
      licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-unchecked",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Xfuture",
        "-Xlint",
        "-Xmacro-settings:print-codecs"
      )
    ))
  ).aggregate(macros, benchmark)

lazy val macros = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test
    )
  )

lazy val benchmark = project
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.1",
      "io.circe" %% "circe-core" % "0.9.0-M1",
      "io.circe" %% "circe-generic" % "0.9.0-M1",
      "io.circe" %% "circe-parser" % "0.9.0-M1",
      "com.typesafe.play" %% "play-json" % "2.6.6",
      "pl.project13.scala" % "sbt-jmh-extras" % "0.2.27",
      "org.scalatest" %% "scalatest" % "3.0.4" % Test
    )
  ).dependsOn(macros)
