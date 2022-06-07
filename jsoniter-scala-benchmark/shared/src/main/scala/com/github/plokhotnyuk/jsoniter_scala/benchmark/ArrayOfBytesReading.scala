package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import io.circe.Decoder
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._
import zio.json.DecoderOps

class ArrayOfBytesReading extends ArrayOfBytesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringInput.read[Array[Byte]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Byte]](byteArrayDec).value

  @Benchmark
  def circe(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[Byte] = io.circe.jawn.decodeByteArray[Array[Byte]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[Byte]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON expects a base64 string for the byte array
  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonDecode[Array[Byte]](jsonBytes)
*/
  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonByteArrayMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes)

  @Benchmark
  def playJson(): Array[Byte] = Json.parse(jsonBytes).as[Array[Byte]]

  @Benchmark
  def playJsonJsoniter(): Array[Byte] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Byte]])

  @Benchmark
  def smithy4sJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._

    readFromArray[Array[Byte]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Byte] = JsonParser(jsonBytes).convertTo[Array[Byte]]

  @Benchmark
  def uPickle(): Array[Byte] = read[Array[Byte]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Byte] = FromJson(jsonBytes).transform(ToScala[Array[Byte]])

  @Benchmark
  def zioJson(): Array[Byte] = new String(jsonBytes, UTF_8).fromJson[Array[Byte]].fold(sys.error, identity)
}