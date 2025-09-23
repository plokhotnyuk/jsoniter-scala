package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

class JsonCodecMakerNamedTupleSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize named tuples" in {
      verifySerDeser(make[NamedTuple.Empty], EmptyTuple, """{}""")
      verifySerDeser(make[(i: Int)], (i = 1), """{"i":1}""")
      verifySerDeser(make[(i: Int, s: String)], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
    }
    "serialize and deserialize generic named tuples" in {
      type GenericNamedTuple[A, B] = (a: A, b: B)

      verifySerDeser(make[GenericNamedTuple[Option[Int], List[String]]], (a = Some(1), b = List("VVV")), """{"a":1,"b":["VVV"]}""")
    }
    "serialize and deserialize higher-kind named tuples" in {
      type HKNamedTuple[F[_], G[_]] = (i: F[Int], s: G[String])

      verifySerDeser(make[HKNamedTuple[Option, List]], (i = Some(1), s = List("VVV")), """{"i":1,"s":["VVV"]}""")
    }
    "serialize and deserialize complex named tuples" in {
      case class Record(i: Int, s: String)

      type *:: = *:
      type Names = "i" *:: "s" *:: EmptyTuple
      type I = Int
      type S = String
      type Values = I *:: S *:: EmptyTuple

      verifySerDeser(make[NamedTuple.NamedTuple[Names, Values]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.From[Record]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.Reverse[(i: Int, s: String)]], (s = "VVV", i = 1), """{"s":"VVV","i":1}""")
      verifySerDeser(make[NamedTuple.Concat[(i: Int), (s: String)]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.Tail[(l: Long, i: Int, s: String)]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.Init[(i: Int, s: String, l: Long)]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.Drop[(l: Long, i: Int, s: String), 1]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.Drop[(l: Long, i: Int, s: String), 3]], EmptyTuple, """{}""")
      verifySerDeser(make[NamedTuple.Take[(i: Int, s: String, l: Long), 2]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.NamedTuple["i" *: "s" *: EmptyTuple, (Int, String)]],
        (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.NamedTuple["i" *: ("s", "l"), (Int, String, Long)]],
        (i = 1, s = "VVV", l = 2L), """{"i":1,"s":"VVV","l":2}""")
      verifySerDeser(make[NamedTuple.NamedTuple[("i", "s"), Int *: String *: EmptyTuple]],
        (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
      verifySerDeser(make[NamedTuple.NamedTuple[("i", "s", "l"), Int *: Tuple2[String, Long]]],
        (i = 1, s = "VVV", l = 2L), """{"i":1,"s":"VVV","l":2}""")
      verifySerDeser(make[NamedTuple.Split[(i: Int, s: String, l: Long), 2]],
        ((i = 1, s = "VVV"), (l = 2L)), """[{"i":1,"s":"VVV"},{"l":2}]""")
      verifySerDeser(make[NamedTuple.Zip[(i: Int, s: String), (i: Long, s: String)]],
        (i = (1, 2L), s = ("VVV", "WWW")), """{"i":[1,2],"s":["VVV","WWW"]}""")
      verifySerDeser(make[NamedTuple.Map[(i: Int, s: String), Option]],
        (i = Some(1), s = Some("VVV")), """{"i":1,"s":"VVV"}""")
    }
    "serialize and deserialize nested named tuples" in {
      verifySerDeser(make[(i: Int, t: (d: Double, s: String))], (i = 1, t = (d = 2.0, s = "VVV")),
        """{"i":1,"t":{"d":2.0,"s":"VVV"}}""")
    }
    "serialize and deserialize named tuples using field name mapping" in {
      verifySerDeser(make[(intField: Int, strField: String)](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case)),
        (intField = 1, strField = "VVV"), """{"int_field":1,"str_field":"VVV"}""")
    }
    "serialize and deserialize named tuples with more than 22 arity" in {
      verifySerDeser(make[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: Int, i23: Int)],
        (i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6, i7 = 7, i8 = 8, i9 = 9, i10 = 10, i11 = 11, i12 = 12, i13 = 13, i14 = 14, i15 = 15, i16 = 16, i17 = 17, i18 = 18, i19 = 19, i20 = 20, i21 = 21, i22 = 22, i23 = 23),
        """{"i1":1,"i2":2,"i3":3,"i4":4,"i5":5,"i6":6,"i7":7,"i8":8,"i9":9,"i10":10,"i11":11,"i12":12,"i13":13,"i14":14,"i15":15,"i16":16,"i17":17,"i18":18,"i19":19,"i20":20,"i21":21,"i22":22,"i23":23}""")
    }
    "serialize and deserialize a case class with fields that are named tuples" in {
      case class NamedTupleFields(f1: (i: Int, s: String), f2: (d: Double, s: String))

      verifySerDeser(make[NamedTupleFields], NamedTupleFields((i = 1, s = "VVV"), (d = 2.0, s = "WWW")),
        """{"f1":{"i":1,"s":"VVV"},"f2":{"d":2.0,"s":"WWW"}}""")
    }
    "serialize and deserialize a collection with fields that are named tuples" in {
      verifySerDeser(make[List[(i: Int, s: String)]], List((i = 1, s = "VVV"), (i = 2, s = "WWW")),
        """[{"i":1,"s":"VVV"},{"i":2,"s":"WWW"}]""")
    }
    "serialize and deserialize complex generic and named tuples" in {
      verifySerDeser(make[Tuple.++[(Byte, Short), (Int, Long)]], (1: Byte, 2: Short, 3, 4L), "[1,2,3,4]")
      verifySerDeser(make[Tuple.Reverse[(String, Int)]], (1, "VVV"), """[1,"VVV"]""")
      verifySerDeser(make[NamedTuple.DropNames[(i: Int, s: String)]], (1, "VVV"), """[1,"VVV"]""")
    }
    "serialize and deserialize recursive regular and named tuples" in {
      type RecursiveTuple = (Int, Option[RecursiveTuple])
      type RecursiveNamedTuple = (value: Int, next: Option[RecursiveNamedTuple])
      verifySerDeser(make[RecursiveTuple](CodecMakerConfig.withAllowRecursiveTypes(true)),
        (1, Some((2, None))), "[1,[2,null]]")
      verifySerDeser(make[RecursiveNamedTuple](CodecMakerConfig.withAllowRecursiveTypes(true)),
        (value = 1, next = Some((2, None))), """{"value":1,"next":{"value":2}}""")
    }
  }
}
