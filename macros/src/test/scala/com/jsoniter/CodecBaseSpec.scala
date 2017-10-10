package com.jsoniter

import org.scalatest.{Matchers, WordSpec}

class CodecBaseSpec extends WordSpec with Matchers {
  "CodecBase.readObjectFieldAsHash" should {
    "compute the same hash value for encoded & non-encoded field names" in {
      hashCode("""Hello""") shouldBe hashCode("Hello")
      hashCode("""Hello""") shouldBe hashCode("\\u0048\\u0065\\u006C\\u006c\\u006f")
      hashCode("""\b\t\n\f\/""") shouldBe hashCode("\b\t\n\f/")
      hashCode("""\b\t\n\f\/Aиბ""") shouldBe hashCode("\\u0008\\u0009\\u000a\\u000C\\u002F\\u0041\\u0438\\u10d1")
      hashCode("𝄞") shouldBe hashCode("\\ud834\\udd1e")
    }
    "throw parsing exception in case of invalid escape character sequence" in {
      assert(intercept[Exception](hashCode("\\x0008")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\u000Z")).getMessage.contains("90 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\u000")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\u00")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\u0")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\udd1e")).getMessage.contains("expect high surrogate character"))
      assert(intercept[Exception](hashCode("\\ud834\\")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\ud834\\x")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\ud834\\ud834")).getMessage.contains("expect low surrogate character"))
    }
  }

  def hashCode(s: String): Long = {
    val buf = s""""$s":""".getBytes("UTF-8")
    CodecBase.readObjectFieldAsHash(JsonIterator.parse(buf))
  }
}
