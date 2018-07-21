package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.json._
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.{JsResultException, Json}
import upickle.default._

case class MissingReqFields(
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) s: String,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) i: Int)

class MissingReqFieldBenchmark extends CommonParams {
  var jsonString: String = """{}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

  @Benchmark
  def readAVSystemGenCodec(): String =
    try {
      JsonStringInput.read[MissingReqFields](new String(jsonBytes, UTF_8)).toString // toString() should not be called
    } catch {
      case ex: GenCodec.ReadFailure => ex.getMessage
    }

  @Benchmark
  def readCirce(): String =
    decode[MissingReqFields](new String(jsonBytes, UTF_8)).fold(_.getMessage, _ => null)

  @Benchmark
  def readDslJsonJava(): String =
    try {
      decodeDslJson[MissingReqFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: IOException => ex.getMessage
    }

  @Benchmark
  def readJacksonScala(): String =
    try {
      jacksonMapper.readValue[MissingReqFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: MismatchedInputException => ex.getMessage
    }

  @Benchmark
  def readJsoniterScala(): String =
    try {
      readFromArray[MissingReqFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def readJsoniterScalaWithoutDump(): String =
    try {
      readFromArray[MissingReqFields](jsonBytes, exceptionWithoutDumpConfig).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def readJsoniterScalaWithStacktrace(): String =
    try {
      readFromArray[MissingReqFields](jsonBytes, exceptionWithStacktraceConfig).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def readPlayJson(): String =
    try {
      Json.parse(jsonBytes).as[MissingReqFields](missingReqFieldFormat).toString // toString() should not be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }

  @Benchmark
  def readUPickle(): String =
    try {
      read[MissingReqFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: ujson.JsonProcessingException => ex.getMessage
    }
}