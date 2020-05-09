package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.collection.immutable.Seq

object GitHubActionsAPI {
  case class Artifact(
    id: Long,
    node_id: String,
    name: String,
    size_in_bytes: Long,
    url: String,
    archive_download_url: String,
    @JsonSerialize(using = classOf[StringifiedBooleanSerializer]) @stringified expired: Boolean,
    created_at: Instant,
    expires_at: Instant)

  case class Response(
    total_count: Int,
    artifacts: Seq[Artifact])
}

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