import scala.sys.process.*
import sbtrelease.ReleaseStateTransformations.*

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

  Seq(
    "README.md",
    "docs/tutorials/1-getting-started.md",
    "docs/tutorials/1-getting-started.scala",
  ).foreach(updateFile)
  updateFile("README.md")
  Seq(
    "example01.sc",
    "example02.sc",
    "example03.scala",
  ).foreach(x => updateFile(s"jsoniter-scala-examples/$x"))

  st
}

addCommandAlias(
  "releaseTest",
  "+jsoniter-scala-coreJVM/test; +jsoniter-scala-coreJS/test; +jsoniter-scala-coreNative/test; " +
  "+jsoniter-scala-macrosJVM/test; +jsoniter-scala-macrosJS/test; +jsoniter-scala-macrosNative/test; " +
  "+jsoniter-scala-circeJVM/test; +jsoniter-scala-circeJS/test; +jsoniter-scala-circeNative/test; "
)

addCommandAlias(
  "releaseMima",
  "+jsoniter-scala-coreJVM/mimaReportBinaryIssues; +jsoniter-scala-coreJS/mimaReportBinaryIssues; +jsoniter-scala-coreNative/mimaReportBinaryIssues; " +
  "+jsoniter-scala-macrosJVM/mimaReportBinaryIssues; +jsoniter-scala-macrosJS/mimaReportBinaryIssues; +jsoniter-scala-macrosNative/mimaReportBinaryIssues; " +
  "+jsoniter-scala-circeJVM/mimaReportBinaryIssues; +jsoniter-scala-circeJS/mimaReportBinaryIssues; +jsoniter-scala-circeNative/mimaReportBinaryIssues; "
)

addCommandAlias(
  "releasePublish",
  "+jsoniter-scala-coreJVM/publishSigned; +jsoniter-scala-coreJS/publishSigned; +jsoniter-scala-coreNative/publishSigned; " +
  "+jsoniter-scala-macrosJVM/publishSigned; +jsoniter-scala-macrosJS/publishSigned; +jsoniter-scala-macrosNative/publishSigned; " +
  "+jsoniter-scala-circeJVM/publishSigned; +jsoniter-scala-circeJS/publishSigned; +jsoniter-scala-circeNative/publishSigned; "
)

releaseCrossBuild := false

releaseProcess := Seq[ReleaseStep](
  ensureJDK11,
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("releaseTest"),
  setReleaseVersion,
  releaseStepCommandAndRemaining("releaseMima"),
  updateVersionInReadmeAndExamples,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("releasePublish"),
  releaseStepCommand("sonaRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

releaseVcsSign := true