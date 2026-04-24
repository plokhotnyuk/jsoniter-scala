/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.OffsetDateTime

class ArrayOfOffsetDateTimesReading extends ArrayOfOffsetDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[OffsetDateTime] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[OffsetDateTime]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[OffsetDateTime]].value
  }

  @Benchmark
  def circe(): Array[OffsetDateTime] = {
    import io.circe.jawn._

    decodeByteArray[Array[OffsetDateTime]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[OffsetDateTime]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[OffsetDateTime] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[OffsetDateTime]]
  }

  @Benchmark
  def json4sNative(): Array[OffsetDateTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[OffsetDateTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[OffsetDateTime]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[OffsetDateTime] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[OffsetDateTime]]
  }

  @Benchmark
  def smithy4sJson(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[OffsetDateTime]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[OffsetDateTime]]
  }

  @Benchmark
  def uPickle(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[OffsetDateTime]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[OffsetDateTime]])
  }

  @Benchmark
  def zioBlocks(): Array[OffsetDateTime] =
    ZioBlocksCodecs.arrayOfOffsetDateTimesCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Array[OffsetDateTime] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[OffsetDateTime]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    arrayOfOffsetDateTimesCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}