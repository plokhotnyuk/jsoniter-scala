package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

import java.time.LocalTime

class ArrayOfLocalTimesReading extends ArrayOfLocalTimesBenchmark {
  @Benchmark
  def borer(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[LocalTime]].value
  }

  @Benchmark
  def circe(): Array[LocalTime] = {
    import io.circe.jawn._

    decodeByteArray[Array[LocalTime]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[LocalTime]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[LocalTime]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Array[LocalTime] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[LocalTime]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Array[LocalTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[LocalTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[LocalTime]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[LocalTime]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[LocalTime] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[LocalTime]]
  }

  @Benchmark
  def sprayJson(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[LocalTime]]
  }

  @Benchmark
  def uPickle(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[LocalTime]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[LocalTime]])
  }

  @Benchmark
  def zioBlocks(): Array[LocalTime] = ZioBlocksCodecs.arrayOfLocalTimesCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Array[LocalTime] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[LocalTime]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    arrayOfLocalTimesCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}