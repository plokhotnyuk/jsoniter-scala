package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BitMaskSpec extends BenchmarkSpecBase {
  "BitMask" should {
    "build properly" in {
      BitMask.toBitMask(Array[Int](), 4).toList shouldBe List()
      BitMask.toBitMask(Array(1, 2, 3), 4).toList shouldBe List(14)
      BitMask.toBitMask(Array(3, 2, 1), 4).toList shouldBe List(14)
    }
    "fail on invalid input" in {
      intercept[Throwable](BitMask.toBitMask(Array(3, 2, 1), 1))
      intercept[Throwable](BitMask.toBitMask(Array(3, 2, 1), 2))
      intercept[Throwable](BitMask.toBitMask(Array(3, 2, 1), 3))
    }
  }
}