package com.github.plokhotnyuk.jsoniter_scala

import java.nio.charset.StandardCharsets

import com.github.plokhotnyuk.jsoniter_scala.macros._
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
      benchmark.readAdtCirce() shouldBe benchmark.adtObj
      benchmark.readAdtJackson() shouldBe benchmark.adtObj
      benchmark.readAdtJsoniter() shouldBe benchmark.adtObj
      benchmark.readAdtPlay() shouldBe benchmark.adtObj
      benchmark.readAsciiStringCirce() shouldBe benchmark.asciiStringObj
      benchmark.readAsciiStringJackson() shouldBe benchmark.asciiStringObj
      benchmark.readAsciiStringJsoniter() shouldBe benchmark.asciiStringObj
      // FIXME: find proper way to parse string value in Play JSON
      //benchmark.readAsciiStringPlay() shouldBe benchmark.asciiStringObj
      benchmark.readNonAsciiStringCirce() shouldBe benchmark.nonAsciiStringObj
      benchmark.readNonAsciiStringJackson() shouldBe benchmark.nonAsciiStringObj
      benchmark.readNonAsciiStringJsoniter() shouldBe benchmark.nonAsciiStringObj
      // FIXME: find proper way to parse string value in Play JSON
      //benchmark.readNonAsciiStringPlay() shouldBe benchmark.nonAsciiStringObj
      benchmark.readGoogleMapsAPICirce() shouldBe benchmark.googleMapsAPIObj
      benchmark.readGoogleMapsAPIJackson() shouldBe benchmark.googleMapsAPIObj
      benchmark.readGoogleMapsAPIJsoniter() shouldBe benchmark.googleMapsAPIObj
      benchmark.readGoogleMapsAPIPlay() shouldBe benchmark.googleMapsAPIObj
      benchmark.readTwitterAPICirce() shouldBe benchmark.twitterAPIObj
      benchmark.readTwitterAPIJackson() shouldBe benchmark.twitterAPIObj
      benchmark.readTwitterAPIJsoniter() shouldBe benchmark.twitterAPIObj
      benchmark.readTwitterAPIPlay() shouldBe benchmark.twitterAPIObj
    }
    "serialize properly" in {
      toString(benchmark.writeAnyRefsCirce()) shouldBe benchmark.anyRefsJsonString
      toString(benchmark.writeAnyRefsJackson()) shouldBe benchmark.anyRefsJsonString
      toString(benchmark.writeAnyRefsJsoniter()) shouldBe benchmark.anyRefsJsonString
      toString(benchmark.writeAnyRefsJsoniterPrealloc()) shouldBe benchmark.anyRefsJsonString
      toString(benchmark.writeAnyRefsPlay()) shouldBe benchmark.anyRefsJsonString
      toString(benchmark.writeArraysCirce()) shouldBe benchmark.arraysJsonString
      toString(benchmark.writeArraysJackson()) shouldBe benchmark.arraysJsonString
      toString(benchmark.writeArraysJsoniter()) shouldBe benchmark.arraysJsonString
      toString(benchmark.writeArraysPlay()) shouldBe benchmark.arraysJsonString
      //FIXME: Circe doesn't support writing of bitsets
      // toString(benchmark.writeBitSetsCirce()) shouldBe benchmark.bitSetsJsonString
      toString(benchmark.writeBitSetsJackson()) shouldBe benchmark.bitSetsJsonString
      toString(benchmark.writeBitSetsJsoniter()) shouldBe benchmark.bitSetsJsonString
      toString(benchmark.writeBitSetsPlay()) shouldBe benchmark.bitSetsJsonString
      toString(benchmark.writeIterablesCirce()) shouldBe benchmark.iterablesJsonString
      toString(benchmark.writeIterablesJackson()) shouldBe benchmark.iterablesJsonString
      toString(benchmark.writeIterablesJsoniter()) shouldBe benchmark.iterablesJsonString
      toString(benchmark.writeIterablesPlay()) shouldBe benchmark.iterablesJsonString
      toString(benchmark.writeMutableIterablesCirce()) shouldBe benchmark.mutableIterablesJsonString
      toString(benchmark.writeMutableIterablesJackson()) shouldBe benchmark.mutableIterablesJsonString
      toString(benchmark.writeMutableIterablesJsoniter()) shouldBe benchmark.mutableIterablesJsonString
      toString(benchmark.writeMutableIterablesPlay()) shouldBe benchmark.mutableIterablesJsonString
      toString(benchmark.writeMapsCirce()) shouldBe benchmark.mapsJsonString
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeMapsJackson()) shouldBe benchmark.mapsJsonString
      toString(benchmark.writeMapsJsoniter()) shouldBe benchmark.mapsJsonString
      toString(benchmark.writeMapsPlay()) shouldBe benchmark.mapsJsonString
      toString(benchmark.writeMutableMapsCirce()) shouldBe benchmark.mutableMapsJsonString
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeMutableMapsJackson()) shouldBe benchmark.mutableMapsJsonString
      toString(benchmark.writeMutableMapsJsoniter()) shouldBe benchmark.mutableMapsJsonString
      toString(benchmark.writeMutableMapsPlay()) shouldBe benchmark.mutableMapsJsonString
      //FIXME: Circe doesn't support writing of int & long maps
      // toString(benchmark.writeIntAndLongMapsCirce()) shouldBe benchmark.intAndLongMapsJsonString
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeIntAndLongMapsJackson()) shouldBe benchmark.intAndLongMapsJsonString
      toString(benchmark.writeIntAndLongMapsJsoniter()) shouldBe benchmark.intAndLongMapsJsonString
      toString(benchmark.writeIntAndLongMapsPlay()) shouldBe benchmark.intAndLongMapsJsonString
      toString(benchmark.writePrimitivesCirce()) shouldBe benchmark.primitivesJsonString
      toString(benchmark.writePrimitivesJackson()) shouldBe benchmark.primitivesJsonString
      toString(benchmark.writePrimitivesJsoniter()) shouldBe benchmark.primitivesJsonString
      toString(benchmark.writePrimitivesJsoniterPrealloc()) shouldBe benchmark.primitivesJsonString
      toString(benchmark.writePrimitivesPlay()) shouldBe benchmark.primitivesJsonString
      // FIXME: circe appends discriminator as a last field
      //toString(benchmark.writeAdtCirce()) shouldBe benchmark.adtJsonString
      toString(benchmark.writeAdtJackson()) shouldBe benchmark.adtJsonString
      toString(benchmark.writeAdtJsoniter()) shouldBe benchmark.adtJsonString
      toString(benchmark.writeAdtPlay()) shouldBe benchmark.adtJsonString
      toString(benchmark.writeAsciiStringCirce()) shouldBe benchmark.asciiStringJsonString
      toString(benchmark.writeAsciiStringJackson()) shouldBe benchmark.asciiStringJsonString
      toString(benchmark.writeAsciiStringJsoniter()) shouldBe benchmark.asciiStringJsonString
      toString(benchmark.writeAsciiStringJsoniterPrealloc()) shouldBe benchmark.asciiStringJsonString
      toString(benchmark.writeAsciiStringPlay()) shouldBe benchmark.asciiStringJsonString
      toString(benchmark.writeNonAsciiStringCirce()) shouldBe benchmark.nonAsciiStringJsonString
      toString(benchmark.writeNonAsciiStringJackson()) shouldBe benchmark.nonAsciiStringJsonString
      toString(benchmark.writeNonAsciiStringJsoniter()) shouldBe benchmark.nonAsciiStringJsonString
      toString(benchmark.writeNonAsciiStringJsoniterPrealloc()) shouldBe benchmark.nonAsciiStringJsonString
      toString(benchmark.writeNonAsciiStringPlay()) shouldBe benchmark.nonAsciiStringJsonString
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeGoogleMapsAPICirce()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIJackson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIJsoniter()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIJsoniterPrealloc()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIPlay()) shouldBe GoogleMapsAPI.compactJsonString
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeTwitterAPICirce()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeTwitterAPIJackson()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeTwitterAPIJsoniter()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeTwitterAPIJsoniterPrealloc()) shouldBe TwitterAPI.compactJsonString
      // FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writeTwitterAPIPlay()) shouldBe TwitterAPI.compactJsonString
    }
  }

  private def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  private def toString(len: Int): String = new String(JsoniterCodecs.preallocatedBuf.get, 0, len, StandardCharsets.UTF_8)

  private def assertArrays(parsedObj: Arrays, obj: Arrays): Unit = {
    parsedObj.aa.deep shouldBe obj.aa.deep
    parsedObj.a.deep shouldBe obj.a.deep
  }
}