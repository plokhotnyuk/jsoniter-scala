package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.scalajs.dom._
import japgolly.scalajs.benchmark.{Benchmark => B, Suite => S}
import japgolly.scalajs.benchmark.engine.{EngineOptions => EO}
import japgolly.scalajs.benchmark.gui.SuiteResultsFormat._
import japgolly.scalajs.benchmark.gui.{Disabled => Off, Enabled => On, BenchmarkGUI => BG, BmResultFormat => BRF, GuiOptions => GO, GuiSuite => GS}
import scala.concurrent.duration._

object Main {
  private val packageName = "com.github.plokhotnyuk.jsoniter_scala.benchmark"

  def main(args: Array[String]): Unit = BG.renderMenu(document.getElementById("body"), engineOptions = EO.default.copy(
    warmupIterations = 5, iterations = 5, iterationTime = 1.seconds,
  ), guiOptions = GO.default.copy(
    batchModeFormats = Map(JmhJson -> On, JmhText -> Off, CSV(8) -> Off),
    bmResultFormats = ctx => Vector(BRF.OpsPerSec, BRF.chooseTimePerOp(ctx))
  ))({
    val benchmark = new ArrayOfBigDecimalsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBigDecimalsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 42667970104045.735577865 as 42667970104045.734
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigDecimalsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBigDecimalsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigIntsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBigIntsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigIntsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBigIntsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBooleansReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBooleansWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfBytesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBytesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfBytesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfBytesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfCharsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfCharsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfCharsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfCharsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDoublesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfDoublesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDoublesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfDoublesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDurationsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfDurationsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDurationsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfDurationsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumADTsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfEnumADTsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumADTsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfEnumADTsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfEnumsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfEnumsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfFloatsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfFloatsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 1.1999999284744263 as 1.2000000476837158
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfFloatsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfFloatsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfInstantsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfInstantsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfInstantsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfInstantsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfIntsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfIntsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfIntsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfIntsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfJavaEnumsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfJavaEnumsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfJavaEnumsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfJavaEnumsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfLocalDatesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLocalDatesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDatesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLocalDatesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDateTimesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLocalDateTimesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDateTimesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLocalDateTimesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalTimesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLocalTimesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalTimesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLocalTimesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLongsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 697125858266480539 as 697125858266480500
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfLongsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfMonthDaysReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfMonthDaysReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfMonthDaysWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfMonthDaysWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetDateTimesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfOffsetDateTimesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetDateTimesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfOffsetDateTimesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetTimesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfOffsetTimesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetTimesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfOffsetTimesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfPeriodsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfPeriodsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfPeriodsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfPeriodsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfShortsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfShortsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfShortsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfShortsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfUUIDsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfUUIDsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfUUIDsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfUUIDsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearMonthsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfYearMonthsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearMonthsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfYearMonthsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfYearsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfYearsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZonedDateTimesReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfZonedDateTimesReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZonedDateTimesWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfZonedDateTimesWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneIdsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfZoneIdsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneIdsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfZoneIdsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneOffsetsReading { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfZoneOffsetsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneOffsetsWriting { size = 512; setup() }
    GS(S(s"$packageName.ArrayOfZoneOffsetsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArraySeqOfBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.ArraySeqOfBooleansReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArraySeqOfBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.ArraySeqOfBooleansWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new Base16Reading { size = 512; setup() }
    GS(S(s"$packageName.Base16Reading")(
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new Base16Writing { size = 512; setup() }
    GS(S(s"$packageName.Base16Writing")(
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new Base64Reading { size = 512; setup() }
    GS(S(s"$packageName.Base64Reading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new Base64Writing { size = 512; setup() }
    GS(S(s"$packageName.Base64Writing")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BigDecimalReading { size = 512; setup() }
    GS(S(s"$packageName.BigDecimalReading")(
      // FIXME: borer parses up to 200 digits only
      // B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      // FIXME: circe-jsoniter parses up to 308 digits only
      // B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BigDecimalWriting { size = 512; setup() }
    GS(S(s"$packageName.BigDecimalWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BigIntReading { size = 512; setup() }
    GS(S(s"$packageName.BigIntReading")(
      // FIXME: borer parses up to 200 digits only
      // B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      // FIXME: circe-jsoniter parses up to 308 digits only
      // B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: smithy4sJson parses up to 308 digits only
      // B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BigIntWriting { size = 512; setup() }
    GS(S(s"$packageName.BigIntWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BitSetReading { size = 512; setup() }
    GS(S(s"$packageName.BitSetReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new BitSetWriting { size = 512; setup() }
    GS(S(s"$packageName.BitSetWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new ExtractFieldsReading { size = 512; setup() }
    GS(S(s"$packageName.ExtractFieldsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GeoJSONReading { setup() }
    GS(S(s"$packageName.GeoJSONReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
      // FIXME: zio-schema-json throws java.lang.RuntimeException: .type.FeatureCollection.features[0].type.Feature.geometry.type.Polygon(unrecognized subtype)
      // B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GeoJSONWriting { setup() }
    GS(S(s"$packageName.GeoJSONWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GitHubActionsAPIReading { setup() }
    GS(S(s"$packageName.GitHubActionsAPIReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GitHubActionsAPIWriting { setup() }
    GS(S(s"$packageName.GitHubActionsAPIWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GoogleMapsAPIPrettyPrinting { setup() }
    GS(S(s"$packageName.GoogleMapsAPIPrettyPrinting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      // FIXME: play-json pretty prints array values in one line
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GoogleMapsAPIReading { setup() }
    GS(S(s"$packageName.GoogleMapsAPIReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GoogleMapsAPIWriting { setup() }
    GS(S(s"$packageName.GoogleMapsAPIWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new IntMapOfBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.IntMapOfBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new IntMapOfBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.IntMapOfBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ListOfBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.ListOfBooleansReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ListOfBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.ListOfBooleansWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MapOfIntsToBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.MapOfIntsToBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MapOfIntsToBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.MapOfIntsToBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MissingRequiredFieldsReading { setup() }
    GS(S(s"$packageName.MissingRequiredFieldsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaWithoutDump")(benchmark.jsoniterScalaWithoutDump()),
      B("jsoniterScalaWithStacktrace")(benchmark.jsoniterScalaWithStacktrace()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MutableBitSetReading { size = 512; setup() }
    GS(S(s"$packageName.MutableBitSetReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new MutableBitSetWriting { size = 512; setup() }
    GS(S(s"$packageName.MutableBitSetWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.MutableLongMapOfBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.MutableLongMapOfBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.MutableMapOfIntsToBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.MutableMapOfIntsToBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MutableSetOfIntsReading { size = 512; setup() }
    GS(S(s"$packageName.MutableSetOfIntsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableSetOfIntsWriting { size = 512; setup() }
    GS(S(s"$packageName.MutableSetOfIntsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new NestedStructsReading { size = 512; setup() }
    GS(S(s"$packageName.NestedStructsReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new NestedStructsWriting { size = 512; setup() }
    GS(S(s"$packageName.NestedStructsWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new OpenRTBReading { setup() }
    GS(S(s"$packageName.OpenRTBReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new OpenRTBWriting { setup() }
    GS(S(s"$packageName.OpenRTBWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks())
      // FIXME: zio-json serializes default values
      // B("zioJson")(benchmark.zioJson()),
      // FIXME: zio-schema-json serializes default values
      // B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new SetOfIntsReading { size = 512; setup() }
    GS(S(s"$packageName.SetOfIntsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new SetOfIntsWriting { size = 512; setup() }
    GS(S(s"$packageName.SetOfIntsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfAsciiCharsReading { size = 512; setup() }
    GS(S(s"$packageName.StringOfAsciiCharsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfAsciiCharsWriting { size = 512; setup() }
    GS(S(s"$packageName.StringOfAsciiCharsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfEscapedCharsReading { size = 512; setup() }
    GS(S(s"$packageName.StringOfEscapedCharsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfEscapedCharsWriting { size = 512; setup() }
    GS(S(s"$packageName.StringOfEscapedCharsWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks())
    ))
  }, {
    val benchmark = new StringOfNonAsciiCharsReading { size = 512; setup() }
    GS(S(s"$packageName.StringOfNonAsciiCharsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfNonAsciiCharsWriting { size = 512; setup() }
    GS(S(s"$packageName.StringOfNonAsciiCharsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new TwitterAPIReading { setup() }
    GS(S(s"$packageName.TwitterAPIReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 850007368138018817 as 850007368138018800
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new TwitterAPIWriting { setup() }
    GS(S(s"$packageName.TwitterAPIWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new VectorOfBooleansReading { size = 512; setup() }
    GS(S(s"$packageName.VectorOfBooleansReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new VectorOfBooleansWriting { size = 512; setup() }
    GS(S(s"$packageName.VectorOfBooleansWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioBlocks")(benchmark.zioBlocks()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  })
}