package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.InputStream

object TestUtils {
  val isJDK8: Boolean = System.getProperty("java.version").startsWith("1.8")
  val isJS: Boolean = false

  def getResourceAsStream(resource: String): java.io.InputStream =
    getClass.getResourceAsStream(resource)

  def bytes(inputStream: InputStream): Array[Byte] =
    scala.reflect.io.Streamable.bytes(inputStream)

  def byteArrayToString(bytes: Array[Byte]): String =
    new String(bytes, 0, 0, bytes.length)
}
