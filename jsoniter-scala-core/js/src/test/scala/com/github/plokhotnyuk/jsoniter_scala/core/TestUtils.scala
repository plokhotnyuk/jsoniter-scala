package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, InputStream}

object TestUtils {
  val isJDK8: Boolean = false

  def getResourceAsStream(resource: String): java.io.InputStream = {
    val resourcePath = Path.join(
      "jsoniter-scala-core",
      "shared", "src", "test", "resources",
      "com", "github", "plokhotnyuk", "jsoniter_scala", "core",
      resource
    )
    new ByteArrayInputStream(FS.readFileSync(resourcePath, "utf8").getBytes)
  }

  def bytes(inputStream: InputStream): Array[Byte] =  {
    Iterator.continually(inputStream.read())
      .takeWhile(_ != -1)
      .map(_.toByte)
      .toArray
  }

  def byteArrayToString(bytes: Array[Byte]): String =
    new String(bytes, 0, bytes.length)
}
