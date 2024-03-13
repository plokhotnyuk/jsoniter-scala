package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class NestedStructsReading extends NestedStructsBenchmark {
/* FIXME: Borer throws io.bullet.borer.Borer$Error$Overflow: This JSON parser does not support more than 64 Array/Object nesting levels
  @Benchmark
  def borer(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[NestedStructs].value
  }
*/
  @Benchmark
  def circe(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[NestedStructs](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[NestedStructs].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[NestedStructs](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): NestedStructs = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[NestedStructs]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): NestedStructs = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[NestedStructs]
  }

  @Benchmark
  def jsoniterScala(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[NestedStructs](jsonBytes)
  }

  @Benchmark
  def playJson(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[NestedStructs]
  }

  @Benchmark
  def playJsonJsoniter(): NestedStructs = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[NestedStructs]
  }

  @Benchmark
  def smithy4sJson(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[NestedStructs](jsonBytes)
  }

  @Benchmark
  def sprayJson(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[NestedStructs](nestedStructsJsonFormat)
  }

  @Benchmark
  def uPickle(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[NestedStructs](jsonBytes)
  }

  @Benchmark
  def weePickle(): NestedStructs = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[NestedStructs])
  }

  @Benchmark
  def zioJson(): NestedStructs = {
    import zio.json._
    import zio.json.JsonDecoder._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[NestedStructs].fold(sys.error, identity)
  }
}