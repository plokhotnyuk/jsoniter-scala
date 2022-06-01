package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json.JsonStringInput
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core.readFromArray
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser.decode
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import smithy4s.ByteArray
import spray.json.JsonParser
import zio.json.DecoderOps

class Base64Reading extends Base64Benchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringInput.read[Array[Byte]](new String(jsonBytes, UTF_8), jsonBase64Options)

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Byte]].value

  @Benchmark
  def circe(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8))(base64D5r).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[Byte] = io.circe.jawn.decodeByteArray[Array[Byte]](jsonBytes)(base64D5r).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[Byte]](base64D5r).decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonDecode[Array[Byte]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes, tooLongStringConfig)(base64Codec)

  @Benchmark
  def playJson(): Array[Byte] = Json.parse(jsonBytes).as[Array[Byte]](base64Format)

  @Benchmark
  def playJsonJsoniter(): Array[Byte] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Byte]](base64Format))

  @Benchmark
  def smithy4s(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    readFromArray[Array[Byte]](jsonBytes, tooLongStringConfig)(base64JCodec)
  }

  @Benchmark
  def sprayJson(): Array[Byte] = JsonParser(jsonBytes).convertTo[Array[Byte]](base64JsonFormat)

  @Benchmark
  def uPickle(): Array[Byte] = read[Array[Byte]](jsonBytes)(base64ReadWriter)

  @Benchmark
  def weePickle(): Array[Byte] = FromJson(jsonBytes).transform(ToScala[Array[Byte]])

  @Benchmark
  def zioJson(): Array[Byte] = new String(jsonBytes, UTF_8).fromJson[Array[Byte]](base64C3c.decoder).fold(sys.error, identity)
}