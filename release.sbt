import ReleaseTransformations._

releaseCrossBuild := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(releaseStepCommand("publishSigned"), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)