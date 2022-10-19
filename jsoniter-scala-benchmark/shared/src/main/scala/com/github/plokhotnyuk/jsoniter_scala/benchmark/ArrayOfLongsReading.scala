package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfLongsReading extends ArrayOfLongsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Long] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[Long]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[Long] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Long]].value
  }

  @Benchmark
  def circe(): Array[Long] = {
    import io.circe.jawn._

    decodeByteArray[Array[Long]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Long]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[Long]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Long]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Long] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._

    mapper.readValue(jsonBytes, classOf[JValue]).extract[Array[Long]]
  }

  @Benchmark
  def json4sNative(): Array[Long] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Long]]
  }

  @Benchmark
  def jsoniterScala(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Long]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Long] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Long]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[Long] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[Long]]
  }

  @Benchmark
  def smithy4sJson(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Long]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Long]]
  }

  @Benchmark
  def uPickle(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[Long]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Long] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Long]])
  }

  @Benchmark
  def zioJson(): Array[Long] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[Long]].fold(sys.error, identity)
  }
}