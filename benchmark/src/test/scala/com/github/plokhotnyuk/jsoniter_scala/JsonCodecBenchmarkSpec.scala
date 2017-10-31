package com.github.plokhotnyuk.jsoniter_scala

import java.nio.charset.StandardCharsets

import org.scalatest.{Matchers, WordSpec}

class JsonCodecBenchmarkSpec extends WordSpec with Matchers {
  val benchmark = new JsonCodecBenchmark
  
  "CodecBenchmark" should {
    "deserialize properly" in {
      benchmark.readAnyRefsCirce() shouldBe benchmark.anyRefsObj
      benchmark.readAnyRefsJackson() shouldBe benchmark.anyRefsObj
      benchmark.readAnyRefsJsoniter() shouldBe benchmark.anyRefsObj
      benchmark.readAnyRefsPlay() shouldBe benchmark.anyRefsObj
      assertArrays(benchmark.readArraysCirce(), benchmark.arraysObj)
      assertArrays(benchmark.readArraysJackson(), benchmark.arraysObj)
      assertArrays(benchmark.readArraysJsoniter(), benchmark.arraysObj)
      assertArrays(benchmark.readArraysPlay(), benchmark.arraysObj)
      //FIXME: Circe doesn't support parsing of bitsets
      // benchmark.readBitSetsCirce() shouldBe benchmark.bitSetsObj
      benchmark.readBitSetsJackson() shouldBe benchmark.bitSetsObj
      benchmark.readBitSetsJsoniter() shouldBe benchmark.bitSetsObj
      benchmark.readBitSetsPlay() shouldBe benchmark.bitSetsObj
      benchmark.readIterablesCirce() shouldBe benchmark.iterablesObj
      benchmark.readIterablesJackson() shouldBe benchmark.iterablesObj
      benchmark.readIterablesJsoniter() shouldBe benchmark.iterablesObj
      benchmark.readIterablesPlay() shouldBe benchmark.iterablesObj
      benchmark.readMapsCirce() shouldBe benchmark.mapsObj
      //FIXME: Jackson-module-scala parse keys as String
      // benchmark.readMapsJackson() shouldBe benchmark.mapsObj
      benchmark.readMapsJsoniter() shouldBe benchmark.mapsObj
      benchmark.readMapsPlay() shouldBe benchmark.mapsObj
      //FIXME: Circe doesn't support parsing of mutable maps
      // benchmark.readMutableMapsCirce() shouldBe benchmark.mutableMapsObj
      //FIXME: Jackson-module-scala parse keys as String
      //benchmark.readMutableMapsJackson() shouldBe benchmark.mutableMapsObj
      benchmark.readMutableMapsJsoniter() shouldBe benchmark.mutableMapsObj
      benchmark.readMutableMapsPlay() shouldBe benchmark.mutableMapsObj
      //FIXME: Circe doesn't support parsing of int & long maps
      // benchmark.readIntAndLongMapsCirce() shouldBe benchmark.intAndLongMapsObj
      //FIXME: Jackson-module-scala doesn`t support parsing of int & long maps
      //benchmark.readIntAndLongMapsJackson() shouldBe benchmark.intAndLongMapsObj
      benchmark.readIntAndLongMapsJsoniter() shouldBe benchmark.intAndLongMapsObj
      benchmark.readIntAndLongMapsPlay() shouldBe benchmark.intAndLongMapsObj
      //FIXME: Circe parses chars from strings
      //benchmark.readPrimitivesCirce() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesJackson() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesJsoniter() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesPlay() shouldBe benchmark.primitivesObj
      benchmark.readExtractFieldsCirce() shouldBe benchmark.extractFieldsObj
      benchmark.readExtractFieldsJackson() shouldBe benchmark.extractFieldsObj
      benchmark.readExtractFieldsJsoniter() shouldBe benchmark.extractFieldsObj
      benchmark.readExtractFieldsPlay() shouldBe benchmark.extractFieldsObj
    }
    "serialize properly" in {
      toString(benchmark.writeAnyRefsCirce()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeAnyRefsJackson()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeAnyRefsJsoniter()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeAnyRefsPlay()) shouldBe toString(benchmark.anyRefsJson)
      toString(benchmark.writeArraysCirce()) shouldBe toString(benchmark.arraysJson)
      toString(benchmark.writeArraysJackson()) shouldBe toString(benchmark.arraysJson)
      toString(benchmark.writeArraysJsoniter()) shouldBe toString(benchmark.arraysJson)
      toString(benchmark.writeArraysPlay()) shouldBe toString(benchmark.arraysJson)
      //FIXME: Circe doesn't support writing of bitsets
      // toString(benchmark.writeBitSetsCirce()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeBitSetsJackson()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeBitSetsJsoniter()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeBitSetsPlay()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeIterablesCirce()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeIterablesJackson()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeIterablesJsoniter()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeIterablesPlay()) shouldBe toString(benchmark.iterablesJson)
      toString(benchmark.writeMapsCirce()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsJackson()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsJsoniter()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsPlay()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMutableMapsCirce()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsJackson()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsJsoniter()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsPlay()) shouldBe toString(benchmark.mutableMapsJson)
      //FIXME: Circe doesn't support writing of int & long maps
      // toString(benchmark.writeIntAndLongMapsCirce()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsJackson()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsJsoniter()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsPlay()) shouldBe toString(benchmark.intAndLongMapsJson)
      //FIXME: Circe writes chars as strings
      // toString(benchmark.writePrimitivesCirce()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesJackson()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesJsoniter()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesPlay()) shouldBe toString(benchmark.primitivesJson)
    }
  }

  private def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  private def assertArrays(parsedObj: Arrays, obj: Arrays): Unit = {
    parsedObj.aa.deep shouldBe obj.aa.deep
    parsedObj.a.deep shouldBe obj.a.deep
  }
}
