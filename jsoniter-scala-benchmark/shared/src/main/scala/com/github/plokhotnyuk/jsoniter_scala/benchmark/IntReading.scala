package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._
import zio.json.DecoderOps

class IntReading extends IntBenchmark {
  @Benchmark
  def avSystemGenCodec(): Int = JsonStringInput.read[Int](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Int = io.bullet.borer.Json.decode(jsonBytes).to[Int].value

  @Benchmark
  def circe(): Int = decode[Int](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Int = io.circe.jawn.decodeByteArray[Int](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Int].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Int = dslJsonDecode[Int](jsonBytes)

  @Benchmark
  def jacksonScala(): Int = jacksonMapper.readValue[Int](jsonBytes)

  @Benchmark
  def jsoniterScala(): Int = readFromArray[Int](jsonBytes)(intCodec)

  @Benchmark
  def playJson(): Int = Json.parse(jsonBytes).as[Int]

  @Benchmark
  def playJsonJsoniter(): Int = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Int])

  @Benchmark
  def smithy4s(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    readFromArray[Int](jsonBytes)(intJCodec)
  }

  @Benchmark
  def sprayJson(): Int = JsonParser(jsonBytes).convertTo[Int]

  @Benchmark
  def uPickle(): Int = read[Int](jsonBytes)

  @Benchmark
  def weePickle(): Int = FromJson(jsonBytes).transform(ToScala[Int])

  @Benchmark
  def zioJson(): Int = new String(jsonBytes, UTF_8).fromJson[Int].fold(sys.error, identity)
}