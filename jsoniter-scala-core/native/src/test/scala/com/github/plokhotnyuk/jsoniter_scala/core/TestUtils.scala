package com.github.plokhotnyuk.jsoniter_scala.core

object TestUtils {
  val isJDK8: Boolean = System.getProperty("java.version").startsWith("1.8")
  val isJS = false
  val isNative = true

  def toHex(b: Byte): String = "%02x".format(b)
}