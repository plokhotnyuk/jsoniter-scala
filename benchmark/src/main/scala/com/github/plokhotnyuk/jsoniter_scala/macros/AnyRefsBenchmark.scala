package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets._

import com.dslplatform.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

class AnyRefsBenchmark extends CommonParams {
  val obj: AnyRefs = AnyRefs("s", 1, Some("os"))
  val jsonString: String = """{"s":"s","bd":1,"os":"os"}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): AnyRefs = decode[AnyRefs](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): AnyRefs = dslJson.deserialize(classOf[AnyRefs], jsonBytes, jsonBytes.length)

  @Benchmark
  def readDslJsonScala(): AnyRefs = dslJson.decode[AnyRefs](jsonBytes)

  @Benchmark
  def readJacksonScala(): AnyRefs = jacksonMapper.readValue[AnyRefs](jsonBytes)

  @Benchmark
  def readJsoniterScala(): AnyRefs = readFromArray[AnyRefs](jsonBytes)

  @Benchmark
  def readPlayJson(): AnyRefs = Json.parse(jsonBytes).as[AnyRefs](anyRefsFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonJava(): Array[Byte] = {
    preallocatedWriter.reset()
    dslJson.serialize(preallocatedWriter, classOf[AnyRefs], obj)
    java.util.Arrays.copyOf(preallocatedBuf, preallocatedWriter.size())
  }

  @Benchmark
  def writeDslJsonJavaPrealloc(): Int = {
    preallocatedWriter.reset()
    dslJson.serialize(preallocatedWriter, classOf[AnyRefs], obj)
    preallocatedWriter.size()
  }

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    dslJson.encode(obj, baos)
    baos.toByteArray
  }

  @Benchmark
  def writeDslJsonScalaPrealloc(): Int = {
    preallocatedOutputStream.count = 0
    dslJson.encode(obj, preallocatedOutputStream)
    preallocatedOutputStream.count
  }

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(anyRefsFormat))
}