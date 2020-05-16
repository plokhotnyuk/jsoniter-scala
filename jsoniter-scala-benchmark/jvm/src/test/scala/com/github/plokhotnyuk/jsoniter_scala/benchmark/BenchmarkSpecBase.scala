package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

abstract class BenchmarkSpecBase extends AnyWordSpec with Matchers {
  def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  def toString(json: Array[Byte], from: Int, to: Int): String = new String(json, from, to - from, StandardCharsets.UTF_8)
}