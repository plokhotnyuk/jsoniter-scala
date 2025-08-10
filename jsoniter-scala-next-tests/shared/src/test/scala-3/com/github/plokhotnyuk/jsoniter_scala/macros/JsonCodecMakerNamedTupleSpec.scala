package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

class JsonCodecMakerNamedTupleSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize Scala 3 named tuples" in {
      verifySerDeser(make[(i: Int, s: String)], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
    }
    "serialize and deserialize generic Scala 3 named tuples" in {
      type GenericNamedTuple[A, B] = (i: A, s: B)

      verifySerDeser(make[GenericNamedTuple[Option[Int], List[String]]], (i = Some(1), s = List("VVV")),
        """{"i":1,"s":["VVV"]}""")
    }
    "serialize and deserialize nested Scala 3 named tuples" in {
      verifySerDeser(make[(i: Int, t: (d: Double, s: String))], (i = 1, t = (d = 2.0, s = "VVV")),
        """{"i":1,"t":{"d":2.0,"s":"VVV"}}""")
    }
    "serialize and deserialize Scala 3 named tuples using field name mapping" in {
      verifySerDeser(make[(intField: Int, strField: String)](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case)),
        (intField = 1, strField = "VVV"), """{"int_field":1,"str_field":"VVV"}""")
    }
    "serialize and deserialize Scala 3 named tuples with more than 22 arity" in {
      verifySerDeser(make[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: Int, i23: Int)],
        (i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6, i7 = 7, i8 = 8, i9 = 9, i10 = 10, i11 = 11, i12 = 12, i13 = 13, i14 = 14, i15 = 15, i16 = 16, i17 = 17, i18 = 18, i19 = 19, i20 = 20, i21 = 21, i22 = 22, i23 = 23),
        """{"i1":1,"i2":2,"i3":3,"i4":4,"i5":5,"i6":6,"i7":7,"i8":8,"i9":9,"i10":10,"i11":11,"i12":12,"i13":13,"i14":14,"i15":15,"i16":16,"i17":17,"i18":18,"i19":19,"i20":20,"i21":21,"i22":22,"i23":23}""")
    }
    "serialize and deserialize a case class with fields that are Scala 3 named tuples" in {
      case class NamedTupleFields(f1: (i: Int, s: String), f2: (d: Double, s: String))

      verifySerDeser(make[NamedTupleFields], NamedTupleFields((i = 1, s = "VVV"), (d = 2.0, s = "WWW")),
        """{"f1":{"i":1,"s":"VVV"},"f2":{"d":2.0,"s":"WWW"}}""")
    }
    "serialize and deserialize a collection with fields that are Scala 3 named tuples" in {
      verifySerDeser(make[List[(i: Int, s: String)]], List((i = 1, s = "VVV"), (i = 2, s = "WWW")),
        """[{"i":1,"s":"VVV"},{"i":2,"s":"WWW"}]""")
    }
  }
}
