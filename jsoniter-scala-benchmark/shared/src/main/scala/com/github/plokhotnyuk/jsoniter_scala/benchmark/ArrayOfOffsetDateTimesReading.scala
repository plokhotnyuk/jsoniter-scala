package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.OffsetDateTime

class ArrayOfOffsetDateTimesReading extends ArrayOfOffsetDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[OffsetDateTime] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[OffsetDateTime]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[OffsetDateTime]].value
  }

  @Benchmark
  def circe(): Array[OffsetDateTime] = {
    import io.circe.jawn._

    decodeByteArray[Array[OffsetDateTime]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[OffsetDateTime]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[OffsetDateTime] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._

    mapper.readValue(jsonBytes, classOf[JValue]).extract[Array[OffsetDateTime]]
  }

  @Benchmark
  def json4sNative(): Array[OffsetDateTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[OffsetDateTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[OffsetDateTime]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[OffsetDateTime] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[OffsetDateTime]]
  }

  @Benchmark
  def sprayJson(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[OffsetDateTime]]
  }

  @Benchmark
  def uPickle(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[OffsetDateTime] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[OffsetDateTime]])
  }

  @Benchmark
  def zioJson(): Array[OffsetDateTime] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[OffsetDateTime]].fold(sys.error, identity)
  }
}