package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import io.circe.Decoder
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
//import play.api.libs.json.Json
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import spray.json._
import zio.json._

class ArrayOfBigIntsReading extends ArrayOfBigIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[BigInt] = JsonStringInput.read[Array[BigInt]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Array[BigInt] = decode[Array[BigInt]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[BigInt] = io.circe.jawn.decodeByteArray[Array[BigInt]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[BigInt]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def borer(): Array[BigInt] = io.bullet.borer.Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[BigInt]].value

  @Benchmark
  def dslJsonScala(): Array[BigInt] = dslJsonDecode[Array[BigInt]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[BigInt] = jacksonMapper.readValue[Array[BigInt]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[BigInt] = readFromArray[Array[BigInt]](jsonBytes)
/* FIXME: Play-JSON looses significant digits in BigInt values
  @Benchmark
  def playJson(): Array[BigInt] = Json.parse(jsonBytes).as[Array[BigInt]](bigIntArrayFormat)
*/
  @Benchmark
  def smithy4s(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    readFromArray[Array[BigInt]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[BigInt] = JsonParser(jsonBytes).convertTo[Array[BigInt]]

  @Benchmark
  def uPickle(): Array[BigInt] = read[Array[BigInt]](jsonBytes)

  @Benchmark
  def weePickle(): Array[BigInt] = FromJson(jsonBytes).transform(ToScala[Array[BigInt]])

  @Benchmark
  def zioJson(): Array[BigInt] = new String(jsonBytes, UTF_8).fromJson[Array[BigInt]].fold(sys.error, identity)
}