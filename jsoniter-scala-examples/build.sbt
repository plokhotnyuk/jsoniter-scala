val `jsoniter-scala-examples` = project.in(file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    scalaVersion := "2.13.5",
    scalacOptions ++= Seq("-Xmacro-settings:print-codecs"),
    crossScalaVersions := Seq("2.13.5", "2.12.13", "2.11.12"),
    Compile / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.examples.Example01"),
    assembly / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.examples.Example01"),
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.7.1",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.7.1" % "provided"
    )
  )
