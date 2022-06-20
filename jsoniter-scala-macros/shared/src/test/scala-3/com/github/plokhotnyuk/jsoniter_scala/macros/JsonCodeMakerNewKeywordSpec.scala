package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalatest.exceptions.TestFailedException

case class DeResult[T](isSucceed: Boolean, data: T, message: String)
case class RootPathFiles(files: List[String])

class JsonCodecMakerNewKeywordSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize generic classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """given JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
          |given JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make""".stripMargin
      }).getMessage.contains {
        "given_JsonValueCodec_DeResult is already defined as given instance given_JsonValueCodec_DeResult"
      })

      given codecOfDeResult1: JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
      given codecOfDeResult2: JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make

      verifySerDeser(summon[JsonValueCodec[DeResult[RootPathFiles]]],
        DeResult[RootPathFiles](true, RootPathFiles(List("VVV")), "WWW"),
        """{"isSucceed":true,"data":{"files":["VVV"]},"message":"WWW"}""")
      verifySerDeser(summon[JsonValueCodec[DeResult[Option[String]]]],
        DeResult[Option[String]](false, Option("VVV"), "WWW"),
        """{"isSucceed":false,"data":"VVV","message":"WWW"}""")
    }
    "serialize and deserialize Scala3 enum ADTs defined with `derives` keyword" in {
      trait DefaultJsonValueCodec[A] extends JsonValueCodec[A]

      object DefaultJsonValueCodec {
        inline def derived[A]: DefaultJsonValueCodec[A] = new DefaultJsonValueCodec[A] {
          private val impl = JsonCodecMaker.make[A](CodecMakerConfig.withDiscriminatorFieldName(Some("name")))
          export impl._
        }
      }

      enum TestEnum derives DefaultJsonValueCodec:
        case Value1
        case Value2(string: String)

      verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"name":"Value2","string":"VVV"}""")
    }
  }
}