package com.github.plokhotnyuk.jsoniter_scala.benchmark

class IntMapOfBooleansReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new IntMapOfBooleansReading {
    setup()
  }
  
  "IntMapOfBooleansReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for scala.collection.immutable.IntMap[Boolean]
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      //FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 2 type parameters for map like types (scala.collection.immutable.IntMap)
      //benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support IntMap
      //benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}