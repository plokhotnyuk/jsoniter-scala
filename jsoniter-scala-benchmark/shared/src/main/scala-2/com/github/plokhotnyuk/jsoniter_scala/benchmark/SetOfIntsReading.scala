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

class SetOfIntsReading extends SetOfIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Set[Int] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Set[Int]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Set[Int] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Set[Int]].value
  }

  @Benchmark
  def circe(): Set[Int] = {
    import io.circe.jawn._

    decodeByteArray[Set[Int]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Set[Int]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Set[Int]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Set[Int]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import org.json4s._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Set[Int]]
  }

  @Benchmark
  def json4sNative(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Set[Int]]
  }

  @Benchmark
  def jsoniterScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Set[Int]](jsonBytes)
  }

  @Benchmark
  def playJson(): Set[Int] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Set[Int]]
  }

  @Benchmark
  def playJsonJsoniter(): Set[Int] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Set[Int]]
  }

  @Benchmark
  def smithy4sJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Set[Int]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Set[Int]]
  }

  @Benchmark
  def uPickle(): Set[Int] = {
    import upickle.default._

    read[Set[Int]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Set[Int]])
  }

  @Benchmark
  def zioBlocks(): Set[Int] = ZioBlocksCodecs.setOfIntsCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Set[Int] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Set[Int]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    setOfIntsCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}