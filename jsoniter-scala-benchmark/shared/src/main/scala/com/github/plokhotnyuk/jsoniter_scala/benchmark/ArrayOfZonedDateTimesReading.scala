package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.ZonedDateTime

class ArrayOfZonedDateTimesReading extends ArrayOfZonedDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[ZonedDateTime] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[ZonedDateTime]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[ZonedDateTime]].value
  }

  @Benchmark
  def circe(): Array[ZonedDateTime] = {
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Array[ZonedDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Array[ZonedDateTime] = {
    io.circe.jawn.decodeByteArray[Array[ZonedDateTime]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[ZonedDateTime]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON does not parse preferred timezone
  @Benchmark
  def readDslJsonScala(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[ZonedDateTime]](jsonBytes)
  }
*/
  @Benchmark
  def jacksonScala(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[ZonedDateTime]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[ZonedDateTime] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[ZonedDateTime]]
  }

  @Benchmark
  def json4sNative(): Array[ZonedDateTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[ZonedDateTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[ZonedDateTime]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[ZonedDateTime]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[ZonedDateTime] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[ZonedDateTime]]
  }

  @Benchmark
  def sprayJson(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[ZonedDateTime]]
  }

  @Benchmark
  def uPickle(): Array[ZonedDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[ZonedDateTime]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[ZonedDateTime] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[ZonedDateTime]])
  }

  @Benchmark
  def zioJson(): Array[ZonedDateTime] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[ZonedDateTime]].fold(sys.error, identity)
  }
}