import scala.sys.process._
import sbtrelease.ReleaseStateTransformations._

lazy val ensureJDK11: ReleaseStep = { st: State =>
  val javaVersion = System.getProperty("java.specification.version")
  if (javaVersion != "11") throw new IllegalStateException("Cancelling release, please use JDK 11")
  st
}

lazy val updateVersionInReadmeAndExamples: ReleaseStep = { st: State =>
  val extracted = Project.extract(st)
  val newVersion = extracted.get(version)
  val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

  def updateFile(path: String): Unit = {
    val oldContent = IO.read(file(path))
    val newContent = oldContent
      .replaceAll('"' + oldVersion + '"', '"' + newVersion + '"')
      .replaceAll('-' + oldVersion + '-', '-' + newVersion + '-')
      .replaceAll(':' + oldVersion + '"', ':' + newVersion + '"')
    IO.write(file(path), newContent)
    s"git add $path" !! st.log
  }

  updateFile("README.md")
  (1 to 3).foreach(n => updateFile(s"jsoniter-scala-examples/example0$n.sc"))

  st
}

releaseCrossBuild := false

releaseProcess := Seq[ReleaseStep](
  ensureJDK11,
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  releaseStepCommandAndRemaining("+mimaReportBinaryIssues"),
  updateVersionInReadmeAndExamples,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

releaseVcsSign := true