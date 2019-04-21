package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.json._
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.{JsResultException, Json}
import spray.json._

case class MissingRequiredFields(
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) s: String,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) i: Int)

class MissingRequiredFieldsReading extends CommonParams {
  var jsonString: String = """{}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

  @Benchmark
  def readAVSystemGenCodec(): String =
    try {
      JsonStringInput.read[MissingRequiredFields](new String(jsonBytes, UTF_8)).toString // toString() should not be called
    } catch {
      case ex: GenCodec.ReadFailure => ex.getMessage
    }

  @Benchmark
  def readCirce(): String =
    decode[MissingRequiredFields](new String(jsonBytes, UTF_8)).fold(_.getMessage, _ => null)

  @Benchmark
  def readDslJsonScala(): String =
    try {
      dslJsonDecode[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: IOException => ex.getMessage
    }

  @Benchmark
  def readJacksonScala(): String =
    try {
      jacksonMapper.readValue[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: MismatchedInputException => ex.getMessage
    }

  @Benchmark
  def readJsoniterScala(): String =
    try {
      readFromArray[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }

  @Benchmark
  def readJsoniterScalaWithoutDump(): String =
    try {
      readFromArray[MissingRequiredFields](jsonBytes, exceptionWithoutDumpConfig).toString // toString() should not be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }

  @Benchmark
  def readJsoniterScalaWithStacktrace(): String =
    try {
      readFromArray[MissingRequiredFields](jsonBytes, exceptionWithStacktraceConfig).toString // toString() should not be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }

  @Benchmark
  def readPlayJson(): String =
    try {
      Json.parse(jsonBytes).as[MissingRequiredFields](missingReqFieldsFormat).toString // toString() should not be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }

  @Benchmark
  def readSprayJson(): String =
    try {
      JsonParser(jsonBytes).convertTo[MissingRequiredFields](missingReqFieldsJsonFormat).toString // toString() should not be called
    } catch {
      case ex: spray.json.DeserializationException => ex.getMessage
    }

  @Benchmark
  def readUPickle(): String =
    try {
      read[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: upickle.core.AbortException => ex.getMessage
    }
}