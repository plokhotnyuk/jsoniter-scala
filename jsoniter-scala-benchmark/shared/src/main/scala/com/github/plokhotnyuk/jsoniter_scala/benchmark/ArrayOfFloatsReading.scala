package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Decoder
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfFloatsReading extends ArrayOfFloatsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Float] = JsonStringInput.read[Array[Float]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Float] = io.bullet.borer.Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[Float]].value

  @Benchmark
  def circe(): Array[Float] = decode[Array[Float]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[Float] = io.circe.jawn.decodeByteArray[Array[Float]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[Float] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[Float]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def dslJsonScala(): Array[Float] = dslJsonDecode[Array[Float]](jsonBytes)
*/
  @Benchmark
  def jacksonScala(): Array[Float] = jacksonMapper.readValue[Array[Float]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Float] = readFromArray[Array[Float]](jsonBytes)

  @Benchmark
  def playJson(): Array[Float] = Json.parse(jsonBytes).as[Array[Float]]

  @Benchmark
  def playJsonJsoniter(): Array[Float] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Float]])

  @Benchmark
  def smithy4sJson(): Array[Float] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._

    readFromArray[Array[Float]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Float] = JsonParser(jsonBytes).convertTo[Array[Float]]

  @Benchmark
  def uPickle(): Array[Float] = read[Array[Float]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Float] = FromJson(jsonBytes).transform(ToScala[Array[Float]])

  @Benchmark
  def zioJson(): Array[Float] = new String(jsonBytes, UTF_8).fromJson[Array[Float]].fold(sys.error, identity)
}