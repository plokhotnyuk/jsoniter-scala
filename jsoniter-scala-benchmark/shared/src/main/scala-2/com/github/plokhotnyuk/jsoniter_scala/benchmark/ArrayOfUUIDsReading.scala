package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.util.UUID

class ArrayOfUUIDsReading extends ArrayOfUUIDsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[UUID] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[UUID]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[UUID]].value
  }

  @Benchmark
  def circe(): Array[UUID] = {
    import io.circe.jawn._

    decodeByteArray[Array[UUID]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[UUID]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[UUID]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[UUID]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[UUID] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[UUID]]
  }

  @Benchmark
  def json4sNative(): Array[UUID] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[UUID]]
  }

  @Benchmark
  def jsoniterScala(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[UUID]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[UUID] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[UUID]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[UUID] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[UUID]]
  }

  @Benchmark
  def smithy4sJson(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[UUID]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[UUID]]
  }

  @Benchmark
  def uPickle(): Array[UUID] = {
    import upickle.default._

    read[Array[UUID]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[UUID]])
  }

  @Benchmark
  def zioJson(): Array[UUID] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[UUID]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    arrayOfUUIDsCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}