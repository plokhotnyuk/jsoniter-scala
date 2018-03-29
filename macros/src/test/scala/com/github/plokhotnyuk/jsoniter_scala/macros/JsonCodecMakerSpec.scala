package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Matchers, WordSpec}

import scala.annotation.switch
import scala.collection.immutable._
import scala.collection.mutable

case class UserId(id: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

class JsonCodecMakerSpec extends WordSpec with Matchers {
  case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

  val primitives = Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f)
  val codecOfPrimitives: JsonValueCodec[Primitives] = make[Primitives](CodecMakerConfig())

  case class BoxedPrimitives(b: java.lang.Byte, s: java.lang.Short, i: java.lang.Integer, l: java.lang.Long,
                             bl: java.lang.Boolean, ch: java.lang.Character, dbl: java.lang.Double, f: java.lang.Float)

  case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

  val standardTypes = StandardTypes("VVV", 1, 1.1)
  val codecOfStandardTypes: JsonValueCodec[StandardTypes] = make[StandardTypes](CodecMakerConfig())

  case class JavaTypes(uuid: UUID)
  val codecOfJavaTypes: JsonValueCodec[JavaTypes] = make[JavaTypes](CodecMakerConfig())

  object LocationType extends Enumeration {
    type LocationType = Value
    val GPS: LocationType = Value(1, "GPS") // always set name explicitly in your Enumeration definition, if you still not sure,
    val IP: LocationType = Value(2, "IP") // then please look and check that following synchronized block will not affect your code in runtime:
    val UserProvided: LocationType = Value(3, "UserProvided") // https://github.com/scala/scala/blob/1692ae306dc9a5ff3feebba6041348dfdee7cfb5/src/library/scala/Enumeration.scala#L203
  }

  case class Enums(lt: LocationType.LocationType)

  val codecOfEnums: JsonValueCodec[Enums] = make[Enums](CodecMakerConfig())

  case class JavaEnums(l: Level, il: Levels.InnerLevel)

  val codecOfJavaEnums: JsonValueCodec[JavaEnums] = make[JavaEnums](CodecMakerConfig())

  case class OuterTypes(s: String, st: Either[String, StandardTypes] = Left("error"))

  case class ValueClassTypes(uid: UserId, oid: OrderId)

  case class Options(os: Option[String], obi: Option[BigInt], osi: Option[Set[Int]], ol: Option[Long],
                     ojl: Option[java.lang.Long])

  val codecOfOptions: JsonValueCodec[Options] = make[Options](CodecMakerConfig())

  case class Tuples(t1: (Int, Double, List[Char]), t2: (String, BigInt, Option[LocationType.LocationType]))

  val codecOfTuples: JsonValueCodec[Tuples] = make[Tuples](CodecMakerConfig())

  case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

  val arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array[BigInt](7))
  val codecOfArrays: JsonValueCodec[Arrays] = make[Arrays](CodecMakerConfig())

  case class MutableTraversables(ml: mutable.MutableList[mutable.SortedSet[String]],
                                 ab: mutable.ArrayBuffer[mutable.Set[BigInt]],
                                 as: mutable.ArraySeq[mutable.LinkedHashSet[Int]],
                                 b: mutable.Buffer[mutable.HashSet[Double]],
                                 lb: mutable.ListBuffer[mutable.TreeSet[Long]],
                                 is: mutable.IndexedSeq[mutable.ArrayStack[Float]],
                                 ub: mutable.UnrolledBuffer[mutable.Traversable[Short]],
                                 ls: mutable.LinearSeq[Byte],
                                 ra: mutable.ResizableArray[mutable.Seq[Double]])

  case class ImmutableTraversables(l: List[ListSet[String]], q: Queue[Set[BigInt]],
                                   is: IndexedSeq[SortedSet[Int]], s: Stream[TreeSet[Double]],
                                   v: Vector[Traversable[Long]])

  val codecOfImmutableTraversables: JsonValueCodec[ImmutableTraversables] = make[ImmutableTraversables](CodecMakerConfig())

  case class MutableMaps(hm: mutable.HashMap[Boolean, mutable.AnyRefMap[BigDecimal, Int]],
                         m: mutable.Map[Float, mutable.ListMap[BigInt, String]],
                         ohm: mutable.OpenHashMap[Double, mutable.LinkedHashMap[Short, Double]])

  val codecOfMutableMaps: JsonValueCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())

  case class ImmutableMaps(m: Map[Int, Double], hm: HashMap[String, ListMap[Char, BigInt]],
                           sm: SortedMap[Long, TreeMap[Byte, Float]])

  val codecOfImmutableMaps: JsonValueCodec[ImmutableMaps] = make[ImmutableMaps](CodecMakerConfig())

  case class MutableLongMaps(lm1: mutable.LongMap[Double], lm2: mutable.LongMap[String])

  case class ImmutableIntLongMaps(im: IntMap[Double], lm: LongMap[String])

  case class BitSets(bs: BitSet, mbs: mutable.BitSet)

  case class CamelSnakeKebabCases(camelCase: Int, snake_case: Int, `kebab-case`: Int,
                                  `camel1`: Int, `snake_1`: Int, `kebab-1`: Int)

  val codecOfNameOverridden: JsonValueCodec[NameOverridden] = make[NameOverridden](CodecMakerConfig())

  case class Indented(s: String, bd: BigDecimal, l: List[Int], m: Map[Char, Double])

  val indented = Indented("VVV", 1.1, List(1, 2, 3), Map('S' -> -90.0, 'N' -> 90.0, 'W' -> -180.0, 'E' -> 180.0))
  val codecOfIndented: JsonValueCodec[Indented] = make[Indented](CodecMakerConfig())

  case class UTF8KeysAndValues(გასაღები: String)

  val codecOfUTF8KeysAndValues: JsonValueCodec[UTF8KeysAndValues] = make[UTF8KeysAndValues](CodecMakerConfig())

  case class Operators(`=<>!#%^&|*/\\~+-:$`: Int)

  case class Stringified(@stringified i: Int, @stringified bi: BigInt, @stringified l1: List[Int], l2: List[Int])

  val stringified = Stringified(1, 2, List(1), List(2))
  val codecOfStringified: JsonValueCodec[Stringified] = make[Stringified](CodecMakerConfig())

  case class Defaults(s: String = "VVV", i: Int = 1, bi: BigInt = -1, oc: Option[Char] = Some('X'),
                      l: List[Int] = List(0), e: Level = Level.HIGH)

  val defaults = Defaults()
  val codecOfDefaults: JsonValueCodec[Defaults] = make[Defaults](CodecMakerConfig())

  case class Transient(@transient transient: String = "default", required: String) {
    val ignored: String = s"$required-$transient"
  }

  case class NullAndNoneValues(opt: Option[String])

  case class EmptyTraversables(l: List[String], s: Set[Int], ls: List[Set[Int]])

  case class Unknown()

  case class Required(r00: Int, r01: Int, r02: Int, r03: Int, r04: Int, r05: Int, r06: Int, r07: Int, r08: Int, r09: Int,
                      r10: Int, r11: Int, r12: Int, r13: Int, r14: Int, r15: Int, r16: Int, r17: Int, r18: Int, r19: Int,
                      r20: Int, r21: Int, r22: Int, r23: Int, r24: Int, r25: Int, r26: Int, r27: Int, r28: Int, r29: Int,
                      r30: Int, r31: Int, r32: Int, r33: Int, r34: Int, r35: Int, r36: Int, r37: Int, r38: Int, r39: Int,
                      r40: Int, r41: Int, r42: Int, r43: Int, r44: Int, r45: Int, r46: Int, r47: Int, r48: Int, r49: Int,
                      r50: Int, r51: Int, r52: Int, r53: Int, r54: Int, r55: Int, r56: Int, r57: Int, r58: Int, r59: Int,
                      r60: Int, r61: Int, r62: Int, r63: Int, r64: Int, r65: Int, r66: Int, r67: Int, r68: Int, r69: Int,
                      r70: Int, r71: Int, r72: Int, r73: Int, r74: Int, r75: Int, r76: Int, r77: Int, r78: Int, r79: Int,
                      r80: Int, r81: Int, r82: Int, r83: Int, r84: Int, r85: Int, r86: Int, r87: Int, r88: Int, r89: Int,
                      r90: Int, r91: Int, r92: Int, r93: Int, r94: Int, r95: Int, r96: Int, r97: Int, r98: Int, r99: Int)

  sealed trait AdtBase extends Product with Serializable

  sealed abstract class Inner extends AdtBase {
    def a: Int
  }

  case class A(a: Int) extends Inner

  case class B(a: String) extends AdtBase

  case class C(a: Int, b: String) extends Inner

  case object D extends AdtBase

  sealed trait TimeZone

  case object `US/Alaska` extends TimeZone

  case object `Europe/Paris` extends TimeZone

  sealed abstract class База

  case class А(б: Б) extends База

  case class Б(с: String) extends База

  val codecOfADTList: JsonValueCodec[List[AdtBase]] = make[List[AdtBase]](CodecMakerConfig())

  case class JavaTimeTypes(d: Duration, i: Instant, ld: LocalDate,
                           ldt: LocalDateTime, lt: LocalTime, md: MonthDay,
                           odt: OffsetDateTime, ot: OffsetTime, p: Period,
                           y: Year, ym: YearMonth, zdt: ZonedDateTime,
                           zi: ZoneId, zo: ZoneOffset)

  type I = Int
  type S = String
  type L = List[I]
  type M = Map[I, S]

  case class TypeAliases(i: I, s: S, l: L, m: M)

  case class PrivatePrimaryConstructor private(i: Int) {
    def this(s: String) = this(s.toInt)
  }

  object PrivatePrimaryConstructor {
    implicit val codec: JsonValueCodec[PrivatePrimaryConstructor] =
      JsonCodecMaker.make[PrivatePrimaryConstructor](CodecMakerConfig())

    def apply(s: String) = new PrivatePrimaryConstructor(s)
  }

  "JsonValueCodec" should {
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
      val codecOfString = make[String](CodecMakerConfig())
      verifySerDeser(codecOfString, text, s""""$text"""".getBytes("UTF-8"))
      val codecOfBigInt = make[BigInt](CodecMakerConfig())
      verifySerDeser(codecOfBigInt, BigInt("123456789012345678901234567890"),
        "123456789012345678901234567890".getBytes("UTF-8"))
      val codecOfBigDecimal = make[BigDecimal](CodecMakerConfig())
      verifySerDeser(codecOfBigDecimal, BigDecimal("1234567890.12345678901234567890"),
        "1234567890.12345678901234567890".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level standard types" in {
      val codecOfBigInt = make[BigInt](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfBigInt,
        BigInt("123456789012345678901234567890"), "\"123456789012345678901234567890\"".getBytes("UTF-8"))
      val codecOfBigDecimal = make[BigDecimal](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfBigDecimal,
        BigDecimal("1234567890.12345678901234567890"), "\"1234567890.12345678901234567890\"".getBytes("UTF-8"))
    }
    "deserialize case classes with duplicated fields, the last field value is accepted" in {
      verifyDeser(codecOfStandardTypes, StandardTypes("VVV", BigInt("1"), BigDecimal("2")),
        s"""{"s":"XXX","s":"VVV","bi":10,"bi":1,"bd":20.0,"bd":2.0}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of invalid value is detected for first occurrence of duplicated fields" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, StandardTypes("VVV", BigInt("1"), BigDecimal("2")),
          s"""{"s":false,"s":"VVV","bi":10,"bi":1,"bd":20.0,"bd":2.0}""".getBytes("UTF-8"))
      }.getMessage.contains("expected '\"', offset: 0x00000005"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, StandardTypes("VVV", BigInt("1"), BigDecimal("2")),
          s"""{"s":"XXX","s":"VVV","bi":false,"bi":1,"bd":20.0,"bd":2.0}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x0000001a"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, StandardTypes("VVV", BigInt("1"), BigDecimal("2")),
          s"""{"s":"XXX","s":"VVV","bi":10,"bi":1,"bd":false,"bd":2.0}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x00000029"))
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
      implicit val codecForEither = new JsonValueCodec[Either[String, StandardTypes]] {
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
      implicit val codecForLevel = new JsonKeyCodec[Level] {
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
      val codecOfTuple2 = make[(String, Int)](CodecMakerConfig())
      verifySerDeser(codecOfTuple2, ("VVV", 1), "[\"VVV\",1]".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level numeric tuples" in {
      val codecOfStringifiedTuple = make[(Long, Float, BigDecimal)](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfStringifiedTuple, (1L, 2.2f, BigDecimal(3.3)), "[\"1\",\"2.2\",\"3.3\"]".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with arrays" in {
      val json = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}""".getBytes("UTF-8")
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = readFromArray(json)(codecOfArrays)
      parsedObj.aa.deep shouldBe arrays.aa.deep
      parsedObj.a.deep shouldBe arrays.a.deep
    }
    "serialize and deserialize top-level arrays" in {
      val json = """[[1,2,3],[4,5,6]]""".getBytes("UTF-8")
      val arrayOfArray = Array(Array(1, 2, 3), Array(4, 5, 6))
      val codecOfArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig())
      verifySer(codecOfArrayOfArray, arrayOfArray, json)
      val parsedObj = readFromArray(json)(codecOfArrayOfArray)
      parsedObj.deep shouldBe arrayOfArray.deep
    }
    "serialize and deserialize stringified top-level arrays" in {
      val json = """[["1","2","3"],["4","5","6"]]""".getBytes("UTF-8")
      val arrayOfArray = Array(Array(1, 2, 3), Array(4, 5, 6))
      val codecOfStringifiedArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig(isStringified = true))
      verifySer(codecOfStringifiedArrayOfArray, arrayOfArray, json)
      val parsedObj = readFromArray(json)(codecOfStringifiedArrayOfArray)
      parsedObj.deep shouldBe arrayOfArray.deep
    }
    "do not serialize fields of case classes with empty arrays" in {
      val json = """{"aa":[[],[]]}""".getBytes("UTF-8")
      val arrays = Arrays(Array(Array(), Array()), Array())
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = readFromArray(json)(codecOfArrays)
      parsedObj.aa.deep shouldBe arrays.aa.deep
      parsedObj.a.deep shouldBe arrays.a.deep
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
    "serialize and deserialize case classes with mutable traversables" in {
      verifySerDeser(make[MutableTraversables](CodecMakerConfig()),
        MutableTraversables(mutable.MutableList(mutable.SortedSet("1", "2", "3")),
          mutable.ArrayBuffer(mutable.Set[BigInt](4), mutable.Set.empty[BigInt]),
          mutable.ArraySeq(mutable.LinkedHashSet(5, 6), mutable.LinkedHashSet.empty[Int]),
          mutable.Buffer(mutable.HashSet(7.7, 8.8)),
          mutable.ListBuffer(mutable.TreeSet(9L, 10L)),
          mutable.IndexedSeq(mutable.ArrayStack(11.11f, 12.12f)),
          mutable.UnrolledBuffer(mutable.Traversable(13.toShort, 14.toShort)),
          mutable.LinearSeq(15.toByte, 16.toByte),
          mutable.ResizableArray(mutable.Seq(17.17, 18.18))),
        """{"ml":[["1","2","3"]],"ab":[[4],[]],"as":[[5,6],[]],"b":[[8.8,7.7]],"lb":[[9,10]],"is":[[11.11,12.12]],"ub":[[13,14]],"ls":[15,16],"ra":[[17.17,18.18]]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with immutable traversables" in {
      verifySerDeser(codecOfImmutableTraversables,
        ImmutableTraversables(List(ListSet("1")), Queue(Set[BigInt](4, 5, 6)),
          IndexedSeq(SortedSet(7, 8), SortedSet()), Stream(TreeSet(9.9)), Vector(Traversable(10L, 11L))),
        """{"l":[["1"]],"q":[[4,5,6]],"is":[[7,8],[]],"s":[[9.9]],"v":[[10,11]]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level traversables" in {
      val codecOfImmutableTraversables = make[mutable.Set[List[BigDecimal]]](CodecMakerConfig())
      verifySerDeser(codecOfImmutableTraversables,
        mutable.Set(List[BigDecimal](1.1, 2.2), List[BigDecimal](3.3)),
        """[[3.3],[1.1,2.2]]""".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level traversables" in {
      val codecOfImmutableTraversables = make[mutable.Set[List[BigDecimal]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfImmutableTraversables,
        mutable.Set(List[BigDecimal](1.1, 2.2), List[BigDecimal](3.3)),
        """[["3.3"],["1.1","2.2"]]""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with mutable maps" in {
      verifySerDeser(codecOfMutableMaps,
        MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
          mutable.Map(1.1f -> mutable.ListMap(BigInt(2) -> "2")),
          mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
        """{"hm":{"true":{"1.1":1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with immutable maps" in {
      verifySerDeser(make[ImmutableMaps](CodecMakerConfig()),
        ImmutableMaps(Map(1 -> 1.1), HashMap("2" -> ListMap('V' -> 2), "3" -> ListMap('X' -> 3)),
          SortedMap(4L -> TreeMap(4.toByte -> 4.4f), 5L -> TreeMap.empty[Byte, Float])),
        """{"m":{"1":1.1},"hm":{"2":{"V":2},"3":{"X":3}},"sm":{"4":{"4":4.4},"5":{}}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level maps" in {
      val codecOfMaps = make[mutable.LinkedHashMap[Int, Map[Char, Boolean]]](CodecMakerConfig())
      verifySerDeser(codecOfMaps,
        mutable.LinkedHashMap(1 -> Map('V' -> true, 'X' -> false), 2 -> Map.empty[Char, Boolean]),
        """{"1":{"V":true,"X":false},"2":{}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level maps" in {
      val codecOfMaps = make[mutable.LinkedHashMap[Int, Map[Char, Boolean]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfMaps,
        mutable.LinkedHashMap(1 -> Map('V' -> true, 'X' -> false), 2 -> Map.empty[Char, Boolean]),
        """{"1":{"V":"true","X":"false"},"2":{}}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of JSON object is not properly started/closed or with leading/trailing comma" in {
      val immutableMaps = ImmutableMaps(Map(1 -> 1.1), HashMap.empty, SortedMap.empty)
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
          MutableMaps(null, mutable.Map(1.1f -> mutable.ListMap(null.asInstanceOf[BigInt] -> "2")), null),
          """{"m":{"1.1":{"null":"2"}}""".getBytes("UTF-8"))
      }.getMessage.contains("illegal number, offset: 0x0000000f"))
    }
    "serialize and deserialize case classes with mutable long maps" in {
      verifySerDeser(make[MutableLongMaps](CodecMakerConfig()),
        MutableLongMaps(mutable.LongMap(1L -> 1.1), mutable.LongMap(3L -> "33")),
        """{"lm1":{"1":1.1},"lm2":{"3":"33"}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with immutable int and long maps" in {
      verifySerDeser(make[ImmutableIntLongMaps](CodecMakerConfig()),
        ImmutableIntLongMaps(IntMap(1 -> 1.1, 2 -> 2.2), LongMap(3L -> "33")),
        """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with mutable & immutable bitsets" in {
      verifySerDeser(make[BitSets](CodecMakerConfig()), BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6)),
        """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets = make[mutable.LongMap[IntMap[mutable.BitSet]]](CodecMakerConfig())
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        mutable.LongMap(1L -> IntMap(2 -> mutable.BitSet(4, 5, 6), 3 -> mutable.BitSet.empty)),
        """{"1":{"2":[4,5,6],"3":[]}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize stringified top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets =
        make[mutable.LongMap[IntMap[mutable.BitSet]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        mutable.LongMap(1L -> IntMap(2 -> mutable.BitSet(4, 5, 6), 3 -> mutable.BitSet.empty)),
        """{"1":{"2":["4","5","6"],"3":[]}}""".getBytes("UTF-8"))
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
          |    "S": -90.0,
          |    "N": 90.0,
          |    "W": -180.0,
          |    "E": 180.0
          |  }
          |}""".stripMargin.getBytes("UTF-8"),
        WriterConfig(indentionStep = 2))
    }
    "deserialize JSON with tabs & line returns" in {
      verifyDeser(codecOfIndented, indented,
        "{\r\t\"s\":\t\"VVV\",\r\t\"bd\":\t1.1,\r\t\"l\":\t[\r\t\t1,\r\t\t2,\r\t\t3\r\t],\r\t\"m\":\t{\r\t\t\"S\":\t-90.0,\r\t\t\"N\":\t90.0,\r\t\t\"W\":\t-180.0,\r\t\t\"E\":\t180.0\r\t}\r}".getBytes("UTF-8"))
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
      verifySerDeser(make[Operators](CodecMakerConfig()), Operators(7), """{"=<>!#%^&|*/\\~+-:$":7}""".getBytes("UTF-8"))
    }
    "don't serialize default values of case classes that defined for fields" in {
      verifySer(codecOfDefaults, defaults, "{}".getBytes("UTF-8"))
      verifySer(codecOfDefaults, defaults.copy(oc = None, l = Nil), """{}""".getBytes("UTF-8"))
    }
    "deserialize default values in case of missing field or null/empty values" in {
      verifyDeser(codecOfDefaults, defaults, """{}""".getBytes("UTF-8"))
      verifyDeser(codecOfDefaults, defaults, """{"s":null,"bi":null,"l":null,"oc":null,"e":null}""".getBytes("UTF-8"))
      verifyDeser(codecOfDefaults, defaults, """{"l":[]}""".getBytes("UTF-8"))
    }
    "don't serialize and deserialize transient and non constructor defined fields of case classes" in {
      verifySerDeser(make[Transient](CodecMakerConfig()), Transient(required = "VVV"), """{"required":"VVV"}""".getBytes("UTF-8"))
    }
    "don't serialize case class fields with 'None' values" in {
      verifySer(make[NullAndNoneValues](CodecMakerConfig()), NullAndNoneValues(None), """{}""".getBytes("UTF-8"))
      verifySer(make[List[NullAndNoneValues]](CodecMakerConfig()), List(NullAndNoneValues(None)), """[{}]""".getBytes("UTF-8"))
    }
    "don't serialize case class fields with empty collections" in {
      verifySer(make[EmptyTraversables](CodecMakerConfig()), EmptyTraversables(List(), Set(), List()), """{}""".getBytes("UTF-8"))
    }
    "throw parse exception in case of unknown case class fields" in {
      verifyDeser(make[Unknown](CodecMakerConfig()), Unknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes("UTF-8"))
    }
    "throw parse exception for unknown case class fields if skipping of them wasn't allowed in materialize call" in {
      assert(intercept[JsonParseException] {
        verifyDeser(make[Unknown](CodecMakerConfig(skipUnexpectedFields = false)), Unknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes("UTF-8"))
      }.getMessage.contains("unexpected field \"x\", offset: 0x00000004"))
    }
    "throw parse exception in case of missing required case class fields detected during deserialization" in {
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
        List(A(1), B("VVV"), C(1, "VVV"), D),
        """[{"type":"A","a":1},{"type":"B","a":"VVV"},{"type":"C","a":1,"b":"VVV"},{"type":"D"}]""".getBytes("UTF-8"))
      val longStr = new String(Array.fill(100000)('W'))
      verifyDeser(codecOfADTList,
        List(C(2, longStr), C(1, "VVV")),
        s"""[{"a":2,"b":"$longStr","type":"C"},{"a":1,"type":"C","b":"VVV"}]""".getBytes("UTF-8"))
      verifySerDeser(make[List[AdtBase]](CodecMakerConfig(discriminatorFieldName = "t")),
        List(C(2, "WWW"), C(1, "VVV")),
        s"""[{"t":"C","a":2,"b":"WWW"},{"t":"C","a":1,"b":"VVV"}]""".getBytes("UTF-8"))
    }
    "serialize and deserialize ADTs using non-ASCII discriminator field & value w/ reusage of case classes w/o ADTs" in {
      verifySerDeser(make[List[База]](CodecMakerConfig(discriminatorFieldName = "тип", skipUnexpectedFields = false)),
        List(А(Б("x")), Б("x")),
        """[{"тип":"А","б":{"с":"x"}},{"тип":"Б","с":"x"}]""".getBytes("UTF-8"))
    }
    "serialize and deserialize ADTs with Scala operators in names" in {
      verifySerDeser(make[List[TimeZone]](CodecMakerConfig(discriminatorFieldName = "zoneId")),
        List(`US/Alaska`, `Europe/Paris`),
        """[{"zoneId":"US/Alaska"},{"zoneId":"Europe/Paris"}]""".getBytes("UTF-8"))
    }
    "throw parse exception in case of missing discriminator field or illegal value of discriminator field" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(A(1)), """[{"a":1}]""".getBytes("UTF-8"))
      }.getMessage.contains("""missing required field "type", offset: 0x00000007"""))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(A(1)), """[{"a":1,"type":"AAA"}]""".getBytes("UTF-8"))
      }.getMessage.contains("""illegal value of discriminator field "type", offset: 0x00000013"""))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(A(1)), """[{"a":1,"type":123}]""".getBytes("UTF-8"))
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
    "don't generate codec for non case classes as ADT leaf classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |class A(i: Int) extends X
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Only case classes & case objects are supported for ADT leaf classes. Please consider using
          |of them for ADT with base 'X' or using a custom implicitly accessible codec for the ADT base."""
          .stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |object A extends X
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Only case classes & case objects are supported for ADT leaf classes. Please consider using
          |of them for ADT with base 'X' or using a custom implicitly accessible codec for the ADT base."""
          .stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codec for ADT base without leaf classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please consider adding them or using a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed abstract class X
          |JsonCodecMaker.make[X](CodecMakerConfig())""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please consider adding them or using a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codec for case objects which are mapped to the same discriminator value" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
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
        """sealed trait DuplicatedJsonName
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
          """"p":"P1Y2M25D","y":2007,"ym":"2007-12","zdt":"2007-12-03T10:15:30+01:00[Europe/Paris]",""" +
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
      verifySerDeser(make[Year](CodecMakerConfig()), Year.parse("2007"), "2007".getBytes("UTF-8"))
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
      val codecOfYear = make[Year](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfYear, Year.of(2008), "\"2008\"".getBytes("UTF-8"))
    }
    "serialize and deserialize case class with aliased typed methods" in {
      verifySerDeser(make[TypeAliases](CodecMakerConfig()), TypeAliases(1, "VVV", List(1, 2, 3), Map(1 -> "VVV")),
        """{"i":1,"s":"VVV","l":[1,2,3],"m":{"1":"VVV"}}""".getBytes("UTF-8"))
    }
    "serialize and deserialize collection with aliased type arguments" in {
      verifySerDeser(make[Map[I, S]](CodecMakerConfig()), Map(1 -> "VVV"), "{\"1\":\"VVV\"}".getBytes("UTF-8"))
    }
    "serialize and deserialize top-level aliased types" in {
      verifySerDeser(make[L](CodecMakerConfig()), List(1, 2, 3), "[1,2,3]".getBytes("UTF-8"))
    }
    "serialize and deserialize case classes with private primary constructor if it can be accessed" in {
      verifySerDeser(PrivatePrimaryConstructor.codec, PrivatePrimaryConstructor("1"), "{\"i\":1}".getBytes("UTF-8"))
    }
    "don't generate codecs for unsupported classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """JsonCodecMaker.make[java.util.Date](CodecMakerConfig())"""
      }).getMessage.contains {
        """No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_]' defined for 'java.util.Date'."""
      })
    }
    "don't generate codecs for too deeply defined case classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """val codecOfFoo = () => {
          |  case class Foo(i: Int)
          |  JsonCodecMaker.make[Foo](CodecMakerConfig())
          |}
          |codecOfFoo()""".stripMargin
      }).getMessage.contains {
        """Can't find companion object for 'Foo'. This can happen when it's nested too deeply. Please consider defining
          |it as a top-level object or directly inside of another class or object.""".stripMargin.replace('\n', ' ')
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
  }
  "JsonCodecMaker.enforceCamelCase" should {
    "transform snake_case names to camelCase" in {
      JsonCodecMaker.enforceCamelCase("o_o") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("o_ooo_") shouldBe "oOoo"
      JsonCodecMaker.enforceCamelCase("OO_OOO_111") shouldBe "ooOoo111"
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
