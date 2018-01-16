package com.github.plokhotnyuk.jsoniter_scala.macros

class MissingReqFieldBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new MissingReqFieldBenchmark
  
  "MissingReqFieldBenchmark" should {
    "return some parsing error" in {
      benchmark.readCirce() shouldBe
        "Attempt to decode value on failed cursor: DownField(s)"
      benchmark.readJackson() shouldBe
        """Missing required creator property 's' (index 0)
          | at [Source: (byte[])"{}"; line: 1, column: 2]""".stripMargin
      benchmark.readJsoniter() shouldBe
        """missing required field(s) "s", "i", offset: 0x00000001, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      benchmark.readJsoniterStackless() shouldBe
        """missing required field(s) "s", "i", offset: 0x00000001, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      benchmark.readJsoniterStacklessNoDump() shouldBe
        """missing required field(s) "s", "i", offset: 0x00000001"""
      benchmark.readPlay() shouldBe
        "JsResultException(errors:List((/s,List(JsonValidationError(List(error.path.missing),WrappedArray()))), (/i,List(JsonValidationError(List(error.path.missing),WrappedArray())))))"
    }
  }
}