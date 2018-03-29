package com.github.plokhotnyuk.jsoniter_scala.core

import org.scalatest.{Matchers, WordSpec}

class UnsafeUtilsSpec extends WordSpec with Matchers {
  "UnsafeUtils" should {
    "be safe when getLatin1Array is called" in {
      val mayBeArray = UnsafeUtils.getLatin1Array("s") // should be not null for JDK 9+ with a string compaction feature turned on
      if (mayBeArray != null) mayBeArray.deep shouldBe Array('s').deep
      UnsafeUtils.getLatin1Array("ś") shouldBe null
      UnsafeUtils.getLatin1Array(null) shouldBe null
    }
  }
}
