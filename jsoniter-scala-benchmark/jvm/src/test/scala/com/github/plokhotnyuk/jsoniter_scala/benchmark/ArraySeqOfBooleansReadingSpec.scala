package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArraySeqOfBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArraySeqOfBooleansReading = new ArraySeqOfBooleansReading {
    setup()
  }

  "ArraySeqOfBooleansReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJawn() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      //FIXME: DSL-JSON doesn't support parsing of ArraySeq
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      //FIXME: jackson throws java.lang.ClassCastException: class scala.collection.immutable.Vector2 cannot be cast to class scala.collection.immutable.ArraySeq
      //benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      //FIXME: spray-json doesn't support parsing of ArraySeq
      //benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      //FIXME: zio-json doesn't support parsing of ArraySeq
      //benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJawn())
      intercept[Throwable](b.circeJsoniter())
      //FIXME: DSL-JSON doesn't support parsing of ArraySeq
      //intercept[Throwable](b.dslJsonScala())
      //FIXME: jackson throws java.lang.ClassCastException: class scala.collection.immutable.Vector2 cannot be cast to class scala.collection.immutable.ArraySeq
      //intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      //FIXME: spray-json doesn't support parsing of ArraySeq
      //intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      //FIXME: zio-json doesn't support parsing of ArraySeq
      //intercept[Throwable](b.zioJson())
    }
  }
}