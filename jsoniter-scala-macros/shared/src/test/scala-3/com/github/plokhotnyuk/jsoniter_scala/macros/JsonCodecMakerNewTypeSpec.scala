package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{LinkedHashMap, Objects, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException
import scala.annotation.switch
import scala.collection.mutable
import scala.compiletime.{error, requireConst}
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.hashing.MurmurHash3

opaque type Gram = Double

object Gram {
  inline def apply(x: Double): Gram = x

  extension (x: Gram)
    inline def toDouble: Double = x
}

opaque type Meter <: Double = Double

object Meter {
  inline def apply(x: Double): Meter = x
}

opaque type Year = Int

object Year {
  def apply(x: Int): Option[Year] = if (x > 1900) Some(x) else None

  inline def of(inline x: Int): Year =
    requireConst(x)
    inline if x > 1900 then x else error("expected year > 1900")

  given Conversion[Year, Int] with
    inline def apply(year: Year): Int = year
}

case class IArrays(aa: IArray[IArray[Int]], a: IArray[BigInt])

case class IArrayDefaults(aa: IArray[IArray[Int]] = IArray(IArray[Int](1)), a: IArray[BigInt] = IArray[BigInt](2))

case class NullableProperty(a: Int | Null | String)

given nullableValueCodec: JsonValueCodec[Int | Null | String] = new JsonValueCodec[Int | Null | String] {
  def decodeValue(in: JsonReader, default: Int | Null | String): Int | Null | String = {
    val t = in.nextToken()
    if (t != 'n') {
      in.rollbackToken()
      if (t == '"') in.readString(null)
      else in.readInt()
    } else {
      in.readNullOrError("", "expected 'Int | Null | String'")
      null
    }
  }

  def encodeValue(x: Int | Null | String, out: JsonWriter): Unit =
    if (x == null) out.writeNull()
    else if (x.isInstanceOf[String]) out.writeVal(x.asInstanceOf[String])
    else out.writeVal(x.asInstanceOf[Int])

  def nullValue: Int | Null | String = null
}

// Borrowed from an amazing work of Matthias Berndt: https://scastie.scala-lang.org/rCmIrOrnRdydyDvWfJRAzw
object FixTypes:
  opaque type Fix[F[_]] = F[Any]

  object Fix:
    def EqPoly[F[_]]: F[Fix[F]] =:= Fix[F] = <:<.refl.asInstanceOf

    extension[F[_]] (fix: Fix[F]) def unfix: F[Fix[F]] = EqPoly[F].flip(fix)

  trait FixCompanion[F[_]]:
    val Eq: F[Fix[F]] =:= Fix[F] = Fix.EqPoly[F]

    def apply(unfix: F[Fix[F]]): Fix[F] = Eq(unfix)

import FixTypes.*

type JsonF[A] = mutable.Buffer[A] | mutable.Map[String, A] | String | Double | Boolean | None.type

type Json = Fix[JsonF]

object Json extends FixCompanion[JsonF]

def obj(values: (String, JsonF[Json] | Json)*): Json =
  val len = values.length
  val map = new LinkedHashMap[String, JsonF[Json] | Json](len << 1, 0.5f)
  var i = 0
  while (i < len) {
    val kv = values(i)
    map.put(kv._1, kv._2)
    i += 1
  }
  Json(Json.Eq.substituteCo[[A] =>> mutable.Map[String, A | Json]](map.asScala))

def arr(values: JsonF[Json] | Json*): Json =
  Json(Json.Eq.substituteCo[[A] =>> mutable.Buffer[A | Json]](mutable.ArrayBuffer[JsonF[Json] | Json](values*)))

class JsonCodecMakerNewTypeSpec extends VerifyingSpec {
  val codecOfIArrays = make[IArrays]

  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize immutable arrays" in {
      val json = """{"aa":[[1,2],[3,4]],"a":[1,2,3,4]}"""
      val iArrays = IArrays(IArray(IArray[Int](1, 2), IArray[Int](3, 4)), IArray[BigInt](1, 2, 3, 4))
      verifySer(codecOfIArrays, iArrays, json)
      val parsedObj = readFromString(json)(codecOfIArrays)
      parsedObj.aa shouldBe iArrays.aa
      parsedObj.a shouldBe iArrays.a
    }
    "don't serialize fields of case classes with empty immutable arrays" in {
      val json = """{"aa":[[],[]]}"""
      val iArrays = IArrays(IArray(IArray[Int](), IArray[Int]()), IArray[BigInt]())
      verifySer(codecOfIArrays, iArrays, json)
      val parsedObj = readFromString(json)(codecOfIArrays)
      parsedObj.aa shouldBe iArrays.aa
      parsedObj.a shouldBe iArrays.a
    }
    "serialize fields of case classes with empty immutable arrays when transientEmpty is off" in {
      val json = """{"aa":[[],[]],"a":[]}"""
      val iArrays = IArrays(IArray(IArray[Int](), IArray[Int]()), IArray[BigInt]())
      verifySer(make[IArrays](CodecMakerConfig.withTransientEmpty(false)), iArrays, json)
      val parsedObj = readFromString(json)(codecOfIArrays)
      parsedObj.aa shouldBe iArrays.aa
      parsedObj.a shouldBe iArrays.a
    }
    "don't serialize default values of case classes that defined for fields when the transientDefault flag is on (by default)" in {
      val codecOfDefaults: JsonValueCodec[IArrayDefaults] = make
      verifySer(codecOfDefaults, IArrayDefaults(), "{}")
      verifySer(codecOfDefaults, IArrayDefaults(aa = IArray[IArray[Int]](), a = IArray[BigInt]()), """{}""")
    }
    "serialize default values of case classes that defined for fields when the transientDefault flag is off" in {
      verifySer(make[IArrayDefaults](CodecMakerConfig.withTransientDefault(false)),
        IArrayDefaults(), """{"aa":[[1]],"a":[2]}""")
    }
    "serialize empty of case classes that defined for fields when the transientEmpty flag is off" in {
      verifySer(make[IArrayDefaults](CodecMakerConfig.withTransientEmpty(false)),
        IArrayDefaults(aa = IArray[IArray[Int]](), a = IArray[BigInt]()), """{"aa":[],"a":[]}""")
    }
    "serialize and deserialize new collection types" in {
      verifySerDeser(make[collection.mutable.CollisionProofHashMap[String, Int]],
        collection.mutable.CollisionProofHashMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[collection.immutable.TreeSeqMap[String, Int]],
        collection.immutable.TreeSeqMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[collection.immutable.VectorMap[String, Int]],
        collection.immutable.VectorMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[collection.immutable.LazyList[Int]],
        collection.immutable.LazyList[Int](1, 2), """[1,2]""")
      verifySerDeser(make[collection.mutable.Stack[Int]],
        collection.mutable.Stack[Int](1, 2), """[1,2]""")
      verifySerDeser(make[collection.mutable.ArrayDeque[Int]],
        collection.mutable.ArrayDeque[Int](1, 2), """[1,2]""")
      verifySer(make[collection.mutable.PriorityQueue[Int]],
        collection.mutable.PriorityQueue[Int](2,1), """[2,1]""")
      verifyDeserByCheck(make[collection.mutable.PriorityQueue[Int]],
        """[2,1]""", (x: collection.mutable.PriorityQueue[Int]) => x.toList shouldBe List(2, 1))
    }
    "don't generate codecs for union types with proper compilation error" in {
      assert(intercept[TestFailedException](assertCompiles {
        """type ABC = "A" | "B" | "C"
          |JsonCodecMaker.make[ABC]""".stripMargin
      }).getMessage.contains {
        """No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for '"A" | "B" | "C"'."""
      })
    }
    "serialize and deserialize Scala3 opaque types" in {
      case class Planet(@stringified radius: Meter, mass: Gram)

      verifySerDeser(make[Meter], Meter(6.37814e6), "6378140.0")
      verifySerDeser(make[Gram](CodecMakerConfig.withIsStringified(true)), Gram(5.976e+27), """"5.976E27"""")
      verifySerDeser(make[Array[Meter]], Array(Meter(6.37814e6)), "[6378140.0]")
      verifySerDeser(make[Array[Gram]], Array(Gram(5.976e+27)), "[5.976E27]")
      verifySerDeser(make[Map[Meter, Gram]], Map(Meter(6.37814e6) -> Gram(5.976e+27)), """{"6378140.0":5.976E27}""")
      verifySerDeser(make[Planet], Planet(Meter(6.37814e6), Gram(5.976e+27)), """{"radius":"6378140.0","mass":5.976E27}""")
    }
    "serialize and deserialize Scala3 opaque types using custom value codecs" in {
      case class Period(start: Year, end: Year)

      given yearCodec: JsonValueCodec[Year] = new JsonValueCodec[Year] {
        def decodeValue(in: JsonReader, default: Year): Year = Year(in.readInt()) match {
          case x: Some[Year] => x.value
          case _ => in.decodeError("expected year > 1900")
        }

        def encodeValue(x: Year, out: JsonWriter): Unit = out.writeVal(x)

        val nullValue: Year = null.asInstanceOf[Year]
      }
      verifySerDeser(make[Period], Period(Year.of(1976), Year.of(2022)), """{"start":1976,"end":2022}""")
      verifySerDeser(make[Array[Year]], Array(Year.of(1976)), "[1976]")
    }
    "serialize and deserialize a Scala3 union type using a custom codec" in {
      type Value = String | Boolean | Double

      given JsonValueCodec[Value] = new JsonValueCodec[Value] {
        override val nullValue: Value = null.asInstanceOf[Value]

        override def decodeValue(in: JsonReader, default: Value): Value =
          val x = in.nextToken()
          in.rollbackToken()
          if (x == '"') in.readString(null)
          else if (x == 't' || x == 'f') in.readBoolean()
          else in.readDouble()

        override def encodeValue(x: Value, out: JsonWriter): Unit = x match
          case s: String => out.writeVal(s)
          case b: Boolean => out.writeVal(b)
          case d: Double => out.writeVal(d)
      }

      sealed trait Base[T]:
        val t: T

      case class A[T](a: T) extends Base[T]:
        override val t: T = a

      case class B[T](b: T) extends Base[T]:
        override val t: T = b

      case class Group(lst: List[Base[Value]])

      object Group:
        given JsonValueCodec[Base[Value]] = make
        given JsonValueCodec[Group] = make(CodecMakerConfig.withInlineOneValueClasses(true))

      val group = Group(List(A("Hi"), B("Bye"), A(3.4), B(4.5), A(true), B(false)))
      verifySerDeser(summon[JsonValueCodec[Group]], group,
        """[{"type":"A","a":"Hi"},{"type":"B","b":"Bye"},{"type":"A","a":3.4},{"type":"B","b":4.5},{"type":"A","a":true},{"type":"B","b":false}]""")
    }
    "serialize and deserialize a Scala3 union type using a custom codec with setMark, resetMark, and rollbackToMark calls" in {
      implicit val intOrBigDecimalCodec: JsonValueCodec[Int | BigDecimal] =
        new JsonValueCodec[Int | BigDecimal]:
          def decodeValue(in: JsonReader, default: Int | BigDecimal): Int | BigDecimal =
            in.setMark()
            try {
              val a = in.readInt()
              in.resetMark()
              a
            } catch { // use this approach wisely taking in account cost of failed parsing with exception throwing and catching
              case _: JsonReaderException =>
                in.rollbackToMark()
                in.readBigDecimal(null)
            }

          def encodeValue(x: Int | BigDecimal, out: JsonWriter): Unit =
            if (x.isInstanceOf[BigDecimal]) out.writeVal(x.asInstanceOf[BigDecimal])
            else out.writeVal(x.asInstanceOf[Int])

          def nullValue: Int | BigDecimal = null.asInstanceOf[Int | BigDecimal]

      verifySerDeser(summon[JsonValueCodec[Int | BigDecimal]], 1, "1")
      verifySerDeser(summon[JsonValueCodec[Int | BigDecimal]], BigDecimal("1" * 33), "1" * 33)
    }
    "serialize and deserialize recursive Scala3 union types using a custom value codec" in {
      val jsonCodec: JsonValueCodec[Json] = new JsonValueCodec[Json] {
        def decodeValue(in: JsonReader, default: Json): Json = decode(in, 128)

        def encodeValue(x: Json, out: JsonWriter): Unit = encode(x, out, 128)

        val nullValue: Json = Json(None)

        private def decode(in: JsonReader, depth: Int): Json = Json {
          val b = in.nextToken()
          if (b == '"') {
            in.rollbackToken()
            in.readString(null)
          } else if (b == 't' || b == 'f') {
            in.rollbackToken()
            in.readBoolean()
          } else if ((b >= '0' && b <= '9') || b == '-') {
            in.rollbackToken()
            in.readDouble()
          } else if (b == '[') {
            if (depth <= 0) in.decodeError("depth limit exceeded")
            val arr = new mutable.ArrayBuffer[Json](4)
            if (!in.isNextToken(']')) {
              in.rollbackToken()
              val dp = depth - 1
              while ({
                arr += decode(in, dp)
                in.isNextToken(',')
              }) ()
              if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
            }
            arr
          } else if (b == '{') {
            if (depth <= 0) in.decodeError("depth limit exceeded")
            val obj = new LinkedHashMap[String, Json](8, 0.5f)
            if (!in.isNextToken('}')) {
              in.rollbackToken()
              val dp = depth - 1
              while ({
                obj.put(in.readKeyAsString(), decode(in, dp))
                in.isNextToken(',')
              }) ()
              if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
            }
            obj.asScala
          } else in.readNullOrError(None, "expected JSON value")
        }

        private def encode(x: Json, out: JsonWriter, depth: Int): Unit = x match
          case str: String => out.writeVal(str)
          case b: Boolean => out.writeVal(b)
          case d: Double => out.writeVal(d)
          case arr: mutable.Buffer[Json] =>
            if (depth <= 0) out.encodeError("depth limit exceeded")
            out.writeArrayStart()
            val dp = depth - 1
            val l = arr.size
            var i = 0
            while (i < l) {
              encode(arr(i), out, dp)
              i += 1
            }
            out.writeArrayEnd()
          case obj: mutable.Map[String, Json] =>
            if (depth <= 0) out.encodeError("depth limit exceeded")
            out.writeObjectStart()
            val dp = depth - 1
            val it = obj.iterator
            while (it.hasNext) {
              val kv = it.next()
              out.writeKey(kv._1)
              encode(kv._2, out, dp)
            }
            out.writeObjectEnd()
          case _ => out.writeNull()
      }

      verifySerDeser(jsonCodec, arr("VVV", 1.2, true, obj("WWW" -> None, "XXX" -> 777)),
        """["VVV",1.2,true,{"WWW":null,"XXX":777.0}]""")
    }
    "serialize and deserialize case class with union types having null value (default behavior)" in {
      verifySerDeser(make[List[NullableProperty]],
        List(NullableProperty(null), NullableProperty(1), NullableProperty("VVV")),
        """[{"a":null},{"a":1},{"a":"VVV"}]""")
    }
    "serialize and deserialize case class with union types having null value (transient null behavior)" in {
      verifySerDeser(make[List[NullableProperty]](CodecMakerConfig.withTransientNull(true)),
        List(NullableProperty(null), NullableProperty(1), NullableProperty("VVV")),
        """[{},{"a":1},{"a":"VVV"}]""")
    }
    "don't generate codecs for non-concrete ADTs with at least one free type parameter" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait TypeBase[T]
          |
          |object TypeBase:
          |  given TypeBase[Int] = new TypeBase[Int] {}
          |  given TypeBase[String] = new TypeBase[String] {}
          |
          |sealed trait Base[T: TypeBase]:
          |  val t: T
          |
          |case class A[T: TypeBase](a: T) extends Base[T]:
          |  override val t: T = a
          |
          |case class B[T: TypeBase](b: T) extends Base[T]:
          |  override val t: T = b
          |
          |JsonCodecMaker.make[Base[_]]
          |""".stripMargin
      }).getMessage.contains {
        "Only concrete (no free type parameters) Scala classes & objects are supported for ADT leaf classes."
      })
    }
    "don't generate codecs for opaque types that hides type arguments" in {
      assert(intercept[TestFailedException](assertCompiles {
        """object Wrappers {
          |  opaque type ReqId <: Option[String] = Option[String]
          |
          |  object ReqId {
          |    inline def apply(x: Option[String]): ReqId = x
          |
          |    given JsonValueCodec[ReqId] = JsonCodecMaker.make[Option[String]]
          |  }
          |}
          |
          |import Wrappers.*
          |import Wrappers.ReqId.*
          |
          |case class A(r: ReqId)
          |
          |given JsonValueCodec[A] = JsonCodecMaker.make
          |""".stripMargin
      }).getMessage.contains {
        "Cannot get 1st type argument in 'Wrappers.ReqId'"
      })
    }
  }
}
