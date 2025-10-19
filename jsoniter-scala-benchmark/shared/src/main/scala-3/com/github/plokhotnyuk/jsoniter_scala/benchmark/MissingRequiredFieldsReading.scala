package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class MissingRequiredFieldsReading extends MissingRequiredFieldsBenchmark {
  @Benchmark
  def borer(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Borer.Error
    import io.bullet.borer.Json

    try {
      Json.decode(jsonBytes).to[MissingRequiredFields].value.toString // toString shouldn't be called
    } catch {
      case ex: Error[?] => ex.getMessage
    }
  }

  @Benchmark
  def circe(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[MissingRequiredFields](jsonBytes).fold(_.getMessage, _.toString) // toString shouldn't be called
  }

  @Benchmark
  def circeJsoniter(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[MissingRequiredFields].decodeJson(readFromArray(jsonBytes)).fold(_.getMessage, _.toString) // toString shouldn't be called
  }

  @Benchmark
  def jacksonScala(): String = {
    import tools.jackson.databind.exc.MismatchedInputException
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    try {
      jacksonMapper.readValue[MissingRequiredFields](jsonBytes).toString // toString shouldn't be called
    } catch {
      case ex: MismatchedInputException => ex.getMessage
    }
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): String = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    try {
      mapper.readValue[JValue](jsonBytes, jValueType).extract[MissingRequiredFields].toString// toString shouldn't be called
    } catch {
      case ex: MappingException => ex.getMessage
    }
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): String = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    try {
      parse(new String(jsonBytes, UTF_8)).extract[MissingRequiredFields].toString// toString shouldn't be called
    } catch {
      case ex: MappingException => ex.getMessage
    }
  }

  @Benchmark
  def jsoniterScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    try {
      readFromArray[MissingRequiredFields](jsonBytes).toString // toString shouldn't be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }
  }

  @Benchmark
  def jsoniterScalaWithoutDump(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    try {
      readFromArray[MissingRequiredFields](jsonBytes, exceptionWithoutDumpConfig).toString // toString shouldn't be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }
  }

  @Benchmark
  def jsoniterScalaWithStacktrace(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    try {
      readFromArray[MissingRequiredFields](jsonBytes, exceptionWithStacktraceConfig).toString // toString shouldn't be called
    } catch {
      case ex: JsonReaderException => ex.getMessage
    }
  }

  @Benchmark
  def playJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json._

    try {
      Json.parse(jsonBytes).as[MissingRequiredFields](missingReqFieldsFormat).toString // toString shouldn't be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }
  }

  @Benchmark
  def playJsonJsoniter(): String = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import play.api.libs.json.JsResultException

    try {
      readFromArray(jsonBytes).as[MissingRequiredFields](missingReqFieldsFormat).toString // toString shouldn't be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }
  }

  @Benchmark
  def smithy4sJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import smithy4s.codecs.PayloadError

    try {
      readFromArray[MissingRequiredFields](jsonBytes).toString // toString shouldn't be called
    } catch {
      case ex: PayloadError => ex.getMessage
    }
  }

  @Benchmark
  def sprayJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    try {
      JsonParser(jsonBytes).convertTo[MissingRequiredFields](missingReqFieldsJsonFormat).toString // toString shouldn't be called
    } catch {
      case ex: DeserializationException => ex.getMessage
    }
  }

  @Benchmark
  def uPickle(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
    import upickle.core.AbortException

    try {
      read[MissingRequiredFields](jsonBytes, trace = false).toString // toString shouldn't be called
    } catch {
      case ex: AbortException => ex.getMessage
    }
  }

  @Benchmark
  def weePickle(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala
    import com.rallyhealth.weepickle.v1.core.TransformException

    try {
      FromJson(jsonBytes).transform(ToScala[MissingRequiredFields]).toString // toString shouldn't be called
    } catch {
      case ex: TransformException => ex.getMessage
    }
  }

  @Benchmark
  def zioJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJsonCodecs._
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[MissingRequiredFields].fold(identity, _.toString) // toString shouldn't be called
  }

  @Benchmark
  def zioSchemaJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    missingRequiredFieldsCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(identity, _.toString) // toString shouldn't be called
  }
}