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
import scala.collection.immutable.LongMap

class LongMapOfBooleansReading extends LongMapOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): LongMap[Boolean] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[LongMap[Boolean]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def circe(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[LongMap[Boolean]](jsonBytes) match {
      case Right(x) => x
      case Left(e) => throw e
    }
  }

  @Benchmark
  def circeJsoniter(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[LongMap[Boolean]].decodeJson(readFromArray(jsonBytes)) match {
      case Right(x) => x
      case Left(e) => throw e
    }
  }

  /* FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for scala.collection.immutable.LongMap[Boolean]
    @Benchmark
    def dslJsonScala(): LongMap[Boolean] = {
      import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

      dslJsonDecode[LongMap[Boolean]](jsonBytes)
    }
  */
  @Benchmark
  def foryJsonScala(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Fory

    Fory.foryJson.fromJson(jsonBytes, Fory.longMapOfBooleansType)
  }

  @Benchmark
  def jacksonScala(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[LongMap[Boolean]](jsonBytes)
  }

  /* FIXME: json4s.jackson throws org.json4s.MappingException: unknown error
    @Benchmark
    def json4sJackson(): LongMap[Boolean] = {
      import org.json4s._
      import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
      import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

      mapper.readValue[JValue](jsonBytes, jValueType).extract[LongMap[Boolean]]
    }
  */
  /* FIXME: json4s.native throws org.json4s.MappingException: unknown error
    @Benchmark
    def json4sNative(): LongMap[Boolean] = {
      import org.json4s._
      import org.json4s.native.JsonMethods._
      import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
      import java.nio.charset.StandardCharsets.UTF_8

      parse(new String(jsonBytes, UTF_8)).extract[LongMap[Boolean]]
    }
  */
  @Benchmark
  def jsoniterScala(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[LongMap[Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[LongMap[Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): LongMap[Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[LongMap[Boolean]]
  }

  @Benchmark
  def uPickle(): LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[LongMap[Boolean]](jsonBytes, trace = false)
  }
}