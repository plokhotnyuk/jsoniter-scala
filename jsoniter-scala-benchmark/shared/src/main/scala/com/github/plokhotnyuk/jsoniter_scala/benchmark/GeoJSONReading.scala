package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json._

class GeoJSONReading extends GeoJSONBenchmark {
  @Benchmark
  def avSystemGenCodec(): GeoJSON = JsonStringInput.read[GeoJSON](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): GeoJSON = io.bullet.borer.Json.decode(jsonBytes).to[GeoJSON].value

  @Benchmark
  def circe(): GeoJSON = decode[GeoJSON](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[GeoJSON].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): GeoJSON = jacksonMapper.readValue[GeoJSON](jsonBytes)

  @Benchmark
  def jsoniterScala(): GeoJSON = readFromArray[GeoJSON](jsonBytes)

  @Benchmark
  def playJson(): GeoJSON = Json.parse(jsonBytes).as[GeoJSON](geoJSONFormat)

  @Benchmark
  def playJsonJsoniter(): GeoJSON = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[GeoJSON](geoJSONFormat))

  @Benchmark
  def sprayJson(): GeoJSON = JsonParser(jsonBytes).convertTo[GeoJSON](geoJSONJsonFormat)

  @Benchmark
  def uPickle(): GeoJSON = read[GeoJSON](jsonBytes)

  @Benchmark
  def weePickle(): GeoJSON = FromJson(jsonBytes).transform(ToScala[GeoJSON])

  @Benchmark
  def zioJson(): GeoJSON = new String(jsonBytes, UTF_8).fromJson[GeoJSON].fold(sys.error, identity)
}