package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
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

class ADTReading extends ADTBenchmark {
  @Benchmark
  def avSystemGenCodec(): ADTBase = JsonStringInput.read[ADTBase](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): ADTBase = io.bullet.borer.Json.decode(jsonBytes).to[ADTBase].value

  @Benchmark
  def circe(): ADTBase = decode[ADTBase](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[ADTBase].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): ADTBase = jacksonMapper.readValue[ADTBase](jsonBytes)

  @Benchmark
  def jsoniterScala(): ADTBase = readFromArray[ADTBase](jsonBytes)

  @Benchmark
  def playJson(): ADTBase = Json.parse(jsonBytes).as[ADTBase](adtFormat)

  @Benchmark
  def playJsonJsoniter(): ADTBase = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[ADTBase](adtFormat))

  @Benchmark
  def sprayJson(): ADTBase = JsonParser(jsonBytes).convertTo[ADTBase](adtBaseJsonFormat)

  @Benchmark
  def uPickle(): ADTBase = read[ADTBase](jsonBytes)

  @Benchmark
  def weePickle(): ADTBase = FromJson(jsonBytes).transform(ToScala[ADTBase])

  @Benchmark
  def zioJson(): ADTBase = new String(jsonBytes, UTF_8).fromJson[ADTBase].fold(sys.error, identity)
}
