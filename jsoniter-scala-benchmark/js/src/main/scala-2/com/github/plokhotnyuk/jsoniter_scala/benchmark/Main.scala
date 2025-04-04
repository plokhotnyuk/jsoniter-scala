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
    val benchmark = new ArrayOfBigDecimalsReading { size = 512; setup() }
    GS(S("ArrayOfBigDecimalsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 42667970104045.735577865 as 42667970104045.734
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigDecimalsWriting { size = 512; setup() }
    GS(S("ArrayOfBigDecimalsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfBigIntsReading { size = 512; setup() }
    GS(S("ArrayOfBigIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigIntsWriting { size = 512; setup() }
    GS(S("ArrayOfBigIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfBooleansReading { size = 512; setup() }
    GS(S("ArrayOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfBooleansWriting { size = 512; setup() }
    GS(S("ArrayOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfBytesReading { size = 512; setup() }
    GS(S("ArrayOfBytesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfBytesWriting { size = 512; setup() }
    GS(S("ArrayOfBytesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfCharsReading { size = 512; setup() }
    GS(S("ArrayOfCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfCharsWriting { size = 512; setup() }
    GS(S("ArrayOfCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDoublesReading { size = 512; setup() }
    GS(S("ArrayOfDoublesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDoublesWriting { size = 512; setup() }
    GS(S("ArrayOfDoublesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfDurationsReading { size = 512; setup() }
    GS(S("ArrayOfDurationsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfDurationsWriting { size = 512; setup() }
    GS(S("ArrayOfDurationsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumADTsReading { size = 512; setup() }
    GS(S("ArrayOfEnumADTsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumADTsWriting { size = 512; setup() }
    GS(S("ArrayOfEnumADTsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumsReading { size = 512; setup() }
    GS(S("ArrayOfEnumsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("ArrayOfEnumsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("ArrayOfFloatsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 1.1999999284744263 as 1.2000000476837158
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfFloatsWriting { size = 512; setup() }
    GS(S("ArrayOfFloatsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfInstantsReading { size = 512; setup() }
    GS(S("ArrayOfInstantsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfInstantsWriting { size = 512; setup() }
    GS(S("ArrayOfInstantsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfIntsReading { size = 512; setup() }
    GS(S("ArrayOfIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfIntsWriting { size = 512; setup() }
    GS(S("ArrayOfIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfJavaEnumsReading { size = 512; setup() }
    GS(S("ArrayOfJavaEnumsReading")(
      // FIXME: Cannot link with Scala.js
      // B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("ArrayOfJavaEnumsWriting")(
      // FIXME: Cannot link with Scala.js
      // B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("ArrayOfLocalDatesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDatesWriting { size = 512; setup() }
    GS(S("ArrayOfLocalDatesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDateTimesReading { size = 512; setup() }
    GS(S("ArrayOfLocalDateTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDateTimesWriting { size = 512; setup() }
    GS(S("ArrayOfLocalDateTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalTimesReading { size = 512; setup() }
    GS(S("ArrayOfLocalTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalTimesWriting { size = 512; setup() }
    GS(S("ArrayOfLocalTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsReading { size = 512; setup() }
    GS(S("ArrayOfLongsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 697125858266480539 as 697125858266480500
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsWriting { size = 512; setup() }
    GS(S("ArrayOfLongsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfMonthDaysReading { size = 512; setup() }
    GS(S("ArrayOfMonthDaysReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfMonthDaysWriting { size = 512; setup() }
    GS(S("ArrayOfMonthDaysWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetDateTimesReading { size = 512; setup() }
    GS(S("ArrayOfOffsetDateTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetDateTimesWriting { size = 512; setup() }
    GS(S("ArrayOfOffsetDateTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetTimesReading { size = 512; setup() }
    GS(S("ArrayOfOffsetTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetTimesWriting { size = 512; setup() }
    GS(S("ArrayOfOffsetTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfPeriodsReading { size = 512; setup() }
    GS(S("ArrayOfPeriodsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfPeriodsWriting { size = 512; setup() }
    GS(S("ArrayOfPeriodsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfShortsReading { size = 512; setup() }
    GS(S("ArrayOfShortsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfShortsWriting { size = 512; setup() }
    GS(S("ArrayOfShortsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfUUIDsReading { size = 512; setup() }
    GS(S("ArrayOfUUIDsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfUUIDsWriting { size = 512; setup() }
    GS(S("ArrayOfUUIDsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new ArrayOfYearMonthsReading { size = 512; setup() }
    GS(S("ArrayOfYearMonthsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearMonthsWriting { size = 512; setup() }
    GS(S("ArrayOfYearMonthsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearsReading { size = 512; setup() }
    GS(S("ArrayOfYearsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfYearsWriting { size = 512; setup() }
    GS(S("ArrayOfYearsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZonedDateTimesReading { size = 512; setup() }
    GS(S("ArrayOfZonedDateTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZonedDateTimesWriting { size = 512; setup() }
    GS(S("ArrayOfZonedDateTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneIdsReading { size = 512; setup() }
    GS(S("ArrayOfZoneIdsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneIdsWriting { size = 512; setup() }
    GS(S("ArrayOfZoneIdsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneOffsetsReading { size = 512; setup() }
    GS(S("ArrayOfZoneOffsetsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneOffsetsWriting { size = 512; setup() }
    GS(S("ArrayOfZoneOffsetsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArraySeqOfBooleansReading { size = 512; setup() }
    GS(S("ArraySeqOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ArraySeqOfBooleansWriting { size = 512; setup() }
    GS(S("ArraySeqOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new Base16Reading { size = 512; setup() }
    GS(S("Base16Reading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new Base16Writing { size = 512; setup() }
    GS(S("Base16Writing")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new Base64Reading { size = 512; setup() }
    GS(S("Base64Reading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("Base64Writing")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("BigDecimalReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      // FIXME: borer parses up to 200 digits only
      // B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      // FIXME: circe-jsoniter parses up to 308 digits only
      // B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BigDecimalWriting { size = 512; setup() }
    GS(S("BigDecimalWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new BigIntReading { size = 512; setup() }
    GS(S("BigIntReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      // FIXME: borer parses up to 200 digits only
      // B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      // FIXME: circe-jsoniter parses up to 308 digits only
      // B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: smithy4sJson parses up to 308 digits only
      // B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new BigIntWriting { size = 512; setup() }
    GS(S("BigIntWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new BitSetReading { size = 512; setup() }
    GS(S("BitSetReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new BitSetWriting { size = 512; setup() }
    GS(S("BitSetWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new ExtractFieldsReading { size = 512; setup() }
    GS(S("ExtractFieldsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GeoJSONReading { setup() }
    GS(S("GeoJSONReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GeoJSONWriting { setup() }
    GS(S("GeoJSONWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("GitHubActionsAPIReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GitHubActionsAPIWriting { setup() }
    GS(S("GitHubActionsAPIWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new GoogleMapsAPIPrettyPrinting { setup() }
    GS(S("GoogleMapsAPIPrettyPrinting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      // FIXME: play-json pretty prints array values in one line
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GoogleMapsAPIReading { setup() }
    GS(S("GoogleMapsAPIReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new GoogleMapsAPIWriting { setup() }
    GS(S("GoogleMapsAPIWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new IntMapOfBooleansReading { size = 512; setup() }
    GS(S("IntMapOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new IntMapOfBooleansWriting { size = 512; setup() }
    GS(S("IntMapOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ListOfBooleansReading { size = 512; setup() }
    GS(S("ListOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new ListOfBooleansWriting { size = 512; setup() }
    GS(S("ListOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new MapOfIntsToBooleansReading { size = 512; setup() }
    GS(S("MapOfIntsToBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MapOfIntsToBooleansWriting { size = 512; setup() }
    GS(S("MapOfIntsToBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MissingRequiredFieldsReading { setup() }
    GS(S("MissingRequiredFieldsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new MutableBitSetReading { size = 512; setup() }
    GS(S("MutableBitSetReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new MutableBitSetWriting { size = 512; setup() }
    GS(S("MutableBitSetWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansReading { size = 512; setup() }
    GS(S("MutableLongMapOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansWriting { size = 512; setup() }
    GS(S("MutableLongMapOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansReading { size = 512; setup() }
    GS(S("MutableMapOfIntsToBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansWriting { size = 512; setup() }
    GS(S("MutableMapOfIntsToBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MutableSetOfIntsReading { size = 512; setup() }
    GS(S("MutableSetOfIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("MutableSetOfIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    GS(S("NestedStructsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new NestedStructsWriting { size = 512; setup() }
    GS(S("NestedStructsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new OpenRTBReading { setup() }
    GS(S("OpenRTBReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new OpenRTBWriting { setup() }
    GS(S("OpenRTBWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle())
      // FIXME: zio-json serializes default values
      // B("zioJson")(benchmark.zioJson()),
      // FIXME: zio-schema-json serializes default values
      // B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new SetOfIntsReading { size = 512; setup() }
    GS(S("SetOfIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new SetOfIntsWriting { size = 512; setup() }
    GS(S("SetOfIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new StringOfAsciiCharsReading { size = 512; setup() }
    GS(S("StringOfAsciiCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfAsciiCharsWriting { size = 512; setup() }
    GS(S("StringOfAsciiCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new StringOfEscapedCharsReading { size = 512; setup() }
    GS(S("StringOfEscapedCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfEscapedCharsWriting { size = 512; setup() }
    GS(S("StringOfEscapedCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new StringOfNonAsciiCharsReading { size = 512; setup() }
    GS(S("StringOfNonAsciiCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new StringOfNonAsciiCharsWriting { size = 512; setup() }
    GS(S("StringOfNonAsciiCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new TwitterAPIReading { setup() }
    GS(S("TwitterAPIReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      // FIXME: play-json parses 850007368138018817 as 850007368138018800
      // B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new TwitterAPIWriting { setup() }
    GS(S("TwitterAPIWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
    val benchmark = new VectorOfBooleansReading { size = 512; setup() }
    GS(S("VectorOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("circeJsoniter")(benchmark.circeJsoniter()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("playJson")(benchmark.playJson()),
      B("playJsonJsoniter")(benchmark.playJsonJsoniter()),
      B("smithy4sJson")(benchmark.smithy4sJson()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson()),
      B("zioSchemaJson")(benchmark.zioSchemaJson())
    ))
  }, {
    val benchmark = new VectorOfBooleansWriting { size = 512; setup() }
    GS(S("VectorOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
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
  })
}