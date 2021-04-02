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
    val benchmark = new ADTReading
    GS(S("ADTReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ADTWriting
    GS(S("ADTWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new AnyValsReading
    GS(S("AnyValsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new AnyValsWriting
    GS(S("AnyValsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayBufferOfBooleansReading { size = 128; setup() }
    GS(S("ArrayBufferOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayBufferOfBooleansWriting { size = 128; setup() }
    GS(S("ArrayBufferOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfBigDecimalsReading { size = 128; setup() }
    GS(S("ArrayOfBigDecimalsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigDecimalsWriting { size = 128; setup() }
    GS(S("ArrayOfBigDecimalsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBigIntsReading { size = 128; setup() }
    GS(S("ArrayOfBigIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfBigIntsWriting { size = 128; setup() }
    GS(S("ArrayOfBigIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfBooleansReading { size = 128; setup() }
    GS(S("ArrayOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBooleansWriting { size = 128; setup() }
    GS(S("ArrayOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBytesReading { size = 128; setup() }
    GS(S("ArrayOfBytesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfBytesWriting { size = 128; setup() }
    GS(S("ArrayOfBytesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfCharsReading { size = 128; setup() }
    GS(S("ArrayOfCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfCharsWriting { size = 128; setup() }
    GS(S("ArrayOfCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfDoublesReading { size = 128; setup() }
    GS(S("ArrayOfDoublesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfDoublesWriting { size = 128; setup() }
    GS(S("ArrayOfDoublesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfDurationsReading { size = 128; setup() }
    GS(S("ArrayOfDurationsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfDurationsWriting { size = 128; setup() }
    GS(S("ArrayOfDurationsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumADTsReading { size = 128; setup() }
    GS(S("ArrayOfEnumADTsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumADTsWriting { size = 128; setup() }
    GS(S("ArrayOfEnumADTsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumsReading { size = 128; setup() }
    GS(S("ArrayOfEnumsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfEnumsWriting { size = 128; setup() }
    GS(S("ArrayOfEnumsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfFloatsReading { size = 128; setup() }
    GS(S("ArrayOfFloatsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfFloatsWriting { size = 128; setup() }
    GS(S("ArrayOfFloatsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfInstantsReading { size = 128; setup() }
    GS(S("ArrayOfInstantsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfInstantsWriting { size = 128; setup() }
    GS(S("ArrayOfInstantsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfIntsReading { size = 128; setup() }
    GS(S("ArrayOfIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfIntsWriting { size = 128; setup() }
    GS(S("ArrayOfIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfJavaEnumsReading { size = 128; setup() }
    GS(S("ArrayOfJavaEnumsReading")(
      //FIXME: Cannot link with Scala.js
      //B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfJavaEnumsWriting { size = 128; setup() }
    GS(S("ArrayOfJavaEnumsWriting")(
      //FIXME: Cannot link with Scala.js
      //B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new ArrayOfLocalDatesReading { size = 128; setup() }
    GS(S("ArrayOfLocalDatesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDatesWriting { size = 128; setup() }
    GS(S("ArrayOfLocalDatesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDateTimesReading { size = 128; setup() }
    GS(S("ArrayOfLocalDateTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalDateTimesWriting { size = 128; setup() }
    GS(S("ArrayOfLocalDateTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalTimesReading { size = 128; setup() }
    GS(S("ArrayOfLocalTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLocalTimesWriting { size = 128; setup() }
    GS(S("ArrayOfLocalTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsReading { size = 128; setup() }
    GS(S("ArrayOfLongsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfLongsWriting { size = 128; setup() }
    GS(S("ArrayOfLongsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfMonthDaysReading { size = 128; setup() }
    GS(S("ArrayOfMonthDaysReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfMonthDaysWriting { size = 128; setup() }
    GS(S("ArrayOfMonthDaysWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetDateTimesReading { size = 128; setup() }
    GS(S("ArrayOfOffsetDateTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetDateTimesWriting { size = 128; setup() }
    GS(S("ArrayOfOffsetDateTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetTimesReading { size = 128; setup() }
    GS(S("ArrayOfOffsetTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfOffsetTimesWriting { size = 128; setup() }
    GS(S("ArrayOfOffsetTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfPeriodsReading { size = 128; setup() }
    GS(S("ArrayOfPeriodsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfPeriodsWriting { size = 128; setup() }
    GS(S("ArrayOfPeriodsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfShortsReading { size = 128; setup() }
    GS(S("ArrayOfShortsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfShortsWriting { size = 128; setup() }
    GS(S("ArrayOfShortsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfUUIDsReading { size = 128; setup() }
    GS(S("ArrayOfUUIDsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfUUIDsWriting { size = 128; setup() }
    GS(S("ArrayOfUUIDsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfYearMonthsReading { size = 128; setup() }
    GS(S("ArrayOfYearMonthsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfYearMonthsWriting { size = 128; setup() }
    GS(S("ArrayOfYearMonthsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfYearsReading { size = 128; setup() }
    GS(S("ArrayOfYearsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfYearsWriting { size = 128; setup() }
    GS(S("ArrayOfYearsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfZonedDateTimesReading { size = 128; setup() }
    GS(S("ArrayOfZonedDateTimesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfZonedDateTimesWriting { size = 128; setup() }
    GS(S("ArrayOfZonedDateTimesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneIdsReading { size = 128; setup() }
    GS(S("ArrayOfZoneIdsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneIdsWriting { size = 128; setup() }
    GS(S("ArrayOfZoneIdsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneOffsetsReading { size = 128; setup() }
    GS(S("ArrayOfZoneOffsetsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ArrayOfZoneOffsetsWriting { size = 128; setup() }
    GS(S("ArrayOfZoneOffsetsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new Base16Reading { size = 128; setup() }
    GS(S("Base16Reading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new Base16Writing { size = 128; setup() }
    GS(S("Base16Writing")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new Base64Reading { size = 128; setup() }
    GS(S("Base64Reading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new Base64Writing { size = 128; setup() }
    GS(S("Base64Writing")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new BigDecimalReading { size = 128; setup() }
    GS(S("BigDecimalReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new BigDecimalWriting { size = 128; setup() }
    GS(S("BigDecimalWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new BigIntReading { size = 128; setup() }
    GS(S("BigIntReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new BigIntWriting { size = 128; setup() }
    GS(S("BigIntWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new BitSetReading { size = 128; setup() }
    GS(S("BitSetReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new BitSetWriting { size = 128; setup() }
    GS(S("BitSetWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new ExtractFieldsReading { size = 128; setup() }
    GS(S("ExtractFieldsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new GeoJSONReading
    GS(S("GeoJSONReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new GeoJSONWriting
    GS(S("GeoJSONWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new GitHubActionsAPIReading
    GS(S("GitHubActionsAPIReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new GitHubActionsAPIWriting
    GS(S("GitHubActionsAPIWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new GoogleMapsAPIPrettyPrinting
    GS(S("GoogleMapsAPIPrettyPrinting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new GoogleMapsAPIReading
    GS(S("GoogleMapsAPIReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new GoogleMapsAPIWriting
    GS(S("GoogleMapsAPIWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new IntMapOfBooleansReading { size = 128; setup() }
    GS(S("IntMapOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new IntMapOfBooleansWriting { size = 128; setup() }
    GS(S("IntMapOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new IntReading
    GS(S("IntReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new IntWriting
    GS(S("IntWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ListOfBooleansReading { size = 128; setup() }
    GS(S("ListOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new ListOfBooleansWriting { size = 128; setup() }
    GS(S("ListOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MapOfIntsToBooleansReading { size = 128; setup() }
    GS(S("MapOfIntsToBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MapOfIntsToBooleansWriting { size = 128; setup() }
    GS(S("MapOfIntsToBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new MissingRequiredFieldsReading
    GS(S("MissingRequiredFieldsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaWithoutDump")(benchmark.jsoniterScalaWithoutDump()),
      B("jsoniterScalaWithStacktrace")(benchmark.jsoniterScalaWithStacktrace()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableBitSetReading { size = 128; setup() }
    GS(S("MutableBitSetReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new MutableBitSetWriting { size = 128; setup() }
    GS(S("MutableBitSetWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansReading { size = 128; setup() }
    GS(S("MutableLongMapOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new MutableLongMapOfBooleansWriting { size = 128; setup() }
    GS(S("MutableLongMapOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansReading { size = 128; setup() }
    GS(S("MutableMapOfIntsToBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala())
    ))
  }, {
    val benchmark = new MutableMapOfIntsToBooleansWriting { size = 128; setup() }
    GS(S("MutableMapOfIntsToBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc())
    ))
  }, {
    val benchmark = new MutableSetOfIntsReading { size = 128; setup() }
    GS(S("MutableSetOfIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new MutableSetOfIntsWriting { size = 128; setup() }
    GS(S("MutableSetOfIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new NestedStructsReading { size = 128; setup() }
    GS(S("NestedStructsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new NestedStructsWriting { size = 128; setup() }
    GS(S("NestedStructsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new OpenRTBReading
    GS(S("OpenRTBReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new OpenRTBWriting
    GS(S("OpenRTBWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new PrimitivesReading
    GS(S("PrimitivesReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new PrimitivesWriting
    GS(S("PrimitivesWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new SetOfIntsReading { size = 128; setup() }
    GS(S("SetOfIntsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new SetOfIntsWriting { size = 128; setup() }
    GS(S("SetOfIntsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new StringOfAsciiCharsReading { size = 128; setup() }
    GS(S("StringOfAsciiCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new StringOfAsciiCharsWriting { size = 128; setup() }
    GS(S("StringOfAsciiCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new StringOfEscapedCharsReading { size = 128; setup() }
    GS(S("StringOfEscapedCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new StringOfEscapedCharsWriting { size = 128; setup() }
    GS(S("StringOfEscapedCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new StringOfNonAsciiCharsReading { size = 128; setup() }
    GS(S("StringOfNonAsciiCharsReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new StringOfNonAsciiCharsWriting { size = 128; setup() }
    GS(S("StringOfNonAsciiCharsWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new TwitterAPIReading
    GS(S("TwitterAPIReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new TwitterAPIWriting
    GS(S("TwitterAPIWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle())
    ))
  }, {
    val benchmark = new VectorOfBooleansReading { size = 128; setup() }
    GS(S("VectorOfBooleansReading")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  }, {
    val benchmark = new VectorOfBooleansWriting { size = 128; setup() }
    GS(S("VectorOfBooleansWriting")(
      B("avSystemGenCodec")(benchmark.avSystemGenCodec()),
      B("borer")(benchmark.borer()),
      B("circe")(benchmark.circe()),
      B("jsoniterScala")(benchmark.jsoniterScala()),
      B("jsoniterScalaPrealloc")(benchmark.jsoniterScalaPrealloc()),
      B("uPickle")(benchmark.uPickle()),
      B("zioJson")(benchmark.zioJson())
    ))
  })
}