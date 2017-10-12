package com.github.plokhotnyuk.jsoniter_scala

import org.scalatest.{Matchers, WordSpec}

class CodecBenchmarkSpec extends WordSpec with Matchers {
  val benchmark = new CodecBenchmark
  
  "CodecBenchmark" should {
    "deserialize properly" in {
      benchmark.readAnyRefsJackson() shouldBe benchmark.anyRefsObj
      benchmark.readAnyRefsJsoniter() shouldBe benchmark.anyRefsObj
      benchmark.readAnyRefsPlay() shouldBe benchmark.anyRefsObj
      //FIXME: Jackson-module-scala doesn`t support parsing of bitsets
      //benchmark.readBitSetsJackson() shouldBe benchmark.bitSetsObj
      benchmark.readBitSetsJsoniter() shouldBe benchmark.bitSetsObj
      benchmark.readBitSetsPlay() shouldBe benchmark.bitSetsObj
      benchmark.readIterablesJackson() shouldBe benchmark.iterablesObj
      benchmark.readIterablesJsoniter() shouldBe benchmark.iterablesObj
      benchmark.readIterablesPlay() shouldBe benchmark.iterablesObj
      //FIXME: Jackson-module-scala parse keys as String
      // benchmark.readMapsJackson() shouldBe benchmark.mapsObj
      benchmark.readMapsJsoniter() shouldBe benchmark.mapsObj
      benchmark.readMapsPlay() shouldBe benchmark.mapsObj
      //FIXME: Jackson-module-scala parse keys as String
      //benchmark.readMutableMapsJackson() shouldBe benchmark.mutableMapsObj
      benchmark.readMutableMapsJsoniter() shouldBe benchmark.mutableMapsObj
      benchmark.readMutableMapsPlay() shouldBe benchmark.mutableMapsObj
      //FIXME: Jackson-module-scala doesn`t support parsing of int & long maps
      //benchmark.readIntAndLongMapsJackson() shouldBe benchmark.intAndLongMapsObj
      benchmark.readIntAndLongMapsJsoniter() shouldBe benchmark.intAndLongMapsObj
      benchmark.readIntAndLongMapsPlay() shouldBe benchmark.intAndLongMapsObj
      benchmark.readPrimitivesJackson() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesJsoniter() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesPlay() shouldBe benchmark.primitivesObj
    }
    "serialize properly" in {
      toString(benchmark.writeAnyRefsJackson()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeAnyRefsJsoniter()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeAnyRefsPlay()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeBitSetsJackson()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeBitSetsJsoniter()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeBitSetsPlay()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeIterablesJackson()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeIterablesJsoniter()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeIterablesPlay()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeMapsJackson()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsJsoniter()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsPlay()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMutableMapsJackson()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsJsoniter()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsPlay()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeIntAndLongMapsJackson()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsJsoniter()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsPlay()) shouldBe toString(benchmark.intAndLongMapsJson)
      //FIXME jackson also should serialize char as int
      //toString(benchmark.writePrimitivesJackson()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesJsoniter()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesPlay()) shouldBe toString(benchmark.primitivesJson)
    }
  }

  def toString(json: Array[Byte]): String = new String(json, "UTF-8")
}
