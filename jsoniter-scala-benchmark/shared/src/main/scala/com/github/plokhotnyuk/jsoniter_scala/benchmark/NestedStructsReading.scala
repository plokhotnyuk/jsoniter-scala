package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import io.circe.Decoder
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class NestedStructsReading extends NestedStructsBenchmark {
  @Benchmark
  def avSystemGenCodec(): NestedStructs = JsonStringInput.read[NestedStructs](new String(jsonBytes, UTF_8))
/* FIXME: Borer throws io.bullet.borer.Borer$Error$Overflow: This JSON parser does not support more than 64 Array/Object nesting levels
  @Benchmark
  def borer(): NestedStructs = io.bullet.borer.Json.decode(jsonBytes).to[NestedStructs].value
*/
  @Benchmark
  def circe(): NestedStructs = decode[NestedStructs](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): NestedStructs = io.circe.jawn.decodeByteArray[NestedStructs](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[NestedStructs].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): NestedStructs = dslJsonDecode[NestedStructs](jsonBytes)

  @Benchmark
  def jacksonScala(): NestedStructs = jacksonMapper.readValue[NestedStructs](jsonBytes)

  @Benchmark
  def jsoniterScala(): NestedStructs = readFromArray[NestedStructs](jsonBytes)

  @Benchmark
  def playJson(): NestedStructs = Json.parse(jsonBytes).as[NestedStructs]

  @Benchmark
  def playJsonJsoniter(): NestedStructs = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[NestedStructs])

  @Benchmark
  def smithy4s(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    readFromArray[NestedStructs](jsonBytes)
  }

  @Benchmark
  def sprayJson(): NestedStructs = JsonParser(jsonBytes).convertTo[NestedStructs](nestedStructsJsonFormat)

  @Benchmark
  def uPickle(): NestedStructs = read[NestedStructs](jsonBytes)

  @Benchmark
  def weePickle(): NestedStructs = FromJson(jsonBytes).transform(ToScala[NestedStructs])
/* FIXME: zio-json throws java.lang.NullPointerException: Cannot invoke "zio.json.JsonDecoder.unsafeDecode(scala.collection.immutable.List, zio.json.internal.RetractReader)" because "this.A$1" is null
  @Benchmark
  def zioJson(): NestedStructs = new String(jsonBytes, UTF_8).fromJson[NestedStructs].fold(sys.error, identity)
*/
}