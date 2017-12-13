import scala.sys.process._
import sbtrelease.ReleaseStateTransformations._

lazy val updateVersionInReadme: ReleaseStep = { st: State =>
  val extracted = Project.extract(st)
  val newVersion = extracted.get(version)
  val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")
  val readme = "README.md"
  val oldContent = IO.read(file(readme))
  val newContent = oldContent.replaceAll('"' + oldVersion + '"', '"' + newVersion + '"')
  IO.write(file(readme), newContent)
  s"git add $readme" !! st.log
  st
}

releaseCrossBuild := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  updateVersionInReadme,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(releaseStepCommand("publishSigned"), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)