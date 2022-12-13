package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.ZoneOffset

class ArrayOfZoneOffsetsReading extends ArrayOfZoneOffsetsBenchmark {
  @Benchmark
  def borer(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[ZoneOffset]].value
  }

  @Benchmark
  def circe(): Array[ZoneOffset] = {
    import io.circe.jawn._

    decodeByteArray[Array[ZoneOffset]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[ZoneOffset]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[ZoneOffset]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Array[ZoneOffset] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[ZoneOffset]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Array[ZoneOffset] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[ZoneOffset]]
  }

  @Benchmark
  def jsoniterScala(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[ZoneOffset]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[ZoneOffset]]
  }

  @Benchmark
  def sprayJson(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[ZoneOffset]]
  }

  @Benchmark
  def uPickle(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[ZoneOffset]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[ZoneOffset]])
  }

  @Benchmark
  def zioJson(): Array[ZoneOffset] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[ZoneOffset]].fold(sys.error, identity)
  }
}