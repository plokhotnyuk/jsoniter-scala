import sbt.Keys.scalacOptions

lazy val `jsoniter-scala` = project.in(file("."))
  .settings(
    crossScalaVersions := Seq("2.12.3", "2.11.11"),
    inThisBuild(Seq(
      organization := "com.github.plokhotnyuk.jsoniter-scala",
      scalaVersion := "2.12.3",
      startYear := Some(2017),
      organizationHomepage := Some(url("https://github.com/plokhotnyuk")),
      homepage := Some(url("http://github.com/plokhotnyuk/jsoniter-scala")),
      licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))
    ))
  ).aggregate(macros, benchmark)

lazy val macros = project
  .settings(
    libraryDependencies ++= Seq(
      "com.jsoniter" % "jsoniter" % "0.9.15",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.javassist" % "javassist" % "3.21.0-GA" % Optional,
      "org.scalatest" %% "scalatest" % "3.0.3" % Test
    ),
    scalacOptions += "-Xmacro-settings:print-codecs"
  )

lazy val benchmark = project
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.1",
      "pl.project13.scala" % "sbt-jmh-extras" % "0.2.27"
    )
  ).dependsOn(macros)
