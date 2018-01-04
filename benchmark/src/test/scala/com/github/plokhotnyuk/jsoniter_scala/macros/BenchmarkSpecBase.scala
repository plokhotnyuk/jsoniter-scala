package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets

import org.scalatest.{Matchers, WordSpec}

abstract class BenchmarkSpecBase extends WordSpec with Matchers {
  def toString(json: Array[Byte]): String = new String(json, StandardCharsets.UTF_8)

  def toString(len: Int): String = new String(JsoniterCodecs.preallocatedBuf.get, 0, len, StandardCharsets.UTF_8)

  def assertArrays(parsedObj: Arrays, obj: Arrays): Unit = {
    parsedObj.aa.deep shouldBe obj.aa.deep
    parsedObj.a.deep shouldBe obj.a.deep
  }
}