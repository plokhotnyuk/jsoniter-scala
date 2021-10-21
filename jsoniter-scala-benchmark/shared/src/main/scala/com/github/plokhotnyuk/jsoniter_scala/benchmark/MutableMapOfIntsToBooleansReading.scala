package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import scala.collection.mutable

class MutableMapOfIntsToBooleansReading extends MutableMapOfIntsToBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.Map[Int, Boolean] =
    JsonStringInput.read[mutable.Map[Int, Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): mutable.Map[Int, Boolean] =
    decode[mutable.Map[Int, Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): mutable.Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[mutable.Map[Int, Boolean]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): mutable.Map[Int, Boolean] = dslJsonDecode[mutable.Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): mutable.Map[Int, Boolean] = jacksonMapper.readValue[mutable.Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): mutable.Map[Int, Boolean] = readFromArray[mutable.Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def playJson(): mutable.Map[Int, Boolean] = Json.parse(jsonBytes).as[mutable.Map[Int, Boolean]]

  @Benchmark
  def playJsonJsoniter(): mutable.Map[Int, Boolean] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[mutable.Map[Int, Boolean]])

  @Benchmark
  def weePickle(): mutable.Map[Int, Boolean] = FromJson(jsonBytes).transform(ToScala[mutable.Map[Int, Boolean]])
}