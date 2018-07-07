package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import upickle.default._

case class ExtractFields(s: String, i: Int)

class ExtractFieldsBenchmark extends CommonParams {
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String =
    """{"i1":["1","2"],"s":"s","i2":{"m":[[1,2],[3,4]],"f":true},"i":1,"i3":{"1":1.1,"2":2.2}}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

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
  def readUPickle(): ExtractFields = read[ExtractFields](jsonBytes)
}