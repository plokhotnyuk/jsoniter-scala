package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableLongMapOfBooleansReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableLongMapOfBooleansReading {
    setup()
  }
  
  "MutableLongMapOfBooleansReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      //FIXME: DSL-JSON doesn't support mutable.LongMap
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      //FIXME: Jackson throws Need exactly 2 type parameters for map like types (scala.collection.mutable.LongMap)
      //benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
    }
  }
}