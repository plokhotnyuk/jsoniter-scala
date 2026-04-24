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

class StringOfAsciiCharsReading extends StringOfAsciiCharsBenchmark {
  @Benchmark
  def borer(): String = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[String].value
  }

  @Benchmark
  def circe(): String = {
    import io.circe.jawn._

    decodeByteArray[String](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[String].decodeJson(readFromArray(jsonBytes, tooLongStringConfig)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[String](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): String = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[String]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  @annotation.nowarn
  def json4sNative(): String = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[String]
  }
*/
  @Benchmark
  def jsoniterScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[String](jsonBytes, tooLongStringConfig)(stringCodec)
  }

  @Benchmark
  def playJson(): String = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[String]
  }

  @Benchmark
  def playJsonJsoniter(): String = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonJsoniterFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[play.api.libs.json.JsValue](jsonBytes, tooLongStringConfig).as[String]
  }

  @Benchmark
  def smithy4sJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[String](jsonBytes, tooLongStringConfig)(stringJCodec)
  }

  @Benchmark
  def sprayJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[String]
  }

  @Benchmark
  def uPickle(): String = {
    import upickle.default._

    read[String](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[String])
  }

  @Benchmark
  def zioBlocks(): String = ZioBlocksCodecs.stringCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): String = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[String].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    stringCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}