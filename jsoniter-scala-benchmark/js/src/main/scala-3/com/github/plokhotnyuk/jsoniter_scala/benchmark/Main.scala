package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.scalajs.dom._
import japgolly.scalajs.benchmark.{Benchmark => B, Suite => S}
import japgolly.scalajs.benchmark.engine.{EngineOptions => EO}
import japgolly.scalajs.benchmark.gui.SuiteResultsFormat._
import japgolly.scalajs.benchmark.gui.{Disabled => Off, Enabled => On, BenchmarkGUI => BG, BmResultFormat => BRF, GuiOptions => GO, GuiSuite => GS}
import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = BG.renderMenu(document.getElementById("body"), engineOptions = EO.default.copy(
    warmupIterations = 5, iterations = 5, iterationTime = 1.seconds,
  ), guiOptions = GO.default.copy(
    batchModeFormats = Map(JmhJson -> On, JmhText -> Off, CSV(8) -> Off),
    bmResultFormats = ctx => Vector(BRF.OpsPerSec, BRF.chooseTimePerOp(ctx))
  ))({
    val benchmark = new ADTReading { setup() }
    GS(S("ADTReading")(
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
    val benchmark = new ADTWriting { setup() }
    GS(S("ADTWriting")(
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
    val benchmark = new AnyValsReading { setup() }
    GS(S("AnyValsReading")(
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
    val benchmark = new AnyValsWriting { setup() }
    GS(S("AnyValsWriting")(
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
    val benchmark = new ArrayBufferOfBooleansReading { size = 128; setup() }
    GS(S("ArrayBufferOfBooleansReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayBufferOfBooleansWriting { size = 128; setup() }
    GS(S("ArrayBufferOfBooleansWriting")(
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
    val benchmark = new ArrayOfBigDecimalsReading { size = 128; setup() }
    GS(S("ArrayOfBigDecimalsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      //FIXME play-json parses 42667970104045.735577865 as 42667970104045.734
      //B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigDecimalsWriting { size = 128; setup() }
    GS(S("ArrayOfBigDecimalsWriting")(
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
    val benchmark = new ArrayOfBigIntsReading { size = 128; setup() }
    GS(S("ArrayOfBigIntsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigIntsWriting { size = 128; setup() }
    GS(S("ArrayOfBigIntsWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBooleansReading { size = 128; setup() }
    GS(S("ArrayOfBooleansReading")(
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
    val benchmark = new ArrayOfBooleansWriting { size = 128; setup() }
    GS(S("ArrayOfBooleansWriting")(
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
    val benchmark = new ArrayOfBytesReading { size = 128; setup() }
    GS(S("ArrayOfBytesReading")(
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
    val benchmark = new ArrayOfBytesWriting { size = 128; setup() }
    GS(S("ArrayOfBytesWriting")(
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
    val benchmark = new ArrayOfCharsReading { size = 128; setup() }
    GS(S("ArrayOfCharsReading")(
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
    val benchmark = new ArrayOfCharsWriting { size = 128; setup() }
    GS(S("ArrayOfCharsWriting")(
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
    val benchmark = new ArrayOfDoublesReading { size = 128; setup() }
    GS(S("ArrayOfDoublesReading")(
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
    val benchmark = new ArrayOfDoublesWriting { size = 128; setup() }
    GS(S("ArrayOfDoublesWriting")(
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
    val benchmark = new ArrayOfDurationsReading { size = 128; setup() }
    GS(S("ArrayOfDurationsReading")(
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
    val benchmark = new ArrayOfDurationsWriting { size = 128; setup() }
    GS(S("ArrayOfDurationsWriting")(
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
    val benchmark = new ArrayOfEnumADTsReading { size = 128; setup() }
    GS(S("ArrayOfEnumADTsReading")(
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
    val benchmark = new ArrayOfEnumADTsWriting { size = 128; setup() }
    GS(S("ArrayOfEnumADTsWriting")(
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
    val benchmark = new ArrayOfEnumsReading { size = 128; setup() }
    GS(S("ArrayOfEnumsReading")(
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
    val benchmark = new ArrayOfEnumsWriting { size = 128; setup() }
    GS(S("ArrayOfEnumsWriting")(
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
    val benchmark = new ArrayOfFloatsReading { size = 128; setup() }
    GS(S("ArrayOfFloatsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      //FIXME play-json parses 1.1999999284744263 as 1.2000000476837158
      //B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfFloatsWriting { size = 128; setup() }
    GS(S("ArrayOfFloatsWriting")(
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
    val benchmark = new ArrayOfInstantsReading { size = 128; setup() }
    GS(S("ArrayOfInstantsReading")(
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
    val benchmark = new ArrayOfInstantsWriting { size = 128; setup() }
    GS(S("ArrayOfInstantsWriting")(
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
    val benchmark = new ArrayOfIntsReading { size = 128; setup() }
    GS(S("ArrayOfIntsReading")(
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
    val benchmark = new ArrayOfIntsWriting { size = 128; setup() }
    GS(S("ArrayOfIntsWriting")(
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
    val benchmark = new ArrayOfJavaEnumsReading { size = 128; setup() }
    GS(S("ArrayOfJavaEnumsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfJavaEnumsWriting { size = 128; setup() }
    GS(S("ArrayOfJavaEnumsWriting")(
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
    val benchmark = new ArrayOfLocalDatesReading { size = 128; setup() }
    GS(S("ArrayOfLocalDatesReading")(
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
    val benchmark = new ArrayOfLocalDatesWriting { size = 128; setup() }
    GS(S("ArrayOfLocalDatesWriting")(
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
    val benchmark = new ArrayOfLocalDateTimesReading { size = 128; setup() }
    GS(S("ArrayOfLocalDateTimesReading")(
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
    val benchmark = new ArrayOfLocalDateTimesWriting { size = 128; setup() }
    GS(S("ArrayOfLocalDateTimesWriting")(
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
    val benchmark = new ArrayOfLocalTimesReading { size = 128; setup() }
    GS(S("ArrayOfLocalTimesReading")(
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
    val benchmark = new ArrayOfLocalTimesWriting { size = 128; setup() }
    GS(S("ArrayOfLocalTimesWriting")(
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
    val benchmark = new ArrayOfLongsReading { size = 128; setup() }
    GS(S("ArrayOfLongsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 697125858266480539 as 697125858266480500
      //B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsWriting { size = 128; setup() }
    GS(S("ArrayOfLongsWriting")(
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
    val benchmark = new ArrayOfMonthDaysReading { size = 128; setup() }
    GS(S("ArrayOfMonthDaysReading")(
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
    val benchmark = new ArrayOfMonthDaysWriting { size = 128; setup() }
    GS(S("ArrayOfMonthDaysWriting")(
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
    val benchmark = new ArrayOfOffsetDateTimesReading { size = 128; setup() }
    GS(S("ArrayOfOffsetDateTimesReading")(
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
    val benchmark = new ArrayOfOffsetDateTimesWriting { size = 128; setup() }
    GS(S("ArrayOfOffsetDateTimesWriting")(
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
    val benchmark = new ArrayOfOffsetTimesReading { size = 128; setup() }
    GS(S("ArrayOfOffsetTimesReading")(
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
    val benchmark = new ArrayOfOffsetTimesWriting { size = 128; setup() }
    GS(S("ArrayOfOffsetTimesWriting")(
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
    val benchmark = new ArrayOfPeriodsReading { size = 128; setup() }
    GS(S("ArrayOfPeriodsReading")(
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
    val benchmark = new ArrayOfPeriodsWriting { size = 128; setup() }
    GS(S("ArrayOfPeriodsWriting")(
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
    val benchmark = new ArrayOfShortsReading { size = 128; setup() }
    GS(S("ArrayOfShortsReading")(
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
    val benchmark = new ArrayOfShortsWriting { size = 128; setup() }
    GS(S("ArrayOfShortsWriting")(
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
    val benchmark = new ArrayOfUUIDsReading { size = 128; setup() }
    GS(S("ArrayOfUUIDsReading")(
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
    val benchmark = new ArrayOfUUIDsWriting { size = 128; setup() }
    GS(S("ArrayOfUUIDsWriting")(
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
    val benchmark = new ArrayOfYearMonthsReading { size = 128; setup() }
    GS(S("ArrayOfYearMonthsReading")(
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
    val benchmark = new ArrayOfYearMonthsWriting { size = 128; setup() }
    GS(S("ArrayOfYearMonthsWriting")(
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
    val benchmark = new ArrayOfYearsReading { size = 128; setup() }
    GS(S("ArrayOfYearsReading")(
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
    val benchmark = new ArrayOfYearsWriting { size = 128; setup() }
    GS(S("ArrayOfYearsWriting")(
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
    val benchmark = new ArrayOfZonedDateTimesReading { size = 128; setup() }
    GS(S("ArrayOfZonedDateTimesReading")(
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
    val benchmark = new ArrayOfZonedDateTimesWriting { size = 128; setup() }
    GS(S("ArrayOfZonedDateTimesWriting")(
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
    val benchmark = new ArrayOfZoneIdsReading { size = 128; setup() }
    GS(S("ArrayOfZoneIdsReading")(
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
    val benchmark = new ArrayOfZoneIdsWriting { size = 128; setup() }
    GS(S("ArrayOfZoneIdsWriting")(
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
    val benchmark = new ArrayOfZoneOffsetsReading { size = 128; setup() }
    GS(S("ArrayOfZoneOffsetsReading")(
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
    val benchmark = new ArrayOfZoneOffsetsWriting { size = 128; setup() }
    GS(S("ArrayOfZoneOffsetsWriting")(
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
    val benchmark = new ArraySeqOfBooleansReading { size = 128; setup() }
    GS(S("ArraySeqOfBooleansReading")(
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
    val benchmark = new ArraySeqOfBooleansWriting { size = 128; setup() }
    GS(S("ArraySeqOfBooleansWriting")(
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
    val benchmark = new Base16Reading { size = 128; setup() }
    GS(S("Base16Reading")(
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new Base16Writing { size = 128; setup() }
    GS(S("Base16Writing")(
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new Base64Reading { size = 128; setup() }
    GS(S("Base64Reading")(
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
    val benchmark = new Base64Writing { size = 128; setup() }
    GS(S("Base64Writing")(
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
    val benchmark = new BigDecimalReading { size = 128; setup() }
    GS(S("BigDecimalReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new BigDecimalWriting { size = 128; setup() }
    GS(S("BigDecimalWriting")(
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
    val benchmark = new BigIntReading { size = 128; setup() }
    GS(S("BigIntReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new BigIntWriting { size = 128; setup() }
    GS(S("BigIntWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BitSetReading { size = 128; setup() }
    GS(S("BitSetReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new BitSetWriting { size = 128; setup() }
    GS(S("BitSetWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new ExtractFieldsReading { size = 128; setup() }
    GS(S("ExtractFieldsReading")(
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
    val benchmark = new GeoJSONReading { setup() }
    GS(S("GeoJSONReading")(
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
    val benchmark = new GeoJSONWriting { setup() }
    GS(S("GeoJSONWriting")(
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
    val benchmark = new GitHubActionsAPIReading { setup() }
    GS(S("GitHubActionsAPIReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new GitHubActionsAPIWriting { setup() }
    GS(S("GitHubActionsAPIWriting")(
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
    val benchmark = new GoogleMapsAPIPrettyPrinting { setup() }
    GS(S("GoogleMapsAPIPrettyPrinting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      //FIXME: play-json pretty prints array values in one line
      //B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new GoogleMapsAPIReading { setup() }
    GS(S("GoogleMapsAPIReading")(
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
    val benchmark = new GoogleMapsAPIWriting { setup() }
    GS(S("GoogleMapsAPIWriting")(
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
    val benchmark = new IntMapOfBooleansReading { size = 128; setup() }
    GS(S("IntMapOfBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new IntMapOfBooleansWriting { size = 128; setup() }
    GS(S("IntMapOfBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new IntReading { setup() }
    GS(S("IntReading")(
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
    val benchmark = new IntWriting { setup() }
    GS(S("IntWriting")(
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
    val benchmark = new ListOfBooleansReading { size = 128; setup() }
    GS(S("ListOfBooleansReading")(
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
    val benchmark = new ListOfBooleansWriting { size = 128; setup() }
    GS(S("ListOfBooleansWriting")(
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
    val benchmark = new MapOfIntsToBooleansReading { size = 128; setup() }
    GS(S("MapOfIntsToBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MapOfIntsToBooleansWriting { size = 128; setup() }
    GS(S("MapOfIntsToBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MissingRequiredFieldsReading { setup() }
    GS(S("MissingRequiredFieldsReading")(
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
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MutableBitSetReading { size = 128; setup() }
    GS(S("MutableBitSetReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new MutableBitSetWriting { size = 128; setup() }
    GS(S("MutableBitSetWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansReading { size = 128; setup() }
    GS(S("MutableLongMapOfBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansWriting { size = 128; setup() }
    GS(S("MutableLongMapOfBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansReading { size = 128; setup() }
    GS(S("MutableMapOfIntsToBooleansReading")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansWriting { size = 128; setup() }
    GS(S("MutableMapOfIntsToBooleansWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MutableSetOfIntsReading { size = 128; setup() }
    GS(S("MutableSetOfIntsReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableSetOfIntsWriting { size = 128; setup() }
    GS(S("MutableSetOfIntsWriting")(
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
    val benchmark = new NestedStructsReading { size = 128; setup() }
    GS(S("NestedStructsReading")(
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
    val benchmark = new NestedStructsWriting { size = 128; setup() }
    GS(S("NestedStructsWriting")(
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
    val benchmark = new OpenRTBReading { setup() }
    GS(S("OpenRTBReading")(
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
    val benchmark = new OpenRTBWriting { setup() }
    GS(S("OpenRTBWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new PrimitivesReading { setup() }
    GS(S("PrimitivesReading")(
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
    val benchmark = new PrimitivesWriting { setup() }
    GS(S("PrimitivesWriting")(
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
    val benchmark = new SetOfIntsReading { size = 128; setup() }
    GS(S("SetOfIntsReading")(
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
    val benchmark = new SetOfIntsWriting { size = 128; setup() }
    GS(S("SetOfIntsWriting")(
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
    val benchmark = new StringOfAsciiCharsReading { size = 128; setup() }
    GS(S("StringOfAsciiCharsReading")(
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
    val benchmark = new StringOfAsciiCharsWriting { size = 128; setup() }
    GS(S("StringOfAsciiCharsWriting")(
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
    val benchmark = new StringOfEscapedCharsReading { size = 128; setup() }
    GS(S("StringOfEscapedCharsReading")(
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
    val benchmark = new StringOfEscapedCharsWriting { size = 128; setup() }
    GS(S("StringOfEscapedCharsWriting")(
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new StringOfNonAsciiCharsReading { size = 128; setup() }
    GS(S("StringOfNonAsciiCharsReading")(
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
    val benchmark = new StringOfNonAsciiCharsWriting { size = 128; setup() }
    GS(S("StringOfNonAsciiCharsWriting")(
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
    val benchmark = new TwitterAPIReading { setup() }
    GS(S("TwitterAPIReading")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      //FIXME: play-json parses 850007368138018817 as 850007368138018800
      //B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new TwitterAPIWriting { setup() }
    GS(S("TwitterAPIWriting")(
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new VectorOfBooleansReading { size = 128; setup() }
    GS(S("VectorOfBooleansReading")(
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
    val benchmark = new VectorOfBooleansWriting { size = 128; setup() }
    GS(S("VectorOfBooleansWriting")(
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
  })
}