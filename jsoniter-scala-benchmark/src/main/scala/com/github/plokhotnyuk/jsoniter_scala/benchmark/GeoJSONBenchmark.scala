package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

abstract class GeoJSONBenchmark extends CommonParams {
  //Borders of Switzerland, from: https://github.com/mledoze/countries/blob/master/data/che.geo.json
  var jsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("che-1.geo.json"))
  var obj: GeoJSON.GeoJSON = readFromArray[GeoJSON.GeoJSON](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  var jsonString1: String = new String(jsonBytes, UTF_8)
  var jsonString2: String = new String(bytes(getClass.getResourceAsStream("che-2.geo.json")), UTF_8)
  var jsonString3: String = new String(bytes(getClass.getResourceAsStream("che-3.geo.json")), UTF_8)
  var jsonString4: String = new String(bytes(getClass.getResourceAsStream("che-4.geo.json")), UTF_8)
}