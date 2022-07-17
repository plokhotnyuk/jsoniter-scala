package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.ZoneId

class ArrayOfZoneIdsReading extends ArrayOfZoneIdsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[ZoneId] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[ZoneId]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[ZoneId]].value
  }

  @Benchmark
  def circe(): Array[ZoneId] = {
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Array[ZoneId]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Array[ZoneId] = {
    import io.circe.jawn._

    decodeByteArray[Array[ZoneId]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[ZoneId]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[ZoneId]](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[ZoneId]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[ZoneId]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[ZoneId] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[ZoneId]]
  }

  @Benchmark
  def sprayJson(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[ZoneId]]
  }

  @Benchmark
  def uPickle(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[ZoneId]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[ZoneId] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[ZoneId]])
  }

  @Benchmark
  def zioJson(): Array[ZoneId] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[ZoneId]].fold(sys.error, identity)
  }
}