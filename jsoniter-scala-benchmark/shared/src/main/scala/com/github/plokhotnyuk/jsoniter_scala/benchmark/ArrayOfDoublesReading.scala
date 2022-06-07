package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
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
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfDoublesReading extends ArrayOfDoublesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Double] = JsonStringInput.read[Array[Double]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Double] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Double]].value

  @Benchmark
  def circe(): Array[Double] = decode[Array[Double]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[Double] = io.circe.jawn.decodeByteArray[Array[Double]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[Double]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Double] = dslJsonDecode[Array[Double]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Double] = jacksonMapper.readValue[Array[Double]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Double] = readFromArray[Array[Double]](jsonBytes)

  @Benchmark
  def playJson(): Array[Double] = Json.parse(jsonBytes).as[Array[Double]]

  @Benchmark
  def playJsonJsoniter(): Array[Double] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Double]])

  @Benchmark
  def smithy4sJson(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._

    readFromArray[Array[Double]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Double] = JsonParser(jsonBytes).convertTo[Array[Double]]

  @Benchmark
  def uPickle(): Array[Double] = read[Array[Double]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Double] = FromJson(jsonBytes).transform(ToScala[Array[Double]])

  @Benchmark
  def zioJson(): Array[Double] = new String(jsonBytes, UTF_8).fromJson[Array[Double]].fold(sys.error, identity)
}