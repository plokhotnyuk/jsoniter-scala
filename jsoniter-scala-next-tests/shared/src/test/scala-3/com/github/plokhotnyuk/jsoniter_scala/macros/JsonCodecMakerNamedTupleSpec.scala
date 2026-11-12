/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
    "deserialize named tuples with fields in any order and skip unexpected fields" in {
      val codec = make[(i: Int, s: String)]
      verifyDeser(codec, (i = 1, s = "VVV"), """{"x":[1,{"y":null}],"s":"VVV","i":1}""")
      verifyDeser(make[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: Int, i23: String)],
        (i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6, i7 = 7, i8 = 8, i9 = 9, i10 = 10, i11 = 11, i12 = 12, i13 = 13, i14 = 14, i15 = 15, i16 = 16, i17 = 17, i18 = 18, i19 = 19, i20 = 20, i21 = 21, i22 = 22, i23 = "VVV"),
        """{"i23":"VVV","i22":22,"i21":21,"i20":20,"i19":19,"i18":18,"i17":17,"i16":16,"i15":15,"i14":14,"i13":13,"i12":12,"i11":11,"i10":10,"i9":9,"i8":8,"i7":7,"i6":6,"i5":5,"i4":4,"i3":3,"i2":2,"i1":1}""")
    }
    "throw parse exception for missing, duplicated, or unexpected fields of named tuples" in {
      val codec = make[(i: Int, s: String)]
      verifyDeserError(codec, """{"i":1}""", """missing required field "s", offset: 0x00000006""")
      verifyDeserError(codec, """{"s":"VVV"}""", """missing required field "i", offset: 0x0000000a""")
      verifyDeserError(codec, """{"i":1,"i":2,"s":"VVV"}""", """duplicated field "i", offset: 0x0000000a""")
      verifyDeser(make[(i: Int, s: String)](CodecMakerConfig.withCheckFieldDuplication(false)), (i = 2, s = "VVV"), """{"i":1,"i":2,"s":"VVV"}""")
      verifyDeserError(codec, """[1,"VVV"]""", """expected '{', offset: 0x00000000""")
      verifyDeserError(make[(i: Int, s: String)](CodecMakerConfig.withSkipUnexpectedFields(false)), """{"i":1,"s":"VVV","x":0}""",
        """unexpected field "x", offset: 0x00000014""")
      verifyDeserError(make[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: Int, i23: Int)],
        """{"i1":1,"i2":2,"i3":3,"i4":4,"i5":5,"i6":6,"i7":7,"i8":8,"i9":9,"i10":10,"i11":11,"i12":12,"i13":13,"i14":14,"i15":15,"i16":16,"i17":17,"i18":18,"i19":19,"i20":20,"i21":21,"i22":22}""",
        """missing required field "i23", offset: 0x000000b4""")
    }
    "serialize and deserialize named tuples with optional and collection fields" in {
      verifySerDeser(make[(o: Option[Int], l: List[Int], s: String)], (o = None, l = Nil, s = "VVV"), """{"s":"VVV"}""")
      verifySerDeser(make[(o: Option[Int], l: List[Int], s: String)], (o = Some(1), l = List(2), s = "VVV"), """{"o":1,"l":[2],"s":"VVV"}""")
      verifySerDeser(make[(o: Option[Int], l: List[Int], s: String)](CodecMakerConfig.withTransientNone(false).withTransientEmpty(false)),
        (o = None, l = Nil, s = "VVV"), """{"o":null,"l":[],"s":"VVV"}""")
      verifyDeserError(make[(o: Option[Int], l: List[Int], s: String)](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        """{"s":"VVV"}""", """missing required field "l", offset: 0x0000000a""")
    }
    "serialize and deserialize named tuples with array and iterator fields" in {
      val codec = make[(a: Array[Int], ia: IArray[Int], s: String)]
      verifySer(codec, (a = Array[Int](), ia = IArray[Int](), s = "VVV"), """{"s":"VVV"}""")
      verifySer(codec, (a = Array(1), ia = IArray(2), s = "VVV"), """{"a":[1],"ia":[2],"s":"VVV"}""")
      verifySer(make[(a: Array[Int], ia: IArray[Int], s: String)](CodecMakerConfig.withTransientEmpty(false)),
        (a = Array[Int](), ia = IArray[Int](), s = "VVV"), """{"a":[],"ia":[],"s":"VVV"}""")
      verifyDeserByCheck(codec, """{"a":[1],"ia":[2],"s":"VVV"}""", check = (x: (a: Array[Int], ia: IArray[Int], s: String)) => {
        x.a.toList shouldBe List(1)
        x.ia.toList shouldBe List(2)
        x.s shouldBe "VVV"
      })
      writeToString((i = Iterator.empty[Int], s = "VVV"))(make[(i: Iterator[Int], s: String)]) shouldBe """{"s":"VVV"}"""
      writeToString((i = Iterator(1, 2), s = "VVV"))(make[(i: Iterator[Int], s: String)]) shouldBe """{"i":[1,2],"s":"VVV"}"""
      writeToString((i = Iterator.empty[Int], s = "VVV"))(make[(i: Iterator[Int], s: String)](CodecMakerConfig.withTransientEmpty(false))) shouldBe
        """{"i":[],"s":"VVV"}"""
    }
    "deserialize named tuples with null collections only when it is allowed" in {
      verifyDeser(make[(l: List[Int], s: String)], (l = Nil, s = "VVV"), """{"l":null,"s":"VVV"}""")
      verifyDeserError(make[(l: List[Int], s: String)](CodecMakerConfig.withAllowNullForCollections(false)), """{"l":null,"s":"VVV"}""",
        """expected '[', offset: 0x00000005""")
    }
    "serialize and deserialize named tuples with nested options" in {
      val codec = make[(o: Option[Option[Int]], s: String)](CodecMakerConfig.withSkipNestedOptionValues(true).withDistinguishNullAndMissingNestedOptions(true))
      verifySerDeser(codec, (o = None, s = "VVV"), """{"s":"VVV"}""")
      verifySerDeser(codec, (o = Some(None), s = "VVV"), """{"o":null,"s":"VVV"}""")
      verifySerDeser(codec, (o = Some(Some(1)), s = "VVV"), """{"o":1,"s":"VVV"}""")
    }
    "serialize and deserialize named tuples with field names that require encoding" in {
      verifySerDeser(make[(`ключ`: Int, `a"b`: String)], (`ключ` = 1, `a"b` = "VVV"), """{"ключ":1,"a\"b":"VVV"}""")
    }
    "serialize and deserialize named tuples with arity 22" in {
      verifySerDeser(make[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: String)],
        (i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6, i7 = 7, i8 = 8, i9 = 9, i10 = 10, i11 = 11, i12 = 12, i13 = 13, i14 = 14, i15 = 15, i16 = 16, i17 = 17, i18 = 18, i19 = 19, i20 = 20, i21 = 21, i22 = "VVV"),
        """{"i1":1,"i2":2,"i3":3,"i4":4,"i5":5,"i6":6,"i7":7,"i8":8,"i9":9,"i10":10,"i11":11,"i12":12,"i13":13,"i14":14,"i15":15,"i16":16,"i17":17,"i18":18,"i19":19,"i20":20,"i21":21,"i22":"VVV"}""")
      verifySerDeser(make[NamedTuple.Concat[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int), (i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: String)]],
        (i1 = 1, i2 = 2, i3 = 3, i4 = 4, i5 = 5, i6 = 6, i7 = 7, i8 = 8, i9 = 9, i10 = 10, i11 = 11, i12 = 12, i13 = 13, i14 = 14, i15 = 15, i16 = 16, i17 = 17, i18 = 18, i19 = 19, i20 = 20, i21 = 21, i22 = "VVV"),
        """{"i1":1,"i2":2,"i3":3,"i4":4,"i5":5,"i6":6,"i7":7,"i8":8,"i9":9,"i10":10,"i11":11,"i12":12,"i13":13,"i14":14,"i15":15,"i16":16,"i17":17,"i18":18,"i19":19,"i20":20,"i21":21,"i22":"VVV"}""")
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
      type NT[N <: Tuple, V <: Tuple] = NamedTuple.NamedTuple[N, V]
      verifySerDeser(make[NT[Names, Values]], (i = 1, s = "VVV"), """{"i":1,"s":"VVV"}""")
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
      verifySerDeser(make[NamedTuple.DropNames[(i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int, i16: Int, i17: Int, i18: Int, i19: Int, i20: Int, i21: Int, i22: Int, i23: String)]],
        (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, "VVV"),
        """[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,"VVV"]""")
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
