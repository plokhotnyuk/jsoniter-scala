package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders.decodingConfig
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
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
import zio.json._

class ArrayOfBigDecimalsReading extends ArrayOfBigDecimalsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[BigDecimal] =
    JsonStringInput.read[Array[BigDecimal]](new String(jsonBytes, UTF_8), jsonOptions)

  @Benchmark
  def borer(): Array[BigDecimal] =
    io.bullet.borer.Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[BigDecimal]].value

  @Benchmark
  def circe(): Array[BigDecimal] = decode[Array[BigDecimal]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[BigDecimal]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[BigDecimal] = dslJsonDecode[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[BigDecimal] = jacksonMapper.readValue[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[BigDecimal] = readFromArray[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def playJson(): Array[BigDecimal] = Json.parse(jsonBytes).as[Array[BigDecimal]]

  @Benchmark
  def playJsonJsoniter(): Array[BigDecimal] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[BigDecimal]])

  @Benchmark
  def sprayJson(): Array[BigDecimal] = JsonParser(jsonBytes).convertTo[Array[BigDecimal]]

  @Benchmark
  def uPickle(): Array[BigDecimal] = read[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def weePickle(): Array[BigDecimal] = FromJson(jsonBytes).transform(ToScala[Array[BigDecimal]])

  @Benchmark
  def zioJson(): Array[BigDecimal] = new String(jsonBytes, UTF_8).fromJson[Array[BigDecimal]].fold(sys.error, identity)
}