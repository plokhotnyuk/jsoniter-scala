package com.github.plokhotnyuk.jsoniter_scala.core

object TestUtils {
  val isJS = false
  val isNative = true

  def toHex(b: Byte): String = "%02x".format(b)
}