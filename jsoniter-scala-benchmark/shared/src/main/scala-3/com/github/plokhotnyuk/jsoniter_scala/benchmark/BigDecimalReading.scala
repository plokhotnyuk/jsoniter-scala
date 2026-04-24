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

class BigDecimalReading extends BigDecimalBenchmark {
/* FIXME: borer parses up to 200 digits only
  @Benchmark
  def borer(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[BigDecimal].value
  }
*/
  @Benchmark
  def circe(): BigDecimal = {
    import io.circe.jawn._

    decodeByteArray[BigDecimal](jsonBytes).fold(throw _, identity)
  }
/* FIXME: circe-jsoniter parses up to 308 digits only
  @Benchmark
  def circeJsoniter(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[BigDecimal].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }
*/
  @Benchmark
  def jacksonScala(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BigDecimal](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): BigDecimal = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    bigNumberMapper.readValue[JValue](jsonBytes, jValueType).extract[BigDecimal]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  @annotation.nowarn
  def json4sNative(): BigDecimal = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[BigDecimal]
  }
*/
  @Benchmark
  def jsoniterScala(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigDecimal](jsonBytes)(bigDecimalCodec)
  }
/* FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def playJson(): BigDecimal = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[BigDecimal]
  }
*/
/* FIXME: smithy4sJson parses up to 308 digits only
  @Benchmark
  def smithy4sJson(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigDecimal](jsonBytes)(bigDecimalJCodec)
  }
*/
  @Benchmark
  def sprayJson(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes, jsonParserSettings).convertTo[BigDecimal]
  }

  @Benchmark
  def uPickle(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[BigDecimal](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[BigDecimal])
  }

  @Benchmark
  def zioBlocks(): BigDecimal = ZioBlocksCodecs.bigDecimalCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJsonCodecs._
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[BigDecimal](bigDecimalC3c.decoder).fold(sys.error, identity)
  }
}