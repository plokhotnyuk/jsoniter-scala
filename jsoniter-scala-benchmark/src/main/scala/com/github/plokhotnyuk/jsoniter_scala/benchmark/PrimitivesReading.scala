package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class PrimitivesReading extends PrimitivesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Primitives = JsonStringInput.read[Primitives](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Primitives = io.bullet.borer.Json.decode(jsonBytes).to[Primitives].value

  @Benchmark
  def circe(): Primitives = decode[Primitives](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON cannot create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.Primitives
  @Benchmark
  def dslJsonScala(): Primitives = dslJsonDecode[Primitives](jsonBytes)
*/
  @Benchmark
  def jacksonScala(): Primitives = jacksonMapper.readValue[Primitives](jsonBytes)

  @Benchmark
  def jsoniterScala(): Primitives = readFromArray[Primitives](jsonBytes)

  @Benchmark
  def playJson(): Primitives = Json.parse(jsonBytes).as[Primitives]

  @Benchmark
  def scalikeJackson(): Primitives = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[Primitives]
  }

  @Benchmark
  def sprayJson(): Primitives = JsonParser(jsonBytes).convertTo[Primitives]

  @Benchmark
  def uPickle(): Primitives = read[Primitives](jsonBytes)
}