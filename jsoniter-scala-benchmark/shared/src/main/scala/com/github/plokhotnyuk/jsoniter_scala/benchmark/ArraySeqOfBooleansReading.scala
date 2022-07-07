package com.github.plokhotnyuk.jsoniter_scala.benchmark

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
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.immutable.ArraySeq

class ArraySeqOfBooleansReading extends ArrayOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): ArraySeq[Boolean] = JsonStringInput.read[ArraySeq[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): ArraySeq[Boolean] = io.bullet.borer.Json.decode(jsonBytes).to[ArraySeq[Boolean]].value

  @Benchmark
  def circe(): ArraySeq[Boolean] = decode[ArraySeq[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): ArraySeq[Boolean] = io.circe.jawn.decodeByteArray[ArraySeq[Boolean]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): ArraySeq[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[ArraySeq[Boolean]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON doesn't support parsing of ArraySeq
  @Benchmark
  def dslJsonScala(): ArraySeq[Boolean] = dslJsonDecode[ArraySeq[Boolean]](jsonBytes)
*/
/* FIXME: Jackson throws java.lang.ClassCastException: class scala.collection.immutable.Vector2 cannot be cast to class scala.collection.immutable.ArraySeq
  @Benchmark
  def jacksonScala(): ArraySeq[Boolean] = jacksonMapper.readValue[ArraySeq[Boolean]](jsonBytes)
*/
  @Benchmark
  def jsoniterScala(): ArraySeq[Boolean] = readFromArray[ArraySeq[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): ArraySeq[Boolean] = Json.parse(jsonBytes).as[ArraySeq[Boolean]]

  @Benchmark
  def playJsonJsoniter(): ArraySeq[Boolean] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[ArraySeq[Boolean]])

  @Benchmark
  def smithy4sJson(): ArraySeq[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._

    readFromArray[ArraySeq[Boolean]](jsonBytes)
  }
/* FIXME: spray-json doesn't support parsing of ArraySeq
  @Benchmark
  def sprayJson(): ArraySeq[Boolean] = JsonParser(jsonBytes).convertTo[ArraySeq[Boolean]]
*/
  @Benchmark
  def uPickle(): ArraySeq[Boolean] = read[ArraySeq[Boolean]](jsonBytes)

  @Benchmark
  def weePickle(): ArraySeq[Boolean] = FromJson(jsonBytes).transform(ToScala[ArraySeq[Boolean]])
/* FIXME: zio-json doesn't support parsing of ArraySeq
  @Benchmark
  def zioJson(): ArraySeq[Boolean] = new String(jsonBytes, UTF_8).fromJson[ArraySeq[Boolean]].fold(sys.error, identity)
*/
}