package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.core.{ReaderConfig, WriterConfig}
import play.api.libs.json._
import java.time._
import scala.collection.immutable.Seq
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._

object PlayJsonJsoniterFormats {
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2).withPreferredBufSize(32768)
  val tooLongStringConfig: ReaderConfig = ReaderConfig.withPreferredCharBufSize(1024 * 1024)
  implicit val gitHubActionsAPIFormat: Format[GitHubActionsAPI.Response] = {
    implicit val v1: Format[Boolean] = PlayJsonFormats.stringFormat[Boolean]("boolean") { s =>
      "true" == s || "false" != s && sys.error("")
    }
    implicit val v2: Format[GitHubActionsAPI.Artifact] = Format({
      for {
        id <- (__ \ "id").read[Long]
        node_id <- (__ \ "node_id").read[String]
        name <- (__ \ "name").read[String]
        size_in_bytes <- (__ \ "size_in_bytes").read[Long]
        url <- (__ \ "url").read[String]
        archive_download_url <- (__ \ "archive_download_url").read[String]
        expired <- (__ \ "expired").read[Boolean]
        created_at <- (__ \ "created_at").read[Instant]
        expires_at <- (__ \ "expires_at").read[Instant]
      } yield GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url, expired,
        created_at, expires_at)
    }, (x: GitHubActionsAPI.Artifact) => {
      PlayJsonFormats.toJsObject(
        "id" -> JsNumber(x.id),
        "node_id" -> JsString(x.node_id),
        "name" -> JsString(x.name),
        "size_in_bytes" -> JsNumber(x.size_in_bytes),
        "url" -> JsString(x.url),
        "archive_download_url" -> JsString(x.archive_download_url),
        "expired" -> Json.toJson(x.expired),
        "created_at" -> Json.toJson(x.created_at),
        "expires_at" -> Json.toJson(x.expires_at)
      )
    })
    Format({
      for {
        total_count <- (__ \ "total_count").read[Int]
        artifacts <- (__ \ "artifacts").read[Seq[GitHubActionsAPI.Artifact]]
      } yield GitHubActionsAPI.Response(total_count, artifacts)
    }, (x: GitHubActionsAPI.Response) => {
      PlayJsonFormats.toJsObject(
        "total_count" -> JsNumber(x.total_count),
        "artifacts" -> Json.toJson(x.artifacts)
      )
    })
  }
}
