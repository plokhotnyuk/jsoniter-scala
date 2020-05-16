package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.io.ByteArrayInputStream

object Utils {
  def getResourceAsStream(resource: String): java.io.InputStream =
    new ByteArrayInputStream(FS.readFileSync(Path.join(
      "jsoniter-scala-benchmark",
      "shared", "src", "main", "resources",
      "com", "github", "plokhotnyuk", "jsoniter_scala", "benchmark",
      resource), "utf8").getBytes)
}
