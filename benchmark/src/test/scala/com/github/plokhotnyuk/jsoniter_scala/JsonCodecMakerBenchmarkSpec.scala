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
      toString(benchmark.writeAnyRefsCirce()) shouldBe toString(benchmark.anyRefsJsonBytes)
      toString(benchmark.writeAnyRefsJackson()) shouldBe toString(benchmark.anyRefsJsonBytes)
      toString(benchmark.writeAnyRefsJsoniter()) shouldBe toString(benchmark.anyRefsJsonBytes)
      toString(benchmark.writeAnyRefsJsoniterPrealloc()) shouldBe toString(benchmark.anyRefsJsonBytes)
      toString(benchmark.writeAnyRefsPlay()) shouldBe toString(benchmark.anyRefsJsonBytes)
      toString(benchmark.writeArraysCirce()) shouldBe toString(benchmark.arraysJsonBytes)
      toString(benchmark.writeArraysJackson()) shouldBe toString(benchmark.arraysJsonBytes)
      toString(benchmark.writeArraysJsoniter()) shouldBe toString(benchmark.arraysJsonBytes)
      toString(benchmark.writeArraysPlay()) shouldBe toString(benchmark.arraysJsonBytes)
      //FIXME: Circe doesn't support writing of bitsets
      // toString(benchmark.writeBitSetsCirce()) shouldBe toString(benchmark.bitSetsJson)
      toString(benchmark.writeBitSetsJackson()) shouldBe toString(benchmark.bitSetsJsonBytes)
      toString(benchmark.writeBitSetsJsoniter()) shouldBe toString(benchmark.bitSetsJsonBytes)
      toString(benchmark.writeBitSetsPlay()) shouldBe toString(benchmark.bitSetsJsonBytes)
      toString(benchmark.writeIterablesCirce()) shouldBe toString(benchmark.iterablesJsonBytes)
      toString(benchmark.writeIterablesJackson()) shouldBe toString(benchmark.iterablesJsonBytes)
      toString(benchmark.writeIterablesJsoniter()) shouldBe toString(benchmark.iterablesJsonBytes)
      toString(benchmark.writeIterablesPlay()) shouldBe toString(benchmark.iterablesJsonBytes)
      toString(benchmark.writeMutableIterablesCirce()) shouldBe toString(benchmark.mutableIterablesJsonBytes)
      toString(benchmark.writeMutableIterablesJackson()) shouldBe toString(benchmark.mutableIterablesJsonBytes)
      toString(benchmark.writeMutableIterablesJsoniter()) shouldBe toString(benchmark.mutableIterablesJsonBytes)
      toString(benchmark.writeMutableIterablesPlay()) shouldBe toString(benchmark.mutableIterablesJsonBytes)
      toString(benchmark.writeMapsCirce()) shouldBe toString(benchmark.mapsJsonBytes)
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeMapsJackson()) shouldBe toString(benchmark.mapsJson)
      toString(benchmark.writeMapsJsoniter()) shouldBe toString(benchmark.mapsJsonBytes)
      toString(benchmark.writeMapsPlay()) shouldBe toString(benchmark.mapsJsonBytes)
      toString(benchmark.writeMutableMapsCirce()) shouldBe toString(benchmark.mutableMapsJsonBytes)
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeMutableMapsJackson()) shouldBe toString(benchmark.mutableMapsJson)
      toString(benchmark.writeMutableMapsJsoniter()) shouldBe toString(benchmark.mutableMapsJsonBytes)
      toString(benchmark.writeMutableMapsPlay()) shouldBe toString(benchmark.mutableMapsJsonBytes)
      //FIXME: Circe doesn't support writing of int & long maps
      // toString(benchmark.writeIntAndLongMapsCirce()) shouldBe toString(benchmark.intAndLongMapsJson)
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeIntAndLongMapsJackson()) shouldBe toString(benchmark.intAndLongMapsJson)
      toString(benchmark.writeIntAndLongMapsJsoniter()) shouldBe toString(benchmark.intAndLongMapsJsonBytes)
      toString(benchmark.writeIntAndLongMapsPlay()) shouldBe toString(benchmark.intAndLongMapsJsonBytes)
      toString(benchmark.writePrimitivesCirce()) shouldBe toString(benchmark.primitivesJsonBytes)
      toString(benchmark.writePrimitivesJackson()) shouldBe toString(benchmark.primitivesJsonBytes)
      toString(benchmark.writePrimitivesJsoniter()) shouldBe toString(benchmark.primitivesJsonBytes)
      toString(benchmark.writePrimitivesJsoniterPrealloc()) shouldBe toString(benchmark.primitivesJsonBytes)
      toString(benchmark.writePrimitivesPlay()) shouldBe toString(benchmark.primitivesJsonBytes)
      // FIXME: circe appends discriminator as a last field
      //toString(benchmark.writeAdtCirce()) shouldBe toString(benchmark.adtJson)
      toString(benchmark.writeAdtJackson()) shouldBe toString(benchmark.adtJsonBytes)
      toString(benchmark.writeAdtJsoniter()) shouldBe toString(benchmark.adtJsonBytes)
      toString(benchmark.writeAdtPlay()) shouldBe toString(benchmark.adtJsonBytes)
      toString(benchmark.writeAsciiStringCirce()) shouldBe toString(benchmark.asciiStringJsonBytes)
      toString(benchmark.writeAsciiStringJackson()) shouldBe toString(benchmark.asciiStringJsonBytes)
      toString(benchmark.writeAsciiStringJsoniter()) shouldBe toString(benchmark.asciiStringJsonBytes)
      toString(benchmark.writeAsciiStringJsoniterPrealloc()) shouldBe toString(benchmark.asciiStringJsonBytes)
      toString(benchmark.writeAsciiStringPlay()) shouldBe toString(benchmark.asciiStringJsonBytes)
      toString(benchmark.writeNonAsciiStringCirce()) shouldBe toString(benchmark.nonAsciiStringJsonBytes)
      toString(benchmark.writeNonAsciiStringJackson()) shouldBe toString(benchmark.nonAsciiStringJsonBytes)
      toString(benchmark.writeNonAsciiStringJsoniter()) shouldBe toString(benchmark.nonAsciiStringJsonBytes)
      toString(benchmark.writeNonAsciiStringJsoniterPrealloc()) shouldBe toString(benchmark.nonAsciiStringJsonBytes)
      toString(benchmark.writeNonAsciiStringPlay()) shouldBe toString(benchmark.nonAsciiStringJsonBytes)
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeGoogleMapsAPICirce()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIJackson()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIJsoniter()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIJsoniterPrealloc()) shouldBe toString(GoogleMapsAPI.compactJson)
      toString(benchmark.writeGoogleMapsAPIPlay()) shouldBe toString(GoogleMapsAPI.compactJson)
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeTwitterAPICirce()) shouldBe toString(TwitterAPI.compactJson)
      toString(benchmark.writeTwitterAPIJackson()) shouldBe toString(TwitterAPI.compactJson)
      toString(benchmark.writeTwitterAPIJsoniter()) shouldBe toString(TwitterAPI.compactJson)
      toString(benchmark.writeTwitterAPIJsoniterPrealloc()) shouldBe toString(TwitterAPI.compactJson)
      // FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writeTwitterAPIPlay()) shouldBe toString(TwitterAPI.compactJson)
    }
  }

  private def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  private def toString(len: Int): String = new String(JsoniterCodecs.preallocatedBuf.get, 0, len, StandardCharsets.UTF_8)

  private def assertArrays(parsedObj: Arrays, obj: Arrays): Unit = {
    parsedObj.aa.deep shouldBe obj.aa.deep
    parsedObj.a.deep shouldBe obj.a.deep
  }
}