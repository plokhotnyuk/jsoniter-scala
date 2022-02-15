package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

abstract class GitHubActionsAPIBenchmark extends CommonParams {
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
  var compactJsonString3: String = """{"artifacts":[{"name":"Rails v1","expired":"false","url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11","node_id":"MDg6QXJ0aWZhY3QxMQ==","archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/11/zip","created_at":"2020-01-10T14:59:20Z","id":11,"size_in_bytes":556,"expires_at":"2020-01-21T14:59:20Z"},{"name":"Rails v2","expired":"false","url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12","node_id":"MDg6QXJ0aWZhY3QxMa==","archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/12/zip","created_at":"2020-01-10T14:59:21Z","id":12,"size_in_bytes":561,"expires_at":"2020-01-21T14:59:21Z"},{"name":"Rails v3","expired":"false","url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13","node_id":"MDg6QXJ0aWZhY3QxMw==","archive_download_url":"https://api.github.com/repos/octo-org/octo-docs/actions/artifacts/13/zip","created_at":"2020-01-10T14:59:22Z","id":13,"size_in_bytes":453,"expires_at":"2020-01-21T14:59:22Z"}],"total_count":3}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)
  var compactJsonBytes1: Array[Byte] = compactJsonString1.getBytes(UTF_8)
  var compactJsonBytes2: Array[Byte] = compactJsonString2.getBytes(UTF_8)
  var obj: GitHubActionsAPI.Response = readFromArray[GitHubActionsAPI.Response](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(compactJsonBytes1.length + 100/*to avoid possible out of bounds error*/)
}