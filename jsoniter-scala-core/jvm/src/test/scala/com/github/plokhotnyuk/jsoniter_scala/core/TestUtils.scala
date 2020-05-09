package com.github.plokhotnyuk.jsoniter_scala.core

object TestUtils {
  val isJDK8: Boolean = System.getProperty("java.version").startsWith("1.8")
  val isJS: Boolean = false

  def getResourceAsStream(resource: String): java.io.InputStream = getClass.getResourceAsStream(resource)

  def byteArrayToString(bytes: Array[Byte]): String = new String(bytes, 0, 0, bytes.length)

  def lowercaseHex(b: Byte): String = "%02x".format(b)

  def uppercaseHex(b: Byte): String = "%02X".format(b)
}
