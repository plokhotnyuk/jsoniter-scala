package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

class JsonCodecMakerGenericTuplesSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize tuples with arities greater than 22" in {
      verifySerDeser(make[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, String)],
        (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, "24"),
        """[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,"24"]""")
    }
    "serialize and deserialize generic tuples" in {
      type B = Byte
      type I = Int
      type L = Long
      type S = Short

      verifySerDeser(make[EmptyTuple], EmptyTuple, "[]")
      verifySerDeser(make[Tuple.Drop[(Long, Int, String), 3]], EmptyTuple, "[]")
      verifySerDeser(make[Byte *: Short *: Int *: Long *: EmptyTuple], (1: Byte, 2: Short, 3, 4L), "[1,2,3,4]")
      verifySerDeser(make[B *: S *: I *: L *: EmptyTuple], (1: B) *: (2: S) *: 3 *: 4L *: EmptyTuple, "[1,2,3,4]")
      verifySerDeser(make[Byte *: Short *: Tuple2[Int, Long]], (1: Byte, 2: Short, 3, 4L), "[1,2,3,4]")
      verifySerDeser(make[Tuple.Concat[(Byte, Short), (Int, Long)]], (1: Byte, 2: Short, 3, 4L), "[1,2,3,4]")
      verifySerDeser(make[Tuple.Append[(Byte, Short), Int]], (1: Byte, 2: Short, 3), "[1,2,3]")
      verifySerDeser(make[Tuple.Drop[(Long, Int, String), 1]], (1, "VVV"), """[1,"VVV"]""")
      verifySerDeser(make[Tuple.Take[(Int, String, Long), 2]], 1 *: "VVV" *: EmptyTuple, """[1,"VVV"]""")
      verifySerDeser(make[Tuple.Zip[Tuple1[Int], Tuple1[String]]], (1, "VVV") *: EmptyTuple, """[[1,"VVV"]]""")
      verifySerDeser(make[Tuple.Map[(Int, String), Option]], (Some(1), Some("VVV")), """[1,"VVV"]""")
      verifySerDeser(make[Tuple.InverseMap[(Option[Int], Option[String]), Option]], (1, "VVV"), """[1,"VVV"]""")
    }
  }
}
