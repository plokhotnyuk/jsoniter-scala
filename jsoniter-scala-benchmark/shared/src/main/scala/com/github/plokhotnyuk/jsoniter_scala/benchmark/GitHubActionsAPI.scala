package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time.Instant
import com.github.plokhotnyuk.jsoniter_scala.macros._

object GitHubActionsAPI {
  case class Artifact(
    id: Long,
    node_id: String,
    name: String,
    size_in_bytes: Long,
    url: String,
    archive_download_url: String,
    @stringified expired: Boolean,
    created_at: Instant,
    expires_at: Instant)

  case class Response(
    total_count: Int,
    artifacts: Seq[Artifact])
}