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

import java.nio.charset.StandardCharsets.UTF_8
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.openjdk.jmh.annotations.Setup

abstract class GitHubActionsAPIBenchmark extends CommonParams {
  var obj: GitHubActionsAPI.Response = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String =
    """{
      |  "total_count": 3,
      |  "artifacts": [
      |    {
      |      "id": 11,
      |      "node_id": "MDg6QXJ0aWZhY3QxMQ==",
      |      "name": "Rails v1",
      |      "size_in_bytes": 556,
      |      "url": "https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11",
      |      "archive_download_url": "https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11/zip",
      |      "expired": "false",
      |      "created_at": "2020-01-10T14:59:20Z",
      |      "expires_at": "2020-01-21T14:59:20Z"
      |    },
      |    {
      |      "id": 12,
      |      "node_id": "MDg6QXJ0aWZhY3QxMa==",
      |      "name": "Rails v2",
      |      "size_in_bytes": 561,
      |      "url": "https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12",
      |      "archive_download_url": "https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12/zip",
      |      "expired": "false",
      |      "created_at": "2020-01-10T14:59:21Z",
      |      "expires_at": "2020-01-21T14:59:21Z"
      |    },
      |    {
      |      "id": 13,
      |      "node_id": "MDg6QXJ0aWZhY3QxMw==",
      |      "name": "Rails v3",
      |      "size_in_bytes": 453,
      |      "url": "https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13",
      |      "archive_download_url": "https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13/zip",
      |      "expired": "false",
      |      "created_at": "2020-01-10T14:59:22Z",
      |      "expires_at": "2020-01-21T14:59:22Z"
      |    }
      |  ]
      |}""".stripMargin
  var compactJsonString1: String = """{"total_count":3,"artifacts":[{"id":11,"node_id":"MDg6QXJ0aWZhY3QxMQ==","name":"Rails v1","size_in_bytes":556,"url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11","archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11/zip","expired":"false","created_at":"2020-01-10T14:59:20Z","expires_at":"2020-01-21T14:59:20Z"},{"id":12,"node_id":"MDg6QXJ0aWZhY3QxMa==","name":"Rails v2","size_in_bytes":561,"url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12","archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12/zip","expired":"false","created_at":"2020-01-10T14:59:21Z","expires_at":"2020-01-21T14:59:21Z"},{"id":13,"node_id":"MDg6QXJ0aWZhY3QxMw==","name":"Rails v3","size_in_bytes":453,"url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13","archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13/zip","expired":"false","created_at":"2020-01-10T14:59:22Z","expires_at":"2020-01-21T14:59:22Z"}]}"""
  var compactJsonString2: String = """{"artifacts":[{"archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11/zip","created_at":"2020-01-10T14:59:20Z","expired":"false","expires_at":"2020-01-21T14:59:20Z","id":11,"name":"Rails v1","node_id":"MDg6QXJ0aWZhY3QxMQ==","size_in_bytes":556,"url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11"},{"archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12/zip","created_at":"2020-01-10T14:59:21Z","expired":"false","expires_at":"2020-01-21T14:59:21Z","id":12,"name":"Rails v2","node_id":"MDg6QXJ0aWZhY3QxMa==","size_in_bytes":561,"url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12"},{"archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13/zip","created_at":"2020-01-10T14:59:22Z","expired":"false","expires_at":"2020-01-21T14:59:22Z","id":13,"name":"Rails v3","node_id":"MDg6QXJ0aWZhY3QxMw==","size_in_bytes":453,"url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13"}],"total_count":3}"""

  @Setup
  def setup(): Unit = {
    jsonBytes = jsonString.getBytes(UTF_8)
    obj = readFromArray[GitHubActionsAPI.Response](jsonBytes)
    preallocatedBuf = new Array(jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}