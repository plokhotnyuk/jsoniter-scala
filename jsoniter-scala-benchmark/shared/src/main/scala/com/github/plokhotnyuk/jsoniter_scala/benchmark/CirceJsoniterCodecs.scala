package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.circe.Decoder._
import io.circe.Encoder._
import io.circe._
import io.circe.generic.extras.semiauto._
import java.time.Instant
import com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import io.circe.generic.extras.Configuration

object CirceJsoniterCodecs {
  import com.github.plokhotnyuk.jsoniter_scala.circe.JavaTimeCodecs._

  implicit val jsonCodec: JsonValueCodec[Json] = JsonCodec.jsonCodec(doSerialize = _ ne Json.Null)
  implicit val config: Configuration = Configuration.default.withDefaults.withDiscriminator("type")
  implicit val gitHubActionsAPIC3c: Codec[GitHubActionsAPI.Response] = {
    implicit val c1: Codec[GitHubActionsAPI.Artifact] =
      Codec.forProduct9("id", "node_id", "name", "size_in_bytes", "url", "archive_download_url",
        "expired", "created_at", "expires_at") {
        (id: Long, node_id: String, name: String, size_in_bytes: Long, url: String, archive_download_url: String,
        expired: String, created_at: Instant, expires_at: Instant) =>
          GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url,
            expired.toBoolean, created_at, expires_at)
      } { a =>
        (a.id, a.node_id, a.name, a.size_in_bytes, a.url, a.archive_download_url,
        a.expired.toString, a.created_at, a.expires_at)
      }
    deriveConfiguredCodec[GitHubActionsAPI.Response]
  }
}