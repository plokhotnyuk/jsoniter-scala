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

class MapOfIntsToBooleansReading extends MapOfIntsToBooleansBenchmark {
  @Benchmark
  def circe(): Map[Int, Boolean] = {
    import io.circe.jawn._

    decodeByteArray[Map[Int, Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Map[Int, Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Map[Int, Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Map[Int, Boolean]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Map[Int, Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Map[Int, Boolean]]
  }

  @Benchmark
  def jsoniterScala(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): Map[Int, Boolean] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Map[Int, Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): Map[Int, Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[play.api.libs.json.JsValue](jsonBytes).as[Map[Int, Boolean]]
  }

  @Benchmark
  def smithy4sJson(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Map[Int, Boolean]](jsonBytes)
  }
/* FIXME: Spray-JSON throws spray.json.DeserializationException: Expected Int as JsNumber, but got "-1"
  @Benchmark
  def sprayJson(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Map[Int, Boolean]]
  }
*/
  @Benchmark
  def uPickle(): Map[Int, Boolean] = {
    import upickle.default._

    read[Map[Int, Boolean]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Map[Int, Boolean]])
  }

  @Benchmark
  def zioBlocks(): Map[Int, Boolean] =
    ZioBlocksCodecs.mapOfIntsToBooleansCodec.decode(jsonBytes).fold(throw _, identity)

  @Benchmark
  def zioJson(): Map[Int, Boolean] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Map[Int, Boolean]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    mapOfIntsToBooleansCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}