package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.immutable.BitSet

class BitSetReading extends BitSetBenchmark {
  @Benchmark
  def circe(): BitSet = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[BitSet](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): BitSet = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[BitSet].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): BitSet = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BitSet](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Set[Int] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Set[Int]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Set[Int] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Set[Int]]
  }

  @Benchmark
  def jsoniterScala(): BitSet = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BitSet](jsonBytes)
  }

  @Benchmark
  def playJson(): BitSet = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[BitSet](bitSetFormat)
  }

  @Benchmark
  def playJsonJsoniter(): BitSet = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[BitSet](bitSetFormat)
  }
}