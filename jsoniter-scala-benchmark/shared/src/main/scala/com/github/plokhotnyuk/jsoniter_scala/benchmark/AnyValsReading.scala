package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.FlatSprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class AnyValsReading extends AnyValsBenchmark {
  @Benchmark
  def avSystemGenCodec(): AnyVals = JsonStringInput.read[AnyVals](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): AnyVals = io.bullet.borer.Json.decode(jsonBytes).to[AnyVals].value

  @Benchmark
  def circe(): AnyVals = decode[AnyVals](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): AnyVals = io.circe.jawn.decodeByteArray[AnyVals](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[AnyVals].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON throws com.dslplatform.json.ParsingException: Expecting '{' to start decoding com.github.plokhotnyuk.jsoniter_scala.benchmark.ByteVal. Found 1 at position: 6, following: `{"b":1`, before: `,"s":2,"i":3,"l":4,"`
  @Benchmark
  def dslJsonScala(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[AnyVals](jsonBytes)
  }
*/
  @Benchmark
  def jacksonScala(): AnyVals = jacksonMapper.readValue[AnyVals](jsonBytes)

  @Benchmark
  def jsoniterScala(): AnyVals = readFromArray[AnyVals](jsonBytes)

  @Benchmark
  def playJson(): AnyVals = Json.parse(jsonBytes).as[AnyVals]

  @Benchmark
  def playJsonJsoniter(): AnyVals = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[AnyVals])

  @Benchmark
  def sprayJson(): AnyVals = JsonParser(jsonBytes).convertTo[AnyVals]

  @Benchmark
  def uPickle(): AnyVals = read[AnyVals](jsonBytes)

  @Benchmark
  def weePickle(): AnyVals = FromJson(jsonBytes).transform(ToScala[AnyVals])

  @Benchmark
  def zioJson(): AnyVals = new String(jsonBytes, UTF_8).fromJson[AnyVals].fold(sys.error, identity)
}
