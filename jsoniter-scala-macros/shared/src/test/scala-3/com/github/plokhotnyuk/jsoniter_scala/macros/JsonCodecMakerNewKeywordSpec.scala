package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException

object Tags:
  opaque type Tagged[+V, +T] = Any

  type @@[+V, +T] = V & Tagged[V, T]

  def tag[T]: [V] => V => V @@ T = [V] => (v: V) => v

  inline given[F[_], V, T](using c: F[V]): F[V @@ T] = c.asInstanceOf[F[V @@ T]]

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
    "serialize and deserialize tagged types using a method to generate custom codecs" in {
      import Tags.{@@, tag}

      implicit val intCodec: JsonValueCodec[Int] = make
      implicit val stringCodec: JsonValueCodec[String] = make

      trait NodeIdTag

      type NodeId = Int @@ NodeIdTag

      trait NodeNameTag

      type NodeName = String @@ NodeNameTag

      case class Node(id: NodeId, name: NodeName)

      case class Edge(n1: NodeId, n2: NodeId)

      verifySerDeser(make[Node], Node(tag[NodeIdTag](1), tag[NodeNameTag]("VVV")), """{"id":1,"name":"VVV"}""")
      verifySerDeser(make[Edge], Edge(tag[NodeIdTag](1), tag[NodeIdTag](2)), """{"n1":1,"n2":2}""")
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
      {
        enum TestEnum derives ConfiguredJsonValueCodec:
          case Value1
          case Value2(string: String)

        verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"type":"Value2","string":"VVV"}""")
      }
      {
        inline given CodecMakerConfig = CodecMakerConfig.withDiscriminatorFieldName(Some("name"))

        enum TestEnum derives ConfiguredJsonValueCodec:
          case Value1
          case Value2(string: String)

        verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"name":"Value2","string":"VVV"}""")
      }
      {
        inline given CodecMakerConfig = CodecMakerConfig.withDiscriminatorFieldName(Some("hint")).withFieldNameMapper {
          case "string" => "str"
        }

        enum TestEnum derives ConfiguredJsonValueCodec:
          case Value1
          case Value2(string: String)

        verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"hint":"Value2","str":"VVV"}""")
      }
    }
    "serialize and deserialize an generic ADT defined with bounded leaf classes using a custom codec" in {
      sealed trait TypeBase[T]

      object TypeBase:
        given TypeBase[Int] = new TypeBase[Int] {}
        given TypeBase[String] = new TypeBase[String] {}

      sealed trait Base[T: TypeBase]:
        val t: T

      case class A[T: TypeBase](a: T) extends Base[T]:
        override val t: T = a

      case class B[T: TypeBase](b: T) extends Base[T]:
        override val t: T = b

      given JsonValueCodec[Base[?]] = new JsonValueCodec[Base[?]]:
        override val nullValue: Base[?] = null

        override def decodeValue(in: JsonReader, default: Base[?]): Base[?] =
          if (in.isNextToken('{')) {
            var x: Base[?] = null
            var p0 = 0x3
            if (!in.isNextToken('}')) {
              in.rollbackToken()
              var l = -1
              while (l < 0 || in.isNextToken(',')) {
                l = in.readKeyAsCharBuf()
                if (in.isCharBufEqualsTo(l, "a")) {
                  if ((p0 & 0x1) != 0) p0 ^= 0x1
                  else in.duplicatedKeyError(l)
                  if (in.isNextToken('"')) {
                    in.rollbackToken()
                    x = new A(in.readString(null))
                  } else {
                    in.rollbackToken()
                    x = new A(in.readInt())
                  }
                } else if (in.isCharBufEqualsTo(l, "b")) {
                  if ((p0 & 0x2) != 0) p0 ^= 0x2
                  else in.duplicatedKeyError(l)
                  if (in.isNextToken('"')) {
                    in.rollbackToken()
                    x = new B(in.readString(null))
                  } else {
                    in.rollbackToken()
                    x = new B(in.readInt())
                  }
                } else in.skip()
              }
              if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
            }
            if (p0 == 0x3) in.decodeError("""missing required fields: "a" or "b"""")
            else if (p0 == 0) in.decodeError("""illegal object with "a" and "b" fields""")
            x
          } else in.readNullOrTokenError(default, '{')

        override def encodeValue(x: Base[?], out: JsonWriter): Unit =
          out.writeObjectStart()
          val t =
            x match {
              case a: A[?] =>
                out.writeNonEscapedAsciiKey("a")
                a.a
              case b: B[?] =>
                out.writeNonEscapedAsciiKey("b")
                b.b
            }
          t match {
            case s: String => out.writeVal(s)
            case i: Int => out.writeVal(i)
            case _ => out.encodeError("unexpected value type")
          }
          out.writeObjectEnd()

      case class Group(lst: List[Base[?]])

      object Group:
        given JsonValueCodec[Group] = make(CodecMakerConfig.withInlineOneValueClasses(true))

      val group = Group(List(A("Hi"), B("Bye"), A(3), B(4)))
      verifySerDeser(summon[JsonValueCodec[Group]], group, """[{"a":"Hi"},{"b":"Bye"},{"a":3},{"b":4}]""")
    }
  }
}
