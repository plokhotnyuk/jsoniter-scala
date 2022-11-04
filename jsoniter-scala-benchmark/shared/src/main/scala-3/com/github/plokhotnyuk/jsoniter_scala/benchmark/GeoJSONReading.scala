package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON._
import org.openjdk.jmh.annotations.Benchmark

class GeoJSONReading extends GeoJSONBenchmark {
  @Benchmark
  def borer(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[GeoJSON].value
  }

  @Benchmark
  def jacksonScala(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
    jacksonMapper.readValue[GeoJSON](jsonBytes)
  }
/* FIXME: json4s.jackson throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
  @Benchmark
  def json4sJackson(): GeoJSON = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJsonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[GeoJSON]
  }
*/
/* FIXME: json4s.native throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
  @Benchmark
  def json4sNative(): GeoJSON = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJsonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[GeoJSON]
  }
*/
  @Benchmark
  def jsoniterScala(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[GeoJSON](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[GeoJSON](jsonBytes)
  }

  @Benchmark
  def weePickle(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[GeoJSON])
  }
}