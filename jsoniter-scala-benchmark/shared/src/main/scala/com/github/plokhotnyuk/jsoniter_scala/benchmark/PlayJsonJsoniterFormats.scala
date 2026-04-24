/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.core.{ReaderConfig, WriterConfig}
import play.api.libs.json._
import java.time._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._

object PlayJsonJsoniterFormats {
  private[this] val __ = JsPath
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2).withPreferredBufSize(32768)
  val tooLongStringConfig: ReaderConfig = ReaderConfig.withPreferredCharBufSize(1024 * 1024)
  implicit val gitHubActionsAPIFormat: Format[GitHubActionsAPI.Response] = {
    implicit val v2: Format[GitHubActionsAPI.Artifact] = Format({
      for {
        id <- (__ \ "id").read[Long]
        node_id <- (__ \ "node_id").read[String]
        name <- (__ \ "name").read[String]
        size_in_bytes <- (__ \ "size_in_bytes").read[Long]
        url <- (__ \ "url").read[String]
        archive_download_url <- (__ \ "archive_download_url").read[String]
        expired <- (__ \ "expired").read[String].map(s => "true" == s || "false" != s && sys.error(""))
        created_at <- (__ \ "created_at").read[Instant]
        expires_at <- (__ \ "expires_at").read[Instant]
      } yield new GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url, expired,
        created_at, expires_at)
    }, (x: GitHubActionsAPI.Artifact) => {
      JsObject(Array(
        "id" -> new JsNumber(x.id),
        "node_id" -> new JsString(x.node_id),
        "name" -> new JsString(x.name),
        "size_in_bytes" -> new JsNumber(x.size_in_bytes),
        "url" -> new JsString(x.url),
        "archive_download_url" -> new JsString(x.archive_download_url),
        "expired" -> new JsString(if (x.expired) "true" else "false"),
        "created_at" -> Json.toJson(x.created_at),
        "expires_at" -> Json.toJson(x.expires_at)
      ))
    })
    Format({
      for {
        total_count <- (__ \ "total_count").read[Int]
        artifacts <- (__ \ "artifacts").read[Seq[GitHubActionsAPI.Artifact]]
      } yield new GitHubActionsAPI.Response(total_count, artifacts)
    }, (x: GitHubActionsAPI.Response) => {
      PlayJsonFormats.toJsObject(
        "total_count" -> new JsNumber(x.total_count),
        "artifacts" -> Json.toJson(x.artifacts)
      )
    })
  }
}
