val `jsoniter-scala-examples` = project.in(file("."))
  .settings(
    scalaVersion := "2.12.6",
    scalacOptions ++= Seq("-Xmacro-settings:print-codecs"),
    crossScalaVersions := Seq("2.13.0-M4", "2.13.0-M3", "2.12.6", "2.11.12"),
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "core" % "0.27.5-SNAPSHOT" % Compile,
      "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.27.5-SNAPSHOT" % Provided // required only in compile-time
    )
  )
