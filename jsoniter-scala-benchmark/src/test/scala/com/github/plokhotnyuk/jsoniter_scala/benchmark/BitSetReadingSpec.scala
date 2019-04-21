package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BitSetReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new BitSetReading {
    setup()
  }
  
  "BitSetReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe doesn't support parsing of bitsets
      //benchmark.circe() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws scala.collection.immutable.HashSet$HashTrieSet cannot be cast to scala.collection.immutable.BitSet
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      //FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 1 type parameter for collection like types (scala.collection.immutable.BitSet)
      //benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support reading of bitsets
      //benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}