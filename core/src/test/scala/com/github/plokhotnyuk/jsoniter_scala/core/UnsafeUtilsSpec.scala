package com.github.plokhotnyuk.jsoniter_scala.core

import org.scalatest.{Matchers, WordSpec}

class UnsafeUtilsSpec extends WordSpec with Matchers {
  "UnsafeUtils" should {
    "be safe when getLatin1Array is called" in {
      UnsafeUtils.getLatin1Array("s")
      UnsafeUtils.getLatin1Array(null) shouldBe null
    }
  }
}
