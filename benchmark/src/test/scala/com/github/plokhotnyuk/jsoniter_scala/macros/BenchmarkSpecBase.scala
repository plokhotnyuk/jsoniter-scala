package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets

import org.scalatest.{Matchers, WordSpec}

abstract class BenchmarkSpecBase extends WordSpec with Matchers {
  def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  def toString(json: Array[Byte], len: Int): String = new String(json, 0, len, StandardCharsets.UTF_8)
}