package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfJavaEnumsReading extends ArrayOfJavaEnumsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Suit] = JsonStringInput.read[Array[Suit]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Suit] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Suit]].value

  @Benchmark
  def circe(): Array[Suit] = decode[Array[Suit]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[Suit] = io.circe.jawn.decodeByteArray[Array[Suit]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[Suit]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Suit] = dslJsonDecode[Array[Suit]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Suit] = jacksonMapper.readValue[Array[Suit]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Suit] = readFromArray[Array[Suit]](jsonBytes)

  @Benchmark
  def playJson(): Array[Suit] = Json.parse(jsonBytes).as[Array[Suit]]

  @Benchmark
  def playJsonJsoniter(): Array[Suit] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Suit]])

  @Benchmark
  def sprayJson(): Array[Suit] = JsonParser(jsonBytes).convertTo[Array[Suit]]

  @Benchmark
  def uPickle(): Array[Suit] = read[Array[Suit]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Suit] = FromJson(jsonBytes).transform(ToScala[Array[Suit]])
}