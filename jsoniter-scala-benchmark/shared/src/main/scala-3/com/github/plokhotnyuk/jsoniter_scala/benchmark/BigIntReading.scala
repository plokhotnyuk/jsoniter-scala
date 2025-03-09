package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class BigIntReading extends BigIntBenchmark {
/* FIXME: borer parses up to 200 digits only
  @Benchmark
  def borer(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[BigInt].value
  }
*/
  @Benchmark
  def circe(): BigInt = {
    import io.circe.jawn._

    decodeByteArray[BigInt](jsonBytes).fold(throw _, identity)
  }
/* FIXME: circe-jsoniter parses up to 308 digits only
  @Benchmark
  def circeJsoniter(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[BigInt].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }
*/
  @Benchmark
  def jacksonScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BigInt](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): BigInt = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    bigNumberMapper.readValue[JValue](jsonBytes, jValueType).extract[BigInt]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  @annotation.nowarn
  def json4sNative(): BigInt = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[BigInt]
  }
*/
  @Benchmark
  def jsoniterScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigInt](jsonBytes)(bigIntCodec)
  }
/* FIXME: Play-JSON looses significant digits in BigInt values
  @Benchmark
  def playJson(): BigInt = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[BigInt]
  }
*/
/* FIXME: smithy4sJson parses up to 308 digits only
  @Benchmark
  def smithy4sJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigInt](jsonBytes)(bigIntJCodec)
  }
*/
  @Benchmark
  def sprayJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes, jsonParserSettings).convertTo[BigInt]
  }

  @Benchmark
  def uPickle(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[BigInt](jsonBytes)
  }

  @Benchmark
  def weePickle(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[BigInt])
  }

  @Benchmark
  def zioJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJsonCodecs._
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[BigInt](bigIntC3c.decoder).fold(sys.error, identity)
  }
}