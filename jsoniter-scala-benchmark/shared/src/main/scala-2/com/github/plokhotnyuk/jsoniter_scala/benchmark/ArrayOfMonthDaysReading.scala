package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.MonthDay

class ArrayOfMonthDaysReading extends ArrayOfMonthDaysBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[MonthDay] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[MonthDay]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[MonthDay]].value
  }

  @Benchmark
  def circe(): Array[MonthDay] = {
    import io.circe.jawn._

    decodeByteArray[Array[MonthDay]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[MonthDay]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[MonthDay]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[MonthDay] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[MonthDay]]
  }

  @Benchmark
  def json4sNative(): Array[MonthDay] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[MonthDay]]
  }

  @Benchmark
  def jsoniterScala(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[MonthDay]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[MonthDay]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[MonthDay] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[MonthDay]]
  }

  @Benchmark
  def sprayJson(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[MonthDay]]
  }

  @Benchmark
  def uPickle(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[MonthDay]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[MonthDay] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[MonthDay]])
  }

  @Benchmark
  def zioJson(): Array[MonthDay] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[MonthDay]].fold(sys.error, identity)
  }
}