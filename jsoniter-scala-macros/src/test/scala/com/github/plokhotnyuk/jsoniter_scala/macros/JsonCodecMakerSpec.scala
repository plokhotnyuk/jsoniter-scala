package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Objects, UUID}

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Matchers, WordSpec}

import scala.annotation.switch

case class UserId(id: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

case class Id[A](id: A) extends AnyVal

class JsonCodecMakerSpec extends WordSpec with Matchers {
  case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

  val primitives = Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f)
  val codecOfPrimitives: JsonValueCodec[Primitives] = make(CodecMakerConfig())

  case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

  val standardTypes = StandardTypes("VVV", 1, 1.1)
  val codecOfStandardTypes: JsonValueCodec[StandardTypes] = make(CodecMakerConfig())

  class NonCaseClass(val id: Int, var name: String) {
    override def hashCode(): Int = id * 31 + Objects.hashCode(name)

    override def equals(obj: scala.Any): Boolean = obj match {
      case c: NonCaseClass => id == c.id && Objects.equals(name, c.name)
      case _ => false
    }

    override def toString: String = s"NonCaseClass(id=$id,name=$name)"
  }

  val codecOfNonCaseClass: JsonValueCodec[NonCaseClass] = make(CodecMakerConfig())

  case class JavaTypes(uuid: UUID)

  val codecOfJavaTypes: JsonValueCodec[JavaTypes] = make(CodecMakerConfig())

  object LocationType extends Enumeration {
    type LocationType = Value
    val GPS: LocationType = Value(1, "GPS") // always set name explicitly in your Enumeration definition, if you still not sure,
    val IP: LocationType = Value(2, "IP") // then please look and check that following synchronized block will not affect your code in runtime:
    val UserProvided: LocationType = Value(3, "UserProvided") // https://github.com/scala/scala/blob/1692ae306dc9a5ff3feebba6041348dfdee7cfb5/src/library/scala/Enumeration.scala#L203
  }

  case class Enums(lt: LocationType.LocationType)

  val codecOfEnums: JsonValueCodec[Enums] = make(CodecMakerConfig())

  case class JavaEnums(l: Level, il: Levels.InnerLevel)

  val codecOfJavaEnums: JsonValueCodec[JavaEnums] = make(CodecMakerConfig())

  case class OuterTypes(s: String, st: Either[String, StandardTypes] = Left("error"))

  case class Options(os: Option[String], obi: Option[BigInt], osi: Option[Set[Int]], ol: Option[Long],
                     ojl: Option[java.lang.Long])

  val codecOfOptions: JsonValueCodec[Options] = make(CodecMakerConfig())

  case class Tuples(t1: (Int, Double, List[Char]), t2: (String, BigInt, Option[LocationType.LocationType]))

  val codecOfTuples: JsonValueCodec[Tuples] = make(CodecMakerConfig())

  case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

  val arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array[BigInt](7))
  val codecOfArrays: JsonValueCodec[Arrays] = make(CodecMakerConfig())

  case class ImmutableIterables(l: List[collection.immutable.ListSet[String]],
                                q: collection.immutable.Queue[Set[BigInt]],
                                is: IndexedSeq[collection.immutable.SortedSet[Int]],
                                s: Stream[collection.immutable.TreeSet[Double]],
                                v: Vector[Iterable[Long]])

  val codecOfImmutableIterables: JsonValueCodec[ImmutableIterables] = make(CodecMakerConfig())

  case class MutableMaps(hm: collection.mutable.HashMap[Boolean, collection.mutable.AnyRefMap[BigDecimal, Int]],
                         m: collection.mutable.Map[Float, collection.mutable.ListMap[BigInt, String]],
                         ohm: collection.mutable.OpenHashMap[Double, collection.mutable.LinkedHashMap[Short, Double]])

  val codecOfMutableMaps: JsonValueCodec[MutableMaps] = make(CodecMakerConfig())

  case class ImmutableMaps(m: Map[Int, Double],
                           hm: collection.immutable.HashMap[String, collection.immutable.ListMap[Char, BigInt]],
                           sm: collection.immutable.SortedMap[Long, collection.immutable.TreeMap[Byte, Float]])

  val codecOfImmutableMaps: JsonValueCodec[ImmutableMaps] = make(CodecMakerConfig())

  case class CamelSnakeKebabCases(camelCase: Int, snake_case: Int, `kebab-case`: Int,
                                  `camel1`: Int, `snake_1`: Int, `kebab-1`: Int)

  val codecOfNameOverridden: JsonValueCodec[NameOverridden] = make(CodecMakerConfig())

  case class Indented(s: String, bd: BigDecimal, l: List[Int], m: Map[Char, Double])

  val indented = Indented("VVV", 1.1, List(1, 2, 3), Map('S' -> -90.0))
  val codecOfIndented: JsonValueCodec[Indented] = make(CodecMakerConfig())

  case class UTF8KeysAndValues(გასაღები: String)

  val codecOfUTF8KeysAndValues: JsonValueCodec[UTF8KeysAndValues] = make(CodecMakerConfig())

  case class Stringified(@stringified i: Int, @stringified bi: BigInt, @stringified l1: List[Int], l2: List[Int])

  val stringified = Stringified(1, 2, List(1), List(2))
  val codecOfStringified: JsonValueCodec[Stringified] = make(CodecMakerConfig())

  case class Required(r00: Int = 0, r01: Int, r02: Int, r03: Int, r04: Int, r05: Int, r06: Int, r07: Int, r08: Int, r09: Int,
                      r10: Int = 10, r11: Int, r12: Int, r13: Int, r14: Int, r15: Int, r16: Int, r17: Int, r18: Int, r19: Int,
                      r20: Int = 20, r21: Int, r22: Int, r23: Int, r24: Int, r25: Int, r26: Int, r27: Int, r28: Int, r29: Int,
                      r30: Int = 30, r31: Int, r32: Int, r33: Int, r34: Int, r35: Int, r36: Int, r37: Int, r38: Int, r39: Int,
                      r40: Int = 40, r41: Int, r42: Int, r43: Int, r44: Int, r45: Int, r46: Int, r47: Int, r48: Int, r49: Int,
                      r50: Int = 50, r51: Int, r52: Int, r53: Int, r54: Int, r55: Int, r56: Int, r57: Int, r58: Int, r59: Int,
                      r60: Int = 60, r61: Int, r62: Int, r63: Int, r64: Int, r65: Int, r66: Int, r67: Int, r68: Int, r69: Int,
                      r70: Int = 70, r71: Int, r72: Int, r73: Int, r74: Int, r75: Int, r76: Int, r77: Int, r78: Int, r79: Int,
                      r80: Int = 80, r81: Int, r82: Int, r83: Int, r84: Int, r85: Int, r86: Int, r87: Int, r88: Int, r89: Int,
                      r90: Int = 90, r91: Int, r92: Int, r93: Int, r94: Int, r95: Int, r96: Int, r97: Int, r98: Int, r99: Int)

  sealed trait AdtBase extends Product with Serializable

  sealed abstract class Inner extends AdtBase {
    def a: Int
  }

  case class AAA(a: Int) extends Inner

  case class BBB(a: String) extends AdtBase

  case class CCC(a: Int, b: String) extends Inner

  case object DDD extends AdtBase

  val codecOfADTList: JsonValueCodec[List[AdtBase]] = make(CodecMakerConfig())

  "JsonCodecMaker.make generate codes which" should {
    "serialize and deserialize case classes with primitives" in {
      verifySerDeser(codecOfPrimitives, primitives,
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level primitives" in {
      verifySerDeser(make[Byte](CodecMakerConfig()), 1.toByte, "1".getBytes("UTF-8"))
      verifySerDeser(make[Short](CodecMakerConfig()), 2.toShort, "2".getBytes("UTF-8"))
      verifySerDeser(make[Int](CodecMakerConfig()), 3, "3".getBytes("UTF-8"))
      verifySerDeser(make[Long](CodecMakerConfig()), 4L, "4".getBytes("UTF-8"))
      verifySerDeser(make[Boolean](CodecMakerConfig()), true, "true".getBytes("UTF-8"))
      verifySerDeser(make[Char](CodecMakerConfig()), 'V', "\"V\"".getBytes("UTF-8"))
      verifySerDeser(make[Double](CodecMakerConfig()), 1.1, "1.1".getBytes("UTF-8"))
      verifySerDeser(make[Float](CodecMakerConfig()), 2.2f, "2.2".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level primitives" in {
      verifySerDeser(make[Byte](CodecMakerConfig(isStringified = true)), 1.toByte, "\"1\"".getBytes("UTF-8"))
      verifySerDeser(make[Short](CodecMakerConfig(isStringified = true)), 2.toShort, "\"2\"".getBytes("UTF-8"))
      verifySerDeser(make[Int](CodecMakerConfig(isStringified = true)), 3, "\"3\"".getBytes("UTF-8"))
      verifySerDeser(make[Long](CodecMakerConfig(isStringified = true)), 4L, "\"4\"".getBytes("UTF-8"))
      verifySerDeser(make[Boolean](CodecMakerConfig(isStringified = true)), true, "\"true\"".getBytes("UTF-8"))
      verifySerDeser(make[Double](CodecMakerConfig(isStringified = true)), 1.1, "\"1.1\"".getBytes("UTF-8"))
      verifySerDeser(make[Float](CodecMakerConfig(isStringified = true)), 2.2f, "\"2.2\"".getBytes("UTF-8"))
    }
    "throw parse exception with hex dump in case of illegal input" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfPrimitives, primitives,
          """{"b":-128,"s":-32768,"i":-2147483648,"l":-9223372036854775808,'bl':true,"ch":"V","dbl":-123456789.0,"f":-12345.0}""".getBytes("UTF-8"))
      }.getMessage.contains(
        """expected '"', offset: 0x0000003e, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000010 | 32 37 36 38 2c 22 69 22 3a 2d 32 31 34 37 34 38 | 2768,"i":-214748 |
          || 00000020 | 33 36 34 38 2c 22 6c 22 3a 2d 39 32 32 33 33 37 | 3648,"l":-922337 |
          || 00000030 | 32 30 33 36 38 35 34 37 37 35 38 30 38 2c 27 62 | 2036854775808,'b |
          || 00000040 | 6c 27 3a 74 72 75 65 2c 22 63 68 22 3a 22 56 22 | l':true,"ch":"V" |
          || 00000050 | 2c 22 64 62 6c 22 3a 2d 31 32 33 34 35 36 37 38 | ,"dbl":-12345678 |
          |+----------+-------------------------------------------------+------------------+""".stripMargin))
    }
    "serialize and deserialize case classes with boxed primitives" in {
      case class BoxedPrimitives(b: java.lang.Byte, s: java.lang.Short, i: java.lang.Integer, l: java.lang.Long,
                                 bl: java.lang.Boolean, ch: java.lang.Character, dbl: java.lang.Double, f: java.lang.Float)

      verifySerDeser(make[BoxedPrimitives](CodecMakerConfig()),
        BoxedPrimitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level boxed primitives" in {
      verifySerDeser(make[java.lang.Byte](CodecMakerConfig()), java.lang.Byte.valueOf(1.toByte), "1".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Short](CodecMakerConfig()), java.lang.Short.valueOf(2.toShort), "2".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Integer](CodecMakerConfig()), java.lang.Integer.valueOf(3), "3".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Long](CodecMakerConfig()), java.lang.Long.valueOf(4L), "4".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Boolean](CodecMakerConfig()), java.lang.Boolean.valueOf(true), "true".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Character](CodecMakerConfig()), java.lang.Character.valueOf('V'), "\"V\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Double](CodecMakerConfig()), java.lang.Double.valueOf(1.1), "1.1".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Float](CodecMakerConfig()), java.lang.Float.valueOf(2.2f), "2.2".getBytes("UTF-8"))
    }
    "serialize and deserialize stringifeid top-level boxed primitives" in {
      verifySerDeser(make[java.lang.Byte](CodecMakerConfig(isStringified = true)),
        java.lang.Byte.valueOf(1.toByte), "\"1\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Short](CodecMakerConfig(isStringified = true)),
        java.lang.Short.valueOf(2.toShort), "\"2\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Integer](CodecMakerConfig(isStringified = true)),
        java.lang.Integer.valueOf(3), "\"3\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Long](CodecMakerConfig(isStringified = true)),
        java.lang.Long.valueOf(4L), "\"4\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Boolean](CodecMakerConfig(isStringified = true)),
        java.lang.Boolean.valueOf(true), "\"true\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Double](CodecMakerConfig(isStringified = true)),
        java.lang.Double.valueOf(1.1), "\"1.1\"".getBytes("UTF-8"))
      verifySerDeser(make[java.lang.Float](CodecMakerConfig(isStringified = true)),
        java.lang.Float.valueOf(2.2f), "\"2.2\"".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with standard types" in {
      val text =
        "JavaScript Object Notation (JSON) is a lightweight, text-based, language-independent data interchange format."
      verifySerDeser(codecOfStandardTypes,
        StandardTypes(text, BigInt("123456789012345678901234567890"), BigDecimal("1234567890.12345678901234567890")),
        s"""{"s":"$text","bi":123456789012345678901234567890,"bd":1234567890.12345678901234567890}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of illegal value for case classes" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, null, "null".getBytes("UTF-8"))
      }.getMessage.contains("expected '{', offset: 0x00000000"))
    }
    "serialize and deserialize top-level standard types" in {
      val text =
        "JavaScript Object Notation (JSON) is a lightweight, text-based, language-independent data interchange format."
      verifySerDeser(make[String](CodecMakerConfig()), text, s""""$text"""".getBytes("UTF-8"))
      verifySerDeser(make[BigInt](CodecMakerConfig()), BigInt("123456789012345678901234567890"),
        "123456789012345678901234567890".getBytes("UTF-8"))
      verifySerDeser(make[BigDecimal](CodecMakerConfig()), BigDecimal("1234567890.12345678901234567890"),
        "1234567890.12345678901234567890".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level standard types" in {
      verifySerDeser(make[BigInt](CodecMakerConfig(isStringified = true)),
        BigInt("123456789012345678901234567890"), "\"123456789012345678901234567890\"".getBytes("UTF-8"))
      verifySerDeser(make[BigDecimal](CodecMakerConfig(isStringified = true)),
        BigDecimal("1234567890.12345678901234567890"), "\"1234567890.12345678901234567890\"".getBytes("UTF-8"))
    }
    "throw parse exception in case of duplicated key for case classe was detected" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, StandardTypes("VVV", BigInt("1"), BigDecimal("2")),
          s"""{"s":"XXX","s":"VVV","bi":10,"bi":1,"bd":20.0,"bd":2.0}""".getBytes("UTF-8"))
      }.getMessage.contains("duplicated field \"s\", offset: 0x0000000e"))
    }
    "throw parse exception in case of illegal UTF-8 encoded field names" in {
      assert(intercept[JsonParseException] {
        val buf = """{"s":"VVV","bi":1,"bd":1.1}""".getBytes("UTF-8")
        buf(2) = 0xF0.toByte
        verifyDeser(codecOfStandardTypes, standardTypes, buf)
      }.getMessage.contains("malformed byte(s): 0xf0, 0x22, 0x3a, 0x22, offset: 0x00000005"))
    }
    "throw parse exception in case of illegal JSON escaped field names" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, "{\"\\udd1e\":\"VVV\",\"bi\":1,\"bd\":1.1}".getBytes("UTF-8"))
      }.getMessage.contains("illegal escape sequence, offset: 0x00000008"))
    }
    "throw parse exception in case of missing or illegal tokens" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """"s":"VVV","bi":1,"bd":1.1}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '{', offset: 0x00000000"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s""VVV","bi":1,"bd":1.1}""".getBytes("UTF-8"))
      }.getMessage.contains("expected ':', offset: 0x00000004"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s":"VVV""bi":1"bd":1.1}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '}' or ',', offset: 0x0000000a"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """["s":"VVV","bi":1,"bd":2}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '{', offset: 0x00000000"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{,"s":"VVV","bi":1,"bd":2}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000001"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s":"VVV","bi":1,"bd":2]""".getBytes("UTF-8"))
      }.getMessage.contains("expected '}' or ',', offset: 0x00000018"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s":"VVV","bi":1,"bd":2,}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000019"))
    }
    "serialize and deserialize Scala classes which has a primary constructor with 'var' or 'var' parameters only" in {
      verifySerDeser(codecOfNonCaseClass, new NonCaseClass(1, "VVV"),
        """{"id":1,"name":"VVV"}""".getBytes("UTF-8"))
    }
    "serialize and deserialize Java types" in {
      verifySerDeser(codecOfJavaTypes, JavaTypes(new UUID(0, 0)),
        """{"uuid":"00000000-0000-0000-0000-000000000000"}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of illegal value of java types" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfJavaTypes, JavaTypes(new UUID(0, 0)),
          """{"uuid":"00000000-XXXX-0000-0000-000000000000"}""".getBytes("UTF-8"))
      }.getMessage.contains("expected hex digit, offset: 0x00000012"))
    }
    "serialize and deserialize top-level java types" in {
      verifySerDeser(make[UUID](CodecMakerConfig()), new UUID(0, 0),
        "\"00000000-0000-0000-0000-000000000000\"".getBytes("UTF-8"))
    }
    "serialize and deserialize Java types as key in maps" in {
      verifySerDeser(make[Map[UUID, Int]](CodecMakerConfig()), Map(new UUID(0, 0) -> 0),
        """{"00000000-0000-0000-0000-000000000000":0}""".getBytes("UTF-8"))
    }
    "serialize and deserialize enumerations" in {
      verifySerDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":"GPS"}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of illegal value of enumeration" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":null}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000006"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":"Galileo"}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal enum value \"Galileo\", offset: 0x0000000e"))
    }
    "serialize and deserialize top-level enumerations" in {
      verifySerDeser(make[LocationType.LocationType](CodecMakerConfig()), LocationType.GPS,
        "\"GPS\"".getBytes("UTF-8"))
    }
    "serialize and deserialize enumerations as key in maps" in {
      verifySerDeser(make[Map[LocationType.LocationType, Int]](CodecMakerConfig()), Map(LocationType.GPS -> 0),
        """{"GPS":0}""".getBytes("UTF-8"))
    }
    "serialize and deserialize Java enumerations" in {
      verifySerDeser(codecOfJavaEnums, JavaEnums(Level.LOW, Levels.InnerLevel.HIGH),
        """{"l":"LOW","il":"HIGH"}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of illegal value of Java enumeration" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfJavaEnums, JavaEnums(Level.HIGH, Levels.InnerLevel.LOW),
          """{"l":null,"il":"HIGH"}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000005"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfJavaEnums, JavaEnums(Level.HIGH, Levels.InnerLevel.LOW),
          """{"l":"LO","il":"HIGH"}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal enum value \"LO\", offset: 0x00000008"))
    }
    "serialize and deserialize top-level Java enumerations" in {
      verifySerDeser(make[Level](CodecMakerConfig()), Level.HIGH, "\"HIGH\"".getBytes("UTF-8"))
    }
    "serialize and deserialize Java enumerations as key in maps" in {
      verifySerDeser(make[Map[Level, Int]](CodecMakerConfig()), Map(Level.HIGH -> 0), """{"HIGH":0}""".getBytes("UTF-8"))
    }
    "serialize and deserialize outer types using custom codecs for inner types" in {
      implicit val customCodecForEither1: JsonValueCodec[Either[String, Int]] =
        new JsonValueCodec[Either[String, Int]] {
          val nullValue: Either[String, Int] = null

          def decodeValue(in: JsonReader, default: Either[String, Int]): Either[String, Int] = {
            val t = in.nextToken()
            in.rollbackToken()
            if (t == '"') Left(in.readString(null))
            else Right(in.readInt())
          }

          def encodeValue(x: Either[String, Int], out: JsonWriter): Unit = x match {
            case Right(i) => out.writeVal(i)
            case Left(s) => out.writeVal(s)
          }
        }
      val codecOfEitherList = make[List[Either[String, Int]]](CodecMakerConfig())
      verifySerDeser(codecOfEitherList, List(Right(1), Left("VVV")), "[1,\"VVV\"]".getBytes("UTF-8"))
      implicit val customCodecForEither2: JsonValueCodec[Either[String, StandardTypes]] =
        new JsonValueCodec[Either[String, StandardTypes]] {
          val nullValue: Either[String, StandardTypes] = null

          def decodeValue(in: JsonReader, default: Either[String, StandardTypes]): Either[String, StandardTypes] =
            (in.nextToken(): @switch) match {
              case '{' =>
                in.rollbackToken()
                Right(codecOfStandardTypes.decodeValue(in, codecOfStandardTypes.nullValue))
              case '"' =>
                in.rollbackToken()
                Left(in.readString(null))
              case _ =>
                in.decodeError("expected '{' or '\"'")
            }

          def encodeValue(x: Either[String, StandardTypes], out: JsonWriter): Unit =
            x match {
              case Left(s) => out.writeVal(s)
              case Right(st) => codecOfStandardTypes.encodeValue(st, out)
            }
        }
      val codecOfOuterTypes = make[OuterTypes](CodecMakerConfig())
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Right(standardTypes)),
        """{"s":"X","st":{"s":"VVV","bi":1,"bd":1.1}}""".getBytes("UTF-8"))
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("fatal error")),
        """{"s":"X","st":"fatal error"}""".getBytes("UTF-8"))
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("error")), // st matches with default value
        """{"s":"X"}""".getBytes("UTF-8"))
      verifySerDeser(codecOfOuterTypes, OuterTypes("X"),
        """{"s":"X"}""".getBytes("UTF-8"))
      implicit object codecOfLocationType extends JsonValueCodec[LocationType.LocationType] {
        val nullValue: LocationType.LocationType = null

        def decodeValue(in: JsonReader, default: LocationType.LocationType): LocationType.LocationType = {
          val v = in.readInt()
          try LocationType.apply(v) catch {
            case _: NoSuchElementException => in.decodeError("invalid enum value: " + v)
          }
        }

        def encodeValue(x: LocationType.LocationType, out: JsonWriter): Unit = out.writeVal(x.id)
      }
      val codecOfEnums = make[Enums](CodecMakerConfig())
      verifySerDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":1}""".getBytes("UTF-8"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":"GPS"}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x00000006"))
    }
    "serialize and deserialize outer types using custom key codecs for map keys" in {
      implicit val codecForLevel: JsonKeyCodec[Level] = new JsonKeyCodec[Level] {
        override def decodeKey(in: JsonReader): Level = in.readKeyAsInt() match {
          case 0 => Level.LOW
          case 1 => Level.HIGH
          case x => in.enumValueError(x.toString)
        }

        override def encodeKey(x: Level, out: JsonWriter): Unit = x match {
          case Level.LOW => out.writeKey(0)
          case Level.HIGH => out.writeKey(1)
          case _ => out.encodeError("illegal enum value")
        }
      }
      verifySerDeser(make[Map[Level, Int]](CodecMakerConfig()), Map(Level.HIGH -> 0), """{"1":0}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with value classes" in {
      case class ValueClassTypes(uid: UserId, oid: OrderId)

      verifySerDeser(make[ValueClassTypes](CodecMakerConfig()),
        ValueClassTypes(UserId("123abc"), OrderId(123123)),
        """{"uid":"123abc","oid":123123}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level value classes" in {
      val codecOfUserId = make[UserId](CodecMakerConfig())
      verifySerDeser(codecOfUserId, UserId("123abc"), "\"123abc\"".getBytes("UTF-8"))
      verifySerDeser(make[OrderId](CodecMakerConfig()), OrderId(123123), "123123".getBytes("UTF-8"))
    }
    "serialize and deserialize strinfigied top-level value classes" in {
      verifySerDeser(make[OrderId](CodecMakerConfig(isStringified = true)), OrderId(123123), "\"123123\"".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with options" in {
      verifySerDeser(codecOfOptions,
        Options(Option("VVV"), Option(BigInt(4)), Option(Set()), Option(1L), Option(2L)),
        """{"os":"VVV","obi":4,"osi":[],"ol":1,"ojl":2}""".getBytes("UTF-8"))
      verifySerDeser(codecOfOptions, Options(None, None, None, None, None), """{}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level options" in {
      val codecOfStringOption = make[Option[String]](CodecMakerConfig())
      verifySerDeser(codecOfStringOption, Some("VVV"), "\"VVV\"".getBytes("UTF-8"))
      verifySerDeser(codecOfStringOption, None, "null".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level numeric options" in {
      val codecOfStringifiedOption = make[Option[BigInt]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfStringifiedOption, Some(BigInt(123)), "\"123\"".getBytes("UTF-8"))
      verifySerDeser(codecOfStringifiedOption, None, "null".getBytes("UTF-8"))
    }
    "throw parse exception in case of unexpected value for option" in {
      val codecOfStringOption = make[Option[String]](CodecMakerConfig())
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStringOption, Option("VVV"), """no!!!""".getBytes("UTF-8"))
      }.getMessage.contains("expected value or null, offset: 0x00000001"))
    }
    "serialize and deserialize case classes with tuples" in {
      verifySerDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
        """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS"]}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of unexpected number of JSON array values" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
          """{"t1":[1,2.2],"t2":["VVV",3,"GPS"]}""".getBytes("UTF-8"))
      }.getMessage.contains("expected ',', offset: 0x0000000c"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
          """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS","XXX"]}""".getBytes("UTF-8"))
      }.getMessage.contains("expected ']', offset: 0x00000027"))
    }
    "serialize and deserialize top-level tuples" in {
      verifySerDeser(make[(String, Int)](CodecMakerConfig()), ("VVV", 1), "[\"VVV\",1]".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level numeric tuples" in {
      verifySerDeser(make[(Long, Float, BigDecimal)](CodecMakerConfig(isStringified = true)),
        (1L, 2.2f, BigDecimal(3.3)), "[\"1\",\"2.2\",\"3.3\"]".getBytes("UTF-8"))
    }
    "serialize and deserialize tuples with type aliases" in {
      type I = Int
      type S = String

      case class Tuples(t1: (S, I), t2: (I, S))

      verifySerDeser(make[Tuples](CodecMakerConfig()), Tuples(("VVV", 1), (2, "WWW")),
        "{\"t1\":[\"VVV\",1],\"t2\":[2,\"WWW\"]}".getBytes("UTF-8"))
      verifySerDeser(make[(S, I)](CodecMakerConfig()), ("VVV", 1), "[\"VVV\",1]".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with arrays" in {
      val json = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}""".getBytes("UTF-8")
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = readFromArray(json)(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "serialize and deserialize top-level arrays" in {
      val json = """[[1,2,3],[4,5,6]]""".getBytes("UTF-8")
      val arrayOfArray = Array(Array(1, 2, 3), Array(4, 5, 6))
      val codecOfArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig())
      verifySer(codecOfArrayOfArray, arrayOfArray, json)
      val parsedObj = readFromArray(json)(codecOfArrayOfArray)
      parsedObj shouldBe arrayOfArray
    }
    "serialize and deserialize stringified top-level arrays" in {
      val json = """[["1","2","3"],["4","5","6"]]""".getBytes("UTF-8")
      val arrayOfArray = Array(Array(1, 2, 3), Array(4, 5, 6))
      val codecOfStringifiedArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig(isStringified = true))
      verifySer(codecOfStringifiedArrayOfArray, arrayOfArray, json)
      val parsedObj = readFromArray(json)(codecOfStringifiedArrayOfArray)
      parsedObj shouldBe arrayOfArray
    }
    "do not serialize fields of case classes with empty arrays" in {
      val json = """{"aa":[[],[]]}""".getBytes("UTF-8")
      val arrays = Arrays(Array(Array(), Array()), Array())
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = readFromArray(json)(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "throw parse exception in case of JSON array is not properly started/closed or with leading/trailing comma" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[{1,2,3]],"a":[]}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '[' or null, offset: 0x00000007"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[[,1,2,3]],"a":[]}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x00000008"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[[1,2,3}],"a":[]}""".getBytes("UTF-8"))
      }.getMessage.contains("expected ']' or ',', offset: 0x0000000d"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[[1,2,3,]],"a":[]}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x0000000e"))
    }
    "serialize and deserialize case classes with mutable Iterables" in {
      case class MutableIterables(ml: collection.mutable.Seq[collection.mutable.SortedSet[String]],
                                     ab: collection.mutable.ArrayBuffer[collection.mutable.Set[BigInt]],
                                     as: collection.mutable.ArraySeq[collection.mutable.LinkedHashSet[Int]],
                                     b: collection.mutable.Buffer[collection.mutable.HashSet[Double]],
                                     lb: collection.mutable.ListBuffer[collection.mutable.TreeSet[Long]],
                                     is: collection.mutable.IndexedSeq[collection.mutable.ArrayStack[Float]],
                                     ub: collection.mutable.UnrolledBuffer[collection.mutable.Iterable[Short]])

      verifySerDeser(make[MutableIterables](CodecMakerConfig()),
        MutableIterables(collection.mutable.Seq(collection.mutable.SortedSet("1", "2", "3")),
          collection.mutable.ArrayBuffer(collection.mutable.Set[BigInt](4), collection.mutable.Set.empty[BigInt]),
          collection.mutable.ArraySeq(collection.mutable.LinkedHashSet(5, 6), collection.mutable.LinkedHashSet.empty[Int]),
          collection.mutable.Buffer(collection.mutable.HashSet(7.7, 8.8)),
          collection.mutable.ListBuffer(collection.mutable.TreeSet(9L, 10L)),
          collection.mutable.IndexedSeq(collection.mutable.ArrayStack(11.11f, 12.12f)),
          collection.mutable.UnrolledBuffer(collection.mutable.Iterable(13.toShort, 14.toShort))),
        """{"ml":[["1","2","3"]],"ab":[[4],[]],"as":[[5,6],[]],"b":[[8.8,7.7]],"lb":[[9,10]],"is":[[11.11,12.12]],"ub":[[13,14]]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with immutable Iterables" in {
      verifySerDeser(codecOfImmutableIterables,
        ImmutableIterables(List(collection.immutable.ListSet("1")), collection.immutable.Queue(Set[BigInt](4)),
          IndexedSeq(collection.immutable.SortedSet(5, 6, 7), collection.immutable.SortedSet()),
          Stream(collection.immutable.TreeSet(8.9)), Vector(Iterable(10L, 11L))),
        """{"l":[["1"]],"q":[[4]],"is":[[5,6,7],[]],"s":[[8.9]],"v":[[10,11]]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level Iterables" in {
      val codecOfImmutableIterables = make[collection.mutable.Set[List[BigDecimal]]](CodecMakerConfig())
      verifySerDeser(codecOfImmutableIterables,
        collection.mutable.Set(List[BigDecimal](1.1, 2.2), List[BigDecimal](3.3)),
        """[[3.3],[1.1,2.2]]""".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level Iterables" in {
      val codecOfImmutableIterables = make[collection.mutable.Set[List[BigDecimal]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfImmutableIterables,
        collection.mutable.Set(List[BigDecimal](1.1, 2.2), List[BigDecimal](3.3)),
        """[["3.3"],["1.1","2.2"]]""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with mutable maps" in {
      verifySerDeser(codecOfMutableMaps,
        MutableMaps(collection.mutable.HashMap(true -> collection.mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
          collection.mutable.Map(1.1f -> collection.mutable.ListMap(BigInt(2) -> "2")),
          collection.mutable.OpenHashMap(1.1 -> collection.mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> collection.mutable.LinkedHashMap())),
        """{"hm":{"true":{"1.1":1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with immutable maps" in {
      verifySerDeser(make[ImmutableMaps](CodecMakerConfig()),
        ImmutableMaps(Map(1 -> 1.1), collection.immutable.HashMap("2" -> collection.immutable.ListMap('V' -> 2),
          "3" -> collection.immutable.ListMap('X' -> 3)),
          collection.immutable.SortedMap(4L -> collection.immutable.TreeMap(4.toByte -> 4.4f),
            5L -> collection.immutable.TreeMap.empty[Byte, Float])),
        """{"m":{"1":1.1},"hm":{"2":{"V":2},"3":{"X":3}},"sm":{"4":{"4":4.4},"5":{}}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level maps" in {
      val codecOfMaps = make[collection.mutable.LinkedHashMap[Int, Map[Char, Boolean]]](CodecMakerConfig())
      verifySerDeser(codecOfMaps,
        collection.mutable.LinkedHashMap(1 -> Map('V' -> true), 2 -> Map.empty[Char, Boolean]),
        """{"1":{"V":true},"2":{}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level maps" in {
      val codecOfMaps = make[collection.mutable.LinkedHashMap[Int, Map[Char, Boolean]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfMaps,
        collection.mutable.LinkedHashMap(1 -> Map('V' -> true), 2 -> Map.empty[Char, Boolean]),
        """{"1":{"V":"true"},"2":{}}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of JSON object is not properly started/closed or with leading/trailing comma" in {
      val immutableMaps = ImmutableMaps(Map(1 -> 1.1), collection.immutable.HashMap.empty,
        collection.immutable.SortedMap.empty)
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":["1":1.1},"hm":{},"sm":{}}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '{' or null, offset: 0x00000005"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":{,"1":1.1},"hm":{},"sm":{}}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000006"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":{"1":1.1],"hm":{},"sm":{}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '}' or ',', offset: 0x0000000d"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":{"1":1.1,},"hm":{},"sm":{}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x0000000e"))
    }
    "throw parse exception in case of illegal keys found during deserialization of maps" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfMutableMaps,
          MutableMaps(null, collection.mutable.Map(1.1f -> collection.mutable.ListMap(null.asInstanceOf[BigInt] -> "2")), null),
          """{"m":{"1.1":{"null":"2"}}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x0000000e"))
    }
    "serialize and deserialize case classes with mutable long maps" in {
      case class MutableLongMaps(lm1: collection.mutable.LongMap[Double], lm2: collection.mutable.LongMap[String])

      verifySerDeser(make[MutableLongMaps](CodecMakerConfig()),
        MutableLongMaps(collection.mutable.LongMap(1L -> 1.1), collection.mutable.LongMap(3L -> "33")),
        """{"lm1":{"1":1.1},"lm2":{"3":"33"}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with immutable int and long maps" in {
      case class ImmutableIntLongMaps(im: collection.immutable.IntMap[Double], lm: collection.immutable.LongMap[String])

      verifySerDeser(make[ImmutableIntLongMaps](CodecMakerConfig()),
        ImmutableIntLongMaps(collection.immutable.IntMap(1 -> 1.1, 2 -> 2.2), collection.immutable.LongMap(3L -> "33")),
        """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with mutable & immutable bitsets" in {
      case class BitSets(bs: collection.immutable.BitSet, mbs: collection.mutable.BitSet)

      verifySerDeser(make[BitSets](CodecMakerConfig()),
        BitSets(collection.immutable.BitSet(1, 2, 3), collection.mutable.BitSet(4, 5, 6)),
        """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets =
        make[collection.mutable.LongMap[collection.immutable.IntMap[collection.mutable.BitSet]]](CodecMakerConfig())
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        collection.mutable.LongMap(1L -> collection.immutable.IntMap(2 -> collection.mutable.BitSet(4, 5, 6),
          3 -> collection.mutable.BitSet.empty)),
        """{"1":{"2":[4,5,6],"3":[]}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets =
        make[collection.mutable.LongMap[collection.immutable.IntMap[collection.mutable.BitSet]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        collection.mutable.LongMap(1L -> collection.immutable.IntMap(2 -> collection.mutable.BitSet(4, 5, 6),
          3 -> collection.mutable.BitSet.empty)),
        """{"1":{"2":["4","5","6"],"3":[]}}""".getBytes("UTF-8"))
    }
    "throw parse exception when too big numbers are parsed for mutable & immutable bitsets" in {
      assert(intercept[JsonParseException] {
        verifySerDeser(make[collection.immutable.BitSet](CodecMakerConfig(bitSetValueLimit = 1000)),
          collection.immutable.BitSet(1, 2, 1000),
          """[1,2,1000]""".getBytes("UTF-8"))
      }.getMessage.contains("illegal value for bit set, offset: 0x00000008"))
      assert(intercept[JsonParseException] {
        verifySerDeser(make[collection.mutable.BitSet](CodecMakerConfig()), collection.mutable.BitSet(1, 2, 10000),
          """[1,2,10000]""".getBytes("UTF-8"))
      }.getMessage.contains("illegal value for bit set, offset: 0x00000009"))
    }
    "throw parse exception when negative numbers are parsed for mutable & immutable bitsets" in {
      assert(intercept[JsonParseException] {
        verifyDeser(make[collection.immutable.BitSet](CodecMakerConfig()), collection.immutable.BitSet(1, 2, 0),
          """[1,2,-1]""".getBytes("UTF-8"))
      }.getMessage.contains("illegal value for bit set, offset: 0x00000006"))
      assert(intercept[JsonParseException] {
        verifyDeser(make[collection.mutable.BitSet](CodecMakerConfig()), collection.mutable.BitSet(1, 2, 0),
          """[1,2,-1]""".getBytes("UTF-8"))
      }.getMessage.contains("illegal value for bit set, offset: 0x00000006"))
    }
    "don't generate codec for maps with not supported types of keys" in {
      assert(intercept[TestFailedException](assertCompiles {
        """JsonCodecMaker.make[Map[java.util.Date,String]](CodecMakerConfig())"""
      }).getMessage.contains {
        """Unsupported type to be used as map key 'java.util.Date'."""
      })
    }
    "serialize and deserialize with keys defined as is by fields" in {
      verifySerDeser(make[CamelSnakeKebabCases](CodecMakerConfig()),
        CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
        """{"camelCase":1,"snake_case":2,"kebab-case":3,"camel1":4,"snake_1":5,"kebab-1":6}""".getBytes("UTF-8"))
    }
    "serialize and deserialize with keys enforced to camelCase and throw parse exception when they are missing" in {
      val codecOfCamelAndSnakeCases = make[CamelSnakeKebabCases](CodecMakerConfig(JsonCodecMaker.enforceCamelCase))
      verifySerDeser(codecOfCamelAndSnakeCases,
        CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
        """{"camelCase":1,"snakeCase":2,"kebabCase":3,"camel1":4,"snake1":5,"kebab1":6}""".getBytes("UTF-8"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
          """{"camel_case":1,"snake_case":2,"kebab_case":3,"camel_1":4,"snake_1":5,"kebab_1":6}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"camelCase\", offset: 0x00000051"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
          """{"camel-case":1,"snake-case":2,"kebab-case":3,"camel-1":4,"snake-1":5,"kebab-1":6}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"camelCase\", offset: 0x00000051"))
    }
    "serialize and deserialize with keys enforced to snake_case and throw parse exception when they are missing" in {
      val codecOfCamelAndSnakeCases = make[CamelSnakeKebabCases](CodecMakerConfig(JsonCodecMaker.enforce_snake_case))
      verifySerDeser(codecOfCamelAndSnakeCases,
        CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
        """{"camel_case":1,"snake_case":2,"kebab_case":3,"camel_1":4,"snake_1":5,"kebab_1":6}""".getBytes("UTF-8"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
          """{"camelCase":1,"snakeCase":2,"kebabCase":3,"camel1":4,"snake1":5,"kebab1":6}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"camel_case\", offset: 0x0000004b"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
          """{"camel-case":1,"snake-case":2,"kebab-case":3,"camel-1":4,"snake-1":5,"kebab-1":6}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"camel_case\", offset: 0x00000051"))
    }
    "serialize and deserialize with keys enforced to kebab-case and throw parse exception when they are missing" in {
      val codecOfCamelAndSnakeCases = make[CamelSnakeKebabCases](CodecMakerConfig(JsonCodecMaker.`enforce-kebab-case`))
      verifySerDeser(codecOfCamelAndSnakeCases,
        CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
        """{"camel-case":1,"snake-case":2,"kebab-case":3,"camel-1":4,"snake-1":5,"kebab-1":6}""".getBytes("UTF-8"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
          """{"camelCase":1,"snakeCase":2,"kebabCase":3,"camel1":4,"snake1":5,"kebab1":6}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"camel-case\", offset: 0x0000004b"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelSnakeKebabCases(1, 2, 3, 4, 5, 6),
          """{"camel_case":1,"snake_case":2,"kebab_case":3,"camel_1":4,"snake_1":5,"kebab_1":6}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"camel-case\", offset: 0x00000051"))
    }
    "serialize and deserialize with keys overridden by annotation and throw parse exception when they are missing" in {
      verifySerDeser(codecOfNameOverridden, NameOverridden(oldName = "VVV"), """{"new_name":"VVV"}""".getBytes("UTF-8"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfNameOverridden, NameOverridden(oldName = "VVV"), """{"oldName":"VVV"}""".getBytes("UTF-8"))
      }.getMessage.contains("missing required field \"new_name\", offset: 0x00000010"))
    }
    "don't generate codec for case classes with field that have duplicated @named annotation" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedNamed(@named("x") @named("y") z: Int)
          |JsonCodecMaker.make[DuplicatedNamed](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Duplicated 'com.github.plokhotnyuk.jsoniter_scala.macros.named' defined for 'z' of 'DuplicatedNamed'."""
      })
    }
    "don't generate codec for case classes with fields that have duplicated JSON names" in {
      val expectedError =
        """Duplicated JSON name(s) defined for 'DuplicatedJsonName': 'x'. Names(s) defined by
          |'com.github.plokhotnyuk.jsoniter_scala.macros.named' annotation(s), name of discriminator field specified by
          |'config.discriminatorFieldName' and name(s) returned by 'config.fieldNameMapper' for non-annotated fields should
          |not match.""".stripMargin.replace('\n', ' ')
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedJsonName(x: Int, @named("x") z: Int)
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig())""".stripMargin
      }).getMessage.contains(expectedError))
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedJsonName(y: Int, z: Int)
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig(fieldNameMapper = _ => "x"))""".stripMargin
      }).getMessage.contains(expectedError))
    }
    "serialize and deserialize fields that stringified by annotation" in {
      verifySerDeser(codecOfStringified, stringified, """{"i":"1","bi":"2","l1":["1"],"l2":[2]}""".getBytes("UTF-8"))
    }
    "throw parse exception when stringified fields have non-string values" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStringified, stringified, """{"i":1,"bi":"2","l1":["1"],"l2":[2]}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000005"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStringified, stringified, """{"i":"1","bi":2,"l1":[1],"l2":[2]}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x0000000e"))
    }
    "serialize and deserialize indented JSON" in {
      verifySerDeser(codecOfIndented, indented,
        """{
          |  "s": "VVV",
          |  "bd": 1.1,
          |  "l": [
          |    1,
          |    2,
          |    3
          |  ],
          |  "m": {
          |    "S": -90.0
          |  }
          |}""".stripMargin.getBytes("UTF-8"),
        WriterConfig(indentionStep = 2))
    }
    "deserialize JSON with tabs & line returns" in {
      verifyDeser(codecOfIndented, indented,
        "{\r\t\"s\":\t\"VVV\",\r\t\"bd\":\t1.1,\r\t\"l\":\t[\r\t\t1,\r\t\t2,\r\t\t3\r\t],\r\t\"m\":\t{\r\t\t\"S\":\t-90.0}\r}".getBytes("UTF-8"))
    }
    "serialize and deserialize UTF-8 keys and values of case classes without hex encoding" in {
      verifySerDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ"),
        """{"გასაღები":"ვვვ"}""".getBytes("UTF-8"))
    }
    "serialize and deserialize UTF-8 keys and values of case classes with hex encoding" in {
      verifyDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t\\/\"}".getBytes("UTF-8"))
      verifySer(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t/\"}".getBytes("UTF-8"),
        WriterConfig(escapeUnicode = true))
    }
    "serialize and deserialize case classes with Scala operators in field names" in {
      case class Operators(`=<>!#%^&|*/\\~+-:$`: Int)

      verifySerDeser(make[Operators](CodecMakerConfig()), Operators(7), """{"=<>!#%^&|*/\\~+-:$":7}""".getBytes("UTF-8"))
    }
    "don't serialize default values of case classes that defined for fields" in {
      case class Defaults1(st: String = "VVV", i: Int = 1, bi: BigInt = -1, oc: Option[Char] = Some('X'),
                           l: List[Int] = List(0), e: Level = Level.HIGH,
                           a: Array[Array[Int]] = Array(Array(1, 2), Array(3, 4)),
                           ab: collection.mutable.ArrayBuffer[Int] = collection.mutable.ArrayBuffer(1, 2),
                           m: Map[Int, Boolean] = Map(1 -> true),
                           mm: collection.mutable.Map[String, Int] = collection.mutable.Map("VVV" -> 1),
                           im: collection.immutable.IntMap[String] = collection.immutable.IntMap(1 -> "VVV"),
                           lm: collection.mutable.LongMap[Int] = collection.mutable.LongMap(1L -> 2),
                           s: Set[String] = Set("VVV"),
                           ms: collection.mutable.Set[Int] = collection.mutable.Set(1),
                           bs: collection.immutable.BitSet = collection.immutable.BitSet(1),
                           mbs: collection.mutable.BitSet = collection.mutable.BitSet(1))

      val codecOfDefaults: JsonValueCodec[Defaults1] = make[Defaults1](CodecMakerConfig())
      verifySer(codecOfDefaults, Defaults1(), "{}".getBytes("UTF-8"))
      verifySer(codecOfDefaults, Defaults1(oc = None, l = Nil), """{}""".getBytes("UTF-8"))
    }
    "deserialize default values of case classes that defined for fields" in {
      case class Defaults2(st: String = "VVV", i: Int = 1, bi: BigInt = -1, oc: Option[Char] = Some('X'),
                           l: List[Int] = List(0), e: Level = Level.HIGH,
                           ab: collection.mutable.ArrayBuffer[Int] = collection.mutable.ArrayBuffer(1, 2),
                           m: Map[Int, Boolean] = Map(1 -> true),
                           mm: collection.mutable.Map[String, Int] = collection.mutable.Map("VVV" -> 1),
                           im: collection.immutable.IntMap[String] = collection.immutable.IntMap(1 -> "VVV"),
                           lm: collection.mutable.LongMap[Int] = collection.mutable.LongMap(1L -> 2),
                           s: Set[String] = Set("VVV"),
                           ms: collection.mutable.Set[Int] = collection.mutable.Set(1),
                           bs: collection.immutable.BitSet = collection.immutable.BitSet(1),
                           mbs: collection.mutable.BitSet = collection.mutable.BitSet(1))

      val codecOfDefaults: JsonValueCodec[Defaults2] = make[Defaults2](CodecMakerConfig())
      verifyDeser(codecOfDefaults, Defaults2(), """{}""".getBytes("UTF-8"))
      verifyDeser(codecOfDefaults, Defaults2(),
        """{"st":null,"bi":null,"l":null,"oc":null,"e":null,"ab":null,"m":null,"mm":null,"im":null,"lm":null,"s":null,"ms":null,"bs":null,"mbs":null}""".stripMargin.getBytes("UTF-8"))
      verifyDeser(codecOfDefaults, Defaults2(), """{"l":[],"ab":[],"m":{},"mm":{},"im":{},"lm":{},"s":[],"ms":[],"bs":[],"mbs":[]}""".getBytes("UTF-8"))
    }
    "don't serialize and deserialize transient and non constructor defined fields of case classes" in {
      case class Transient(@transient transient: String = "default", required: String) {
        val ignored: String = s"$required-$transient"
      }

      verifySer(make[Transient](CodecMakerConfig()), Transient(required = "VVV"), """{"required":"VVV"}""".getBytes("UTF-8"))
      verifyDeser(make[Transient](CodecMakerConfig()), Transient(required = "VVV"), """{"transient":"XXX","required":"VVV"}""".getBytes("UTF-8"))
    }
    "don't serialize case class fields with 'None' values" in {
      case class NullAndNoneValues(opt: Option[String])

      verifySer(make[NullAndNoneValues](CodecMakerConfig()), NullAndNoneValues(None), """{}""".getBytes("UTF-8"))
      verifySer(make[List[NullAndNoneValues]](CodecMakerConfig()), List(NullAndNoneValues(None)), """[{}]""".getBytes("UTF-8"))
    }
    "don't serialize case class fields with empty collections" in {
      case class EmptyIterables(l: List[String], s: Set[Int], ls: List[Set[Int]])

      verifySer(make[EmptyIterables](CodecMakerConfig()), EmptyIterables(List(), Set(), List()), """{}""".getBytes("UTF-8"))
    }
    "parse with skipping of unknown case class fields" in {
      case class SkipUnknown()

      verifyDeser(make[SkipUnknown](CodecMakerConfig()), SkipUnknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes("UTF-8"))
    }
    "throw parse exception for unknown case class fields if skipping of them wasn't allowed in materialize call" in {
      case class DetectUnknown()

      assert(intercept[JsonParseException] {
        verifyDeser(make[DetectUnknown](CodecMakerConfig(skipUnexpectedFields = false)), DetectUnknown(),
          """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes("UTF-8"))
      }.getMessage.contains("unexpected field \"x\", offset: 0x00000004"))
    }
    "throw parse exception in case of missing required case class fields detected during deserialization" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, StandardTypes("VVV", 0, 1),
          """{"s":null,"bi":0,"bd":1}""".stripMargin.getBytes("UTF-8"))
      }.getMessage.contains("""expected '"', offset: 0x00000005"""))

      case class RequiredAfterOptionalFields(f1: Option[Long], f2: Long, f3: Option[Long], f4: Long)

      assert(intercept[JsonParseException] {
        verifyDeser(make[RequiredAfterOptionalFields](CodecMakerConfig()),
          RequiredAfterOptionalFields(None, 2, None, 4), """{}""".getBytes("UTF-8"))
      }.getMessage.contains("""missing required field "f2", offset: 0x00000001"""))
      assert(intercept[JsonParseException] {
        verifyDeser(make[RequiredAfterOptionalFields](CodecMakerConfig()),
          RequiredAfterOptionalFields(None, 2, None, 4), """{"f2":2}""".getBytes("UTF-8"))
      }.getMessage.contains("""missing required field "f4", offset: 0x00000007"""))
      assert(intercept[JsonParseException] {
        verifyDeser(make[RequiredAfterOptionalFields](CodecMakerConfig()),
          RequiredAfterOptionalFields(None, 2, None, 4), """{"f1":1,"f2":2}""".getBytes("UTF-8"))
      }.getMessage.contains("""missing required field "f4", offset: 0x0000000e"""))

      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, StandardTypes("VVV", 0, 1),
          """{"s":"VVV","bi":null,"bd":1}""".stripMargin.getBytes("UTF-8"))
      }.getMessage.contains("""illegal number, offset: 0x00000010"""))
      assert(intercept[JsonParseException] {
        val obj = Required(
          0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
          10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
          20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
          30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
          40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
          50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
          60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
          70, 71, 72, 78, 74, 75, 76, 77, 78, 79,
          80, 81, 82, 83, 84, 85, 86, 87, 88, 89,
          90, 91, 92, 93, 94, 95, 96, 97, 98, 99)
        verifyDeser(make[Required](CodecMakerConfig()), obj,
          """{
            |"r00":0,"r01":1,"r02":2,"r03":3,"r04":4,"r05":5,"r06":6,"r07":7,"r08":8,"r09":9,
            |"r10":10,"r11":11,"r12":12,"r13":13,"r14":14,"r15":15,"r16":16,"r17":17,"r18":18,"r19":19,
            |"r20":20,"r21":21,"r22":22,"r23":23,"r24":24,"r25":25,"r26":26,"r27":27,"r28":28,"r29":29,
            |"r30":30,"r31":31,"r32":32,"r33":33,"r34":34,"r35":35,"r36":36,"r37":37,"r38":38,"r39":39,
            |"r40":40,"r41":41,"r42":42,"r43":43,"r44":44,"r45":45,"r46":46,"r47":47,"r48":48,"r49":49,
            |"r50":50,"r51":51,"r52":52,"r53":53,"r54":54,"r55":55,"r56":56,"r57":57,"r58":58,"r59":59,
            |"r60":60,"r61":61,"r62":62,"r63":63,"r64":64,"r65":65,"r66":66,"r67":67,"r68":68,"r69":69,
            |"r70":70,"r71":71,"r72":72,"r73":73,"r74":74,"r75":75,"r76":76,"r77":77,"r78":78,"r79":79,
            |"r80":80,"r81":81,"r82":82,"r83":83,"r84":84,"r85":85,"r86":86,"r87":87,"r88":88,"r89":89,
            |"r90":90,"r91":91,"r92":92,"r93":93,"r94":94,"r95":95,"r96":96,"r97":97,"r98":98
            |}""".stripMargin.getBytes("UTF-8"))
      }.getMessage.contains("""missing required field "r99", offset: 0x0000037c"""))
    }
    "serialize and deserialize ADTs using ASCII discriminator field & value" in {
      verifySerDeser(codecOfADTList,
        List(AAA(1), BBB("VVV"), CCC(1, "VVV"), DDD),
        """[{"type":"AAA","a":1},{"type":"BBB","a":"VVV"},{"type":"CCC","a":1,"b":"VVV"},{"type":"DDD"}]"""
          .getBytes("UTF-8"))
      val longStr = new String(Array.fill(100000)('W'))
      verifyDeser(codecOfADTList,
        List(CCC(2, longStr), CCC(1, "VVV")),
        s"""[{"a":2,"b":"$longStr","type":"CCC"},{"a":1,"type":"CCC","b":"VVV"}]""".getBytes("UTF-8"))
      verifySerDeser(make[List[AdtBase]](CodecMakerConfig(discriminatorFieldName = "t")),
        List(CCC(2, "WWW"), CCC(1, "VVV")),
        s"""[{"t":"CCC","a":2,"b":"WWW"},{"t":"CCC","a":1,"b":"VVV"}]""".getBytes("UTF-8"))
    }
    "serialize and deserialize ADTs with leaf types that are not case classes or case objects" in {
      sealed trait X

      class A() extends X {
        override def hashCode(): Int = 0

        override def equals(obj: scala.Any): Boolean = obj.isInstanceOf[A]

        override def toString: String = "A()"
      }

      object B extends X {
        override def toString: String = "B"
      }

      verifySerDeser(make[List[X]](CodecMakerConfig()),
        List(new A(), B), """[{"type":"A"},{"type":"B"}]""".getBytes("UTF-8"))
    }
    "serialize and deserialize ADTs using non-ASCII discriminator field & value w/ reusage of case classes w/o ADTs" in {
      sealed abstract class База extends Product with Serializable

      case class А(б: Б) extends База

      case class Б(с: String) extends База

      verifySerDeser(make[List[База]](CodecMakerConfig(discriminatorFieldName = "тип", skipUnexpectedFields = false)),
        List(А(Б("x")), Б("x")),
        """[{"тип":"А","б":{"с":"x"}},{"тип":"Б","с":"x"}]""".getBytes("UTF-8"))
    }
    "serialize and deserialize ADTs with Scala operators in names" in {
      sealed trait TimeZone extends Product with Serializable

      case object `US/Alaska` extends TimeZone

      case object `Europe/Paris` extends TimeZone

      verifySerDeser(make[List[TimeZone]](CodecMakerConfig(discriminatorFieldName = "zoneId")),
        List(`US/Alaska`, `Europe/Paris`),
        """[{"zoneId":"US/Alaska"},{"zoneId":"Europe/Paris"}]""".getBytes("UTF-8"))
    }
    "throw parse exception in case of duplicated discriminator field" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(AAA(1)), """[{"type":"AAA","a":1,"type":"AAA"}]""".getBytes("UTF-8"))
      }.getMessage.contains("""duplicated field "type", offset: 0x0000001b"""))
    }
    "throw parse exception in case of missing discriminator field or illegal value of discriminator field" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(AAA(1)), """[{"a":1}]""".getBytes("UTF-8"))
      }.getMessage.contains("""missing required field "type", offset: 0x00000007"""))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(AAA(1)), """[{"a":1,"type":"aaa"}]""".getBytes("UTF-8"))
      }.getMessage.contains("""illegal value of discriminator field "type", offset: 0x00000013"""))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(AAA(1)), """[{"a":1,"type":123}]""".getBytes("UTF-8"))
      }.getMessage.contains("""expected '"', offset: 0x0000000f"""))
    }
    "don't generate codec for non sealed traits or abstract classes as an ADT base" in {
      assert(intercept[TestFailedException](assertCompiles {
        """trait X
          |case class A(i: Int) extends X
          |case object B extends X
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Only sealed traits & abstract classes are supported for an ADT base. Please consider adding of a sealed
          |definition for 'X' or using a custom implicitly accessible codec for the ADT base."""
          .stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """abstract class X
          |case class A(i: Int) extends X
          |case object B extends X
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Only sealed traits & abstract classes are supported for an ADT base. Please consider adding of a sealed
          |definition for 'X' or using a custom implicitly accessible codec for the ADT base."""
          .stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codec for ADT base without leaf classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X extends Product with Serializable
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please consider adding them or using a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed abstract class X extends Product with Serializable
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please consider adding them or using a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codec for case objects which are mapped to the same discriminator value" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X extends Product with Serializable
          |case object A extends X
          |case object B extends X
          |JsonCodecMaker.make[X](CodecMakerConfig(adtLeafClassNameMapper = _ => "Z"))""".stripMargin
      }).getMessage.contains {
        """Duplicated values defined for 'type': 'Z'. Values returned by 'config.adtLeafClassNameMapper'
          |should not match.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codec for case classes with fields that the same name as discriminator name" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait DuplicatedJsonName extends Product with Serializable
          |case class A(x: Int) extends DuplicatedJsonName
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig(discriminatorFieldName = "x"))""".stripMargin
      }).getMessage.contains {
        """Duplicated JSON name(s) defined for 'A': 'x'. Names(s) defined by
          |'com.github.plokhotnyuk.jsoniter_scala.macros.named' annotation(s), name of discriminator field specified by
          |'config.discriminatorFieldName' and name(s) returned by 'config.fieldNameMapper' for non-annotated fields should
          |not match.""".stripMargin.replace('\n', ' ')
      })
    }
    "serialize and deserialize when the root codec defined as an impicit val" in {
      implicit val implicitRootCodec: JsonValueCodec[Int] = make[Int](CodecMakerConfig())
      verifySerDeser(implicitRootCodec, 1, "1".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with Java time types" in {
      case class JavaTimeTypes(d: Duration, i: Instant, ld: LocalDate,
                               ldt: LocalDateTime, lt: LocalTime, md: MonthDay,
                               odt: OffsetDateTime, ot: OffsetTime, p: Period,
                               y: Year, ym: YearMonth, zdt: ZonedDateTime,
                               zi: ZoneId, zo: ZoneOffset)

      verifySerDeser(make[JavaTimeTypes](CodecMakerConfig()),
        obj = JavaTimeTypes(
          d = Duration.parse("PT10H30M"),
          i = Instant.parse("2007-12-03T10:15:30.001Z"),
          ld = LocalDate.parse("2007-12-03"),
          ldt = LocalDateTime.parse("2007-12-03T10:15:30"),
          lt = LocalTime.parse("10:15:30"),
          md = MonthDay.parse("--12-03"),
          odt = OffsetDateTime.parse("2007-12-03T10:15:30+01:00"),
          ot = OffsetTime.parse("10:15:30+01:00"),
          p = Period.parse("P1Y2M25D"),
          y = Year.parse("2007"),
          ym = YearMonth.parse("2007-12"),
          zdt = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"),
          zi = ZoneId.of("Europe/Paris"),
          zo = ZoneOffset.of("+01:00")
        ),
        json =
          ("""{"d":"PT10H30M","i":"2007-12-03T10:15:30.001Z","ld":"2007-12-03","ldt":"2007-12-03T10:15:30",""" +
          """"lt":"10:15:30","md":"--12-03","odt":"2007-12-03T10:15:30+01:00","ot":"10:15:30+01:00",""" +
          """"p":"P1Y2M25D","y":"2007","ym":"2007-12","zdt":"2007-12-03T10:15:30+01:00[Europe/Paris]",""" +
          """"zi":"Europe/Paris","zo":"+01:00"}""").getBytes("UTF-8"))
    }
    "serialize and deserialize top-level Java time types" in {
      verifySerDeser(make[Duration](CodecMakerConfig()),
        Duration.parse("PT10H30M"), "\"PT10H30M\"".getBytes("UTF-8"))
      verifySerDeser(make[Instant](CodecMakerConfig()),
        Instant.parse("2007-12-03T10:15:30.001Z"), "\"2007-12-03T10:15:30.001Z\"".getBytes("UTF-8"))
      verifySerDeser(make[LocalDate](CodecMakerConfig()),
        LocalDate.parse("2007-12-03"), "\"2007-12-03\"".getBytes("UTF-8"))
      verifySerDeser(make[LocalDateTime](CodecMakerConfig()),
        LocalDateTime.parse("2007-12-03T10:15:30"), "\"2007-12-03T10:15:30\"".getBytes("UTF-8"))
      verifySerDeser(make[LocalTime](CodecMakerConfig()),
        LocalTime.parse("10:15:30"), "\"10:15:30\"".getBytes("UTF-8"))
      verifySerDeser(make[MonthDay](CodecMakerConfig()),
        MonthDay.parse("--12-03"), "\"--12-03\"".getBytes("UTF-8"))
      verifySerDeser(make[OffsetDateTime](CodecMakerConfig()),
        OffsetDateTime.parse("2007-12-03T10:15:30+01:00"), "\"2007-12-03T10:15:30+01:00\"".getBytes("UTF-8"))
      verifySerDeser(make[OffsetTime](CodecMakerConfig()),
        OffsetTime.parse("10:15:30+01:00"), "\"10:15:30+01:00\"".getBytes("UTF-8"))
      verifySerDeser(make[Period](CodecMakerConfig()),
        Period.parse("P1Y2M25D"), "\"P1Y2M25D\"".getBytes("UTF-8"))
      verifySerDeser(make[Year](CodecMakerConfig()), Year.parse("2007"), "\"2007\"".getBytes("UTF-8"))
      verifySerDeser(make[YearMonth](CodecMakerConfig()),
        YearMonth.parse("2007-12"), "\"2007-12\"".getBytes("UTF-8"))
      verifySerDeser(make[ZonedDateTime](CodecMakerConfig()),
        ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"),
        "\"2007-12-03T10:15:30+01:00[Europe/Paris]\"".getBytes("UTF-8"))
      verifySerDeser(make[ZoneId](CodecMakerConfig()),
        ZoneId.of("Europe/Paris"), "\"Europe/Paris\"".getBytes("UTF-8"))
      verifySerDeser(make[ZoneOffset](CodecMakerConfig()),
        ZoneOffset.of("+01:00"), "\"+01:00\"".getBytes("UTF-8"))
    }
    "serialize and deserialize Java time types as key in maps" in {
      verifySerDeser(make[Map[Duration, Int]](CodecMakerConfig()),
        Map(Duration.parse("PT10H30M") -> 0), "{\"PT10H30M\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[Instant, Int]](CodecMakerConfig()),
        Map(Instant.parse("2007-12-03T10:15:30.001Z") -> 0), "{\"2007-12-03T10:15:30.001Z\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[LocalDate, Int]](CodecMakerConfig()),
        Map(LocalDate.parse("2007-12-03") -> 0), "{\"2007-12-03\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[LocalDateTime, Int]](CodecMakerConfig()),
        Map(LocalDateTime.parse("2007-12-03T10:15:30") -> 0), "{\"2007-12-03T10:15:30\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[LocalTime, Int]](CodecMakerConfig()),
        Map(LocalTime.parse("10:15:30") -> 0), "{\"10:15:30\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[MonthDay, Int]](CodecMakerConfig()),
        Map(MonthDay.parse("--12-03") -> 0), "{\"--12-03\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[OffsetDateTime, Int]](CodecMakerConfig()),
        Map(OffsetDateTime.parse("2007-12-03T10:15:30+01:00") -> 0), "{\"2007-12-03T10:15:30+01:00\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[OffsetTime, Int]](CodecMakerConfig()),
        Map(OffsetTime.parse("10:15:30+01:00") -> 0), "{\"10:15:30+01:00\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[Period, Int]](CodecMakerConfig()),
        Map(Period.parse("P1Y2M25D") -> 0), "{\"P1Y2M25D\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[Year, Int]](CodecMakerConfig()),
        Map(Year.parse("2007") -> 0), "{\"2007\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[YearMonth, Int]](CodecMakerConfig()),
        Map(YearMonth.parse("2007-12") -> 0), "{\"2007-12\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[ZonedDateTime, Int]](CodecMakerConfig()),
        Map(ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]") -> 0),
        "{\"2007-12-03T10:15:30+01:00[Europe/Paris]\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[ZoneId, Int]](CodecMakerConfig()),
        Map(ZoneId.of("Europe/Paris") -> 0), "{\"Europe/Paris\":0}".getBytes("UTF-8"))
      verifySerDeser(make[Map[ZoneOffset, Int]](CodecMakerConfig()),
        Map(ZoneOffset.of("+01:00") -> 0), "{\"+01:00\":0}".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level Java time types" in {
      verifySerDeser(make[Year](CodecMakerConfig(isStringified = true)), Year.of(2008), "\"2008\"".getBytes("UTF-8"))
    }
    "serialize and deserialize case class with aliased typed methods" in {
      type I = Int
      type S = String
      type L = List[I]
      type M = Map[I, S]

      case class TypeAliases(i: I, s: S, l: L, m: M)

      verifySerDeser(make[TypeAliases](CodecMakerConfig()), TypeAliases(1, "VVV", List(1, 2, 3), Map(1 -> "VVV")),
        """{"i":1,"s":"VVV","l":[1,2,3],"m":{"1":"VVV"}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize collection with aliased type arguments" in {
      type I = Int
      type S = String

      verifySerDeser(make[Map[I, S]](CodecMakerConfig()), Map(1 -> "VVV"), "{\"1\":\"VVV\"}".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level aliased types" in {
      type I = Int
      type L = List[I]

      verifySerDeser(make[L](CodecMakerConfig()), List(1, 2, 3), "[1,2,3]".getBytes("UTF-8"))
    }
    "serialize and deserialize first-order types" in {
      verifySerDeser(make[Either[Int, String]](CodecMakerConfig(fieldNameMapper = _ => "value")), Right("VVV"),
        """{"type":"Right","value":"VVV"}""".getBytes("UTF-8"))

      case class FirstOrderType[A, B](a: A, b: B, oa: Option[A], bs: List[B])

      verifySerDeser(make[FirstOrderType[Int, String]](CodecMakerConfig()),
        FirstOrderType[Int, String](1, "VVV", Some(1), List("WWW")),
        """{"a":1,"b":"VVV","oa":1,"bs":["WWW"]}""".getBytes("UTF-8"))
      verifySerDeser(make[FirstOrderType[Id[Int], Id[String]]](CodecMakerConfig()),
        FirstOrderType[Id[Int], Id[String]](Id[Int](1), Id[String]("VVV"), Some(Id[Int](2)), List(Id[String]("WWW"))),
        """{"a":1,"b":"VVV","oa":2,"bs":["WWW"]}""".getBytes("UTF-8"))
    }
    "don't generate codecs for first-order types that are specified using 'Any' type parameter" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class FirstOrder[A](a: A)
          |JsonCodecMaker.make[FirstOrder[_]](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Only sealed traits & abstract classes are supported for an ADT base. Please consider adding of a sealed
          |definition for 'Any' or using a custom implicitly accessible codec for the ADT base."""
          .stripMargin.replace('\n', ' ')
      })
    }
    "serialize and deserialize higher-kinded types" in {
      import scala.language.higherKinds

      sealed trait Foo[A[_]] extends Product with Serializable

      case class Bar[A[_]](a: A[Int]) extends Foo[A]

      case class Baz[A[_]](a: A[String]) extends Foo[A]

      val codecOfFooOption = make[Foo[Option]](CodecMakerConfig())
      verifySerDeser(codecOfFooOption, Bar[Option](Some(1)), """{"type":"Bar","a":1}""".getBytes("UTF-8"))
      verifySerDeser(codecOfFooOption, Baz[Option](Some("VVV")), """{"type":"Baz","a":"VVV"}""".getBytes("UTF-8"))

      case class HigherKindedType[F[_]](f: F[Int], fs: F[HigherKindedType[F]])

      verifySerDeser(make[HigherKindedType[Option]](CodecMakerConfig()),
        HigherKindedType[Option](Some(1), Some(HigherKindedType[Option](Some(2), None))),
        """{"f":1,"fs":{"f":2}}""".getBytes("UTF-8"))
      verifySerDeser(make[HigherKindedType[List]](CodecMakerConfig()),
        HigherKindedType[List](List(1), List(HigherKindedType[List](List(2, 3, 4), Nil))),
        """{"f":[1],"fs":[{"f":[2,3,4]}]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with private primary constructor if it can be accessed" in {
      object PrivatePrimaryConstructor {
        implicit val codec: JsonValueCodec[PrivatePrimaryConstructor] =
          JsonCodecMaker.make[PrivatePrimaryConstructor](CodecMakerConfig())

        def apply(s: String) = new PrivatePrimaryConstructor(s)
      }

      case class PrivatePrimaryConstructor private(i: Int) {
        def this(s: String) = this(s.toInt)
      }

      verifySerDeser(PrivatePrimaryConstructor.codec, PrivatePrimaryConstructor("1"), "{\"i\":1}".getBytes("UTF-8"))
    }
    "don't generate codecs for unsupported classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """JsonCodecMaker.make[java.util.Date](CodecMakerConfig())"""
      }).getMessage.contains {
        """No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_]' defined for 'java.util.Date'."""
      })
    }
    "don't generate codecs for case classes with multiple parameter lists in a primary constructor" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class MultiListOfArgs(i: Int)(l: Long)
          |JsonCodecMaker.make[MultiListOfArgs](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """'MultiListOfArgs' has a primary constructor with multiple parameter lists.
          |Please consider using a custom implicitly accessible codec for this type.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for classes with parameters in a primary constructor that have no accessor for read" in {
      assert(intercept[TestFailedException](assertCompiles {
        """class ParamHasNoAccessor(val i: Int, a: String)
          |JsonCodecMaker.make[ParamHasNoAccessor](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """'a' parameter of 'ParamHasNoAccessor' should be defined as 'val' or 'var' in the primary constructor."""
      })
    }
  }
  "JsonCodecMaker.enforceCamelCase" should {
    "transform snake_case names to camelCase" in {
      JsonCodecMaker.enforceCamelCase("o_o") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("o_ooo_") shouldBe "oOoo"
      JsonCodecMaker.enforceCamelCase("OO_OOO_111") shouldBe "ooOoo111"
      JsonCodecMaker.enforceCamelCase("ooo_111") shouldBe "ooo111"
    }
    "transform kebab-case names to camelCase" in {
      JsonCodecMaker.enforceCamelCase("o-o") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("o-ooo-") shouldBe "oOoo"
      JsonCodecMaker.enforceCamelCase("O-OOO-111") shouldBe "oOoo111"
    }
    "leave camelCase names as is" in {
      JsonCodecMaker.enforceCamelCase("oO") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("oOoo") shouldBe "oOoo"
      JsonCodecMaker.enforceCamelCase("OOoo111") shouldBe "OOoo111"
    }
  }
  "JsonCodecMaker.enforce_snake_case" should {
    "transform camelCase names to snake_case" in {
      JsonCodecMaker.enforce_snake_case("oO") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("oOoo") shouldBe "o_ooo"
      JsonCodecMaker.enforce_snake_case("OOOoo111") shouldBe "oo_ooo_111"
      JsonCodecMaker.enforce_snake_case("Ooo111") shouldBe "ooo_111"
    }
    "transform kebab-case names to snake_case" in {
      JsonCodecMaker.enforce_snake_case("o-O") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o-ooo-") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case("O-OOO-111") shouldBe "o_ooo_111"
    }
    "enforce lower case for snake_case names" in {
      JsonCodecMaker.enforce_snake_case("o_O") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o_ooo_") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case("O_OOO_111") shouldBe "o_ooo_111"
    }
  }
  "JsonCodecMaker.enforce-kebab-case" should {
    "transform camelCase names to kebab-case" in {
      JsonCodecMaker.`enforce-kebab-case`("oO") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("oOoo") shouldBe "o-ooo"
      JsonCodecMaker.`enforce-kebab-case`("OOOoo111") shouldBe "oo-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("Ooo111") shouldBe "ooo-111"
    }
    "transform snake_case names to kebab-case" in {
      JsonCodecMaker.`enforce-kebab-case`("o_O") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("o_ooo_") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case`("O_OOO_111") shouldBe "o-ooo-111"
    }
    "enforce lower case for kebab-case names" in {
      JsonCodecMaker.`enforce-kebab-case`("o-O") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("o-ooo-") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case`("O-OOO-111") shouldBe "o-ooo-111"
    }
  }
  "JsonCodecMaker.simpleClassName" should {
    "shorten full class name to simple class name" in {
      JsonCodecMaker.simpleClassName("com.github.plohkotnyuk.jsoniter_scala.Test") shouldBe "Test"
      JsonCodecMaker.simpleClassName("JsonCodecMakerSpec.this.Test") shouldBe "Test"
      JsonCodecMaker.simpleClassName(".Test") shouldBe "Test"
      JsonCodecMaker.simpleClassName("Test") shouldBe "Test"
    }
  }

  def verifySerDeser[T](codec: JsonValueCodec[T], obj: T, json: Array[Byte], cfg: WriterConfig = WriterConfig()): Unit = {
    verifySer(codec, obj, json, cfg)
    verifyDeser(codec, obj, json)
  }

  def verifySer[T](codec: JsonValueCodec[T], obj: T, json: Array[Byte], cfg: WriterConfig = WriterConfig()): Unit = {
    val baos = new ByteArrayOutputStream
    writeToStream(obj, baos, cfg)(codec)
    toString(baos.toByteArray) shouldBe toString(json)
    toString(writeToArray(obj, cfg)(codec)) shouldBe toString(json)
  }

  def verifyDeser[T](codec: JsonValueCodec[T], obj: T, json: Array[Byte]): Unit = {
    readFromStream(new ByteArrayInputStream(json))(codec) shouldBe obj
    readFromArray(json)(codec) shouldBe obj
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}

case class NameOverridden(@named("new_" + "name") oldName: String) // intentionally declared after the `make` call
