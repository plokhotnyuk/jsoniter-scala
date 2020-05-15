package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

abstract class OpenRTBBenchmark extends CommonParams {
  var jsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("openrtb_bidrequest.json"))
  var obj: OpenRTB.BidRequest = readFromArray[OpenRTB.BidRequest](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  var jsonString: String = new String(jsonBytes, UTF_8)
}