package com.github.plokhotnyuk.jsoniter_scala.core

object TestUtils {
  val isJDK8: Boolean = false
  val isJS: Boolean = true

  def byteArrayToString(bytes: Array[Byte]): String = new String(bytes, 0, bytes.length)

  def lowercaseHex(b: Byte): String = "%02x".format(b & 0xff)

  def uppercaseHex(b: Byte): String = "%02X".format(b & 0xff)
}
