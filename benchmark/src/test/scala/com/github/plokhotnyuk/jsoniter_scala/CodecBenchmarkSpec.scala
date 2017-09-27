package com.github.plokhotnyuk.jsoniter_scala

import org.scalatest.{Matchers, WordSpec}

class CodecBenchmarkSpec extends WordSpec with Matchers {
  val benchmark = new CodecBenchmark
  
  "CodecBenchmark" should {
    "deserialize properly" in {
      benchmark.readAnyRefsJackson() shouldBe benchmark.anyRefsObj
      benchmark.readAnyRefsJsoniter() shouldBe benchmark.anyRefsObj
      benchmark.readIterablesJackson() shouldBe benchmark.iterablesObj
      benchmark.readIterablesJsoniter() shouldBe benchmark.iterablesObj
      //FIXME: Jackson-module-scala instantiates Map instead of HashMap
      // benchmark.readMapsJackson() shouldBe benchmark.mapsObj
      benchmark.readMapsJsoniter() shouldBe benchmark.mapsObj
      benchmark.readPrimitivesJackson() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesJsoniter() shouldBe benchmark.primitivesObj
    }
    "serialize properly" in {
      toString(benchmark.writeAnyRefsJackson()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeAnyRefsJsoniter()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeIterablesJackson()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeIterablesJsoniter()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeMapsJackson()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsJsoniter()) shouldBe toString(benchmark.mapsJson)
      //FIXME: by default Jackson stores Char as String, while Jsoniter stores it as Int
      // toString(benchmark.writePrimitivesJackson()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesJsoniter()) shouldBe toString(benchmark.primitivesJson)
    }
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, "UTF-8")
}
