package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

abstract class GitHubActionsAPIBenchmark extends CommonParams {
  var jsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("github_actions_api_response.json"))
  var compactJsonBytes1: Array[Byte] = bytes(getClass.getResourceAsStream("github_actions_api_compact_response-1.json"))
  var compactJsonBytes2: Array[Byte] = bytes(getClass.getResourceAsStream("github_actions_api_compact_response-2.json"))
  var obj: GitHubActionsAPI.Response = readFromArray[GitHubActionsAPI.Response](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(compactJsonBytes1.length + 100/*to avoid possible out of bounds error*/)
  var jsonString: String = new String(jsonBytes, UTF_8)
  var compactJsonString1: String = new String(compactJsonBytes1, UTF_8)
  var compactJsonString2: String = new String(compactJsonBytes2, UTF_8)
}