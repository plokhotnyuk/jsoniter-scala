package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.OffsetTime

class ArrayOfOffsetTimesReading extends ArrayOfOffsetTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[OffsetTime] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[OffsetTime]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[OffsetTime]].value
  }

  @Benchmark
  def circe(): Array[OffsetTime] = {
    import io.circe.jawn._

    decodeByteArray[Array[OffsetTime]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[OffsetTime]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[OffsetTime]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[OffsetTime]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[OffsetTime] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[OffsetTime]]
  }

  @Benchmark
  def json4sNative(): Array[OffsetTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[OffsetTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[OffsetTime]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[OffsetTime]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[OffsetTime] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[OffsetTime]]
  }

  @Benchmark
  def sprayJson(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[OffsetTime]]
  }

  @Benchmark
  def uPickle(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[OffsetTime]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[OffsetTime]])
  }

  @Benchmark
  def zioBlocks(): Array[OffsetTime] = ZioBlocksCodecs.arrayOfOffsetTimesCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Array[OffsetTime] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[OffsetTime]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    arrayOfOffsetTimesCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}