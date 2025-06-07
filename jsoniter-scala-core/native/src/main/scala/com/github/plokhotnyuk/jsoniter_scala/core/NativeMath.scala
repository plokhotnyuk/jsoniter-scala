package com.github.plokhotnyuk.jsoniter_scala.core

import scala.scalanative.annotation.alwaysinline
import scala.scalanative.unsafe._

@extern
private[core] object NativeMath {
  @alwaysinline
  @name("jsoniter_scala_multiply_high")
  def multiplyHigh(x: Long, y: Long): Long = extern

  @alwaysinline
  @name("jsoniter_scala_unsigned_multiply_high")
  def unsignedMultiplyHigh(x: Long, y: Long): Long = extern
}