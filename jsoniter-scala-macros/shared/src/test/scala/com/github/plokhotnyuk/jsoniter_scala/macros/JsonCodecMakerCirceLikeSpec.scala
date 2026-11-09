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

import java.time.{Instant, LocalDate}
import java.util.UUID
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Try

object CirceLikeConformance {
  case class Numbers(b: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, bi: BigInt, bd: BigDecimal)

  case class Others(b: Boolean, c: Char, s: String, u: UUID, ld: LocalDate, in: Instant, t: (Int, String))

  case class Options(o: Option[Int], oo: Option[Option[Int]], loo: List[Option[Option[Int]]])

  case class Collections(l: List[Int], v: Vector[String], s: Set[Int], m: Map[String, Int], im: Map[Int, String])

  case class Defaults(l: List[Int] = List(1), o: Option[Int] = Some(2), i: Int = 3, s: String = "VVV")

  case class Empty()

  sealed trait Adt

  case object AdtObject extends Adt

  case class AdtClass(x: Int) extends Adt

  sealed trait NestedAdt extends Adt

  case object NestedAdtObject extends NestedAdt

  case class NestedAdtClass(y: Int) extends NestedAdt

  case class Outer(a: Adt, e: Empty, o: Option[Adt])

  object circe {
    implicit val circeNumbersCodec: io.circe.Codec[Numbers] = deriveCodec
    implicit val circeOthersCodec: io.circe.Codec[Others] = deriveCodec
    implicit val circeOptionsCodec: io.circe.Codec[Options] = deriveCodec
    implicit val circeCollectionsCodec: io.circe.Codec[Collections] = deriveCodec
    implicit val circeDefaultsCodec: io.circe.Codec[Defaults] = deriveCodec
    implicit val circeEmptyCodec: io.circe.Codec[Empty] = deriveCodec
    implicit val circeAdtObjectCodec: io.circe.Codec[AdtObject.type] = deriveCodec
    implicit val circeAdtClassCodec: io.circe.Codec[AdtClass] = deriveCodec
    implicit val circeNestedAdtObjectCodec: io.circe.Codec[NestedAdtObject.type] = deriveCodec
    implicit val circeNestedAdtClassCodec: io.circe.Codec[NestedAdtClass] = deriveCodec
    implicit val circeNestedAdtCodec: io.circe.Codec[NestedAdt] = deriveCodec
    implicit val circeAdtCodec: io.circe.Codec[Adt] = deriveCodec
    implicit val circeOuterCodec: io.circe.Codec[Outer] = deriveCodec
  }

  object jsoniter {
    implicit val jsoniterNumbersCodec: JsonValueCodec[Numbers] = makeCirceLike
    implicit val jsoniterOthersCodec: JsonValueCodec[Others] = makeCirceLike
    implicit val jsoniterOptionsCodec: JsonValueCodec[Options] = makeCirceLike
    implicit val jsoniterCollectionsCodec: JsonValueCodec[Collections] = makeCirceLike
    implicit val jsoniterDefaultsCodec: JsonValueCodec[Defaults] = makeCirceLike
    implicit val jsoniterEmptyCodec: JsonValueCodec[Empty] = makeCirceLike
    implicit val jsoniterAdtCodec: JsonValueCodec[Adt] = makeCirceLike
    implicit val jsoniterOuterCodec: JsonValueCodec[Outer] = makeCirceLike
  }
}

class JsonCodecMakerCirceLikeSpec extends AnyWordSpec with Matchers {
  import CirceLikeConformance._
  import CirceLikeConformance.circe._
  import CirceLikeConformance.jsoniter._

  def circeDecode[A](json: String)(implicit d: Decoder[A]): Option[A] = io.circe.parser.decode[A](json).toOption

  def jsoniterDecode[A](json: String)(implicit c: JsonValueCodec[A]): Option[A] = Try(readFromString[A](json)).toOption

  def conformDecoding[A](jsons: String*)(implicit d: Decoder[A], c: JsonValueCodec[A]): Unit = jsons.foreach { json =>
    withClue(s"decoding of $json: ") {
      jsoniterDecode[A](json) shouldBe circeDecode[A](json)
    }
  }

  def conformEncoding[A](xs: A*)(implicit e: Encoder[A], d: Decoder[A], c: JsonValueCodec[A]): Unit = xs.foreach { x =>
    withClue(s"encoding of $x: ") {
      val json = writeToString(x)
      json shouldBe e(x).noSpaces
      circeDecode[A](json) shouldBe jsoniterDecode[A](json)
    }
  }

  "JsonCodecMaker.makeCirceLike" should {
    "encode and decode numbers like circe with its default derivation" in {
      // Numbers are selected to be parsed and serialized by circe in the same way on all platforms: JS parses them
      // to doubles, and serializes floating-point numbers using JS formatting
      conformEncoding(
        Numbers(1, 2, 3, 4L, 5.5f, 6.6, BigInt(7), BigDecimal("8.80")),
        Numbers(-1, -2, Int.MinValue, 9007199254740992L, -0.25f, -1.5, BigInt(-9007199254740992L), BigDecimal("1E+3")),
        Numbers(0, 0, 0, 0L, 0.5f, 1.25, BigInt(0), BigDecimal(0)))
      conformDecoding[Numbers](
        """{"b":1,"s":2,"i":3,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8.80}""",
        """{"b":"1","s":"2.0","i":3e0,"l":"4","f":"5.5","d":"6.6","bi":"7.0","bd":"8.80"}""",
        """{"b":1.0,"s":20e-1,"i":3E+0,"l":4.000,"f":5,"d":6,"bi":7e0,"bd":-0}""",
        """{"b":-0,"s":2,"i":3,"l":9007199254740992,"f":5,"d":-0.0,"bi":7,"bd":8}""",
        """{"b":128,"s":2,"i":3,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":32768,"i":3,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":2,"i":3.5,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":2,"i":3,"l":4,"f":5.5,"d":6.6,"bi":7.5,"bd":8}""",
        """{"b":1,"s":2,"i":3,"l":4,"f":"NaN","d":"Infinity","bi":7,"bd":8}""",
        """{"b":1,"s":2,"i":" 3","l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":2,"i":"","l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":2,"i":true,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":2,"i":null,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""",
        """{"b":1,"s":2,"l":4,"f":5.5,"d":6.6,"bi":7,"bd":8}""")
    }
    "encode and decode other primitives, strings, UUIDs, java.time values, and tuples like circe with its default derivation" in {
      conformEncoding(
        Others(b = true, 'x', "a\"b\\c\n\t\u0001é /", UUID.fromString("00000000-0000-0001-0000-000000000002"),
          LocalDate.of(2020, 1, 2), Instant.parse("2020-01-02T03:04:05.006Z"), (1, "VVV")))
      conformDecoding[Others](
        """{"b":true,"c":"x","s":"A","u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":"true","c":"x","s":"a","u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":true,"c":"xy","s":"a","u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":true,"c":"x","s":1,"u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":true,"c":"x","s":null,"u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":true,"c":"x","s":"a","u":"x","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":true,"c":"x","s":"a","u":"00000000-0000-0001-0000-000000000002","ld":"2020-1-2","in":"2020-01-02T03:04:05Z","t":[1,"VVV"]}""",
        """{"b":true,"c":"x","s":"a","u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1,"VVV",2]}""",
        """{"b":true,"c":"x","s":"a","u":"00000000-0000-0001-0000-000000000002","ld":"2020-01-02","in":"2020-01-02T03:04:05Z","t":[1]}""")
    }
    "encode and decode options like circe with its default derivation" in {
      conformEncoding(
        Options(None, None, Nil),
        Options(Some(1), Some(None), List(None, Some(None), Some(Some(2)))),
        Options(Some(1), Some(Some(2)), List(Some(Some(3)))))
      conformDecoding[Options](
        """{"loo":[]}""",
        """{"o":null,"oo":null,"loo":[null,1]}""",
        """{"o":1,"oo":2,"loo":[3]}""",
        """{"o":"1","oo":"2","loo":["3"]}""",
        """{"o":[],"oo":2,"loo":[3]}""",
        """{"o":1,"oo":2}""",
        """{"o":1,"oo":2,"loo":null}""")
    }
    "encode and decode collections like circe with its default derivation" in {
      conformEncoding(
        Collections(List(1, 2), Vector("VVV"), Set(3), Map("VVV" -> 4), Map(5 -> "VVV")),
        Collections(Nil, Vector(), Set(), Map(), Map()))
      conformDecoding[Collections](
        """{"l":[1,2],"v":["VVV"],"s":[3,3],"m":{"VVV":4,"VVV":5},"im":{"5":"VVV"}}""",
        """{"l":[],"v":[],"s":[],"m":{},"im":{}}""",
        """{"l":null,"v":[],"s":[],"m":{},"im":{}}""",
        """{"l":[],"v":null,"s":[],"m":{},"im":{}}""",
        """{"l":[],"v":[],"s":null,"m":{},"im":{}}""",
        """{"l":[],"v":[],"s":[],"m":null,"im":{}}""",
        """{"l":[],"v":[],"s":[],"m":{},"im":null}""",
        """{"v":[],"s":[],"m":{},"im":{}}""",
        """{"l":[],"v":[],"s":[],"im":{}}""",
        """{"l":[null],"v":[],"s":[],"m":{},"im":{}}""",
        """{"l":[],"v":[],"s":[],"m":{"VVV":null},"im":{}}""",
        """{"l":[],"v":[],"s":[],"m":{},"im":{"x":"VVV"}}""",
        """{"l":{},"v":[],"s":[],"m":[],"im":{}}""")
    }
    "ignore default values of fields like circe with its default derivation" in {
      conformEncoding(Defaults(), Defaults(Nil, None, 0, ""))
      conformDecoding[Defaults](
        """{}""",
        """{"l":[1],"o":2,"i":3}""",
        """{"l":[1],"i":3,"s":"VVV"}""",
        """{"l":[1],"o":2,"i":3,"s":"VVV"}""",
        """{"l":[],"o":null,"i":3,"s":"VVV"}""",
        """{"l":null,"o":2,"i":3,"s":"VVV"}""",
        """{"l":[1],"o":2,"i":null,"s":"VVV"}""",
        """{"l":[1],"o":2,"i":3,"s":null}""")
    }
    "encode and decode classes and ADTs like circe with its default derivation" in {
      conformEncoding(Empty())
      conformEncoding[Adt](AdtObject, AdtClass(1), NestedAdtObject, NestedAdtClass(2))
      conformEncoding(Outer(AdtClass(1), Empty(), Some(AdtObject)), Outer(AdtObject, Empty(), None))
      conformDecoding[Empty]("{}", """{"x":1}""", """{"x":1,"x":2}""", "[]", "null", "1")
      conformDecoding[Adt](
        """{"AdtObject":{}}""",
        """{"AdtObject":{"x":1}}""",
        """{"AdtClass":{"x":1}}""",
        """{"AdtClass":{"x":1,"x":2}}""",
        """{"AdtClass":{"x":1,"y":2}}""",
        """{"AdtClass":{}}""",
        """{"NestedAdtObject":{}}""",
        """{"NestedAdtClass":{"y":2}}""",
        """{"Unknown":{}}""",
        """{"AdtObject":1}""",
        """{"AdtObject":[]}""",
        """{"type":"AdtObject"}""",
        "\"AdtObject\"",
        "{}",
        "null")
      conformDecoding[Outer](
        """{"a":{"AdtObject":{}},"e":{},"o":null}""",
        """{"a":{"AdtObject":{}},"e":{}}""",
        """{"a":{"AdtClass":{"x":1}},"e":{},"o":{"AdtObject":{}}}""",
        """{"a":null,"e":{},"o":null}""",
        """{"a":{"AdtObject":{}},"e":null,"o":null}""",
        """{"e":{},"o":null}""")
    }
    "have known differences from circe with its default derivation" in {
      // Both libraries reject them, but circe picks one of keys (the first one for Scala 3 and the last one for Scala 2)
      circeDecode[Adt]("""{"AdtObject":{},"AdtClass":{"x":1}}""") should not be empty
      jsoniterDecode[Adt]("""{"AdtObject":{},"AdtClass":{"x":1}}""") shouldBe empty
      // circe rejects JSON `null` values for Scala objects
      circeDecode[Adt]("""{"AdtObject":null}""") shouldBe empty
      jsoniterDecode[Adt]("""{"AdtObject":null}""") shouldBe Some(AdtObject)
    }
  }
}
