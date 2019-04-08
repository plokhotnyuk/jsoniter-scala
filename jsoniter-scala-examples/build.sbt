val `jsoniter-scala-examples` = project.in(file("."))
  .settings(
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq("-Xmacro-settings:print-codecs"),
    crossScalaVersions := Seq("2.13.0-RC1", "2.13.0-M5", "2.12.8", "2.11.12"),
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "latest.integration",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "latest.integration" % Provided // required only in compile-time
    )
  )
