package com.github.plokhotnyuk.jsoniter_scala

import java.util

import org.scalatest.{Matchers, WordSpec}

class CodecBenchmarkSpec extends WordSpec with Matchers {
  val benchmark = new CodecBenchmark
  
  "CodecBenchmark" should {
    "deserialize properly" in {
      assert(benchmark.readAnyRefsJackson() == benchmark.anyRefsObj)
      assert(benchmark.readAnyRefsJsoniter() == benchmark.anyRefsObj)
      assert(benchmark.readIterablesJackson() == benchmark.iterablesObj)
      assert(benchmark.readIterablesJsoniter() == benchmark.iterablesObj)
      //FIXME: Jackson-module-scala instantiates Map instead of HashMap
      // assert(benchmark.readMapsJackson() == benchmark.mapsObj)
      assert(benchmark.readMapsJsoniter() == benchmark.mapsObj)
      assert(benchmark.readPrimitivesJackson() == benchmark.primitivesObj)
      assert(benchmark.readPrimitivesJsoniter() == benchmark.primitivesObj)
    }
    "serialize properly" in {
      assert(util.Arrays.equals(benchmark.writeAnyRefsJackson(), benchmark.anyRefsJson))
      assert(util.Arrays.equals(benchmark.writeAnyRefsJsoniter(), benchmark.anyRefsJson))
      assert(util.Arrays.equals(benchmark.writeIterablesJackson(), benchmark.iterablesJson))
      assert(util.Arrays.equals(benchmark.writeIterablesJsoniter(), benchmark.iterablesJson))
      assert(util.Arrays.equals(benchmark.writeMapsJackson(), benchmark.mapsJson))
      assert(util.Arrays.equals(benchmark.writeMapsJsoniter(), benchmark.mapsJson))
      //FIXME: by default Jackson stores Char as String, while Jsoniter stores it as Int
      // assert(util.Arrays.equals(benchmark.writePrimitivesJackson(), benchmark.primitivesJson))
      assert(util.Arrays.equals(benchmark.writePrimitivesJsoniter(), benchmark.primitivesJson))
    }
  }
}
