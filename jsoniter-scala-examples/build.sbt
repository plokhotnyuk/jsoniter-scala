val `jsoniter-scala-examples` = project.in(file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    scalaVersion := "2.13.8",
    scalacOptions ++= Seq("-Xmacro-settings:print-codecs"),
    crossScalaVersions := Seq("3.1.2", "2.13.8", "2.12.15"),
    Compile / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.examples.Example01"),
    assembly / mainClass := Some("com.github.plokhotnyuk.jsoniter_scala.examples.Example01"),
    libraryDependencySchemes += "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "always",
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "latest.integration",
      // Use the "provided" scope instead when the "compile-internal" scope is not supported
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "latest.integration" % "compile-internal"
    ),
    nativeImageOptions ++= List("--no-fallback"),
    nativeImageVersion := "22.0.0.2"
  )
