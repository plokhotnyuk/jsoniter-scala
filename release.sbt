import scala.sys.process._
import sbtrelease.ReleaseStateTransformations._

lazy val ensureJDK11: ReleaseStep = { st: State =>
  val javaVersion = System.getProperty("java.specification.version")
  if (javaVersion != "11") throw new IllegalStateException("Cancelling release, please use JDK 11")
  st
}

lazy val updateVersionInReadme: ReleaseStep = { st: State =>
  val extracted = Project.extract(st)
  val newVersion = extracted.get(version)
  val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")
  val readme = "README.md"
  val oldContent = IO.read(file(readme))
  val newContent = oldContent.replaceAll('"' + oldVersion + '"', '"' + newVersion + '"')
    .replaceAll('-' + oldVersion + '-', '-' + newVersion + '-')
  IO.write(file(readme), newContent)
  s"git add $readme" !! st.log
  st
}

releaseCrossBuild := false

releaseProcess := Seq[ReleaseStep](
  ensureJDK11,
  // FIXME: Uncomment after using a stable version of jackson libraries for benchmarks
  //checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  releaseStepCommandAndRemaining("+mimaReportBinaryIssues"),
  updateVersionInReadme,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

releaseVcsSign := true