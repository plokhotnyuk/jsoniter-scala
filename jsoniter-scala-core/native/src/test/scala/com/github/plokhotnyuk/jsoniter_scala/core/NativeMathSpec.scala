package com.github.plokhotnyuk.jsoniter_scala.core

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class NativeMathSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "NativeMath" should {
    "have multiplyHigh consistent with Math.multiplyHigh" in {
      def check(x: Long, y: Long): Unit = NativeMath.multiplyHigh(x, y) shouldBe Math.multiplyHigh(x, y)

      check(Long.MaxValue, Long.MaxValue)
      check(Long.MinValue, Long.MaxValue)
      check(Long.MaxValue, Long.MinValue)
      check(Long.MinValue, Long.MinValue)
      forAll(arbitrary[Long], arbitrary[Long], minSuccessful(100000))(check)
    }
    "have unsignedMultiplyHigh consistent with Math.unsignedMultiplyHigh" in {
      def check(x: Long, y: Long): Unit = NativeMath.unsignedMultiplyHigh(x, y) shouldBe unsignedMultiplyHigh(x, y)

      check(Long.MaxValue, Long.MaxValue)
      check(Long.MinValue, Long.MaxValue)
      check(Long.MaxValue, Long.MinValue)
      check(Long.MinValue, Long.MinValue)
      forAll(arbitrary[Long], arbitrary[Long], minSuccessful(100000))(check)
    }
  }

  def unsignedMultiplyHigh(x: Long, y: Long): Long = { // FIXME: replace by Math.unsignedMultiplyHigh after dropping JDK 17 support
    var r = Math.multiplyHigh(x, y)
    r += y & (x >> 63)
    r += x & (y >> 63)
    r
  }
}