package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json
import spray.json._

class BigIntReading extends BigIntBenchmark {
  @Benchmark
  def avSystemGenCodec(): BigInt = JsonStringInput.read[BigInt](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): BigInt = io.bullet.borer.Json.decode(jsonBytes).withConfig(decodingConfig).to[BigInt].value

  @Benchmark
  def circe(): BigInt = decode[BigInt](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): BigInt = io.circe.jawn.decodeByteArray[BigInt](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[BigInt].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): BigInt = dslJsonDecode[BigInt](jsonBytes)

  @Benchmark
  def jacksonScala(): BigInt = jacksonMapper.readValue[BigInt](jsonBytes)

  @Benchmark
  def jsoniterScala(): BigInt = readFromArray[BigInt](jsonBytes)(bigIntCodec)
/* FIXME: Play-JSON looses significant digits in BigInt values
  @Benchmark
  def playJson(): BigInt = Json.parse(jsonBytes).as[BigInt]
*/
  @Benchmark
  def smithy4sJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._

    readFromArray[BigInt](jsonBytes)(bigIntJCodec)
  }

  @Benchmark
  def sprayJson(): BigInt = JsonParser(jsonBytes, jsonParserSettings).convertTo[BigInt]

  @Benchmark
  def uPickle(): BigInt = read[BigInt](jsonBytes)

  @Benchmark
  def weePickle(): BigInt = FromJson(jsonBytes).transform(ToScala[BigInt])
}