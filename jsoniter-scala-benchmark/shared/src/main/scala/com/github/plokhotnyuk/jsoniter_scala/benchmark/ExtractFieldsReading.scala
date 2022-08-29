package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.bullet.borer.Json
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}

class ExtractFieldsReading extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.HashCodeCollider._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import java.nio.charset.StandardCharsets.UTF_8

    val value = """{"number":0.0,"boolean":false,"string":null}"""
    jsonString = zeroHashCodeStrings.map(s => writeToString(s)(JsoniterScalaCodecs.stringCodec)).take(size)
      .mkString("""{"s":"s",""", s""":$value,""", s""":$value,"i":1}""")
    jsonBytes = jsonString.getBytes(UTF_8)
  }

  @Benchmark
  def avSystemGenCodec(): ExtractFields = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[ExtractFields](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[ExtractFields].value
  }

  @Benchmark
  def circe(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[ExtractFields](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeBorer(): ExtractFields = {
    import io.bullet.borer.compat.circe._ // the borer codec for the circe AST
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._

    Json.decode(jsonBytes).to[ExtractFields].value
  }

  @Benchmark
  def circeJawn(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[ExtractFields](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[ExtractFields].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[ExtractFields](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[ExtractFields](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[ExtractFields](jsonBytes)
  }

  @Benchmark
  def ninnyJson(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.NinnyFormats._
    import nrktkt.ninny.Json
    import scala.collection.immutable.ArraySeq

    Json.parseArray(ArraySeq.unsafeWrapArray(jsonBytes)).to[ExtractFields].get
  }

  @Benchmark
  def playJson(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[ExtractFields]
  }

  @Benchmark
  def playJsonJsoniter(): ExtractFields = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[ExtractFields]
  }

  @Benchmark
  def smithy4sJson(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[ExtractFields](jsonBytes)
  }

  @Benchmark
  def sprayJson(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[ExtractFields]
  }

  @Benchmark
  def uPickle(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[ExtractFields](jsonBytes)
  }

  @Benchmark
  def weePickle(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[ExtractFields])
  }

  @Benchmark
  def zioJson(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[ExtractFields].fold(sys.error, identity)
  }
}