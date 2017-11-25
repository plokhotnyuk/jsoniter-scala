package com.github.plokhotnyuk.jsoniter_scala

import java.nio.charset.StandardCharsets

import org.scalatest.{Matchers, WordSpec}

class JsonCodecMakerBenchmarkSpec extends WordSpec with Matchers {
  val benchmark = new JsonCodecMakerBenchmark
  
  "JsonCodecMakerBenchmark" should {
    "deserialize properly" in {
      benchmark.missingReqFieldCirce() shouldBe
        "Attempt to decode value on failed cursor: DownField(s)"
      benchmark.missingReqFieldJackson() shouldBe
        """Missing required creator property 's' (index 0)
          | at [Source: (byte[])"{}"; line: 1, column: 2]""".stripMargin
      benchmark.missingReqFieldJsoniter() shouldBe
        """missing required field(s) "s", "i", offset: 0x00000001, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      benchmark.missingReqFieldJsoniterStackless() shouldBe
        """missing required field(s) "s", "i", offset: 0x00000001, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      benchmark.missingReqFieldJsoniterStacklessNoDump() shouldBe
        """missing required field(s) "s", "i", offset: 0x00000001"""
      benchmark.missingReqFieldPlay() shouldBe
        "JsResultException(errors:List((/s,List(JsonValidationError(List(error.path.missing),WrappedArray()))), (/i,List(JsonValidationError(List(error.path.missing),WrappedArray())))))"
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
      benchmark.readMutableIterablesCirce() shouldBe benchmark.mutableIterablesObj
      //FIXME: Jackson-module-scala doesn't support parsing of tree sets
      //benchmark.readMutableIterablesJackson() shouldBe benchmark.mutableIterablesObj
      benchmark.readMutableIterablesJsoniter() shouldBe benchmark.mutableIterablesObj
      benchmark.readMutableIterablesPlay() shouldBe benchmark.mutableIterablesObj
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
      //FIXME: Jackson-module-scala doesn't support parsing of int & long maps
      //benchmark.readIntAndLongMapsJackson() shouldBe benchmark.intAndLongMapsObj
      benchmark.readIntAndLongMapsJsoniter() shouldBe benchmark.intAndLongMapsObj
      benchmark.readIntAndLongMapsPlay() shouldBe benchmark.intAndLongMapsObj
      benchmark.readPrimitivesCirce() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesJackson() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesJsoniter() shouldBe benchmark.primitivesObj
      benchmark.readPrimitivesPlay() shouldBe benchmark.primitivesObj
      benchmark.readExtractFieldsCirce() shouldBe benchmark.extractFieldsObj
      benchmark.readExtractFieldsJackson() shouldBe benchmark.extractFieldsObj
      benchmark.readExtractFieldsJsoniter() shouldBe benchmark.extractFieldsObj
      benchmark.readExtractFieldsPlay() shouldBe benchmark.extractFieldsObj
      // FIXME: don't know how circe can parse ADTs with discriminator
      //benchmark.readAdtCirce() shouldBe benchmark.adtObj
      benchmark.readAdtJackson() shouldBe benchmark.adtObj
      benchmark.readAdtJsoniter() shouldBe benchmark.adtObj
      benchmark.readAdtPlay() shouldBe benchmark.adtObj
      benchmark.readGoogleMapsAPICirce() shouldBe GoogleMapsAPI.obj
      benchmark.readGoogleMapsAPIJackson() shouldBe GoogleMapsAPI.obj
      benchmark.readGoogleMapsAPIJsoniter() shouldBe GoogleMapsAPI.obj
      benchmark.readGoogleMapsAPIPlay() shouldBe GoogleMapsAPI.obj
      benchmark.readTwitterAPICirce() shouldBe TwitterAPI.obj
      benchmark.readTwitterAPIJackson() shouldBe TwitterAPI.obj
      benchmark.readTwitterAPIJsoniter() shouldBe TwitterAPI.obj
      //FIXME: format doesn't compile
      //benchmark.readTwitterAPIPlay() shouldBe TwitterAPI.obj
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
      toString(benchmark.writeMutableIterablesCirce()) shouldBe toString(benchmark.mutableIterablesJson)
      toString(benchmark.writeMutableIterablesJackson()) shouldBe toString(benchmark.mutableIterablesJson)
      toString(benchmark.writeMutableIterablesJsoniter()) shouldBe toString(benchmark.mutableIterablesJson)
      toString(benchmark.writeMutableIterablesPlay()) shouldBe toString(benchmark.mutableIterablesJson)
      toString(benchmark.writeMapsCirce()) shouldBe toString(benchmark.mapsJson)
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeMapsJackson()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsJsoniter()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsPlay()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMutableMapsCirce()) shouldBe toString(benchmark.mutableMapsJson)
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeMutableMapsJackson()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsJsoniter()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsPlay()) shouldBe toString(benchmark.mutableMapsJson)
      //FIXME: Circe doesn't support writing of int & long maps
      // toString(benchmark.writeIntAndLongMapsCirce()) shouldBe toString(benchmark.intAndLongMapsJson)
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeIntAndLongMapsJackson()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsJsoniter()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsPlay()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writePrimitivesCirce()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesJackson()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesJsoniter()) shouldBe toString(benchmark.primitivesJson)
      toString(benchmark.writePrimitivesPlay()) shouldBe toString(benchmark.primitivesJson)
      // FIXME: don't know how circe can serialize ADTs with discriminator
      //toString(benchmark.writeAdtCirce()) shouldBe toString(benchmark.adtJson)
      toString(benchmark.writeAdtJackson()) shouldBe toString(benchmark.adtJson)
      toString(benchmark.writeAdtJsoniter()) shouldBe toString(benchmark.adtJson)
      toString(benchmark.writeAdtPlay()) shouldBe toString(benchmark.adtJson)
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeGoogleMapsAPICirce()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIJackson()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIJsoniter()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIPlay()) shouldBe toString(GoogleMapsAPI.compactJson)
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeTwitterAPICirce()) shouldBe toString(TwitterAPI.compactJson)
      toString(benchmark.writeTwitterAPIJackson()) shouldBe toString(TwitterAPI.compactJson)
      toString(benchmark.writeTwitterAPIJsoniter()) shouldBe toString(TwitterAPI.compactJson)
      // FIXME: format doesn't compile
      //toString(benchmark.writeTwitterAPIPlay()) shouldBe toString(TwitterAPI.compactJson)
    }
  }

  private def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  private def assertArrays(parsedObj: Arrays, obj: Arrays): Unit = {
    parsedObj.aa.deep shouldBe obj.aa.deep
    parsedObj.a.deep shouldBe obj.a.deep
  }
}