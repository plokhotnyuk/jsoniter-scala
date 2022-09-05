package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalatest.exceptions.TestFailedException

class JsonCodecMakerNewKeywordSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "don't generate codecs for generic classes using anonymous given constants" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class DeResult[A](isSucceed: Boolean, data: A, message: String)
          |
          |case class RootPathFiles(files: List[String])
          |
          |given JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
          |given JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make""".stripMargin
      }).getMessage.contains {
        "given_JsonValueCodec_DeResult is already defined as given instance given_JsonValueCodec_DeResult"
      })
    }
    "serialize and deserialize generic classes using named given constants" in {
      case class DeResult[A](isSucceed: Boolean, data: A, message: String)

      case class RootPathFiles(files: List[String])

      given codecOfDeResult1: JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
      given codecOfDeResult2: JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make

      verifySerDeser(summon[JsonValueCodec[DeResult[RootPathFiles]]],
        DeResult[RootPathFiles](true, RootPathFiles(List("VVV")), "WWW"),
        """{"isSucceed":true,"data":{"files":["VVV"]},"message":"WWW"}""")
      verifySerDeser(summon[JsonValueCodec[DeResult[Option[String]]]],
        DeResult[Option[String]](false, Option("VVV"), "WWW"),
        """{"isSucceed":false,"data":"VVV","message":"WWW"}""")
    }
    "serialize and deserialize generic classes using a given function" in {
      case class GenDoc[A, B, C](a: A, opt: Option[B], list: List[C])

      object GenDoc:
        given [A, B, C](using JsonValueCodec[A], JsonValueCodec[B], JsonValueCodec[C]): JsonValueCodec[GenDoc[A, B, C]] =
          JsonCodecMaker.make

      implicit val aCodec: JsonValueCodec[Boolean] = JsonCodecMaker.make
      implicit val bCodec: JsonValueCodec[String] = JsonCodecMaker.make
      implicit val cCodec: JsonValueCodec[Int] = JsonCodecMaker.make

      verifySerDeser(summon[JsonValueCodec[GenDoc[Boolean, String, Int]]],
        GenDoc(true, Some("VVV"), List(1, 2, 3)), """{"a":true,"opt":"VVV","list":[1,2,3]}""")
    }
    "don't generate codecs for generic classes and a `given` function with a missing codec" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class GenDoc[A, B, C](a: A, opt: Option[B], list: List[C])
          |
          |object GenDoc:
          |  given [A, B, C](using JsonValueCodec[B], JsonValueCodec[C]): JsonValueCodec[GenDoc[A, B, C]] =
          |    JsonCodecMaker.make
          |""".stripMargin
      }).getMessage.contains {
        "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for 'A'."
      })
    }
    "serialize and deserialize Scala3 enum ADTs defined with `derives` keyword" in {
      trait DefaultJsonValueCodec[A] extends JsonValueCodec[A]

      object DefaultJsonValueCodec:
        inline def derived[A]: DefaultJsonValueCodec[A] = new:
          private val impl = JsonCodecMaker.make[A](CodecMakerConfig.withDiscriminatorFieldName(Some("name")))
          export impl._

      enum TestEnum derives DefaultJsonValueCodec:
        case Value1
        case Value2(string: String)

      verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"name":"Value2","string":"VVV"}""")
    }
  }
}