package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.YearMonth

class ArrayOfYearMonthsReading extends ArrayOfYearMonthsBenchmark {
  @Benchmark
  def borer(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[YearMonth]].value
  }

  @Benchmark
  def circe(): Array[YearMonth] = {
    import io.circe.jawn._

    decodeByteArray[Array[YearMonth]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[YearMonth]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[YearMonth]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Array[YearMonth] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[YearMonth]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Array[YearMonth] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[YearMonth]]
  }

  @Benchmark
  def jsoniterScala(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[YearMonth]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[YearMonth]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[YearMonth] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[YearMonth]]
  }

  @Benchmark
  def sprayJson(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[YearMonth]]
  }

  @Benchmark
  def uPickle(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[YearMonth]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[YearMonth]])
  }

  @Benchmark
  def zioBlocks(): Array[YearMonth] = ZioBlocksCodecs.arrayOfYearMonthsCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Array[YearMonth] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[YearMonth]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Array[YearMonth] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    arrayOfYearMonthsCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}