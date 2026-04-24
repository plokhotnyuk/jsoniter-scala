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

class VectorOfBooleansReading extends VectorOfBooleansBenchmark {
  @Benchmark
  def borer(): Vector[Boolean] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Vector[Boolean]].value
  }

  @Benchmark
  def circe(): Vector[Boolean] = {
    import io.circe.jawn._

    decodeByteArray[Vector[Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Vector[Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Vector[Boolean]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Vector[Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Vector[Boolean]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Vector[Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Vector[Boolean]]
  }

  @Benchmark
  def jsoniterScala(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Vector[Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): Vector[Boolean] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Vector[Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): Vector[Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Vector[Boolean]]
  }

  @Benchmark
  def smithy4sJson(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Vector[Boolean]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Vector[Boolean]]
  }

  @Benchmark
  def uPickle(): Vector[Boolean] = {
    import upickle.default._

    read[Vector[Boolean]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Vector[Boolean]])
  }

  @Benchmark
  def zioBlocks(): Vector[Boolean] = ZioBlocksCodecs.vectorOfBooleansCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Vector[Boolean] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Vector[Boolean]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Vector[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    vectorOfBooleansCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}