package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class Base64Reading extends Base64Benchmark {
  @Benchmark
  def borer(): Array[Byte] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Byte]].value
  }

  @Benchmark
  def jacksonScala(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Byte]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Byte] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Base64Json4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Byte]]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  def json4sNative(): Array[Byte] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Base64Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Byte]]
  }
*/
  @Benchmark
  def jsoniterScala(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Byte]](jsonBytes, tooLongStringConfig)(base64Codec)
  }

  @Benchmark
  def smithy4sJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Byte]](jsonBytes, tooLongStringConfig)(base64JCodec)
  }

  @Benchmark
  def weePickle(): Array[Byte] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Byte]])
  }
}