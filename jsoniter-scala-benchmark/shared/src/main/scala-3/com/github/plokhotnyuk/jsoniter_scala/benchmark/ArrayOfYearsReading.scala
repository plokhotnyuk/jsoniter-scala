package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.Year

class ArrayOfYearsReading extends ArrayOfYearsBenchmark {
  @Benchmark
  def borer(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Year]].value
  }

  @Benchmark
  def circe(): Array[Year] = {
    import io.circe.jawn._

    decodeByteArray[Array[Year]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Year]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Year]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Array[Year] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Year]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Array[Year] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Year]]
  }

  @Benchmark
  def jsoniterScala(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Year]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Year]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[Year] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[play.api.libs.json.JsValue](jsonBytes).as[Array[Year]]
  }

  @Benchmark
  def sprayJson(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Year]]
  }

  @Benchmark
  def uPickle(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[Year]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Year] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Year]])
  }

  @Benchmark
  def zioJson(): Array[Year] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[Year]].fold(sys.error, identity)
  }
}