package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Objects, UUID}

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException

import scala.annotation.{switch, tailrec}
import scala.util.hashing.MurmurHash3

case class UserId(id: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

case class Id[A](id: A) extends AnyVal

sealed trait Weapon extends Product with Serializable
object Weapon {
  final case object Axe extends Weapon
  final case object Sword extends Weapon
}

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

case class BoxedPrimitives(
  b: java.lang.Byte,
  s: java.lang.Short,
  i: java.lang.Integer,
  l: java.lang.Long,
  bl: java.lang.Boolean,
  ch: java.lang.Character,
  dbl: java.lang.Double,
  f: java.lang.Float)

case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

class NonCaseClass(val id: Int, var name: String) {
  override def hashCode(): Int = id * 31 + Objects.hashCode(name)

  override def equals(obj: Any): Boolean = obj match {
    case c: NonCaseClass => id == c.id && Objects.equals(name, c.name)
    case _ => false
  }

  override def toString: String = s"NonCaseClass(id=$id,name=$name)"
}

case class JavaTypes(uuid: UUID)

object LocationType extends Enumeration {
  type LocationType = Value

  val IP, GPS: LocationType = Value

  def extra(name: String): LocationType = Value(nextId, name)
}

case class Enums(lt: LocationType.LocationType)

case class JavaEnums(l: Level, il: Levels.InnerLevel)

case class OuterTypes(s: String, st: Either[String, StandardTypes] = Left("error"))

case class Options(os: Option[String], obi: Option[BigInt], osi: Option[Set[Int]], ol: Option[Long], ojl: Option[java.lang.Long])

case class Tuples(t1: (Int, Double, List[Char]), t2: (String, BigInt, Option[LocationType.LocationType]))

case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

case class MutableMaps(
  hm: collection.mutable.HashMap[Boolean, collection.mutable.AnyRefMap[BigDecimal, Int]],
  m: collection.mutable.Map[Float, collection.mutable.ListMap[BigInt, String]],
  ohm: collection.mutable.OpenHashMap[Double, collection.mutable.LinkedHashMap[Short, Double]])

case class ImmutableMaps(
  m: Map[Int, Double],
  hm: collection.immutable.HashMap[String, collection.immutable.ListMap[Char, BigInt]],
  sm: collection.immutable.SortedMap[Long, collection.immutable.TreeMap[Byte, Float]])

case class EmptyMaps(im: collection.immutable.Map[String, Int], mm: collection.mutable.Map[Long, Int])

case class GenericIterables(
  s: collection.Set[collection.SortedSet[String]],
  is: collection.IndexedSeq[collection.Seq[Float]],
  i: collection.Iterable[Int])

case class MutableIterables(
  ml: collection.mutable.Seq[collection.mutable.SortedSet[String]],
  ab: collection.mutable.ArrayBuffer[collection.mutable.Set[BigInt]],
  as: collection.mutable.ArraySeq[collection.mutable.LinkedHashSet[Int]],
  b: collection.mutable.Buffer[collection.mutable.HashSet[Double]],
  lb: collection.mutable.ListBuffer[collection.mutable.TreeSet[Long]],
  is: collection.mutable.IndexedSeq[collection.mutable.ArrayStack[Float]],
  ub: collection.mutable.UnrolledBuffer[collection.mutable.Iterable[Short]])

case class ImmutableIterables(
  l: List[collection.immutable.ListSet[String]],
  q: collection.immutable.Queue[Set[BigInt]],
  is: IndexedSeq[collection.immutable.SortedSet[Int]],
  s: Stream[collection.immutable.TreeSet[Double]],
  v: Vector[Iterable[Long]])

case class EmptyIterables(l: List[String], a: collection.mutable.ArrayBuffer[Int])

case class MutableLongMaps(lm1: collection.mutable.LongMap[Double], lm2: collection.mutable.LongMap[String])

case class ImmutableIntLongMaps(im: collection.immutable.IntMap[Double], lm: collection.immutable.LongMap[String])

case class BitSets(bs: collection.BitSet, ibs: collection.immutable.BitSet, mbs: collection.mutable.BitSet)

case class CamelPascalSnakeKebabCases(
  camelCase: Int,
  PascalCase: Int,
  snake_case: Int,
  `kebab-case`: Int,
  camel1: Int,
  Pascal1: Int,
  snake_1: Int,
  `kebab-1`: Int)

case class Recursive(s: String, bd: BigDecimal, l: List[Int], m: Map[Char, Recursive])

case class UTF8KeysAndValues(გასაღები: String)

case class Stringified(@stringified i: Int, @stringified bi: BigInt, @stringified l1: List[Int], l2: List[Int])

case class Defaults(
  st: String = "VVV",
  i: Int = 1,
  bi: BigInt = -1,
  oc: Option[Char] = Some('X'),
  l: List[Int] = collection.immutable.List(0),
  e: Level = Level.HIGH,
  a: Array[Array[Int]] = Array(Array(1, 2), Array(3, 4)),
  ab: collection.mutable.ArrayBuffer[Int] = collection.mutable.ArrayBuffer(1, 2),
  m: Map[Int, Boolean] = Map(1 -> true),
  mm: collection.mutable.Map[String, Int] = collection.mutable.Map("VVV" -> 1),
  im: collection.immutable.IntMap[String] = collection.immutable.IntMap(1 -> "VVV"),
  lm: collection.mutable.LongMap[Int] = collection.mutable.LongMap(1L -> 2),
  s: Set[String] = Set("VVV"),
  ms: collection.mutable.Set[Int] = collection.mutable.Set(1),
  bs: collection.immutable.BitSet = collection.immutable.BitSet(1),
  mbs: collection.mutable.BitSet = scala.collection.mutable.BitSet(1))

case class Defaults2(
  st: String = "VVV",
  i: Int = 1,
  bi: BigInt = -1,
  oc: Option[Char] = Some('X'),
  l: List[Int] = collection.immutable.List(0),
  e: Level = Level.HIGH,
  ab: collection.mutable.ArrayBuffer[Int] = collection.mutable.ArrayBuffer(1, 2),
  m: Map[Int, Boolean] = Map(1 -> true),
  mm: collection.mutable.Map[String, Int] = collection.mutable.Map("VVV" -> 1),
  im: collection.immutable.IntMap[String] = collection.immutable.IntMap(1 -> "VVV"),
  lm: collection.mutable.LongMap[Int] = collection.mutable.LongMap(1L -> 2),
  s: Set[String] = Set("VVV"),
  ms: collection.mutable.Set[Int] = collection.mutable.Set(1),
  bs: collection.immutable.BitSet = collection.immutable.BitSet(1),
  mbs: collection.mutable.BitSet = collection.mutable.BitSet(1))

case class PolymorphicDefaults[A, B](i: A = 1, cs: List[B] = Nil)

case class JavaTimeTypes(
  dow: DayOfWeek,
  d: Duration,
  i: Instant,
  ld: LocalDate,
  ldt: LocalDateTime,
  lt: LocalTime,
  m: Month,
  md: MonthDay,
  odt: OffsetDateTime,
  ot: OffsetTime,
  p: Period,
  y: Year,
  ym: YearMonth,
  zdt: ZonedDateTime,
  zi: ZoneId,
  zo: ZoneOffset)

sealed trait AdtBase extends Product with Serializable

sealed abstract class Inner extends AdtBase {
  def a: Int
}

case class AAA(a: Int) extends Inner

case class BBB(a: BigInt) extends AdtBase

case class CCC(a: Int, b: String) extends Inner

case object DDD extends AdtBase

class JsonCodecMakerSpec extends VerifyingSpec {
  import NamespacePollutions._

  val codecOfPrimitives: JsonValueCodec[Primitives] = make
  val codecOfStandardTypes: JsonValueCodec[StandardTypes] = make
  val codecOfJavaTypes: JsonValueCodec[JavaTypes] = make
  val codecOfEnums: JsonValueCodec[Enums] = make
  val codecOfJavaEnums: JsonValueCodec[JavaEnums] = make
  val codecOfOptions: JsonValueCodec[Options] = make
  val codecOfTuples: JsonValueCodec[Tuples] = make
  val codecOfArrays: JsonValueCodec[Arrays] = make
  val codecOfMutableMaps: JsonValueCodec[MutableMaps] = make
  val codecOfImmutableMaps: JsonValueCodec[ImmutableMaps] = make
  val codecOfNameOverridden: JsonValueCodec[NameOverridden] = make
  val codecOfRecursive: JsonValueCodec[Recursive] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
  val codecOfUTF8KeysAndValues: JsonValueCodec[UTF8KeysAndValues] = make
  val codecOfStringified: JsonValueCodec[Stringified] = make
  val codecOfADTList: JsonValueCodec[List[AdtBase]] = make(CodecMakerConfig.withBigIntDigitsLimit(20001))
  val codecOfADTList2: JsonValueCodec[List[AdtBase]] = make(CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None))

  "JsonCodecMaker.make generate codes which" should {
    "serialize and deserialize case classes with primitives" in {
      verifySerDeser(codecOfPrimitives, Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""")
    }
    "serialize and deserialize top-level primitives" in {
      verifySerDeser(make[_root_.scala.Byte], 1.toByte, "1")
      verifySerDeser(make[_root_.scala.Short], 2.toShort, "2")
      verifySerDeser(make[Int], 3, "3")
      verifySerDeser(make[Long], 4L, "4")
      verifySerDeser(make[_root_.scala.Boolean], true, "true")
      verifySerDeser(make[Char], 'V', "\"V\"")
      verifySerDeser(make[Double], 1.1, "1.1")
      verifySerDeser(make[Float], 2.2f, "2.2")
    }
    "serialize and deserialize stringified top-level primitives" in {
      verifySerDeser(make[_root_.scala.Byte](CodecMakerConfig.withIsStringified(true)), 1.toByte, "\"1\"")
      verifySerDeser(make[_root_.scala.Short](CodecMakerConfig.withIsStringified(true)), 2.toShort, "\"2\"")
      verifySerDeser(make[Int](CodecMakerConfig.withIsStringified(true)), 3, "\"3\"")
      verifySerDeser(make[Long](CodecMakerConfig.withIsStringified(true)), 4L, "\"4\"")
      verifySerDeser(make[_root_.scala.Boolean](CodecMakerConfig.withIsStringified(true)), true, "\"true\"")
      verifySerDeser(make[Double](CodecMakerConfig.withIsStringified(true)), 1.1, "\"1.1\"")
      verifySerDeser(make[Float](CodecMakerConfig.withIsStringified(true)), 2.2f, "\"2.2\"")
    }
    "throw parse exception with hex dump in case of illegal input" in {
      verifyDeserError(codecOfPrimitives,
        """{"b":-128,"s":-32768,"i":-2147483648,"l":-9223372036854775808,'bl':true,"ch":"V","dbl":-123456789.0,"f":-12345.0}""",
        """expected '"', offset: 0x0000003e, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000010 | 32 37 36 38 2c 22 69 22 3a 2d 32 31 34 37 34 38 | 2768,"i":-214748 |
          || 00000020 | 33 36 34 38 2c 22 6c 22 3a 2d 39 32 32 33 33 37 | 3648,"l":-922337 |
          || 00000030 | 32 30 33 36 38 35 34 37 37 35 38 30 38 2c 27 62 | 2036854775808,'b |
          || 00000040 | 6c 27 3a 74 72 75 65 2c 22 63 68 22 3a 22 56 22 | l':true,"ch":"V" |
          || 00000050 | 2c 22 64 62 6c 22 3a 2d 31 32 33 34 35 36 37 38 | ,"dbl":-12345678 |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw parse exception in case of unexpected end of input is detected" in {
      verifyDeserError(codecOfPrimitives,
        """{"b":-128,"s":-32768,"i":-2147483648,"l":-9223372036854775808,"bl":true,"ch":"V","dbl":-123456789.0,"f":""",
        """unexpected end of input, offset: 0x00000068""".stripMargin)
    }
    "serialize and deserialize case classes with boxed primitives" in {
      verifySerDeser(make[BoxedPrimitives],
        BoxedPrimitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""")
    }
    "serialize and deserialize top-level boxed primitives" in {
      verifySerDeser(make[_root_.java.lang.Byte], _root_.java.lang.Byte.valueOf(1.toByte), "1")
      verifySerDeser(make[_root_.java.lang.Short], _root_.java.lang.Short.valueOf(2.toShort), "2")
      verifySerDeser(make[_root_.java.lang.Integer], _root_.java.lang.Integer.valueOf(3), "3")
      verifySerDeser(make[_root_.java.lang.Long], _root_.java.lang.Long.valueOf(4L), "4")
      verifySerDeser(make[_root_.java.lang.Boolean], _root_.java.lang.Boolean.valueOf(true), "true")
      verifySerDeser(make[_root_.java.lang.Character], _root_.java.lang.Character.valueOf('V'), "\"V\"")
      verifySerDeser(make[_root_.java.lang.Double], _root_.java.lang.Double.valueOf(1.1), "1.1")
      verifySerDeser(make[_root_.java.lang.Float], _root_.java.lang.Float.valueOf(2.2f), "2.2")
    }
    "serialize and deserialize stringifeid top-level boxed primitives" in {
      verifySerDeser(make[_root_.java.lang.Byte](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Byte.valueOf(1.toByte), "\"1\"")
      verifySerDeser(make[_root_.java.lang.Short](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Short.valueOf(2.toShort), "\"2\"")
      verifySerDeser(make[_root_.java.lang.Integer](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Integer.valueOf(3), "\"3\"")
      verifySerDeser(make[_root_.java.lang.Long](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Long.valueOf(4L), "\"4\"")
      verifySerDeser(make[_root_.java.lang.Boolean](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Boolean.valueOf(true), "\"true\"")
      verifySerDeser(make[_root_.java.lang.Double](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Double.valueOf(1.1), "\"1.1\"")
      verifySerDeser(make[_root_.java.lang.Float](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Float.valueOf(2.2f), "\"2.2\"")
    }
    "serialize and deserialize case classes with standard types" in {
      val text = "text" * 100000
      val number = "1234567890"
      verifySerDeser(codecOfStandardTypes,
        StandardTypes(text, BigInt(number), BigDecimal(s"$number.$number")),
        s"""{"s":"$text","bi":$number,"bd":$number.$number}""")
    }
    "throw parse exception in case of illegal value for case classes" in {
      verifyDeserError(codecOfStandardTypes, "null", "expected '{', offset: 0x00000000")
    }
    "serialize and deserialize top-level standard types" in {
      val text =
        "JavaScript Object Notation (JSON) is a lightweight, text-based, language-independent data interchange format."
      verifySerDeser(make[String], text, s""""$text"""")
      verifySerDeser(make[BigInt], BigInt("123456789012345678901234567890"),
        "123456789012345678901234567890")
      verifySerDeser(make[BigDecimal], BigDecimal("1234567890.12345678901234567890"),
        "1234567890.12345678901234567890")
    }
    "serialize and deserialize stringified top-level standard types" in {
      verifySerDeser(make[BigInt](CodecMakerConfig.withIsStringified(true)),
        BigInt("123456789012345678901234567890"), "\"123456789012345678901234567890\"")
      verifySerDeser(make[BigDecimal](CodecMakerConfig.withIsStringified(true)),
        BigDecimal("1234567890.12345678901234567890"), "\"1234567890.12345678901234567890\"")
    }
    "throw parse exception in case of duplicated key for case classe was detected" in {
      verifyDeserError(codecOfStandardTypes, s"""{"s":"XXX","s":"VVV","bi":10,"bi":1,"bd":20.0,"bd":2.0}""",
        "duplicated field \"s\", offset: 0x0000000e")
    }
    "throw parse exception in case of illegal UTF-8 encoded field names" in {
      val jsonBytes = """{"s":"VVV","bi":1,"bd":1.1}""".getBytes(UTF_8)
      jsonBytes(2) = 0xF0.toByte
      verifyDeserError(codecOfStandardTypes, jsonBytes, "malformed byte(s): 0xf0, 0x22, 0x3a, 0x22, offset: 0x00000005")
    }
    "throw parse exception in case of illegal JSON escaped field names" in {
      verifyDeserError(codecOfStandardTypes, "{\"\\udd1e\":\"VVV\",\"bi\":1,\"bd\":1.1}",
        "illegal escape sequence, offset: 0x00000008")
    }
    "throw parse exception in case of missing or illegal tokens" in {
      verifyDeserError(codecOfStandardTypes, """"s":"VVV","bi":1,"bd":1.1}""", "expected '{', offset: 0x00000000")
      verifyDeserError(codecOfStandardTypes, """{"s""VVV","bi":1,"bd":1.1}""", "expected ':', offset: 0x00000004")
      verifyDeserError(codecOfStandardTypes, """{"s":"VVV""bi":1"bd":1.1}""", "expected '}' or ',', offset: 0x0000000a")
      verifyDeserError(codecOfStandardTypes, """["s":"VVV","bi":1,"bd":2}""", "expected '{', offset: 0x00000000")
      verifyDeserError(codecOfStandardTypes, """{,"s":"VVV","bi":1,"bd":2}""", "expected '\"', offset: 0x00000001")
      verifyDeserError(codecOfStandardTypes, """{"s":"VVV","bi":1,"bd":2]""", "expected '}' or ',', offset: 0x00000018")
      verifyDeserError(codecOfStandardTypes, """{"s":"VVV","bi":1,"bd":2,}""", "expected '\"', offset: 0x00000019")
    }
    "serialize and deserialize Scala classes which has a primary constructor with 'var' or 'var' parameters only" in {
      verifySerDeser(make[NonCaseClass], new NonCaseClass(1, "VVV"), """{"id":1,"name":"VVV"}""")
    }
    "serialize and deserialize Java types" in {
      verifySerDeser(codecOfJavaTypes, JavaTypes(new UUID(0, 0)), """{"uuid":"00000000-0000-0000-0000-000000000000"}""")
    }
    "throw parse exception in case of illegal value of java types" in {
      verifyDeserError(codecOfJavaTypes,  """{"uuid":"00000000-XXXX-0000-0000-000000000000"}""",
        "expected hex digit, offset: 0x00000012")
    }
    "serialize and deserialize top-level java types" in {
      verifySerDeser(make[UUID], new UUID(0, 0), "\"00000000-0000-0000-0000-000000000000\"")
    }
    "serialize and deserialize Java types as key in maps" in {
      verifySerDeser(make[Map[UUID, Int]], Map(new UUID(0, 0) -> 0),
        """{"00000000-0000-0000-0000-000000000000":0}""")
    }
    "serialize and deserialize enumerations" in {
      verifySerDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":"GPS"}""")
      verifySerDeser(codecOfEnums, Enums(LocationType.extra("Galileo")), """{"lt":"Galileo"}""")
    }
    "throw parse exception in case of illegal value of enumeration" in {
      verifyDeserError(codecOfEnums, """{"lt":null}""", "expected '\"', offset: 0x00000006")
      verifyDeserError(codecOfEnums, """{"lt":"GLONASS"}""", "illegal enum value \"GLONASS\", offset: 0x0000000e")
    }
    "serialize and deserialize top-level enumerations" in {
      verifySerDeser(make[LocationType.LocationType], LocationType.GPS, "\"GPS\"")
    }
    "serialize and deserialize enumerations as key in maps" in {
      verifySerDeser(make[Map[LocationType.LocationType, Int]], Map(LocationType.GPS -> 0),
        """{"GPS":0}""")
    }
    "serialize and deserialize Java enumerations" in {
      verifySerDeser(codecOfJavaEnums, JavaEnums(Level.LOW, Levels.InnerLevel.HIGH), """{"l":"LOW","il":"HIGH"}""")
    }
    "serialize and deserialize Java enumerations with renamed value" in {
      verifySerDeser(make[JavaEnums](CodecMakerConfig.withJavaEnumValueNameMapper(JsonCodecMaker.enforce_snake_case)),
        JavaEnums(Level.LOW, Levels.InnerLevel.HIGH), """{"l":"low","il":"high"}""")
      verifySerDeser(make[JavaEnums](CodecMakerConfig.withJavaEnumValueNameMapper(JsonCodecMaker.enforce_snake_case
        .andThen(JsonCodecMaker.EnforcePascalCase))),
        JavaEnums(Level.LOW, Levels.InnerLevel.HIGH), """{"l":"Low","il":"High"}""")
      verifySerDeser(make[JavaEnums](CodecMakerConfig.withJavaEnumValueNameMapper {
        case "LOW" => "lo"
        case "HIGH" => "hi"
      }), JavaEnums(Level.LOW, Levels.InnerLevel.HIGH), """{"l":"lo","il":"hi"}""")
    }
    "don't generate codecs for Java enumerations when duplicated transformed names detected" in {
      assert(intercept[TestFailedException](assertCompiles {
        """make[JavaEnums](CodecMakerConfig.withJavaEnumValueNameMapper { case _ => "dup" })"""
      }).getMessage.contains {
        """Duplicated JSON value(s) defined for 'com.github.plokhotnyuk.jsoniter_scala.macros.Levels.InnerLevel': 'dup'.
          |Values are derived from value names of the enum that are mapped by the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.javaEnumValueNameMapper' function.
          |Result values should be unique per enum class.""".stripMargin.replace('\n', ' ')
      })
    }
    "throw parse exception in case of illegal value of Java enumeration" in {
      verifyDeserError(codecOfJavaEnums, """{"l":null,"il":"HIGH"}""", "expected '\"', offset: 0x00000005")
      verifyDeserError(codecOfJavaEnums, """{"l":"LO","il":"HIGH"}""", "illegal enum value \"LO\", offset: 0x00000008")
    }
    "serialize and deserialize top-level Java enumerations" in {
      verifySerDeser(make[Level], Level.HIGH, "\"HIGH\"")
    }
    "serialize and deserialize Java enumerations as key in maps" in {
      verifySerDeser(make[Map[Level, Int]], Map(Level.HIGH -> 0), """{"HIGH":0}""")
    }
    "serialize and deserialize outer types using custom value codecs for primitive types" in {
      implicit val customCodecOfLong: JsonValueCodec[Long] = new JsonValueCodec[Long] {
        val nullValue: Long = 0

        def decodeValue(in: JsonReader, default: Long): Long =
          if (in.isNextToken('"')) {
            in.rollbackToken()
            in.readStringAsLong() // or in.readString().toLong is less efficient and less safe but more universal because can accepts escaped characters
          } else {
            in.rollbackToken()
            in.readLong()
          }

        def encodeValue(x: Long, out: JsonWriter): _root_.scala.Unit =
          if (x > 9007199254740992L || x < -9007199254740992L) out.writeValAsString(x)
          else out.writeVal(x)
      }
      val codecOfLongList = make[List[Long]]
      verifyDeser(codecOfLongList, List(1L, 9007199254740992L, 9007199254740993L),
        "[\"1\",9007199254740992,\"9007199254740993\"]")
      verifySer(codecOfLongList, List(1L, 9007199254740992L, 9007199254740993L),
        "[1,9007199254740992,\"9007199254740993\"]")
      implicit val customCodecOfBoolean: JsonValueCodec[_root_.scala.Boolean] = new JsonValueCodec[_root_.scala.Boolean] {
        val nullValue: _root_.scala.Boolean = false

        def decodeValue(in: JsonReader, default: _root_.scala.Boolean): _root_.scala.Boolean =
          if (in.isNextToken('"')) {
            in.rollbackToken()
            val v = in.readString(null)
            if ("true".equalsIgnoreCase(v)) true
            else if ("false".equalsIgnoreCase(v)) false
            else in.decodeError("illegal boolean")
          } else {
            in.rollbackToken()
            in.readBoolean()
          }

        def encodeValue(x: _root_.scala.Boolean, out: JsonWriter): _root_.scala.Unit =
          out.writeNonEscapedAsciiVal(if (x) "TRUE" else "FALSE")
      }

      case class Flags(f1: _root_.scala.Boolean, f2: _root_.scala.Boolean)

      val codecOfFlags = make[Flags]
      verifyDeser(codecOfFlags, Flags(f1 = true, f2 = false), "{\"f1\":true,\"f2\":\"False\"}")
      verifySer(codecOfFlags, Flags(f1 = true, f2 = false), "{\"f1\":\"TRUE\",\"f2\":\"FALSE\"}")
      verifyDeserError(codecOfFlags, "{\"f1\":\"XALSE\",\"f2\":true}", "illegal boolean, offset: 0x0000000c")
      verifyDeserError(codecOfFlags, "{\"f1\":xalse,\"f2\":true}", "illegal boolean, offset: 0x00000006")
      implicit val customCodecOfDouble: JsonValueCodec[Double] = new JsonValueCodec[Double] {
        val nullValue: Double = 0.0f

        def decodeValue(in: JsonReader, default: Double): Double =
          if (in.isNextToken('"')) {
            in.rollbackToken()
            val len = in.readStringAsCharBuf()
            if (in.isCharBufEqualsTo(len, "NaN")) Double.NaN
            else if (in.isCharBufEqualsTo(len, "Infinity")) Double.PositiveInfinity
            else if (in.isCharBufEqualsTo(len, "-Infinity")) Double.NegativeInfinity
            else in.decodeError("illegal double")
          } else {
            in.rollbackToken()
            in.readDouble()
          }

        def encodeValue(x: Double, out: JsonWriter): _root_.scala.Unit =
          if (_root_.java.lang.Double.isFinite(x)) out.writeVal(x)
          else out.writeNonEscapedAsciiVal {
            if (x != x) "NaN"
            else if (x >= 0) "Infinity"
            else "-Infinity"
          }
      }
      val codecOfDoubleArray = make[Array[Double]]
      verifySerDeser(codecOfDoubleArray, _root_.scala.Array(-1e300 / 1e-300, 1e300 / 1e-300, 0.0, 1.0e10),
        "[\"-Infinity\",\"Infinity\",0.0,1.0E10]")
      verifyDeserError(codecOfDoubleArray, "[\"Inf\",\"-Inf\"]", "illegal double, offset: 0x00000005")
    }
    "serialize and deserialize outer types using custom value codecs for opaque types" in {
      abstract class Foo {
        type Bar
      }

      val foo: Foo = new Foo {
        type Bar = Int
      }

      type Bar = foo.Bar

      case class Baz(bar: Bar)

      implicit val customCodecOfBar: JsonValueCodec[Bar] = new JsonValueCodec[Bar] {
        val nullValue: Bar = null.asInstanceOf[Bar]

        def encodeValue(x: Bar, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.asInstanceOf[Int])

        def decodeValue(in: JsonReader, default: Bar): Bar = in.readInt().asInstanceOf[Bar]
      }
      verifySerDeser(make[Baz], Baz(42.asInstanceOf[Bar]), "{\"bar\":42}")
    }
    "serialize and deserialize outer types using custom value codecs for nested types" in {
      implicit val customCodecOfEither1: JsonValueCodec[Either[String, Int]] =
        new JsonValueCodec[Either[String, Int]] {
          val nullValue: Either[String, Int] = null

          def decodeValue(in: JsonReader, default: Either[String, Int]): Either[String, Int] = {
            val t = in.nextToken()
            in.rollbackToken()
            if (t == '"') Left(in.readString(null))
            else Right(in.readInt())
          }

          def encodeValue(x: Either[String, Int], out: JsonWriter): _root_.scala.Unit = x match {
            case Right(i) => out.writeVal(i)
            case Left(s) => out.writeVal(s)
          }
        }
      val codecOfEitherList = make[List[Either[String, Int]]]
      verifySerDeser(codecOfEitherList, List(Right(1), Left("VVV")), "[1,\"VVV\"]")
      implicit val customCodecOfEither2: JsonValueCodec[Either[String, StandardTypes]] =
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

          def encodeValue(x: Either[String, StandardTypes], out: JsonWriter): _root_.scala.Unit =
            x match {
              case Left(s) => out.writeVal(s)
              case Right(st) => codecOfStandardTypes.encodeValue(st, out)
            }
        }
      val codecOfOuterTypes = make[OuterTypes]
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Right(StandardTypes("VVV", 1, 1.1))),
        """{"s":"X","st":{"s":"VVV","bi":1,"bd":1.1}}""")
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("fatal error")), """{"s":"X","st":"fatal error"}""")
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("error")), """{"s":"X"}""") // st matches with default value
      verifySerDeser(codecOfOuterTypes, OuterTypes("X"), """{"s":"X"}""")
      implicit object codecOfLocationType extends JsonValueCodec[LocationType.LocationType] {
        val nullValue: LocationType.LocationType = null

        def decodeValue(in: JsonReader, default: LocationType.LocationType): LocationType.LocationType = {
          val v = in.readInt()
          try LocationType.apply(v) catch {
            case _: NoSuchElementException => in.decodeError("invalid enum value: " + v)
          }
        }

        def encodeValue(x: LocationType.LocationType, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.id)
      }
      val codecOfEnums = make[Enums]
      verifySerDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":1}""")
      verifyDeserError(codecOfEnums, """{"lt":"GPS"}""", "illegal number, offset: 0x00000006")
    }
    "serialize and deserialize types as a JSON object or a JSON string using custom value codecs" in {
      val customCodecOfStandardTypes: JsonValueCodec[StandardTypes] =
        new JsonValueCodec[StandardTypes] {
          val nullValue: StandardTypes = null

          def decodeValue(in: JsonReader, default: StandardTypes): StandardTypes = (in.nextToken(): @switch) match {
            case '{' =>
              in.rollbackToken()
              codecOfStandardTypes.decodeValue(in, codecOfStandardTypes.nullValue)
            case '"' =>
              in.rollbackToken()
              readFromString(in.readString(null))(codecOfStandardTypes)
            case _ =>
              in.decodeError("expected '{' or '\"'")
          }

          def encodeValue(x: StandardTypes, out: JsonWriter): _root_.scala.Unit = x.s match {
            case "VVV" => codecOfStandardTypes.encodeValue(x, out)
            case "XXX" => out.writeVal(writeToString(x)(codecOfStandardTypes))
          }
        }
      verifySerDeser(customCodecOfStandardTypes, StandardTypes("VVV", 1, 1.1), """{"s":"VVV","bi":1,"bd":1.1}""")
      verifySerDeser(customCodecOfStandardTypes, StandardTypes("XXX", 1, 1.1), """"{\"s\":\"XXX\",\"bi\":1,\"bd\":1.1}"""")
    }
    "serialize and deserialize types using a custom key codec and a custom ordering for map keys" in {
      implicit val codecOfLevel: JsonKeyCodec[Level] = new JsonKeyCodec[Level] {
        override def decodeKey(in: JsonReader): Level = in.readKeyAsInt() match {
          case 0 => Level.LOW
          case 1 => Level.HIGH
          case x => in.enumValueError(x.toString)
        }

        override def encodeKey(x: Level, out: JsonWriter): _root_.scala.Unit = x match {
          case Level.LOW => out.writeKey(0)
          case Level.HIGH => out.writeKey(1)
          case _ => out.encodeError("illegal enum value")
        }
      }
      verifySerDeser(make[Map[Level, Int]], Map(Level.HIGH -> 100), """{"1":100}""")
      implicit val levelOrdering: Ordering[Level] = new Ordering[Level] {
        override def compare(x: Level, y: Level): Int = y.ordinal - x.ordinal
      }
      val codecOfOrderedLevelTreeMap = make[_root_.scala.collection.immutable.TreeMap[Level, Int]]
      verifySerDeser(codecOfOrderedLevelTreeMap,
        _root_.scala.collection.immutable.TreeMap[Level, Int](Level.HIGH -> 100, Level.LOW -> 10), """{"0":10,"1":100}""")
      verifyDeserByCheck(codecOfOrderedLevelTreeMap, """{"0":10,"1":100}""",
        check = (actual: _root_.scala.collection.immutable.TreeMap[Level, Int]) => actual.ordering shouldBe levelOrdering)
    }
    "serialize and deserialize types using a custom value codec and a custom ordering for set values" in {
      implicit val codecOfLevel: JsonValueCodec[Level] = new JsonValueCodec[Level] {
        override def decodeValue(in: JsonReader, default: Level): Level = in.readInt() match {
          case 0 => Level.LOW
          case 1 => Level.HIGH
          case x => in.enumValueError(x.toString)
        }

        override def encodeValue(x: Level, out: JsonWriter): _root_.scala.Unit = x match {
          case Level.LOW => out.writeVal(0)
          case Level.HIGH => out.writeVal(1)
          case _ => out.encodeError("illegal enum value")
        }

        override def nullValue: Level = null.asInstanceOf[Level]
      }
      implicit val levelOrdering: Ordering[Level] = new Ordering[Level] {
        override def compare(x: Level, y: Level): Int = y.ordinal - x.ordinal
      }
      val codecOfOrderedLevelTreeSet = make[_root_.scala.collection.immutable.TreeSet[Level]]
      verifySerDeser(codecOfOrderedLevelTreeSet,
        _root_.scala.collection.immutable.TreeSet[Level](Level.HIGH, Level.LOW), """[0,1]""")
      verifyDeserByCheck(codecOfOrderedLevelTreeSet, """[0,1]""",
        check = (actual: _root_.scala.collection.immutable.TreeSet[Level]) => actual.ordering shouldBe levelOrdering)
    }
    "serialize and deserialize sequences of tuples as JSON object with duplicated keys using a custom codec" in {
      implicit val codecOfSeqOfTuples: JsonValueCodec[Seq[(String, Int)]] = new JsonValueCodec[Seq[(String, Int)]] {
        override def decodeValue(in: JsonReader, default: Seq[(String, Int)]): Seq[(String, Int)] =
          if (in.isNextToken('{')) {
            val kvs = _root_.scala.collection.immutable.List.newBuilder[(String, Int)]
            if (!in.isNextToken('}')) {
              in.rollbackToken()
              while ({
                kvs.+=((in.readKeyAsString(), in.readInt()))
                in.isNextToken(',')
              }) ()
              if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
            }
            kvs.result()
          } else in.readNullOrTokenError(default, '{')

        override def encodeValue(kvs: Seq[(String, Int)], out: JsonWriter): _root_.scala.Unit = {
          out.writeObjectStart()
          kvs.foreach { case (k, v) =>
            out.writeKey(k)
            out.writeVal(v)
          }
          out.writeObjectEnd()
        }

        override val nullValue: Seq[(String, Int)] = Seq.empty
      }
      verifySerDeser(codecOfSeqOfTuples, Seq("foo" -> 1, "bar" -> 2, "foo" -> 3),"""{"foo":1,"bar":2,"foo":3}""")
    }
    "serialize and deserialize raw untouched bytes using a custom value codec" in {
      object RawVal {
        def apply(s: String) = new RawVal(s.getBytes)

        implicit val codec: JsonValueCodec[RawVal] = new JsonValueCodec[RawVal] {
          override def decodeValue(in: JsonReader, default: RawVal): RawVal = new RawVal(in.readRawValAsBytes())

          override def encodeValue(x: RawVal, out: JsonWriter): _root_.scala.Unit = out.writeRawVal(x.bs)

          override val nullValue: RawVal = new RawVal(new _root_.scala.Array[_root_.scala.Byte](0))
        }
      }

      case class RawVal private(bs: _root_.scala.Array[_root_.scala.Byte]) {
        def this(s: _root_.scala.Predef.String) = this(s.getBytes(UTF_8))

        override lazy val hashCode: Int = MurmurHash3.arrayHash(bs)

        override def equals(obj: Any): _root_.scala.Boolean = obj match {
          case that: RawVal => _root_.java.util.Arrays.equals(bs, that.bs)
          case _ => false
        }
      }

      case class Message(param1: String, param2: String, payload: RawVal, param3: String)

      verifySerDeser(make[Message],
        Message("A", "B", RawVal("""{"x":[-1.0,1,4.0E20],"y":{"xx":true,"yy":false,"zz":null},"z":"Z"}"""), "C"),
        """{"param1":"A","param2":"B","payload":{"x":[-1.0,1,4.0E20],"y":{"xx":true,"yy":false,"zz":null},"z":"Z"},"param3":"C"}""")
    }
    "serialize and deserialize case classes with value classes" in {
      case class ValueClassTypes(uid: UserId, oid: OrderId)

      verifySerDeser(make[ValueClassTypes],
        ValueClassTypes(UserId("123abc"), OrderId(123123)), """{"uid":"123abc","oid":123123}""")
    }
    "serialize and deserialize top-level value classes" in {
      val codecOfUserId = make[UserId]
      verifySerDeser(codecOfUserId, UserId("123abc"), "\"123abc\"")
      verifySerDeser(make[OrderId], OrderId(123123), "123123")
    }
    "serialize and deserialize strinfigied top-level value classes" in {
      verifySerDeser(make[OrderId](CodecMakerConfig.withIsStringified(true)), OrderId(123123), "\"123123\"")
    }
    "serialize and deserialize case classes with options" in {
      verifySerDeser(codecOfOptions,
        Options(Option("VVV"), Option(BigInt(4)), Option(Set()), Option(1L), Option(_root_.java.lang.Long.valueOf(2L))),
        """{"os":"VVV","obi":4,"osi":[],"ol":1,"ojl":2}""")
      verifySerDeser(codecOfOptions,
        Options(_root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None),
        """{}""")
    }
    "serialize case classes with empty options as null when the transientNone flag is off" in {
      verifySerDeser(make[Options](CodecMakerConfig.withTransientNone(false)),
        Options(Option("VVV"), Option(BigInt(4)), Option(Set()), Option(1L), Option(_root_.java.lang.Long.valueOf(2L))),
        """{"os":"VVV","obi":4,"osi":[],"ol":1,"ojl":2}""")
      verifySerDeser(make[Options](CodecMakerConfig.withTransientNone(false)),
        Options(_root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None),
        """{"os":null,"obi":null,"osi":null,"ol":null,"ojl":null}""")
    }
    "serialize and deserialize top-level options" in {
      val codecOfStringOption = make[Option[String]]
      verifySerDeser(codecOfStringOption, Some("VVV"), "\"VVV\"")
      verifySerDeser(codecOfStringOption, _root_.scala.None, "null")
    }
    "serialize and deserialize stringified top-level numeric options" in {
      val codecOfStringifiedOption = make[Option[BigInt]](CodecMakerConfig.withIsStringified(true))
      verifySerDeser(codecOfStringifiedOption, Some(BigInt(123)), "\"123\"")
      verifySerDeser(codecOfStringifiedOption, _root_.scala.None, "null")
    }
    "throw parse exception in case of unexpected value for option" in {
      val codecOfStringOption = make[Option[String]]
      verifyDeserError(codecOfStringOption, """no!!!""", "expected value or null, offset: 0x00000001")
    }
    "serialize and deserialize case classes with tuples" in {
      verifySerDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
        """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS"]}""")
    }
    "throw parse exception in case of unexpected number of JSON array values" in {
      verifyDeserError(codecOfTuples, """{"t1":[1,2.2],"t2":["VVV",3,"GPS"]}""", "expected ',', offset: 0x0000000c")
      verifyDeserError(codecOfTuples, """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS","XXX"]}""",
        "expected ']', offset: 0x00000027")
    }
    "serialize and deserialize top-level tuples" in {
      verifySerDeser(make[(String, Int)], ("VVV", 1), "[\"VVV\",1]")
    }
    "serialize and deserialize stringified top-level numeric tuples" in {
      verifySerDeser(make[(Long, Float, BigDecimal)](CodecMakerConfig.withIsStringified(true)),
        (1L, 2.2f, BigDecimal(3.3)), "[\"1\",\"2.2\",\"3.3\"]")
    }
    "serialize and deserialize tuples with type aliases" in {
      type I = Int
      type S = String

      case class Tuples(t1: (S, I), t2: (I, S))

      verifySerDeser(make[Tuples], Tuples(("VVV", 1), (2, "WWW")),
        "{\"t1\":[\"VVV\",1],\"t2\":[2,\"WWW\"]}")
      verifySerDeser(make[(S, I)], ("VVV", 1), "[\"VVV\",1]")
    }
    "serialize and deserialize case classes with arrays" in {
      val arrays = Arrays(_root_.scala.Array(_root_.scala.Array(1, 2), _root_.scala.Array(3, 4)),
        _root_.scala.Array[BigInt](1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
      val json = """{"aa":[[1,2],[3,4]],"a":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17]}"""
      verifySer(codecOfArrays, arrays, json)
      verifySer(make[Arrays](CodecMakerConfig.withTransientEmpty(false)), arrays, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "serialize and deserialize top-level arrays" in {
      val json = """[[1,2,3],[4,5,6]]"""
      val arrayOfArray = _root_.scala.Array(_root_.scala.Array(1, 2, 3), _root_.scala.Array(4, 5, 6))
      val codecOfArrayOfArray = make[Array[Array[Int]]]
      verifySer(codecOfArrayOfArray, arrayOfArray, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfArrayOfArray)
      parsedObj shouldBe arrayOfArray
    }
    "serialize and deserialize stringified top-level arrays" in {
      val json = """[["1","2","3"],["4","5","6"]]"""
      val arrayOfArray = _root_.scala.Array(_root_.scala.Array(1, 2, 3), _root_.scala.Array(4, 5, 6))
      val codecOfStringifiedArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig.withIsStringified(true))
      verifySer(codecOfStringifiedArrayOfArray, arrayOfArray, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfStringifiedArrayOfArray)
      parsedObj shouldBe arrayOfArray
    }
    "don't serialize fields of case classes with empty arrays" in {
      val json = """{"aa":[[],[]]}"""
      val arrays = Arrays(_root_.scala.Array(_root_.scala.Array(), _root_.scala.Array()), _root_.scala.Array())
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "serialize fields of case classes with empty arrays when transientEmpty is off" in {
      val json = """{"aa":[[],[]],"a":[]}"""
      val arrays = Arrays(_root_.scala.Array(_root_.scala.Array(), _root_.scala.Array()), _root_.scala.Array())
      verifySer(make[Arrays](CodecMakerConfig.withTransientEmpty(false)), arrays, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "throw parse exception for missing array field when the requireCollectionFields flag is on" in {
      val codecOfArrays2 = make[Arrays](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false))
      verifyDeserError(codecOfArrays2, "{}", "missing required field \"aa\", offset: 0x00000001")
      verifyDeserError(codecOfArrays2, """{"aa":[[],[]]}""", "missing required field \"a\", offset: 0x0000000d")
    }
    "throw parse exception in case of JSON array is not properly started/closed or with leading/trailing comma" in {
      verifyDeserError(codecOfArrays, """{"aa":[{1,2,3]],"a":[]}""", "expected '[' or null, offset: 0x00000007")
      verifyDeserError(codecOfArrays, """{"aa":[[,1,2,3]],"a":[]}""", "illegal number, offset: 0x00000008")
      verifyDeserError(codecOfArrays, """{"aa":[[1,2,3}],"a":[]}""", "expected ']' or ',', offset: 0x0000000d")
      verifyDeserError(codecOfArrays, """{"aa":[[1,2,3,]],"a":[]}""", "illegal number, offset: 0x0000000e")
    }
    "serialize and deserialize case classes with generic Iterables" in {
      val codecOfGenericIterables = make[GenericIterables]
      verifySerDeser(codecOfGenericIterables,
        GenericIterables(_root_.scala.collection.Set(_root_.scala.collection.SortedSet("1", "2", "3")),
          _root_.scala.collection.IndexedSeq(_root_.scala.collection.Seq(1.1f, 2.2f, 3.3f)),
          _root_.scala.collection.Iterable(4, 5, 6)),
        """{"s":[["1","2","3"]],"is":[[1.1,2.2,3.3]],"i":[4,5,6]}""")
      verifySer(codecOfGenericIterables,
        GenericIterables(_root_.scala.collection.immutable.Set(_root_.scala.collection.immutable.SortedSet("1", "2", "3")),
          _root_.scala.  collection.immutable.IndexedSeq(_root_.scala.collection.immutable.Seq(1.1f, 2.2f, 3.3f)),
          _root_.scala.collection.immutable.Iterable(4, 5, 6)),
        """{"s":[["1","2","3"]],"is":[[1.1,2.2,3.3]],"i":[4,5,6]}""")
      verifySer(codecOfGenericIterables,
        GenericIterables(_root_.scala.collection.mutable.Set(_root_.scala.collection.mutable.SortedSet("1", "2", "3")),
          _root_.scala.collection.mutable.IndexedSeq(_root_.scala.collection.mutable.Seq(1.1f, 2.2f, 3.3f)),
          _root_.scala.collection.mutable.Iterable(4, 5, 6)),
        """{"s":[["1","2","3"]],"is":[[1.1,2.2,3.3]],"i":[4,5,6]}""")
    }
    "serialize and deserialize case classes with mutable Iterables" in {
      verifySerDeser(make[MutableIterables],
        MutableIterables(
          _root_.scala.collection.mutable.Seq(_root_.scala.collection.mutable.SortedSet("1", "2", "3")),
          _root_.scala.collection.mutable.ArrayBuffer(_root_.scala.collection.mutable.Set[BigInt](4),
            _root_.scala.collection.mutable.Set.empty[BigInt]),
          _root_.scala.collection.mutable.ArraySeq(_root_.scala.collection.mutable.LinkedHashSet(5, 6),
            _root_.scala.collection.mutable.LinkedHashSet.empty[Int]),
          _root_.scala.collection.mutable.Buffer(_root_.scala.collection.mutable.HashSet(7.7)),
          _root_.scala.collection.mutable.ListBuffer(_root_.scala.collection.mutable.TreeSet(9L, 10L)),
          _root_.scala.collection.mutable.IndexedSeq(_root_.scala.collection.mutable.ArrayStack(11.11f, 12.12f)),
          _root_.scala.collection.mutable.UnrolledBuffer(_root_.scala.collection.mutable.Iterable(13.toShort, 14.toShort))),
        """{"ml":[["1","2","3"]],"ab":[[4],[]],"as":[[5,6],[]],"b":[[7.7]],"lb":[[9,10]],"is":[[11.11,12.12]],"ub":[[13,14]]}""")
    }
    "serialize and deserialize case classes with immutable Iterables" in {
      verifySerDeser(make[ImmutableIterables],
        ImmutableIterables(List(_root_.scala.collection.immutable.ListSet("1")), _root_.scala.collection.immutable.Queue(Set[BigInt](4)),
          IndexedSeq(_root_.scala.collection.immutable.SortedSet(5, 6, 7), _root_.scala.collection.immutable.SortedSet()),
          Stream(_root_.scala.collection.immutable.TreeSet(8.9)), Vector(Iterable(10L, 11L))),
        """{"l":[["1"]],"q":[[4]],"is":[[5,6,7],[]],"s":[[8.9]],"v":[[10,11]]}""")
    }
    "serialize and deserialize case class fields with empty iterables when transientEmpty is off" in {
      verifySerDeser(make[EmptyIterables](CodecMakerConfig.withTransientEmpty(false)),
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":[],"a":[]}""")
      verifySerDeser(make[EmptyIterables](CodecMakerConfig.withTransientEmpty(false)),
        EmptyIterables(List("VVV"), _root_.scala.collection.mutable.ArrayBuffer(1)), """{"l":["VVV"],"a":[1]}""")
    }
    "throw parse exception for missing collection field when the requireCollectionFields flag is on" in {
      verifyDeserError(make[EmptyIterables](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        "{}", "missing required field \"l\", offset: 0x00000001")

      case class NestedIterables(lessi: List[Either[String, Set[Int]]])

      val codecOfNestedIterables = make[NestedIterables](CodecMakerConfig.withFieldNameMapper {
        case "b" => "value"
        case "a" => "value"
      }.withRequireCollectionFields(true).withTransientEmpty(false))
      verifyDeserError(codecOfNestedIterables, """{"lessi":[{"type":"Left"}]}""",
        "missing required field \"value\", offset: 0x00000018")
      verifyDeserError(codecOfNestedIterables, """{"lessi":[{"type":"Right"}]}""",
        "missing required field \"value\", offset: 0x00000019")
    }
    "serialize and deserialize case classes with collection fields that has default values when the requireCollectionFields flag is on" in {
      case class IterablesWithDefaults(l: List[Int] = _root_.scala.collection.immutable.Nil, s: Set[Option[String]] = Set())

      verifySerDeser(make[IterablesWithDefaults](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        IterablesWithDefaults(), "{}")
    }
    "serialize and deserialize case classes with empty Iterables when the requireCollectionFields flag is on and transientEmpty is off" in {
      verifySerDeser(make[EmptyIterables](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":[],"a":[]}""")
    }
    "deserialize null values as empty Iterables for fields with collection types" in {
      verifyDeser(make[EmptyIterables],
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":null,"a":null}""")
      verifyDeser(make[EmptyIterables](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":null,"a":null}""")
    }
    "serialize and deserialize top-level Iterables" in {
      verifySerDeser(make[_root_.scala.collection.mutable.Set[List[BigDecimal]]],
        _root_.scala.collection.mutable.Set(List[BigDecimal](1.1, 2.2)), """[[1.1,2.2]]""")
    }
    "serialize and deserialize stringified top-level Iterables" in {
      verifySerDeser(make[_root_.scala.collection.mutable.Set[List[BigDecimal]]](CodecMakerConfig.withIsStringified(true)),
        _root_.scala.collection.mutable.Set(List[BigDecimal](1.1, 2.2)), """[["1.1","2.2"]]""")
    }
    "throw parse exception when too many inserts into set was completed" in {
      verifyDeserError(make[_root_.scala.collection.immutable.Set[Int]](CodecMakerConfig.withSetMaxInsertNumber(10)),
        """[1,2,3,4,5,6,7,8,9,10,11]""", "too many set inserts, offset: 0x00000017")
      verifyDeserError(make[_root_.scala.collection.mutable.Set[Int]](CodecMakerConfig.withSetMaxInsertNumber(10)),
        """[1,2,3,4,5,6,7,8,9,10,11]""", "too many set inserts, offset: 0x00000017")
    }
    "serialize and deserialize case classes with generic maps" in {
      case class GenericMaps(m: _root_.scala.collection.Map[Int, _root_.scala.Boolean])

      val codecOfGenericMaps = make[GenericMaps]
      verifySerDeser(codecOfGenericMaps, GenericMaps(_root_.scala.collection.Map(1 -> true)),
        """{"m":{"1":true}}""")
      verifySer(codecOfGenericMaps, GenericMaps(_root_.scala.collection.mutable.Map(1 -> true)),
        """{"m":{"1":true}}""")
      verifySer(codecOfGenericMaps, GenericMaps(_root_.scala.collection.immutable.Map(1 -> true)),
        """{"m":{"1":true}}""")
    }
    "serialize and deserialize case classes with mutable maps" in {
      verifySerDeser(codecOfMutableMaps,
        MutableMaps(_root_.scala.collection.mutable.HashMap(true -> _root_.scala.collection.mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
          _root_.scala.collection.mutable.Map(1.1f -> _root_.scala.collection.mutable.ListMap(BigInt(2) -> "2")),
          _root_.scala.collection.mutable.OpenHashMap(1.1 -> _root_.scala.collection.mutable.LinkedHashMap(3.toShort -> 3.3),
            2.2 -> _root_.scala.collection.mutable.LinkedHashMap())),
        """{"hm":{"true":{"1.1":1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""")
    }
    "serialize and deserialize case classes with immutable maps" in {
      verifySerDeser(codecOfImmutableMaps,
        ImmutableMaps(Map(1 -> 1.1),
          _root_.scala.collection.immutable.HashMap("2" -> _root_.scala.collection.immutable.ListMap('V' -> 2),
          "3" -> _root_.scala.collection.immutable.ListMap('X' -> 3)),
          _root_.scala.collection.immutable.SortedMap(4L -> _root_.scala.collection.immutable.TreeMap(4.toByte -> 4.4f),
            5L -> _root_.scala.collection.immutable.TreeMap.empty[_root_.scala.Byte, Float])),
        """{"m":{"1":1.1},"hm":{"2":{"V":2},"3":{"X":3}},"sm":{"4":{"4":4.4},"5":{}}}""")
    }
    "serialize case class fields with empty maps if transientEmpty is off" in {
      verifySerDeser(make[EmptyMaps](CodecMakerConfig.withTransientEmpty(false)),
        EmptyMaps(Map(), _root_.scala.collection.mutable.Map()), """{"im":{},"mm":{}}""")
      verifySerDeser(make[EmptyMaps](CodecMakerConfig.withTransientEmpty(false)),
        EmptyMaps(Map("VVV" -> 1), _root_.scala.collection.mutable.Map(2L -> 3)),
        """{"im":{"VVV":1},"mm":{"2":3}}""")
    }
    "throw parse exception for missing map field when the requireCollectionFields flag is on" in {
      verifyDeserError(make[EmptyMaps](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        "{}", "missing required field \"im\", offset: 0x00000001")
    }
    "serialize and deserialize case classes with empty maps when the requireCollectionFields flag is on and transientEmpty is off" in {
      verifySerDeser(make[EmptyMaps](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        EmptyMaps(Map(), _root_.scala.collection.mutable.Map()), """{"im":{},"mm":{}}""")
    }
    "deserialize null values as empty maps for fields with map types" in {
      verifyDeser(make[EmptyMaps],
        EmptyMaps(Map(), _root_.scala.collection.mutable.Map()), """{"im":null,"mm":null}""")
      verifyDeser(make[EmptyMaps](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        EmptyMaps(Map(), _root_.scala.collection.mutable.Map()), """{"im":null,"mm":null}""")
    }
    "serialize and deserialize top-level maps" in {
      verifySerDeser(make[_root_.scala.collection.mutable.LinkedHashMap[Int, Map[Char, _root_.scala.Boolean]]],
        _root_.scala.collection.mutable.LinkedHashMap(1 -> Map('V' -> true), 2 -> Map.empty[Char, _root_.scala.Boolean]),
        """{"1":{"V":true},"2":{}}""")
    }
    "serialize and deserialize stringified top-level maps" in {
      verifySerDeser(make[_root_.scala.collection.mutable.LinkedHashMap[Int, Map[Char, _root_.scala.Boolean]]](CodecMakerConfig.withIsStringified(true)),
        _root_.scala.collection.mutable.LinkedHashMap(1 -> Map('V' -> true), 2 -> Map.empty[Char, _root_.scala.Boolean]),
        """{"1":{"V":"true"},"2":{}}""")
    }
    "serialize and deserialize top-level maps as arrays" in {
      verifySerDeser(make[_root_.scala.collection.Map[Int, _root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true)),
        _root_.scala.collection.Map(1 -> true), """[[1,true]]""")
      verifySerDeser(make[_root_.scala.collection.mutable.LinkedHashMap[Int, _root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true)),
        _root_.scala.collection.mutable.LinkedHashMap(1 -> true), """[[1,true]]""")
      verifySerDeser(make[_root_.scala.collection.mutable.LongMap[_root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true)),
        _root_.scala.collection.mutable.LongMap(1L -> true), """[[1,true]]""")
      verifySerDeser(make[_root_.scala.collection.immutable.LongMap[_root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true)),
        _root_.scala.collection.immutable.LongMap(1L -> true), """[[1,true]]""")
      verifySerDeser(make[_root_.scala.collection.immutable.IntMap[_root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true)),
        _root_.scala.collection.immutable.IntMap(1 -> true), """[[1,true]]""")
    }
    "serialize and deserialize stringified top-level maps as arrays" in {
      verifySerDeser(make[_root_.scala.collection.Map[Int, _root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true).withIsStringified(true)),
        _root_.scala.collection.Map(1 -> true), """[["1","true"]]""")
      verifySerDeser(make[_root_.scala.collection.mutable.LinkedHashMap[Int, _root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true).withIsStringified(true)),
        _root_.scala.collection.mutable.LinkedHashMap(1 -> true), """[["1","true"]]""")
      verifySerDeser(make[_root_.scala.collection.mutable.LongMap[_root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true).withIsStringified(true)),
        _root_.scala.collection.mutable.LongMap(1L -> true), """[["1","true"]]""")
      verifySerDeser(make[_root_.scala.collection.immutable.LongMap[_root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true).withIsStringified(true)),
        _root_.scala.collection.immutable.LongMap(1L -> true), """[["1","true"]]""")
      verifySerDeser(make[_root_.scala.collection.immutable.IntMap[_root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true).withIsStringified(true)),
        _root_.scala.collection.immutable.IntMap(1 -> true), """[["1","true"]]""")
    }
    "throw parse exception when too many inserts into map was completed" in {
      verifyDeserError(make[_root_.scala.collection.immutable.Map[Int, Int]](CodecMakerConfig.withMapMaxInsertNumber(10)),
        """{"1":1,"2":2,"3":3,"4":4,"5":5,"6":6,"7":7,"8":8,"9":9,"10":10,"11":11}""",
        "too many map inserts, offset: 0x00000045")
    }
    "throw parse exception in case of JSON object is not properly started/closed or with leading/trailing comma" in {
      verifyDeserError(codecOfImmutableMaps, """{"m":["1":1.1},"hm":{},"sm":{}}""",
        "expected '{' or null, offset: 0x00000005")
      verifyDeserError(codecOfImmutableMaps, """{"m":{,"1":1.1},"hm":{},"sm":{}}""",
        "expected '\"', offset: 0x00000006")
      verifyDeserError(codecOfImmutableMaps, """{"m":{"1":1.1],"hm":{},"sm":{}""",
        "expected '}' or ',', offset: 0x0000000d")
      verifyDeserError(codecOfImmutableMaps, """{"m":{"1":1.1,},"hm":{},"sm":{}""",
        "expected '\"', offset: 0x0000000e")
    }
    "throw parse exception in case of illegal keys found during deserialization of maps" in {
      verifyDeserError(codecOfMutableMaps, """{"m":{"1.1":{"null":"2"}}""", "illegal number, offset: 0x0000000e")
    }
    "serialize and deserialize case classes with mutable long maps" in {
      verifySerDeser(make[MutableLongMaps],
        MutableLongMaps(_root_.scala.collection.mutable.LongMap(1L -> 1.1),
          _root_.scala.collection.mutable.LongMap(3L -> "33")), """{"lm1":{"1":1.1},"lm2":{"3":"33"}}""")
    }
    "serialize and deserialize case classes with immutable int and long maps" in {
      verifySerDeser(make[ImmutableIntLongMaps],
        ImmutableIntLongMaps(_root_.scala.collection.immutable.IntMap(1 -> 1.1, 2 -> 2.2),
          _root_.scala.collection.immutable.LongMap(3L -> "33")), """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""")
    }
    "serialize and deserialize case classes with mutable & immutable bitsets" in {
      verifySerDeser(make[BitSets],
        BitSets(_root_.scala.collection.BitSet(0), _root_.scala.collection.immutable.BitSet(1, 2, 3),
          _root_.scala.collection.mutable.BitSet(4, 5, 6)), """{"bs":[0],"ibs":[1,2,3],"mbs":[4,5,6]}""")
    }
    "serialize and deserialize top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets =
        make[_root_.scala.collection.mutable.LongMap[_root_.scala.collection.immutable.IntMap[_root_.scala.collection.mutable.BitSet]]]
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        _root_.scala.collection.mutable.LongMap(1L -> _root_.scala.collection.immutable.IntMap(2 -> _root_.scala.collection.mutable.BitSet(4, 5, 6),
          3 -> _root_.scala.collection.mutable.BitSet.empty)), """{"1":{"2":[4,5,6],"3":[]}}""")
    }
    "serialize and deserialize stringified top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets =
        make[_root_.scala.collection.mutable.LongMap[_root_.scala.collection.immutable.IntMap[_root_.scala.collection.mutable.BitSet]]](CodecMakerConfig.withIsStringified(true))
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        _root_.scala.collection.mutable.LongMap(1L -> _root_.scala.collection.immutable.IntMap(2 -> _root_.scala.collection.mutable.BitSet(4, 5, 6),
          3 -> _root_.scala.collection.mutable.BitSet.empty)), """{"1":{"2":["4","5","6"],"3":[]}}""")
    }
    "throw parse exception when too big numbers are parsed for mutable & immutable bitsets" in {
      verifyDeserError(make[_root_.scala.collection.immutable.BitSet](CodecMakerConfig.withBitSetValueLimit(1000)),
        """[1,2,1000]""", "illegal value for bit set, offset: 0x00000008")
      verifyDeserError(make[_root_.scala.collection.mutable.BitSet],
        """[1,2,10000]""", "illegal value for bit set, offset: 0x00000009")
    }
    "throw parse exception when negative numbers are parsed for mutable & immutable bitsets" in {
      verifyDeserError(make[_root_.scala.collection.immutable.BitSet],
        """[1,2,-1]""", "illegal value for bit set, offset: 0x00000006")
      verifyDeserError(make[_root_.scala.collection.mutable.BitSet],
        """[1,2,-1]""", "illegal value for bit set, offset: 0x00000006")
    }
    "don't generate codecs for maps with not supported types of keys" in {
      assert(intercept[TestFailedException](assertCompiles {
        """JsonCodecMaker.make[Map[_root_.java.util.Date,String]]"""
      }).getMessage.contains {
        """No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[_]' defined for 'java.util.Date'."""
      })
    }
    "serialize and deserialize with keys defined as is by fields" in {
      verifySerDeser(make[CamelPascalSnakeKebabCases], CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camelCase":1,"PascalCase":2,"snake_case":3,"kebab-case":4,"camel1":5,"Pascal1":6,"snake_1":7,"kebab-1":8}""")
    }
    "serialize and deserialize with keys renamed" in {
      verifySerDeser(make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper {
        case "camelCase" => "CMLCS"
        case "kebab-1" => "KBB1"
      }), CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"CMLCS":1,"PascalCase":2,"snake_case":3,"kebab-case":4,"camel1":5,"Pascal1":6,"snake_1":7,"KBB1":8}""")
    }
    "serialize and deserialize with keys enforced to camelCase and throw parse exception when they are missing" in {
      val codecOfEnforcedCamelCase =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforceCamelCase))
      verifySerDeser(codecOfEnforcedCamelCase, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""")
      verifyDeserError(codecOfEnforcedCamelCase,
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""",
        "missing required field \"camelCase\", offset: 0x0000006e")
      verifyDeserError(codecOfEnforcedCamelCase,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        "missing required field \"camelCase\", offset: 0x0000006e")
      verifyDeserError(codecOfEnforcedCamelCase,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        "missing required field \"camelCase\", offset: 0x00000066")
    }
    "serialize and deserialize with keys enforced to snake_case and throw parse exception when they are missing" in {
      val codec_of_enforced_snake_case =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case))
      verifySerDeser(codec_of_enforced_snake_case, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""")
      verifyDeserError(codec_of_enforced_snake_case,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        "missing required field \"camel_case\", offset: 0x00000066")
      verifyDeserError(codec_of_enforced_snake_case,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        "missing required field \"camel_case\", offset: 0x0000006e")
      verifyDeserError(codec_of_enforced_snake_case,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        "missing required field \"camel_case\", offset: 0x00000066")
    }
    "serialize and deserialize with keys enforced to kebab-case and throw parse exception when they are missing" in {
      val `codec-of-enforced-kebab-case` =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.`enforce-kebab-case`))
      verifySerDeser(`codec-of-enforced-kebab-case`, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""")
      verifyDeserError(`codec-of-enforced-kebab-case`,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        "missing required field \"camel-case\", offset: 0x00000066")
      verifyDeserError(`codec-of-enforced-kebab-case`,
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""",
        "missing required field \"camel-case\", offset: 0x0000006e")
      verifyDeserError(`codec-of-enforced-kebab-case`,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        "missing required field \"camel-case\", offset: 0x00000066")
    }
    "serialize and deserialize with keys enforced to PascalCase and throw parse exception when they are missing" in {
      val CodecOfEnforcedPascalCase =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.EnforcePascalCase))
      verifySerDeser(CodecOfEnforcedPascalCase, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""")
      verifyDeserError(CodecOfEnforcedPascalCase,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        "missing required field \"CamelCase\", offset: 0x00000066")
      verifyDeserError(CodecOfEnforcedPascalCase,
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""",
        "missing required field \"CamelCase\", offset: 0x0000006e")
      verifyDeserError(CodecOfEnforcedPascalCase,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        "missing required field \"CamelCase\", offset: 0x0000006e")
    }
    "serialize and deserialize with keys overridden by annotation and throw parse exception when they are missing" in {
      verifySerDeser(codecOfNameOverridden, NameOverridden(oldName = "VVV"), """{"new_name":"VVV"}""")
      verifyDeserError(codecOfNameOverridden, """{"oldName":"VVV"}""",
        "missing required field \"new_name\", offset: 0x00000010")
    }
    "don't generate codecs for case classes with field that have duplicated @named annotation" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedNamed(@named("x") @named("y") z: Int)
          |JsonCodecMaker.make[DuplicatedNamed]""".stripMargin
      }).getMessage.contains {
        """Duplicated 'com.github.plokhotnyuk.jsoniter_scala.macros.named' defined for 'z' of 'DuplicatedNamed'."""
      })
    }
    "don't generate codecs for case classes with fields that have duplicated JSON names" in {
      val expectedError =
        """Duplicated JSON key(s) defined for 'DuplicatedJsonName': 'x'. Keys are derived from field names of the class
          |that are mapped by the 'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.fieldNameMapper'
          |function or can be overridden by 'com.github.plokhotnyuk.jsoniter_scala.macros.named' annotation(s). Result
          |keys should be unique and should not match with a key for the discriminator field that is specified by the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.discriminatorFieldName' option.""".stripMargin.replace('\n', ' ')
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedJsonName(x: Int, @named("x") z: Int)
          |JsonCodecMaker.make[DuplicatedJsonName]""".stripMargin
      }).getMessage.contains(expectedError))
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedJsonName(y: Int, z: Int)
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig.withFieldNameMapper { case _ => "x" })""".stripMargin
      }).getMessage.contains(expectedError))
    }
    "serialize and deserialize fields that stringified by annotation" in {
      verifySerDeser(codecOfStringified, Stringified(1, 2, List(1), List(2)),
        """{"i":"1","bi":"2","l1":["1"],"l2":[2]}""")
    }
    "throw parse exception when stringified fields have non-string values" in {
      verifyDeserError(codecOfStringified, """{"i":1,"bi":"2","l1":["1"],"l2":[2]}""",
        "expected '\"', offset: 0x00000005")
      verifyDeserError(codecOfStringified, """{"i":"1","bi":2,"l1":[1],"l2":[2]}""",
        "expected '\"', offset: 0x0000000e")
    }
    "serialize and deserialize recursive types if it was allowed" in {
      verifySerDeser(make[Recursive](CodecMakerConfig.withAllowRecursiveTypes(true)),
        Recursive("VVV", 1.1, List(1, 2, 3), Map('S' -> Recursive("WWW", 2.2, List(4, 5, 6), Map()))),
        "{\"s\":\"VVV\",\"bd\":1.1,\"l\":[1,2,3],\"m\":{\"S\":{\"s\":\"WWW\",\"bd\":2.2,\"l\":[4,5,6]}}}")
    }
    "don't generate codecs for recursive types by default" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class Recursive(r: Recursive)
          |JsonCodecMaker.make[Recursive]""".stripMargin
      }).getMessage.contains {
        """Recursive type(s) detected: 'Recursive'. Please consider using a custom implicitly accessible codec for this
          |type to control the level of recursion or turn on the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.allowRecursiveTypes' for the trusted input
          |that will not exceed the thread stack size.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """case class NonRecursive(r1: Recursive1)
          |case class Recursive1(r2: Recursive2)
          |case class Recursive2(r3: Recursive3)
          |case class Recursive3(r1: Recursive1)
          |JsonCodecMaker.make[NonRecursive]""".stripMargin
      }).getMessage.contains {
        """Recursive type(s) detected: 'Recursive1', 'Recursive2', 'Recursive3'. Please consider using a custom
          |implicitly accessible codec for this type to control the level of recursion or turn on the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.allowRecursiveTypes' for the trusted input
          |that will not exceed the thread stack size.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """case class HigherKindedType[F[_]](f: F[Int], fs: F[HigherKindedType[F]])
          |JsonCodecMaker.make[HigherKindedType[Option]]""".stripMargin
      }).getMessage.contains {
        """Recursive type(s) detected: 'HigherKindedType[Option]', 'Option[HigherKindedType[Option]]'. Please consider
          |using a custom implicitly accessible codec for this type to control the level of recursion or turn on the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.allowRecursiveTypes' for the trusted input
          |that will not exceed the thread stack size.""".stripMargin.replace('\n', ' ')
      })
    }
    "serialize and deserialize recursive structures with an implicit codec using a minimal thread stack" in {
      case class Nested(n: Option[Nested] = _root_.scala.None)

      @tailrec
      def construct(d: Int = 1000000, n: Nested = Nested()): Nested =
        if (d <= 0) n
        else construct(d - 1, Nested(Some(n)))

      implicit val codecOfNestedStructs: JsonValueCodec[Nested] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
      val bytes = ("{" + "\"n\":{" * 1000000 + "}" * 1000000 + "}").getBytes
      val readStackTrace = new StringWriter
      intercept[StackOverflowError](readFromArray[Nested](bytes)).printStackTrace(new PrintWriter(readStackTrace))
      assert(readStackTrace.toString.contains(".d0("))
      assert(!readStackTrace.toString.contains(".d1("))
      assert(!readStackTrace.toString.contains(".decodeValue("))
      val writeStackTrace = new StringWriter
      intercept[StackOverflowError](writeToArray[Nested](construct())).printStackTrace(new PrintWriter(writeStackTrace))
      assert(writeStackTrace.toString.contains(".e0("))
      assert(!writeStackTrace.toString.contains(".e1("))
      assert(!writeStackTrace.toString.contains(".encodeValue("))
    }
    "serialize and deserialize indented by spaces and new lines if it was configured for writer" in {
      verifySerDeser(codecOfRecursive,
        Recursive("VVV", 1.1, List(1, 2, 3), Map('S' -> Recursive("WWW", 2.2, List(4, 5, 6), Map()))),
        """{
          |  "s": "VVV",
          |  "bd": 1.1,
          |  "l": [
          |    1,
          |    2,
          |    3
          |  ],
          |  "m": {
          |    "S": {
          |      "s": "WWW",
          |      "bd": 2.2,
          |      "l": [
          |        4,
          |        5,
          |        6
          |      ]
          |    }
          |  }
          |}""".stripMargin,
        WriterConfig.withIndentionStep(2))
    }
    "deserialize JSON with whitespaces, tabs, new lines, and line returns" in {
      verifyDeser(codecOfRecursive,
        Recursive("VVV", 1.1, List(1, 2, 3), Map('S' -> Recursive("WWW", 2.2, List(4, 5, 6), Map()))),
        "{\r \"s\":\t\"VVV\",\n\t\"bd\":\t1.1,\r  \"l\":\t[\r\t\t1,\r\t\t2,\r\t\t3\r\t],\r\t\"m\":\t{\n\t\t\"S\":\t{\r  \t\t\"s\":\t\t \t\"WWW\",\n\t\"bd\":\t2.2,\"l\":\t[\r\t\t4,\n\n\n5,\r\t\t6\r\t]\n}\r}\r}")
    }
    "serialize and deserialize UTF-8 keys and values of case classes without hex encoding" in {
      verifySerDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ"), """{"გასაღები":"ვვვ"}""")
    }
    "serialize and deserialize UTF-8 keys and values of case classes with hex encoding" in {
      verifyDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t\\/\"}")
      verifySer(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t/\"}",
        WriterConfig.withEscapeUnicode(true))
    }
    "serialize and deserialize case classes with Scala operators in field names" in {
      case class Operators(`=<>!#%^&|*/\\~+-:$`: Int)

      verifySerDeser(make[Operators], Operators(7), """{"=<>!#%^&|*/\\~+-:$":7}""")
    }
    "don't serialize default values of case classes that defined for fields when the transientDefault flag is on (by default)" in {
      val codecOfDefaults: JsonValueCodec[Defaults] = make
      verifySer(codecOfDefaults, Defaults(), "{}")
      verifySer(codecOfDefaults, Defaults(oc = _root_.scala.None, l = _root_.scala.collection.immutable.Nil), """{}""")
    }
    "serialize default values of case classes that defined for fields when the transientDefault flag is off" in {
      verifySer(make[Defaults](CodecMakerConfig.withTransientDefault(false)), Defaults(),
        """{"st":"VVV","i":1,"bi":-1,"oc":"X","l":[0],"e":"HIGH","a":[[1,2],[3,4]],"ab":[1,2],"m":{"1":true},"mm":{"VVV":1},"im":{"1":"VVV"},"lm":{"1":2},"s":["VVV"],"ms":[1],"bs":[1],"mbs":[1]}""")
    }
    "serialize empty of case classes that defined for fields when the transientEmpty and transientNone flags are off" in {
      verifySer(make[Defaults](CodecMakerConfig.withTransientEmpty(false).withTransientNone(false)),
        Defaults(oc = _root_.scala.None, l = _root_.scala.collection.immutable.List(), a = _root_.scala.Array(), ab = _root_.scala.collection.mutable.ArrayBuffer(), m = Map(),
          mm = _root_.scala.collection.mutable.Map(), im = _root_.scala.collection.immutable.IntMap(),
          lm = _root_.scala.collection.mutable.LongMap(), s = _root_.scala.collection.immutable.Set(),
          ms = _root_.scala.collection.mutable.Set(), bs = _root_.scala.collection.immutable.BitSet(),
          mbs = _root_.scala.collection.mutable.BitSet()),
        """{"oc":null,"l":[],"a":[],"ab":[],"m":{},"mm":{},"im":{},"lm":{},"s":[],"ms":[],"bs":[],"mbs":[]}""")
      verifySer(make[Defaults](CodecMakerConfig.withTransientEmpty(false).withTransientNone(false)), Defaults(), """{}""")
    }
    "deserialize default values of case classes that defined for fields" in {
      val codecOfDefaults: JsonValueCodec[Defaults2] = make
      verifyDeser(codecOfDefaults, Defaults2(), """{}""")
      verifyDeser(codecOfDefaults, Defaults2(),
        """{"st":null,"bi":null,"l":null,"oc":null,"e":null,"ab":null,"m":null,"mm":null,"im":null,"lm":null,"s":null,"ms":null,"bs":null,"mbs":null}""".stripMargin)
      verifyDeser(codecOfDefaults, Defaults2(),
        """{"l":[],"ab":[],"m":{},"mm":{},"im":{},"lm":{},"s":[],"ms":[],"bs":[],"mbs":[]}""")
    }
    "deserialize default values of polymorphic case classes that defined for fields" in {
      val polymorphicDefaults: JsonValueCodec[PolymorphicDefaults[Int, String]] = make
      verifyDeser(polymorphicDefaults, PolymorphicDefaults[Int, String](), """{}""")
      verifyDeser(polymorphicDefaults, PolymorphicDefaults[Int, String](), """{"i":1,"s":[]}""")
    }
    "don't generate codecs for case classes that have ill-typed default values defined for fields" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[PolymorphicDefaults[String, Int]]"
      }).getMessage.contains {
        "polymorphic expression cannot be instantiated to expected type"
      })
    }
    "don't serialize and deserialize transient and non constructor defined fields of case classes" in {
      case class Transient(@transient transient: String = "default", required: String) {
        val ignored: String = s"$required-$transient"
      }

      val codecOfTransient = make[Transient]
      verifySer(codecOfTransient, Transient(required = "VVV"), """{"required":"VVV"}""")
      verifyDeser(codecOfTransient, Transient(required = "VVV"), """{"transient":"XXX","required":"VVV"}""")
      verifySer(codecOfTransient, Transient(required = "VVV", transient = "non-default"), """{"required":"VVV"}""")
      val codecOfTransient2 = make[Transient](CodecMakerConfig.withTransientDefault(false))
      verifySer(codecOfTransient2, Transient(required = "VVV"), """{"required":"VVV"}""")
      verifyDeser(codecOfTransient2, Transient(required = "VVV"), """{"transient":"XXX","required":"VVV"}""")
      verifySer(codecOfTransient2, Transient(required = "VVV", transient = "non-default"), """{"required":"VVV"}""")
    }
    "don't serialize case class fields with 'None' values" in {
      case class NoneValues(opt: Option[String])

      verifySer(make[NoneValues], NoneValues(_root_.scala.None), """{}""")
      verifySer(make[List[NoneValues]], List(NoneValues(_root_.scala.None)), """[{}]""")
    }
    "don't serialize case class fields with empty collections" in {
      case class EmptyIterables(l: List[String], s: Set[Int], ls: List[Set[Int]])

      verifySer(make[EmptyIterables], EmptyIterables(List(), Set(), List()), """{}""")
    }
    "parse with skipping of unknown case class fields" in {
      case class SkipUnknown()

      verifyDeser(make[SkipUnknown], SkipUnknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""")
    }
    "throw parse exception for unknown case class fields if skipping of them wasn't allowed in materialize call" in {
      case class DetectUnknown()

      verifyDeserError(make[DetectUnknown](CodecMakerConfig.withSkipUnexpectedFields(false)),
        """{"x":1,"y":[1,2],"z":{"a",3}}""", "unexpected field \"x\", offset: 0x00000004")
    }
    "throw parse exception in case of missing values for required fields if case class detected during deserialization" in {
      verifyDeserError(codecOfStandardTypes, """{"s":null,"bi":0,"bd":1}""", """expected '"', offset: 0x00000005""")
      verifyDeserError(codecOfStandardTypes, """{"s":"VVV","bi":null,"bd":1}""", "illegal number, offset: 0x00000010")
    }
    "throw parse exception in case of missing required case class fields detected during deserialization" in {
      case class Required32(
        r00: Int = 0, r01: Int, r02: Int, r03: Int, r04: Int, r05: Int, r06: Int, r07: Int, r08: Int, r09: Int,
        r10: Int = 10, r11: Int, r12: Int, r13: Int, r14: Int, r15: Int, r16: Int, r17: Int, r18: Int, r19: Int,
        r20: Int = 20, r21: Int, r22: Int, r23: Int, r24: Int, r25: Int, r26: Int, r27: Int, r28: Int, r29: Int,
        r30: Int = 30, r31: Int)

      case class Required100(
        r00: Int = 0, r01: Int, r02: Int, r03: Int, r04: Int, r05: Int, r06: Int, r07: Int, r08: Int, r09: Int,
        r10: Int = 10, r11: Int, r12: Int, r13: Int, r14: Int, r15: Int, r16: Int, r17: Int, r18: Int, r19: Int,
        r20: Int = 20, r21: Int, r22: Int, r23: Int, r24: Int, r25: Int, r26: Int, r27: Int, r28: Int, r29: Int,
        r30: Int = 30, r31: Int, r32: Int, r33: Int, r34: Int, r35: Int, r36: Int, r37: Int, r38: Int, r39: Int,
        r40: Int = 40, r41: Int, r42: Int, r43: Int, r44: Int, r45: Int, r46: Int, r47: Int, r48: Int, r49: Int,
        r50: Int = 50, r51: Int, r52: Int, r53: Int, r54: Int, r55: Int, r56: Int, r57: Int, r58: Int, r59: Int,
        r60: Int = 60, r61: Int, r62: Int, r63: Int, r64: Int, r65: Int, r66: Int, r67: Int, r68: Int, r69: Int,
        r70: Int = 70, r71: Int, r72: Int, r73: Int, r74: Int, r75: Int, r76: Int, r77: Int, r78: Int, r79: Int,
        r80: Int = 80, r81: Int, r82: Int, r83: Int, r84: Int, r85: Int, r86: Int, r87: Int, r88: Int, r89: Int,
        r90: Int = 90, r91: Int, r92: Int, r93: Int, r94: Int, r95: Int, r96: Int, r97: Int, r98: Int, r99: Int)

      verifyDeserError(make[Required32],
        """{
          |"r00":0,"r01":1,"r02":2,"r03":3,"r04":4,"r05":5,"r06":6,"r07":7,"r08":8,"r09":9,
          |"r10":10,"r11":11,"r12":12,"r13":13,"r14":14,"r15":15,"r16":16,"r17":17,"r18":18,"r19":19,
          |"r20":20,"r21":21,"r22":22,"r23":23,"r24":24,"r25":25,"r26":26,"r27":27,"r28":28,"r29":29,
          |"r30":30
          |}""".stripMargin, """missing required field "r31", offset: 0x00000112""")
      verifyDeserError(make[Required100],
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
          |}""".stripMargin, """missing required field "r99", offset: 0x0000037c""")
    }
    "throw parse exception in case of missing required fields that are defined after optional was detected" in {
      case class RequiredAfterOptionalFields(f1: Option[Long], f2: Long, f3: Option[Long], f4: Long)

      val codecOfRequiredAfterOptionalFields = make[RequiredAfterOptionalFields]
      verifyDeserError(codecOfRequiredAfterOptionalFields, """{}""",
        """missing required field "f2", offset: 0x00000001""")
      verifyDeserError(codecOfRequiredAfterOptionalFields, """{"f2":2}""",
        """missing required field "f4", offset: 0x00000007""")
      verifyDeserError(codecOfRequiredAfterOptionalFields, """{"f1":1,"f2":2}""",
        """missing required field "f4", offset: 0x0000000e""")
    }
    "throw the stack overflow error in case of serialization of a cyclic graph" in {
      case class Cyclic(var opt: Option[Cyclic])

      val codecOfCyclic = make[Cyclic](CodecMakerConfig.withAllowRecursiveTypes(true))
      val cyclic = Cyclic(_root_.scala.None)
      cyclic.opt = Some(cyclic)
      val len = 10000000
      val cfg = WriterConfig.withPreferredBufSize(1)
      intercept[StackOverflowError](verifyDirectByteBufferSer(codecOfCyclic, cyclic, len, cfg, ""))
      intercept[StackOverflowError](verifyHeapByteBufferSer(codecOfCyclic, cyclic, len, cfg, ""))
      intercept[StackOverflowError](verifyOutputStreamSer(codecOfCyclic, cyclic, cfg, ""))
      intercept[StackOverflowError](verifyArraySer(codecOfCyclic, cyclic, cfg, ""))
    }
    "serialize and deserialize ADTs using ASCII discriminator field & value" in {
      verifySerDeser(codecOfADTList, List(AAA(1), BBB(BigInt(1)), CCC(1, "VVV"), DDD),
        """[{"type":"AAA","a":1},{"type":"BBB","a":1},{"type":"CCC","a":1,"b":"VVV"},{"type":"DDD"}]""")
      verifySerDeser(codecOfADTList2, List(AAA(1), BBB(BigInt(1)), CCC(1, "VVV"), DDD),
        """[{"AAA":{"a":1}},{"BBB":{"a":1}},{"CCC":{"a":1,"b":"VVV"}},"DDD"]""")
      verifySerDeser(make[List[AdtBase]](CodecMakerConfig.withDiscriminatorFieldName(Some("t"))),
        List(CCC(2, "WWW"), CCC(1, "VVV")), """[{"t":"CCC","a":2,"b":"WWW"},{"t":"CCC","a":1,"b":"VVV"}]""")
      verifySerDeser(make[List[Weapon]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        List(Weapon.Axe, Weapon.Sword), """["Axe","Sword"]""")
    }
    "serialize and deserialize product types without discriminators if their codecs are derived not from the base ADT type" in {
      verifySerDeser(make[AAA], AAA(1), """{"a":1}""")
      verifySerDeser(make[BBB], BBB(BigInt(1)), """{"a":1}""")
      verifySerDeser(make[CCC], CCC(1, "VVV"), """{"a":1,"b":"VVV"}""")
      verifySerDeser(make[DDD.type], DDD, """{}""")
    }
    "deserialize ADTs when discriminator field was serialized in far away last position" in {
      val longStr = "W" * 100000
      verifyDeser(codecOfADTList, List(CCC(2, longStr), CCC(1, "VVV")),
        s"""[{"a":2,"b":"$longStr","type":"CCC"},{"a":1,"type":"CCC","b":"VVV"}]""")
      val longBigInt = BigInt("9" * 20000)
      verifyDeser(codecOfADTList, List(BBB(longBigInt), BBB(BigInt(1))),
        s"""[{"a":$longBigInt,"type":"BBB"},{"a":1,"type":"BBB"}]""")
    }
    "serialize and deserialize ADTs with leaf types that are not case classes or case objects" in {
      sealed trait X

      class A() extends X {
        override def hashCode(): Int = 1

        override def equals(obj: Any): _root_.scala.Boolean = obj.isInstanceOf[A]

        override def toString: String = "A()"
      }

      object B extends X {
        override def toString: String = "B"
      }

      verifySerDeser(make[List[X]], List(new A(), B), """[{"type":"A"},{"type":"B"}]""")
    }
    "serialize and deserialize ADTs with non abstract sealed base" in {
      sealed class A {
        override def hashCode(): Int = 1

        override def equals(obj: Any): _root_.scala.Boolean = obj.isInstanceOf[A]

        override def toString: String = "A()"
      }

      case class B(n: Int) extends A

      case class C(s: String) extends A

      verifySerDeser(make[List[A]], List(new A(), B(1), C("VVV")),
        """[{"type":"A"},{"type":"B","n":1},{"type":"C","s":"VVV"}]""")
    }
    "serialize and deserialize ADTs using a custom name of the discriminator field" in {
      sealed abstract class Base extends Product with Serializable

      final case class A(b: B) extends Base

      final case class B(c: String) extends Base

      verifySerDeser(make[List[Base]](CodecMakerConfig.withDiscriminatorFieldName(Some("t")).withSkipUnexpectedFields(false)),
        List(A(B("x")), B("x")), """[{"t":"A","b":{"c":"x"}},{"t":"B","c":"x"}]""")
    }
    "serialize and deserialize ADTs using custom values of the discriminator field" in {
      sealed abstract class Base extends Product with Serializable

      final case class A(b: B) extends Base

      final case class B(c: String) extends Base

      verifySerDeser(make[List[Base]](CodecMakerConfig.withAdtLeafClassNameMapper(x => JsonCodecMaker.simpleClassName(x) match {
        case "A" => "X"
        case "B" => "Y"
      }).withSkipUnexpectedFields(false)),
      List(A(B("x")), B("x")), """[{"type":"X","b":{"c":"x"}},{"type":"Y","c":"x"}]""")
    }
    "serialize and deserialize ADTs using non-ASCII characters for the discriminator field name and it's values" in {
      sealed abstract class База extends Product with Serializable

      case class А(б: Б) extends База

      case class Б(с: String) extends База

      verifySerDeser(make[List[База]](CodecMakerConfig.withDiscriminatorFieldName(Some("тип")).withSkipUnexpectedFields(false)),
        List(А(Б("x")), Б("x")), """[{"тип":"А","б":{"с":"x"}},{"тип":"Б","с":"x"}]""")
    }
    "serialize and deserialize ADTs with Scala operators in names" in {
      sealed trait TimeZone extends Product with Serializable

      case object `US/Alaska` extends TimeZone

      case object `Europe/Paris` extends TimeZone

      verifySerDeser(make[List[TimeZone]](CodecMakerConfig.withDiscriminatorFieldName(Some("zoneId"))),
        List(`US/Alaska`, `Europe/Paris`),
        """[{"zoneId":"US/Alaska"},{"zoneId":"Europe/Paris"}]""")
    }
    "serialize and deserialize ADTs with leafs that have mixed traits that extends the same base" in {
      sealed trait Base extends Product with Serializable

      sealed trait Base2 extends Base

      final case class A(a: Int) extends Base with Base2

      final case class B(b: String) extends Base with Base2

      verifySerDeser(make[List[Base]], List(A(1), B("VVV")),
        """[{"type":"A","a":1},{"type":"B","b":"VVV"}]""")
    }
    "serialize and deserialize ADTs with leafs that have a mixed trait hierarchy that makes a diamond" in {
      sealed trait Base extends Product with Serializable

      sealed trait Base1 extends Base

      sealed trait Base2 extends Base

      final case class A(a: Int) extends Base1 with Base2

      final case class B(b: String) extends Base1 with Base2

      verifySerDeser(make[List[Base]], List(A(1), B("VVV")),
        """[{"type":"A","a":1},{"type":"B","b":"VVV"}]""")
    }
    "throw parse exception in case of duplicated discriminator field" in {
      verifyDeserError(codecOfADTList, """[{"type":"AAA","a":1,"type":"AAA"}]""",
        """duplicated field "type", offset: 0x0000001b""")
    }
    "throw parse exception in case of missing discriminator field or illegal value of discriminator field" in {
      verifyDeserError(codecOfADTList, """[{"a":1}]""", """missing required field "type", offset: 0x00000007""")
      verifyDeserError(codecOfADTList, """[{"a":1,"type":"aaa"}]""",
        """illegal value of discriminator field "type", offset: 0x00000013""")
      verifyDeserError(codecOfADTList, """[{"a":1,"type":123}]""", """expected '"', offset: 0x0000000f""")
      verifyDeserError(codecOfADTList2, """[true]""", """expected '"' or '{' or null, offset: 0x00000001""")
      verifyDeserError(codecOfADTList2, """[{{"a":1}}]""", """expected '"', offset: 0x00000002""")
      verifyDeserError(codecOfADTList2, """[{"aaa":{"a":1}}]""", """illegal discriminator, offset: 0x00000007""")
    }
    "don't generate codecs for non sealed traits or abstract classes as an ADT base" in {
      assert(intercept[TestFailedException](assertCompiles {
        """trait X
          |case class A(i: Int) extends X
          |case object B extends X
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'X' or
          |provide a custom implicitly accessible codec for it.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """abstract class X
          |case class A(i: Int) extends X
          |case object B extends X
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'X' or
          |provide a custom implicitly accessible codec for it.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for ADTs that have intermediate non-sealed traits or abstract classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |sealed abstract class AX extends X
          |abstract class BX extends X
          |case class A(i: Int) extends AX
          |case object B extends BX
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed intermediate traits or abstract classes are supported. Please consider using of them for ADT
          |with base 'X' or provide a custom implicitly accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |sealed trait AX extends X
          |trait BX extends X
          |case class A(i: Int) extends AX
          |case object B extends BX
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed intermediate traits or abstract classes are supported. Please consider using of them for ADT
          |with base 'X' or provide a custom implicitly accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for ADT bases without leaf classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X extends Product with Serializable
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please add them or provide a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed abstract class X extends Product with Serializable
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please add them or provide a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for case objects which are mapped to the same discriminator value" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X extends Product with Serializable
          |case object A extends X
          |case object B extends X
          |JsonCodecMaker.make[X](CodecMakerConfig.withAdtLeafClassNameMapper(_ => "Z"))""".stripMargin
      }).getMessage.contains {
        """Duplicated discriminator defined for ADT base 'X': 'Z'. Values for leaf classes of ADT that are returned by
          |the 'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.adtLeafClassNameMapper' function
          |should be unique.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait Data extends Product with Serializable
          |case class Data1(i: Int, s: String) extends Data
          |case object Data1 extends Data
          |val c = make[Data]""".stripMargin
      }).getMessage.contains {
        "Data1" //FIXME: an error message with Scala 2.11.12 is "Data1 is already defined as (compiler-generated) case class companion object Data1"
      })
    }
    "don't generate codecs for case classes with fields that the same name as discriminator name" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait DuplicatedJsonName extends Product with Serializable
          |case class A(x: Int) extends DuplicatedJsonName
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig.withDiscriminatorFieldName(Some("x")))""".stripMargin
      }).getMessage.contains {
        """Duplicated JSON key(s) defined for 'A': 'x'. Keys are derived from field names of the class that are mapped
          |by the 'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.fieldNameMapper' function or can be
          |overridden by 'com.github.plokhotnyuk.jsoniter_scala.macros.named' annotation(s). Result keys should be
          |unique and should not match with a key for the discriminator field that is specified by the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.discriminatorFieldName' option.""".stripMargin.replace('\n', ' ')
      })
    }
    "serialize and deserialize ADTs with self-recursive (aka F-bounded) types without discriminators" in {
      sealed trait Fruit[T <: Fruit[T]] extends Product with Serializable

      final case class Apple(family: String) extends Fruit[Apple]

      final case class Orange(color: Int) extends Fruit[Orange]

      case class Basket[T <: Fruit[T]](fruits: List[T])

      val oneFruit: Basket[Apple] = Basket(List(Apple("golden")))
      val twoFruits: Basket[Apple] = oneFruit.copy(fruits = oneFruit.fruits :+ Apple("red"))
      assert(intercept[TestFailedException](assertCompiles {
        """oneFruit.copy(fruits = oneFruit.fruits :+ Orange(0))"""
      }).getMessage.contains {
        """do not conform to method copy's type parameter bounds [T <: Fruit[T]]"""
      })
      verifySerDeser(make[Basket[Apple]], twoFruits,
        """{"fruits":[{"family":"golden"},{"family":"red"}]}""")
      verifySerDeser(make[Basket[Orange]], Basket(List(Orange(1), Orange(2))),
        """{"fruits":[{"color":1},{"color":2}]}""")
    }
    "serialize and deserialize when the root codec defined as an impicit val" in {
      implicit val implicitRootCodec: JsonValueCodec[Int] = make
      verifySerDeser(implicitRootCodec, 1, "1")
    }
    "serialize and deserialize dependent codecs which use lazy val to don't depend on order of definition" in {
      verifySerDeser(make[List[Int]], List(1, 2), "[\"1\",\"2\"]")
      implicit lazy val intCodec: JsonValueCodec[Int] = make(CodecMakerConfig.withIsStringified(true))
    }
    "serialize and deserialize case classes with Java time types" in {
      verifySerDeser(make[JavaTimeTypes],
        obj = JavaTimeTypes(
          dow = DayOfWeek.FRIDAY,
          d = Duration.parse("PT10H30M"),
          i = Instant.parse("2007-12-03T10:15:30.001Z"),
          ld = LocalDate.parse("2007-12-03"),
          ldt = LocalDateTime.parse("2007-12-03T10:15:30"),
          lt = LocalTime.parse("10:15:30"),
          m = Month.APRIL,
          md = MonthDay.parse("--12-03"),
          odt = OffsetDateTime.parse("2007-12-03T10:15:30+01:00"),
          ot = OffsetTime.parse("10:15:30+01:00"),
          p = Period.parse("P1Y2M25D"),
          y = Year.parse("2007"),
          ym = YearMonth.parse("2007-12"),
          zdt = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"),
          zi = ZoneId.of("Europe/Paris"),
          zo = ZoneOffset.of("+01:00")),
        json =
          """{"dow":"FRIDAY","d":"PT10H30M","i":"2007-12-03T10:15:30.001Z","ld":"2007-12-03","ldt":"2007-12-03T10:15:30",""" +
          """"lt":"10:15:30","m":"APRIL","md":"--12-03","odt":"2007-12-03T10:15:30+01:00","ot":"10:15:30+01:00",""" +
          """"p":"P1Y2M25D","y":"2007","ym":"2007-12","zdt":"2007-12-03T10:15:30+01:00[Europe/Paris]",""" +
          """"zi":"Europe/Paris","zo":"+01:00"}""")
    }
    "serialize and deserialize top-level Java time types" in {
      verifySerDeser(make[DayOfWeek], DayOfWeek.FRIDAY, "\"FRIDAY\"")
      verifySerDeser(make[Duration], Duration.parse("PT10H30M"), "\"PT10H30M\"")
      verifySerDeser(make[Instant],
        Instant.parse("2007-12-03T10:15:30.001Z"), "\"2007-12-03T10:15:30.001Z\"")
      verifySerDeser(make[LocalDate], LocalDate.parse("2007-12-03"), "\"2007-12-03\"")
      verifySerDeser(make[LocalDateTime],
        LocalDateTime.parse("2007-12-03T10:15:30"), "\"2007-12-03T10:15:30\"")
      verifySerDeser(make[LocalTime], LocalTime.parse("10:15:30"), "\"10:15:30\"")
      verifySerDeser(make[Month], Month.APRIL, "\"APRIL\"")
      verifySerDeser(make[MonthDay], MonthDay.parse("--12-03"), "\"--12-03\"")
      verifySerDeser(make[OffsetDateTime],
        OffsetDateTime.parse("2007-12-03T10:15:30+01:00"), "\"2007-12-03T10:15:30+01:00\"")
      verifySerDeser(make[OffsetTime], OffsetTime.parse("10:15:30+01:00"), "\"10:15:30+01:00\"")
      verifySerDeser(make[Period], Period.parse("P1Y2M25D"), "\"P1Y2M25D\"")
      verifySerDeser(make[Year], Year.parse("2007"), "\"2007\"")
      verifySerDeser(make[YearMonth], YearMonth.parse("2007-12"), "\"2007-12\"")
      verifySerDeser(make[ZonedDateTime],
        ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"), "\"2007-12-03T10:15:30+01:00[Europe/Paris]\"")
      verifySerDeser(make[ZoneId], ZoneId.of("Europe/Paris"), "\"Europe/Paris\"")
      verifySerDeser(make[ZoneOffset], ZoneOffset.of("+01:00"), "\"+01:00\"")
    }
    "serialize and deserialize Java time types as key in maps" in {
      verifySerDeser(make[Map[DayOfWeek, Int]],
        Map(DayOfWeek.FRIDAY -> 0), "{\"FRIDAY\":0}")
      verifySerDeser(make[Map[Duration, Int]],
        Map(Duration.parse("PT10H30M") -> 0), "{\"PT10H30M\":0}")
      verifySerDeser(make[Map[Instant, Int]],
        Map(Instant.parse("2007-12-03T10:15:30.001Z") -> 0), "{\"2007-12-03T10:15:30.001Z\":0}")
      verifySerDeser(make[Map[LocalDate, Int]],
        Map(LocalDate.parse("2007-12-03") -> 0), "{\"2007-12-03\":0}")
      verifySerDeser(make[Map[LocalDateTime, Int]],
        Map(LocalDateTime.parse("2007-12-03T10:15:30") -> 0), "{\"2007-12-03T10:15:30\":0}")
      verifySerDeser(make[Map[LocalTime, Int]],
        Map(LocalTime.parse("10:15:30") -> 0), "{\"10:15:30\":0}")
      verifySerDeser(make[Map[Month, Int]],
        Map(Month.APRIL -> 0), "{\"APRIL\":0}")
      verifySerDeser(make[Map[MonthDay, Int]],
        Map(MonthDay.parse("--12-03") -> 0), "{\"--12-03\":0}")
      verifySerDeser(make[Map[OffsetDateTime, Int]],
        Map(OffsetDateTime.parse("2007-12-03T10:15:30+01:00") -> 0), "{\"2007-12-03T10:15:30+01:00\":0}")
      verifySerDeser(make[Map[OffsetTime, Int]],
        Map(OffsetTime.parse("10:15:30+01:00") -> 0), "{\"10:15:30+01:00\":0}")
      verifySerDeser(make[Map[Period, Int]], Map(Period.parse("P1Y2M25D") -> 0), "{\"P1Y2M25D\":0}")
      verifySerDeser(make[Map[Year, Int]], Map(Year.parse("2007") -> 0), "{\"2007\":0}")
      verifySerDeser(make[Map[YearMonth, Int]],
        Map(YearMonth.parse("2007-12") -> 0), "{\"2007-12\":0}")
      verifySerDeser(make[Map[ZonedDateTime, Int]],
        Map(ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]") -> 0),
        "{\"2007-12-03T10:15:30+01:00[Europe/Paris]\":0}")
      verifySerDeser(make[Map[ZoneId, Int]],
        Map(ZoneId.of("Europe/Paris") -> 0), "{\"Europe/Paris\":0}")
      verifySerDeser(make[Map[ZoneOffset, Int]],
        Map(ZoneOffset.of("+01:00") -> 0), "{\"+01:00\":0}")
    }
    "serialize and deserialize case class with aliased typed methods" in {
      type I = Int
      type S = String
      type L = List[I]
      type M = Map[I, S]

      case class TypeAliases(i: I, s: S, l: L, m: M)

      verifySerDeser(make[TypeAliases], TypeAliases(1, "VVV", List(1, 2, 3), Map(1 -> "VVV")),
        """{"i":1,"s":"VVV","l":[1,2,3],"m":{"1":"VVV"}}""")
    }
    "serialize and deserialize collection with aliased type arguments" in {
      type I = Int
      type S = String

      verifySerDeser(make[Map[I, S]], Map(1 -> "VVV"), "{\"1\":\"VVV\"}")
    }
    "serialize and deserialize top-level aliased types" in {
      type I = Int
      type L = List[I]

      verifySerDeser(make[L], List(1, 2, 3), "[1,2,3]")
    }
    "serialize and deserialize first-order types" in {
      verifySerDeser(make[Array[Id[String]]], _root_.scala.Array[Id[String]](Id("1"), Id("2")),
        """["1","2"]""")
      verifySerDeser(make[Either[Int, String]](CodecMakerConfig.withFieldNameMapper {
        case "b" => "value"
        case "a" => "value"
      }), Right("VVV"), """{"type":"Right","value":"VVV"}""")

      case class FirstOrderType[A, B](a: A, b: B, oa: Option[A], bs: List[B])

      verifySerDeser(make[FirstOrderType[Int, String]],
        FirstOrderType[Int, String](1, "VVV", Some(1), List("WWW")),
        """{"a":1,"b":"VVV","oa":1,"bs":["WWW"]}""")
      verifySerDeser(make[FirstOrderType[Id[Int], Id[String]]],
        FirstOrderType[Id[Int], Id[String]](Id[Int](1), Id[String]("VVV"), Some(Id[Int](2)), List(Id[String]("WWW"))),
        """{"a":1,"b":"VVV","oa":2,"bs":["WWW"]}""")
    }
    "don't generate codecs for first-order types that are specified using 'Any' type parameter" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class FirstOrder[A](a: A)
          |JsonCodecMaker.make[FirstOrder[_]]""".stripMargin
      }).getMessage.contains {
        """Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'Any' or
          |provide a custom implicitly accessible codec for it.""".stripMargin.replace('\n', ' ')
      })
    }
    "serialize and deserialize arrays of generic types" in {
      sealed trait GADT[A] extends Product with Serializable

      case object Foo extends GADT[_root_.scala.Boolean]

      case object Bar extends GADT[_root_.scala.Unit]

      case object Baz extends GADT[Int]

      case object Qux extends GADT[String]

      verifySerDeser(make[Array[GADT[_]]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        _root_.scala.Array[GADT[_]](Foo, Bar, Baz, Qux), """["Foo","Bar","Baz","Qux"]""")

      sealed trait GADT2[A] extends Product with Serializable

      case class IsDir(path: String) extends GADT2[_root_.scala.Boolean]

      case class Exists(path: String) extends GADT2[_root_.scala.Boolean]

      case class ReadBytes(path: String) extends GADT2[_root_.scala.Array[_root_.scala.Byte]]

      case class CopyOver(src: Seq[_root_.scala.Byte], path: String) extends GADT2[Int]

      verifySerDeser(make[Array[GADT2[_]]],
        _root_.scala.Array[GADT2[_]](Exists("WWW"), ReadBytes("QQQ"), CopyOver("AAA".getBytes.toSeq, "OOO")),
        """[{"type":"Exists","path":"WWW"},{"type":"ReadBytes","path":"QQQ"},{"type":"CopyOver","src":[65,65,65],"path":"OOO"}]""")
    }
    "serialize and deserialize higher-kinded types" in {
      sealed trait Foo[A[_]] extends Product with Serializable

      case class Bar[A[_]](a: A[Int]) extends Foo[A]

      case class Baz[A[_]](a: A[String]) extends Foo[A]

      val codecOfFooForOption = make[Foo[Option]]
      verifySerDeser(codecOfFooForOption, Bar[Option](Some(1)), """{"type":"Bar","a":1}""")
      verifySerDeser(codecOfFooForOption, Baz[Option](Some("VVV")), """{"type":"Baz","a":"VVV"}""")
    }
    "serialize and deserialize case classes with an auxiliary constructor using primary one" in {
      case class AuxiliaryConstructor(i: Int, s: String = "") {
        def this(s: String) = this(0, s)
      }

      val codecOfAuxiliaryConstructor = make[AuxiliaryConstructor]
      verifySerDeser(codecOfAuxiliaryConstructor, new AuxiliaryConstructor("VVV"),"{\"i\":0,\"s\":\"VVV\"}")
      verifySerDeser(codecOfAuxiliaryConstructor, AuxiliaryConstructor(1),"{\"i\":1}")
    }
    "serialize and deserialize case classes with private primary constructor if it can be accessed" in {
      object PrivatePrimaryConstructor {
        implicit val codec: JsonValueCodec[PrivatePrimaryConstructor] = make

        def apply(s: String) = new PrivatePrimaryConstructor(s)
      }

      case class PrivatePrimaryConstructor private(i: Int) {
        def this(s: String) = this(s.toInt)
      }

      verifySerDeser(PrivatePrimaryConstructor.codec, PrivatePrimaryConstructor("1"), "{\"i\":1}")
    }
    "don't generate codecs for classes without a primary constructor" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[_root_.scala.concurrent.duration.Duration]"
      }).getMessage.contains {
        "Cannot find a primary constructor for 'Infinite.this.<local child>'"
      })
    }
    "don't generate codecs for case classes with multiple parameter lists in a primary constructor" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class MultiListOfArgs(i: Int)(l: Long)
          |JsonCodecMaker.make[MultiListOfArgs]""".stripMargin
      }).getMessage.contains {
        """'MultiListOfArgs' has a primary constructor with multiple parameter lists.
          |Please consider using a custom implicitly accessible codec for this type.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for classes with parameters in a primary constructor that have no accessor for read" in {
      assert(intercept[TestFailedException](assertCompiles {
        """class ParamHasNoAccessor(val i: Int, a: String)
          |JsonCodecMaker.make[ParamHasNoAccessor]""".stripMargin
      }).getMessage.contains {
        """'a' parameter of 'ParamHasNoAccessor' should be defined as 'val' or 'var' in the primary constructor."""
      })
    }
    "don't generate codecs when a parameter of the 'make' call depends on not yet compiled code" in {
      assert(intercept[TestFailedException](assertCompiles {
        """object A {
          |  def f(fullClassName: String): String = fullClassName.split('.').head.charAt(0).toString
          |  case class B(i: Int)
          |  implicit val c = JsonCodecMaker.make[B](CodecMakerConfig.withAdtLeafClassNameMapper(f))
          |}""".stripMargin
      }).getMessage.contains {
        """Cannot evaluate a parameter of the 'make' macro call for type
          |'com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.A.B'.
          |It should not depend on code from the same compilation module where the 'make' macro is called.
          |Use a separated submodule of the project to compile all such dependencies before their usage for
          |generation of codecs. Cause:""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs when a parameter of the '@named' annotation depends on not yet compiled code" in {
      assert(intercept[TestFailedException](assertCompiles {
        """object A {
          |  def f(x: String): String = x
          |  case class B(@named(f("XXX")) i: Int)
          |  implicit val c = JsonCodecMaker.make[B]
          |}""".stripMargin
      }).getMessage.contains {
        """Cannot evaluate a parameter of the '@named' annotation in type
          |'com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.A.B'.
          |It should not depend on code from the same compilation module where the 'make' macro is called.
          |Use a separated submodule of the project to compile all such dependencies before their usage for
          |generation of codecs. Cause:""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs when all generic type parameters cannot be resolved" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait Foo[F[_]] extends Product with Serializable
          |case class FooImpl[F[_], A](fa: F[A], as: Vector[A]) extends Foo[F]
          |sealed trait Bar[A] extends Product with Serializable
          |case object Baz extends Bar[Int]
          |case object Qux extends Bar[String]
          |val v = FooImpl[Bar, String](Qux, Vector.empty[String])
          |val c = make[Foo[Bar]]""".stripMargin
      }).getMessage.contains {
        "Cannot resolve generic type(s) for `FooImpl[F,A]`. Please provide a custom implicitly accessible codec for it."
      })
    }
    "don't generate codecs that cannot parse own output" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[Arrays](CodecMakerConfig.withRequireCollectionFields(true))"
      }).getMessage.contains {
        "'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously"
      })
    }
    "don't generate codecs for unsupported classes like java.util.Date" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[_root_.java.util.Date]"
      }).getMessage.contains {
        "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_]' defined for 'java.util.Date'."
      })
    }
  }
  "deserialize using the nullValue of codecs that are injected by implicits" in {
    import test._

    verifyDeser(C3.codec, C3(List((C1("A"), C2("0")))),"""{"member":[["A","0"]]}""")
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
    "transform PascalCase names to camelCase" in {
      JsonCodecMaker.enforceCamelCase("OoOo111") shouldBe "ooOo111"
      JsonCodecMaker.enforceCamelCase("OOoo111") shouldBe "oOoo111"
    }
    "leave camelCase names as is" in {
      JsonCodecMaker.enforceCamelCase("") shouldBe ""
      JsonCodecMaker.enforceCamelCase("o") shouldBe "o"
      JsonCodecMaker.enforceCamelCase("oO") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("oOoo") shouldBe "oOoo"
    }
  }
  "JsonCodecMaker.EnforcePascalCase" should {
    "transform snake_case names to PascalCase" in {
      JsonCodecMaker.EnforcePascalCase("o_o") shouldBe "OO"
      JsonCodecMaker.EnforcePascalCase("o_ooo_") shouldBe "OOoo"
      JsonCodecMaker.EnforcePascalCase("OO_OOO_111") shouldBe "OoOoo111"
      JsonCodecMaker.EnforcePascalCase("ooo_111") shouldBe "Ooo111"
    }
    "transform kebab-case names to PascalCase" in {
      JsonCodecMaker.EnforcePascalCase("o-o") shouldBe "OO"
      JsonCodecMaker.EnforcePascalCase("o-ooo-") shouldBe "OOoo"
      JsonCodecMaker.EnforcePascalCase("O-OOO-111") shouldBe "OOoo111"
    }
    "transform camelCase names to PascalCase" in {
      JsonCodecMaker.EnforcePascalCase("ooOo111") shouldBe "OoOo111"
      JsonCodecMaker.EnforcePascalCase("oOoo111") shouldBe "OOoo111"
    }
    "leave PascalCase names as is" in {
      JsonCodecMaker.EnforcePascalCase("") shouldBe ""
      JsonCodecMaker.EnforcePascalCase("O") shouldBe "O"
      JsonCodecMaker.EnforcePascalCase("Oo") shouldBe "Oo"
      JsonCodecMaker.EnforcePascalCase("OOoo") shouldBe "OOoo"
    }
  }
  "JsonCodecMaker.enforce_snake_case" should {
    "transform camelCase names to snake_case" in {
      JsonCodecMaker.enforce_snake_case("oO") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("oOoo") shouldBe "o_ooo"
    }
    "transform PascalCase names to snake_case" in {
      JsonCodecMaker.enforce_snake_case("OOOoo111") shouldBe "oo_ooo_111"
      JsonCodecMaker.enforce_snake_case("Ooo111") shouldBe "ooo_111"
    }
    "transform kebab-case names to snake_case" in {
      JsonCodecMaker.enforce_snake_case("o-O") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o-ooo-") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case("O-OOO-111") shouldBe "o_ooo_111"
    }
    "leave snake_case as is" in {
      JsonCodecMaker.enforce_snake_case("") shouldBe ""
      JsonCodecMaker.enforce_snake_case("o") shouldBe "o"
      JsonCodecMaker.enforce_snake_case("o_o") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o_ooo_") shouldBe "o_ooo_"
    }
  }
  "JsonCodecMaker.enforce-kebab-case" should {
    "transform camelCase names to kebab-case" in {
      JsonCodecMaker.`enforce-kebab-case`("oO") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("oOoo") shouldBe "o-ooo"
    }
    "transform PascalCase names to kebab-case" in {
      JsonCodecMaker.`enforce-kebab-case`("OOOoo111") shouldBe "oo-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("Ooo111") shouldBe "ooo-111"
    }
    "transform snake_case names to kebab-case" in {
      JsonCodecMaker.`enforce-kebab-case`("o_O") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("o_ooo_") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case`("O_OOO_111") shouldBe "o-ooo-111"
    }
    "leave kebab-case names as is" in {
      JsonCodecMaker.`enforce-kebab-case`("") shouldBe ""
      JsonCodecMaker.`enforce-kebab-case`("o") shouldBe "o"
      JsonCodecMaker.`enforce-kebab-case`("o-o") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("o-ooo-") shouldBe "o-ooo-"
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
}

case class NameOverridden(@named("new" + '_' + "Name".toLowerCase) oldName: String) // intentionally declared after the `make` call

package object test {
  implicit def tup2[A: JsonValueCodec, B: JsonValueCodec]: JsonValueCodec[(A, B)] = make

  case class C1(s: String) extends AnyVal

  object C1 {
    implicit val codec: JsonValueCodec[C1] = make
  }

  case class C2(s: String) extends AnyVal

  object C2 {
    implicit val codec: JsonValueCodec[C2] = make
  }

  case class C3(member: Seq[(C1, C2)])

  object C3 {
    implicit val codec: JsonValueCodec[C3] = make
  }
}
