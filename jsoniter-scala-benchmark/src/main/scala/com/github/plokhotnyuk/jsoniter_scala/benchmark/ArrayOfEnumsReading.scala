package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfEnumsReading extends ArrayOfEnumsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[SuitEnum] = JsonStringInput.read[Array[SuitEnum]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[SuitEnum] = io.bullet.borer.Json.decode(jsonBytes).to[Array[SuitEnum]].value

  @Benchmark
  def circe(): Array[SuitEnum] = decode[Array[SuitEnum]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[SuitEnum] = jacksonMapper.readValue[Array[SuitEnum]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[SuitEnum] = readFromArray[Array[SuitEnum]](jsonBytes)

  @Benchmark
  def playJson(): Array[SuitEnum] = Json.parse(jsonBytes).as[Array[SuitEnum]]
/* FIXME: ScalikeJackson throws java.lang.Exception: No Value[] ScalaJacksonFormat found for json input
  @Benchmark
  def scalikeJackson(): Array[SuitEnum] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[Array[SuitEnum]]
  }
*/
  @Benchmark
  def sprayJson(): Array[SuitEnum] = JsonParser(jsonBytes).convertTo[Array[SuitEnum]]

  @Benchmark
  def uPickle(): Array[SuitEnum] = read[Array[SuitEnum]](jsonBytes)
}