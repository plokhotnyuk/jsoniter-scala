package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
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

      given codecOfDeResult1: JsonValueCodec[DeResult[Option[String]]] = make
      given codecOfDeResult2: JsonValueCodec[DeResult[RootPathFiles]] = make
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
        given [A : JsonValueCodec, B : JsonValueCodec, C : JsonValueCodec]: JsonValueCodec[GenDoc[A, B, C]] = make

      given aCodec: JsonValueCodec[Boolean] = make
      given bCodec: JsonValueCodec[String] = make
      given cCodec: JsonValueCodec[Int] = make
      verifySerDeser(summon[JsonValueCodec[GenDoc[Boolean, String, Int]]],
        GenDoc(true, Some("VVV"), List(1, 2, 3)), """{"a":true,"opt":"VVV","list":[1,2,3]}""")
    }
    "don't generate codecs for generic classes and a `given` function with a missing codec" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class GenDoc[A, B, C](a: A, opt: Option[B], list: List[C])
          |
          |object GenDoc:
          |  given [A, B : JsonValueCodec, C : JsonValueCodec]: JsonValueCodec[GenDoc[A, B, C]] = JsonCodecMaker.make
          |""".stripMargin
      }).getMessage.contains {
        "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for 'A'."
      })
    }
    "serialize and deserialize classes defined with `derives` keyword and different compile-time configurations" in {
      trait ConfigurableJsonValueCodec[A] extends JsonValueCodec[A]

      object ConfigurableJsonValueCodec:
        inline def derived[A](using inline config: CodecMakerConfig = CodecMakerConfig): ConfigurableJsonValueCodec[A] = new:
          private val impl = JsonCodecMaker.make[A](config)
          export impl._

      {
        enum TestEnum derives ConfigurableJsonValueCodec:
          case Value1
          case Value2(string: String)

        verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"type":"Value2","string":"VVV"}""")
      }
      {
        inline given CodecMakerConfig = CodecMakerConfig.withDiscriminatorFieldName(Some("name"))

        enum TestEnum derives ConfigurableJsonValueCodec:
          case Value1
          case Value2(string: String)

        verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"name":"Value2","string":"VVV"}""")
      }
      {
        inline given CodecMakerConfig = CodecMakerConfig.withDiscriminatorFieldName(Some("hint")).withFieldNameMapper {
          case "string" => "str"
        }

        enum TestEnum derives ConfigurableJsonValueCodec:
          case Value1
          case Value2(string: String)

        verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"hint":"Value2","str":"VVV"}""")
      }
    }
    "serialize and deserialize option types using a custom codec to handle missing fields and 'null' values differently" in {
      object CustomOptionCodecs {
        inline given optionCodec[A](using inline config: CodecMakerConfig = CodecMakerConfig): JsonValueCodec[Option[A]] =
          new JsonValueCodec[Option[A]] {
            private val aCodec = make[A](config)

            override def decodeValue(in: JsonReader, default: Option[A]): Option[A] =
              if (in.isNextToken('n')) in.readNullOrError(None, "expected 'null' or JSON value")
              else {
                in.rollbackToken()
                Some(aCodec.decodeValue(in, aCodec.nullValue))
              }

            override def encodeValue(x: Option[A], out: JsonWriter): Unit =
              if (x eq None) out.writeNull()
              else aCodec.encodeValue(x.get, out)

            override def nullValue: Option[A] = None
          }
      }

      case class Ex(opt: Option[String] = Some("hiya"), level: Option[Int] = Some(10))

      inline given codecMakerConfig: CodecMakerConfig = CodecMakerConfig.withIsStringified(true)

      import CustomOptionCodecs.given

      val codecOfEx = make[Ex](CodecMakerConfig.withTransientNone(false))
      verifySerDeser(codecOfEx, Ex(None, None), """{"opt":null,"level":null}""")
      verifySerDeser(codecOfEx, Ex(Some("hiya"), Some(10)), """{}""")
      verifySerDeser(codecOfEx, Ex(Some("pakikisama"), Some(5)), """{"opt":"pakikisama","level":"5"}""")
    }
  }
}