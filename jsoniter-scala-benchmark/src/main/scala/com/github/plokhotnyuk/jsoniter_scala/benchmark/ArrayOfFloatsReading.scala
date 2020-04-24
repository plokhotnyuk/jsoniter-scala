package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.jsoniter.input.JsoniterJavaParser
//import com.rallyhealth.weejson.v1.jackson.FromJson
//import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfFloatsReading extends ArrayOfFloatsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Float] = JsonStringInput.read[Array[Float]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[Float] =
    io.bullet.borer.Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[Float]].value

  @Benchmark
  def circe(): Array[Float] = decode[Array[Float]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def dslJsonScala(): Array[Float] = dslJsonDecode[Array[Float]](jsonBytes)
*/
/* FIXME: Jackson Scala parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def jacksonScala(): Array[Float] = jacksonMapper.readValue[Array[Float]](jsonBytes)
*/
/* FIXME: Jsoniter Java parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def jsoniterJava(): Array[Float] = JsoniterJavaParser.parse[Array[Float]](jsonBytes, classOf[Array[Float]])
*/
  @Benchmark
  def jsoniterScala(): Array[Float] = readFromArray[Array[Float]](jsonBytes)

  @Benchmark
  def playJson(): Array[Float] = Json.parse(jsonBytes).as[Array[Float]]

  @Benchmark
  def sprayJson(): Array[Float] = JsonParser(jsonBytes).convertTo[Array[Float]]

  @Benchmark
  def uPickle(): Array[Float] = read[Array[Float]](jsonBytes)
/* FIXME: weePickle parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def weePickle(): Array[Float] = FromJson(jsonBytes).transform(ToScala[Array[Float]])
*/
  @Benchmark
  def sjson(): Array[Float] = {
    import sjsonnew.support.scalajson.unsafe._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SJsonEncodersDecoders._
    Converter.fromJsonUnsafe[Array[Float]](Parser.parseFromByteArray(jsonBytes).get)
  }
}
