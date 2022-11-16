package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ADTReading extends ADTBenchmark {
  @Benchmark
  def borer(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[ADTBase].value
  }

  @Benchmark
  def circe(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[ADTBase](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[ADTBase].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[ADTBase](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): ADTBase = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ADTJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[ADTBase]
  }

  @Benchmark
  def json4sNative(): ADTBase = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ADTJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[ADTBase]
  }

  @Benchmark
  def jsoniterScala(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[ADTBase](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[ADTBase](jsonBytes)
  }

  @Benchmark
  def sprayJson(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[ADTBase](adtBaseJsonFormat)
  }
/* FIXME: uPuckle hungs in endless loop
  @Benchmark
  def uPickle(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[ADTBase](jsonBytes)
  }
*/
  @Benchmark
  def weePickle(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[ADTBase])
  }

  @Benchmark
  def zioJson(): ADTBase = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import zio.json.JsonDecoder._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[ADTBase].fold(sys.error, identity)
  }
}