package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.HashCodeCollider._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.NinnyFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps
import scala.collection.immutable.ArraySeq

class ExtractFieldsReading extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    val value = """{"number":0.0,"boolean":false,"string":null}"""
    jsonString = zeroHashCodeStrings.map(s => writeToString(s)(JsoniterScalaCodecs.stringCodec)).take(size)
      .mkString("""{"s":"s",""", s""":$value,""", s""":$value,"i":1}""")
    jsonBytes = jsonString.getBytes(UTF_8)
  }

  @Benchmark
  def avSystemGenCodec(): ExtractFields = JsonStringInput.read[ExtractFields](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): ExtractFields = io.bullet.borer.Json.decode(jsonBytes).to[ExtractFields].value

  @Benchmark
  def circe(): ExtractFields = decode[ExtractFields](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): ExtractFields = io.circe.jawn.decodeByteArray[ExtractFields](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[ExtractFields].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): ExtractFields = dslJsonDecode[ExtractFields](jsonBytes)

  @Benchmark
  def jacksonScala(): ExtractFields = jacksonMapper.readValue[ExtractFields](jsonBytes)

  @Benchmark
  def jsoniterScala(): ExtractFields = readFromArray[ExtractFields](jsonBytes)

  @Benchmark
  def ninnyJson(): ExtractFields = {
    import nrktkt.ninny.Json

    Json.parseArray(ArraySeq.unsafeWrapArray(jsonBytes)).to[ExtractFields].get
  }

  @Benchmark
  def playJson(): ExtractFields = Json.parse(jsonBytes).as[ExtractFields]

  @Benchmark
  def playJsonJsoniter(): ExtractFields = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[ExtractFields])

  @Benchmark
  def smithy4s(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    readFromArray[ExtractFields](jsonBytes)
  }

  @Benchmark
  def sprayJson(): ExtractFields = JsonParser(jsonBytes).convertTo[ExtractFields]

  @Benchmark
  def uPickle(): ExtractFields = read[ExtractFields](jsonBytes)

  @Benchmark
  def weePickle(): ExtractFields = FromJson(jsonBytes).transform(ToScala[ExtractFields])

  @Benchmark
  def zioJson(): ExtractFields = new String(jsonBytes, UTF_8).fromJson[ExtractFields].fold(sys.error, identity)
}