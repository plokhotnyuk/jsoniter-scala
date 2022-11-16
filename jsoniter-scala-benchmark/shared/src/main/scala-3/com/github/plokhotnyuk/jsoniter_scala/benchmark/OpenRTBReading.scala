package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.OpenRTB.BidRequest
import org.openjdk.jmh.annotations.Benchmark

class OpenRTBReading extends OpenRTBBenchmark {
  @Benchmark
  def borer(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[BidRequest].value
  }
/* FIXME: circe requires a custom codec
  @Benchmark
  def circe(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[BidRequest](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[BidRequest].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }
*/
  @Benchmark
  def jacksonScala(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BidRequest](jsonBytes)
  }
/* FIXME: json4s.jackson throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.OpenRTB$BidRequest
  @Benchmark
  def json4sJackson(): BidRequest = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[BidRequest]
  }
*/
/* FIXME: json4s.native throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.OpenRTB$BidRequest
  @Benchmark
  def json4sNative(): BidRequest = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[BidRequest]
  }
*/
  @Benchmark
  def jsoniterScala(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BidRequest](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BidRequest](jsonBytes)
  }
/* FIXME: Spray-JSON throws spray.json.DeserializationException: Object is missing required member 'expdir'
  @Benchmark
  def sprayJson(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json.JsonParser

    JsonParser(jsonBytes).convertTo[BidRequest]
  }
*/
  @Benchmark
  def uPickle(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[BidRequest](jsonBytes)
  }

  @Benchmark
  def weePickle(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[BidRequest])
  }

  @Benchmark
  def zioJson(): BidRequest = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import zio.json.JsonEncoder._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[BidRequest].fold(sys.error, identity)
  }
}