package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.macros.HashCodeCollider.zeroHashCodeStrings
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import spray.json._
import upickle.default._

case class ExtractFields(s: String, i: Int)

class ExtractFieldsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jsonString = """{"s":"s","x":""" + "9" * size + ""","i":1}"""
    jsonBytes = jsonString.getBytes(UTF_8)
  }

  @Benchmark
  def readAVSystemGenCodec(): ExtractFields = JsonStringInput.read[ExtractFields](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): ExtractFields = decode[ExtractFields](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): ExtractFields = decodeDslJson[ExtractFields](jsonBytes)

  @Benchmark
  def readJacksonScala(): ExtractFields = jacksonMapper.readValue[ExtractFields](jsonBytes)

  @Benchmark
  def readJsoniterScala(): ExtractFields = readFromArray[ExtractFields](jsonBytes)

  @Benchmark
  def readPlayJson(): ExtractFields = Json.parse(jsonBytes).as[ExtractFields](extractFieldsFormat)

  @Benchmark
  def readSpray(): ExtractFields = JsonParser(jsonBytes).convertTo[ExtractFields](extractFieldsJsonFormat)

  @Benchmark
  def readUPickle(): ExtractFields = read[ExtractFields](jsonBytes)
}