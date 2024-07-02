package com.github.plokhotnyuk.jsoniter_scala.core

import scala.scalanative.unsafe._

// FIXME: Replace by cross-platform version later, see: https://github.com/scala-native/scala-native/issues/2473
@extern
private[core] object NativeMath {
  @name("jsoniter_scala_multiply_high")
  def multiplyHigh(x: Long, y: Long): Long = extern

  @name("jsoniter_scala_unsigned_multiply_high")
  def unsignedMultiplyHigh(x: Long, y: Long): Long = extern
}