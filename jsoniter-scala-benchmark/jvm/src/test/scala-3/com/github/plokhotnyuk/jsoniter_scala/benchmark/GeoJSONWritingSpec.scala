package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONWritingSpec extends BenchmarkSpecBase {
  def benchmark: GeoJSONWriting = new GeoJSONWriting {
    setup()
  }

  "GeoJSONWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.jacksonScala()) shouldBe b.jsonString1
      //FIXME: json4s.jackson throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
      //toString(b.json4sJackson()) shouldBe b.jsonString1
      //FIXME: json4s.native throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
      //toString(b.json4sNative()) shouldBe b.jsonString1
      //FIXME: jsoniter-scala serializes `properties: Map[String, String] = Map.empty` as `Iterable`
      //toString(b.jsoniterScala()) shouldBe b.jsonString1
      //FIXME: jsoniter-scala serializes `properties: Map[String, String] = Map.empty` as `Iterable`
      //toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString1
    }
  }
}