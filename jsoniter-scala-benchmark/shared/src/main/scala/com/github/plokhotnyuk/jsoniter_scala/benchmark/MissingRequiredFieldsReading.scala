package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.{JsResultException, Json}
import spray.json._
import zio.json.DecoderOps

class MissingRequiredFieldsReading extends CommonParams {
  var jsonString: String = """{}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

  @Benchmark
  def avSystemGenCodec(): String =
    try {
      JsonStringInput.read[MissingRequiredFields](new String(jsonBytes, UTF_8)).toString // toString() should not be called
    } catch {
      case ex: GenCodec.ReadFailure => ex.getMessage
    }

  @Benchmark
  def borer(): String =
    try {
      io.bullet.borer.Json.decode(jsonBytes).to[MissingRequiredFields].value.toString // toString() should not be called
    } catch {
      case ex: io.bullet.borer.Borer.Error[_] => ex.getMessage
    }

  @Benchmark
  def circe(): String =
    decode[MissingRequiredFields](new String(jsonBytes, UTF_8)).fold(_.getMessage, _.toString) // toString() should not be called

  @Benchmark
  def dslJsonScala(): String =
    try {
      dslJsonDecode[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: IOException => ex.getMessage
    }

  @Benchmark
  def jacksonScala(): String =
    try {
      jacksonMapper.readValue[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: MismatchedInputException => ex.getMessage
    }

  @Benchmark
  def jsoniterScala(): String =
    try {
      readFromArray[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }

  @Benchmark
  def jsoniterScalaWithoutDump(): String =
    try {
      readFromArray[MissingRequiredFields](jsonBytes, exceptionWithoutDumpConfig).toString // toString() should not be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }

  @Benchmark
  def jsoniterScalaWithStacktrace(): String =
    try {
      readFromArray[MissingRequiredFields](jsonBytes, exceptionWithStacktraceConfig).toString // toString() should not be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }

  @Benchmark
  def playJson(): String =
    try {
      Json.parse(jsonBytes).as[MissingRequiredFields](missingReqFieldsFormat).toString // toString() should not be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }

  @Benchmark
  def playJsonJsoniter(): String =
    try {
      PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[MissingRequiredFields](missingReqFieldsFormat).toString) // toString() should not be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }

  @Benchmark
  def sprayJson(): String =
    try {
      JsonParser(jsonBytes).convertTo[MissingRequiredFields](missingReqFieldsJsonFormat).toString // toString() should not be called
    } catch {
      case ex: spray.json.DeserializationException => ex.getMessage
    }

  @Benchmark
  def uPickle(): String =
    try {
      read[MissingRequiredFields](jsonBytes).toString // toString() should not be called
    } catch {
      case ex: upickle.core.AbortException => ex.getMessage
    }

  @Benchmark
  def weePickle(): String =
    try {
      FromJson(jsonBytes).transform(ToScala[MissingRequiredFields]).toString // toString() should not be called
    } catch {
      case ex: com.rallyhealth.weepickle.v1.core.TransformException => ex.getMessage
    }

  @Benchmark
  def zioJson(): String =
    new String(jsonBytes, UTF_8).fromJson[MissingRequiredFields].fold(identity, _.toString) // toString() should not be called
}