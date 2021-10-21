package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfLocalDatesReading extends ArrayOfLocalDatesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[LocalDate] = JsonStringInput.read[Array[LocalDate]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[LocalDate] = io.bullet.borer.Json.decode(jsonBytes).to[Array[LocalDate]].value

  @Benchmark
  def circe(): Array[LocalDate] = decode[Array[LocalDate]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[LocalDate] = {
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[LocalDate]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[LocalDate] = dslJsonDecode[Array[LocalDate]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[LocalDate] = jacksonMapper.readValue[Array[LocalDate]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[LocalDate] = readFromArray[Array[LocalDate]](jsonBytes)

  @Benchmark
  def playJson(): Array[LocalDate] = Json.parse(jsonBytes).as[Array[LocalDate]]

  @Benchmark
  def playJsonJsoniter(): Array[LocalDate] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[LocalDate]])

  @Benchmark
  def sprayJson(): Array[LocalDate] = JsonParser(jsonBytes).convertTo[Array[LocalDate]]

  @Benchmark
  def uPickle(): Array[LocalDate] = read[Array[LocalDate]](jsonBytes)

  @Benchmark
  def weePickle(): Array[LocalDate] = FromJson(jsonBytes).transform(ToScala[Array[LocalDate]])

  @Benchmark
  def zioJson(): Array[LocalDate] = new String(jsonBytes, UTF_8).fromJson[Array[LocalDate]].fold(sys.error, identity)
}