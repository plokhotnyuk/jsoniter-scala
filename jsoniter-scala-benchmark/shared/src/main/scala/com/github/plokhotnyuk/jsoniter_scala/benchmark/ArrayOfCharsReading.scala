package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfCharsReading extends ArrayOfCharsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Char] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[Char]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[Char] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Char]].value
  }

  @Benchmark
  def circe(): Array[Char] = {
    import io.circe.jawn._

    decodeByteArray[Array[Char]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Char] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Char]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[Char] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Char]](jsonBytes)
  }
/* FIXME: json4s.jackson throws org.json4s.MappingException: Do not know how to convert JString(3) into byte
  @Benchmark
  def json4sJackson(): Array[Byte] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._

    mapper.readValue(jsonBytes, classOf[JValue]).extract[Array[Byte]]
  }
*/
/* FIXME: json4s.native throws org.json4s.MappingException: Do not know how to convert JString(3) into byte
  @Benchmark
  def json4sNative(): Array[Byte] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Byte]]
  }
*/
  @Benchmark
  def jsoniterScala(): Array[Char] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Char]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Char] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Char]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[Char] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[Char]]
  }

  @Benchmark
  def sprayJson(): Array[Char] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Char]]
  }

  @Benchmark
  def uPickle(): Array[Char] = {
    import upickle.default._

    read[Array[Char]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Char] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Char]])
  }

  @Benchmark
  def zioJson(): Array[Char] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[Char]].fold(sys.error, identity)
  }
}