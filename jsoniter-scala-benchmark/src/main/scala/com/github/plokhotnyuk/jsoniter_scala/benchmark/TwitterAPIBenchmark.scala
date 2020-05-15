package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.collection.immutable.Seq

abstract class TwitterAPIBenchmark extends CommonParams {
  var jsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("twitter_api_response.json"))
  var compactJsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("twitter_api_compact_response.json"))
  var obj: Seq[TwitterAPI.Tweet] = readFromArray[Seq[TwitterAPI.Tweet]](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(compactJsonBytes.length + 100/*to avoid possible out of bounds error*/)
  var jsonString: String = new String(jsonBytes, UTF_8)
  var compactJsonString: String = new String(compactJsonBytes, UTF_8)
}