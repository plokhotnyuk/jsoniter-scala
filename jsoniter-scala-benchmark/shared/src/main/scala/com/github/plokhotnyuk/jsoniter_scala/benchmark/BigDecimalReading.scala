package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
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

class BigDecimalReading extends BigDecimalBenchmark {
  @Benchmark
  def avSystemGenCodec(): BigDecimal = JsonStringInput.read[BigDecimal](new String(jsonBytes, UTF_8), jsonOptions)

  @Benchmark
  def borer(): BigDecimal = io.bullet.borer.Json.decode(jsonBytes).withConfig(decodingConfig).to[BigDecimal].value

  @Benchmark
  def circe(): BigDecimal = decode[BigDecimal](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): BigDecimal = io.circe.jawn.decodeByteArray[BigDecimal](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[BigDecimal].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): BigDecimal = dslJsonDecode[BigDecimal](jsonBytes)

  @Benchmark
  def jacksonScala(): BigDecimal = jacksonMapper.readValue[BigDecimal](jsonBytes)

  @Benchmark
  def jsoniterScala(): BigDecimal = readFromArray[BigDecimal](jsonBytes)(bigDecimalCodec)
/* FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def playJson(): BigDecimal = Json.parse(jsonBytes).as[BigDecimal]
*/
/* FIXME: smithy4s: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def smithy4s(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    readFromArray[BigDecimal](jsonBytes)(bigDecimalJCodec)
  }
*/
  @Benchmark
  def sprayJson(): BigDecimal = JsonParser(jsonBytes, jsonParserSettings).convertTo[BigDecimal]

  @Benchmark
  def uPickle(): BigDecimal = read[BigDecimal](jsonBytes)

  @Benchmark
  def weePickle(): BigDecimal = FromJson(jsonBytes).transform(ToScala[BigDecimal])
}