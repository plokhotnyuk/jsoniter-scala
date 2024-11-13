package com.github.plokhotnyuk.jsoniter_scala.benchmark

class HashCodeColliderSpec extends BenchmarkSpecBase {
  "zeroHashCodeStrings" should {
    "generate strings with zero hash code" in {
      HashCodeCollider.zeroHashCodeStrings.take(100000).foldLeft(0)(_ | _.hashCode) shouldBe 0
    }
  }
}