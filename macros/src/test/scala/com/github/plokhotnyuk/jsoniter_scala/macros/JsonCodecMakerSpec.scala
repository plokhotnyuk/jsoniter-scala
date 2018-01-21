package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import org.scalatest.{Matchers, WordSpec}

import scala.annotation.switch
import scala.collection.immutable._
import scala.collection.mutable

case class UserId(id: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

class JsonCodecMakerSpec extends WordSpec with Matchers {
  case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

  val primitives = Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f)
  val codecOfPrimitives: JsonCodec[Primitives] = make[Primitives](CodecMakerConfig())

  case class BoxedPrimitives(b: java.lang.Byte, s: java.lang.Short, i: java.lang.Integer, l: java.lang.Long,
                             bl: java.lang.Boolean, ch: java.lang.Character, dbl: java.lang.Double, f: java.lang.Float)

  case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

  val standardTypes = StandardTypes("VVV", 1, 1.1)
  val codecOfStandardTypes: JsonCodec[StandardTypes] = make[StandardTypes](CodecMakerConfig())

  object LocationType extends Enumeration {
    type LocationType = Value
    val GPS: LocationType = Value(1)
    val IP: LocationType = Value(2)
    val UserProvided: LocationType = Value(3)
  }

  case class Enums(lt: LocationType.LocationType)

  val codecOfEnums: JsonCodec[Enums] = make[Enums](CodecMakerConfig())

  case class OuterTypes(s: String, st: Either[String, StandardTypes] = Left("error"))

  case class ValueClassTypes(uid: UserId, oid: OrderId)

  case class Options(os: Option[String], obi: Option[BigInt], osi: Option[Set[Int]])

  case class Tuples(t1: (Int, Double, List[Char]), t2: (String, BigInt, Option[LocationType.LocationType]))

  val codecOfTuples: JsonCodec[Tuples] = make[Tuples](CodecMakerConfig())

  case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

  val arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array[BigInt](7))
  val codecOfArrays: JsonCodec[Arrays] = make[Arrays](CodecMakerConfig())

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

  val codecOfImmutableTraversables: JsonCodec[ImmutableTraversables] = make[ImmutableTraversables](CodecMakerConfig())

  case class MutableMaps(hm: mutable.HashMap[Boolean, mutable.AnyRefMap[BigDecimal, Int]],
                         m: mutable.Map[Float, mutable.ListMap[BigInt, String]],
                         ohm: mutable.OpenHashMap[Double, mutable.LinkedHashMap[Short, Double]])

  val codecOfMutableMaps: JsonCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())

  case class ImmutableMaps(m: Map[Int, Double], hm: HashMap[String, ListMap[Char, BigInt]],
                           sm: SortedMap[Long, TreeMap[Byte, Float]])

  val codecOfImmutableMaps: JsonCodec[ImmutableMaps] = make[ImmutableMaps](CodecMakerConfig())

  case class MutableLongMaps(lm1: mutable.LongMap[Double], lm2: mutable.LongMap[String])

  case class ImmutableIntLongMaps(im: IntMap[Double], lm: LongMap[String])

  case class BitSets(bs: BitSet, mbs: mutable.BitSet)

  case class CamelAndSnakeCases(camelCase: String, snake_case: String, `camel1`: String, `snake_1`: String)

  case class Indented(s: String, bd: BigDecimal, l: List[Int], m: Map[Char, Double])

  val indented = Indented("VVV", 1.1, List(1, 2, 3), Map('S' -> -90.0, 'N' -> 90.0, 'W' -> -180.0, 'E' -> 180.0))
  val codecOfIndented: JsonCodec[Indented] = make[Indented](CodecMakerConfig())

  case class UTF8KeysAndValues(გასაღები: String)

  val codecOfUTF8KeysAndValues: JsonCodec[UTF8KeysAndValues] = make[UTF8KeysAndValues](CodecMakerConfig())

  //FIXME: classes with field annotation(s) should be defined in source file before `make` call
  case class NameOverridden(@named("new_name") oldName: String)

  val codecOfNameOverridden: JsonCodec[NameOverridden] = make[NameOverridden](CodecMakerConfig())

  case class Stringified(@stringified i: Int, @stringified bi: BigInt, @stringified l1: List[Int], l2: List[Int])

  val stringified = Stringified(1, 2, List(1), List(2))
  val codecOfStringified: JsonCodec[Stringified] = make[Stringified](CodecMakerConfig())

  case class Defaults(s: String = "VVV", i: Int = 1, bi: BigInt = -1, oc: Option[Char] = Some('X'),
                      l: List[Int] = List(0), a: Array[Array[Double]] = Array(Array(-1.0, 0.0), Array(1.0)),
                      ml: mutable.LongMap[Int] = null, mm: mutable.Map[Int, Int] = null,
                      ms: mutable.BitSet = null, mb: mutable.Buffer[Int] = null)

  val defaults = Defaults()
  val codecOfDefaults: JsonCodec[Defaults] = make[Defaults](CodecMakerConfig())

  case class Transient(@transient transient: String = "default", required: String) {
    val ignored: String = s"$required-$transient"
  }

  case class NullAndNoneValues(str: String, bi: BigInt, bd: BigDecimal, lt: LocationType.LocationType,
                               nv: NullAndNoneValues, opt: Option[String])

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

  sealed abstract class База

  case class А(б: Б) extends База

  case class Б(а: А) extends База

  val codecOfADTList: JsonCodec[List[AdtBase]] = make[List[AdtBase]](CodecMakerConfig())

  "JsonCodec" should {
    "serialize and deserialize case classes with primitives" in {
      verifySerDeser(codecOfPrimitives, primitives,
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""".getBytes)
    }
    "serialize and deserialize top-level primitives" in {
      verifySerDeser(make[Byte](CodecMakerConfig()), 1.toByte, "1".getBytes)
      verifySerDeser(make[Short](CodecMakerConfig()), 2.toShort, "2".getBytes)
      verifySerDeser(make[Int](CodecMakerConfig()), 3, "3".getBytes)
      verifySerDeser(make[Long](CodecMakerConfig()), 4L, "4".getBytes)
      verifySerDeser(make[Boolean](CodecMakerConfig()), true, "true".getBytes)
      verifySerDeser(make[Char](CodecMakerConfig()), 'V', "\"V\"".getBytes)
      verifySerDeser(make[Double](CodecMakerConfig()), 1.1, "1.1".getBytes)
      verifySerDeser(make[Float](CodecMakerConfig()), 2.2f, "2.2".getBytes)
    }
    "serialize and deserialize stringified top-level primitives" in {
      verifySerDeser(make[Byte](CodecMakerConfig(isStringified = true)), 1.toByte, "\"1\"".getBytes)
      verifySerDeser(make[Short](CodecMakerConfig(isStringified = true)), 2.toShort, "\"2\"".getBytes)
      verifySerDeser(make[Int](CodecMakerConfig(isStringified = true)), 3, "\"3\"".getBytes)
      verifySerDeser(make[Long](CodecMakerConfig(isStringified = true)), 4L, "\"4\"".getBytes)
      verifySerDeser(make[Boolean](CodecMakerConfig(isStringified = true)), true, "\"true\"".getBytes)
      verifySerDeser(make[Double](CodecMakerConfig(isStringified = true)), 1.1, "\"1.1\"".getBytes)
      verifySerDeser(make[Float](CodecMakerConfig(isStringified = true)), 2.2f, "\"2.2\"".getBytes)
    }
    "throw parse exception with hex dump in case of illegal input" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfPrimitives, primitives,
          """{"b":-128,"s":-32768,"i":-2147483648,"l":-9223372036854775808,'bl':true,"ch":"V","dbl":-123456789.0,"f":-12345.0}""".getBytes)
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
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""".getBytes)
    }
    "serialize and deserialize top-level boxed primitives" in {
      verifySerDeser(make[java.lang.Byte](CodecMakerConfig()), java.lang.Byte.valueOf(1.toByte), "1".getBytes)
      verifySerDeser(make[java.lang.Short](CodecMakerConfig()), java.lang.Short.valueOf(2.toShort), "2".getBytes)
      verifySerDeser(make[java.lang.Integer](CodecMakerConfig()), java.lang.Integer.valueOf(3), "3".getBytes)
      verifySerDeser(make[java.lang.Long](CodecMakerConfig()), java.lang.Long.valueOf(4L), "4".getBytes)
      verifySerDeser(make[java.lang.Boolean](CodecMakerConfig()), java.lang.Boolean.valueOf(true), "true".getBytes)
      verifySerDeser(make[java.lang.Character](CodecMakerConfig()), java.lang.Character.valueOf('V'), "\"V\"".getBytes)
      verifySerDeser(make[java.lang.Double](CodecMakerConfig()), java.lang.Double.valueOf(1.1), "1.1".getBytes)
      verifySerDeser(make[java.lang.Float](CodecMakerConfig()), java.lang.Float.valueOf(2.2f), "2.2".getBytes)
    }
    "serialize and deserialize stringifeid top-level boxed primitives" in {
      verifySerDeser(make[java.lang.Byte](CodecMakerConfig(isStringified = true)),
        java.lang.Byte.valueOf(1.toByte), "\"1\"".getBytes)
      verifySerDeser(make[java.lang.Short](CodecMakerConfig(isStringified = true)),
        java.lang.Short.valueOf(2.toShort), "\"2\"".getBytes)
      verifySerDeser(make[java.lang.Integer](CodecMakerConfig(isStringified = true)),
        java.lang.Integer.valueOf(3), "\"3\"".getBytes)
      verifySerDeser(make[java.lang.Long](CodecMakerConfig(isStringified = true)),
        java.lang.Long.valueOf(4L), "\"4\"".getBytes)
      verifySerDeser(make[java.lang.Boolean](CodecMakerConfig(isStringified = true)),
        java.lang.Boolean.valueOf(true), "\"true\"".getBytes)
      verifySerDeser(make[java.lang.Double](CodecMakerConfig(isStringified = true)),
        java.lang.Double.valueOf(1.1), "\"1.1\"".getBytes)
      verifySerDeser(make[java.lang.Float](CodecMakerConfig(isStringified = true)),
        java.lang.Float.valueOf(2.2f), "\"2.2\"".getBytes)
    }
    "serialize and deserialize case classes with standard types" in {
      val text =
        "JavaScript Object Notation (JSON) is a lightweight, text-based, language-independent data interchange format."
      verifySerDeser(codecOfStandardTypes,
        StandardTypes(text, BigInt("123456789012345678901234567890"), BigDecimal("1234567890.12345678901234567890")),
        s"""{"s":"$text","bi":123456789012345678901234567890,"bd":1234567890.12345678901234567890}""".getBytes)
      verifySerDeser(codecOfStandardTypes, null, "null".getBytes)
    }
    "throw parse exception in case of illegal value for case classes" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, null, "nux".getBytes)
      }.getMessage.contains("unexpected end of input, offset: 0x00000003"))
    }
    "serialize and deserialize top-level standard types" in {
      val text =
        "JavaScript Object Notation (JSON) is a lightweight, text-based, language-independent data interchange format."
      val codecOfString = make[String](CodecMakerConfig())
      verifySerDeser(codecOfString, text, s""""$text"""".getBytes)
      verifySerDeser(codecOfString, null, "null".getBytes)
      val codecOfBigInt = make[BigInt](CodecMakerConfig())
      verifySerDeser(codecOfBigInt, BigInt("123456789012345678901234567890"),
        "123456789012345678901234567890".getBytes)
      verifySerDeser(codecOfBigInt, null, "null".getBytes)
      val codecOfBigDecimal = make[BigDecimal](CodecMakerConfig())
      verifySerDeser(codecOfBigDecimal, BigDecimal("1234567890.12345678901234567890"),
        "1234567890.12345678901234567890".getBytes)
      verifySerDeser(codecOfBigDecimal, null, "null".getBytes)
    }
    "serialize and deserialize stringified top-level standard types" in {
      val codecOfBigInt = make[BigInt](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfBigInt,
        BigInt("123456789012345678901234567890"), "\"123456789012345678901234567890\"".getBytes)
      verifySerDeser(codecOfBigInt, null, "null".getBytes)
      val codecOfBigDecimal = make[BigDecimal](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfBigDecimal,
        BigDecimal("1234567890.12345678901234567890"), "\"1234567890.12345678901234567890\"".getBytes)
      verifySerDeser(codecOfBigDecimal, null, "null".getBytes)
    }
    "throw parse exception in case of illegal UTF-8 encoded field names" in {
      assert(intercept[JsonParseException] {
        val buf = """{"s":"VVV","bi":1,"bd":1.1}""".getBytes
        buf(2) = 0xF0.toByte
        verifyDeser(codecOfStandardTypes, standardTypes, buf)
      }.getMessage.contains("malformed byte(s): 0xf0, 0x22, 0x3a, 0x22, offset: 0x00000005"))
    }
    "throw parse exception in case of illegal JSON escaped field names" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, "{\"\\udd1e\":\"VVV\",\"bi\":1,\"bd\":1.1}".getBytes)
      }.getMessage.contains("illegal escape sequence, offset: 0x00000008"))
    }
    "throw parse exception in case of missing or illegal tokens" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """"s":"VVV","bi":1,"bd":1.1}""".getBytes)
      }.getMessage.contains("expected '{' or null, offset: 0x00000000"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s""VVV","bi":1,"bd":1.1}""".getBytes)
      }.getMessage.contains("expected ':', offset: 0x00000004"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s":"VVV""bi":1"bd":1.1}""".getBytes)
      }.getMessage.contains("expected '}' or ',', offset: 0x0000000a"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """["s":"VVV","bi":1,"bd":2}""".getBytes)
      }.getMessage.contains("expected '{' or null, offset: 0x00000000"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{,"s":"VVV","bi":1,"bd":2}""".getBytes)
      }.getMessage.contains("expected '\"', offset: 0x00000001"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s":"VVV","bi":1,"bd":2]""".getBytes)
      }.getMessage.contains("expected '}' or ',', offset: 0x00000018"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStandardTypes, standardTypes, """{"s":"VVV","bi":1,"bd":2,}""".getBytes)
      }.getMessage.contains("expected '\"', offset: 0x00000019"))
    }
    "serialize and deserialize enumerations" in {
      verifySerDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":"GPS"}""".getBytes)
    }
    "throw parse exception in case of illegal value of enumeration" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":"Galileo"}""".getBytes)
      }.getMessage.contains("illegal enum value: \"Galileo\", offset: 0x0000000e"))
    }
    "serialize and deserialize outer types using custom codecs for inner types" in {
      implicit val codecForEither = new JsonCodec[Either[String, StandardTypes]] {
        val nullValue: Either[String, StandardTypes] = null

        def decode(in: JsonReader, default: Either[String, StandardTypes]): Either[String, StandardTypes] =
          (in.nextToken(): @switch) match {
            case '{' =>
              in.rollbackToken()
              Right(codecOfStandardTypes.decode(in, codecOfStandardTypes.nullValue))
            case '"' =>
              in.rollbackToken()
              Left(in.readString())
            case _ =>
              in.decodeError("expected '{' or '\"'")
          }

        def encode(x: Either[String, StandardTypes], out: JsonWriter): Unit =
          x match {
            case Left(s) => out.writeVal(s)
            case Right(st) => codecOfStandardTypes.encode(st, out)
          }
      }
      val codecOfOuterTypes = make[OuterTypes](CodecMakerConfig())
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Right(standardTypes)),
        """{"s":"X","st":{"s":"VVV","bi":1,"bd":1.1}}""".getBytes)
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("fatal error")),
        """{"s":"X","st":"fatal error"}""".getBytes)
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("error")), // st matches with default value
        """{"s":"X"}""".getBytes)
      verifySerDeser(codecOfOuterTypes, OuterTypes("X"),
        """{"s":"X"}""".getBytes)
      implicit object codecOfLocationType extends JsonCodec[LocationType.LocationType] {
        val nullValue: LocationType.LocationType = null

        def decode(in: JsonReader, default: LocationType.LocationType): LocationType.LocationType =
          if (in.isNextToken('n')) {
            in.rollbackToken()
            in.readNullOrError(default, "expected number value or null")
          } else {
            in.rollbackToken()
            val v = in.readInt()
            try LocationType.apply(v) catch {
              case _: NoSuchElementException => in.decodeError("invalid enum value: " + v)
            }
          }

        def encode(x: LocationType.LocationType, out: JsonWriter): Unit =
          if (x ne null) out.writeVal(x.id) else out.writeNull()
      }
      val codecOfEnums = make[Enums](CodecMakerConfig())
      verifySerDeser(codecOfEnums, Enums(LocationType.GPS), """{"lt":1}""".getBytes)
      verifySerDeser(codecOfEnums, Enums(null), """{"lt":null}""".getBytes)
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfEnums, Enums(null), """{"lt":none}""".getBytes)
      }.getMessage.contains("expected number value or null, offset: 0x00000007"))
    }
    "serialize and deserialize case classes with value classes" in {
      verifySerDeser(make[ValueClassTypes](CodecMakerConfig()),
        ValueClassTypes(UserId("123abc"), OrderId(123123)),
        """{"uid":"123abc","oid":123123}""".getBytes)
    }
    "serialize and deserialize top-level value classes" in {
      val codecOfUserId = make[UserId](CodecMakerConfig())
      verifySerDeser(codecOfUserId, UserId("123abc"), "\"123abc\"".getBytes)
      verifySerDeser(codecOfUserId, UserId(null), "null".getBytes)
      verifySerDeser(make[OrderId](CodecMakerConfig()), OrderId(123123), "123123".getBytes)
    }
    "serialize and deserialize strinfigied top-level value classes" in {
      verifySerDeser(make[OrderId](CodecMakerConfig(isStringified = true)), OrderId(123123), "\"123123\"".getBytes)
    }
    "serialize and deserialize case classes with options" in {
      verifySerDeser(make[Options](CodecMakerConfig()),
        Options(Option("VVV"), Option(BigInt(4)), Option(Set())),
        """{"os":"VVV","obi":4,"osi":[]}""".getBytes)
    }
    "serialize and deserialize top-level options" in {
      val codecOfStringOption = make[Option[String]](CodecMakerConfig())
      verifySerDeser(codecOfStringOption, Some("VVV"), "\"VVV\"".getBytes)
      verifySerDeser(codecOfStringOption, None, "null".getBytes)
      verifySer(codecOfStringOption, null, "null".getBytes)
    }
    "serialize and deserialize stringified top-level numeric options" in {
      val codecOfStringifiedOption = make[Option[BigInt]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfStringifiedOption, Some(BigInt(123)), "\"123\"".getBytes)
      verifySerDeser(codecOfStringifiedOption, None, "null".getBytes)
      verifySer(codecOfStringifiedOption, null, "null".getBytes)
    }
    "serialize and deserialize case classes with tuples" in {
      verifySerDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
        """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS"]}""".getBytes)
    }
    "throw parse exception in case of unexpected number of JSON array values" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
          """{"t1":[1,2.2],"t2":["VVV",3,"GPS"]}""".getBytes)
      }.getMessage.contains("expected ',', offset: 0x0000000c"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, Some(LocationType.GPS))),
          """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS","XXX"]}""".getBytes)
      }.getMessage.contains("expected ']', offset: 0x00000027"))
    }
    "serialize and deserialize top-level tuples" in {
      val codecOfTuple2 = make[(String, Int)](CodecMakerConfig())
      verifySerDeser(codecOfTuple2, ("VVV", 1), "[\"VVV\",1]".getBytes)
      verifySer(codecOfTuple2, null, "null".getBytes)
    }
    "serialize and deserialize stringified top-level numeric tuples" in {
      val codecOfStringifiedTuple = make[(Long, Float, BigDecimal)](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfStringifiedTuple, (1L, 2.2f, BigDecimal(3.3)), "[\"1\",\"2.2\",\"3.3\"]".getBytes)
      verifySer(codecOfStringifiedTuple, null, "null".getBytes)
    }
    "serialize and deserialize case classes with arrays" in {
      val json = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}""".getBytes
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = JsonReader.read(codecOfArrays, json)
      parsedObj.aa.deep shouldBe arrays.aa.deep
      parsedObj.a.deep shouldBe arrays.a.deep
    }
    "serialize and deserialize top-level arrays" in {
      val json = """[[1,2,3],[4,5,6]]""".getBytes
      val arrayOfArray = Array(Array(1, 2, 3), Array(4, 5, 6))
      val codecOfArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig())
      verifySer(codecOfArrayOfArray, arrayOfArray, json)
      verifySer(codecOfArrayOfArray, null, "null".getBytes)
      val parsedObj = JsonReader.read(codecOfArrayOfArray, json)
      parsedObj.deep shouldBe arrayOfArray.deep
    }
    "serialize and deserialize stringified top-level arrays" in {
      val json = """[["1","2","3"],["4","5","6"]]""".getBytes
      val arrayOfArray = Array(Array(1, 2, 3), Array(4, 5, 6))
      val codecOfStringifiedArrayOfArray = make[Array[Array[Int]]](CodecMakerConfig(isStringified = true))
      verifySer(codecOfStringifiedArrayOfArray, arrayOfArray, json)
      verifySer(codecOfStringifiedArrayOfArray, null, "null".getBytes)
      val parsedObj = JsonReader.read(codecOfStringifiedArrayOfArray, json)
      parsedObj.deep shouldBe arrayOfArray.deep
    }
    "do not serialize fields of case classes with empty arrays" in {
      val json = """{"aa":[[],[]]}""".getBytes
      val arrays = Arrays(Array(Array(), Array()), Array())
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = JsonReader.read(codecOfArrays, json)
      parsedObj.aa.deep shouldBe arrays.aa.deep
      parsedObj.a.deep shouldBe arrays.a.deep
    }
    "throw parse exception in case of JSON array is not properly started/closed or with leading/trailing comma" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[{1,2,3]],"a":[]}""".getBytes)
      }.getMessage.contains("expected '[' or null, offset: 0x00000007"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[[,1,2,3]],"a":[]}""".getBytes)
      }.getMessage.contains("illegal number, offset: 0x00000008"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[[1,2,3}],"a":[]}""".getBytes)
      }.getMessage.contains("expected ']' or ',', offset: 0x0000000d"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfArrays, arrays, """{"aa":[[1,2,3,]],"a":[]}""".getBytes)
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
        """{"ml":[["1","2","3"]],"ab":[[4],[]],"as":[[5,6],[]],"b":[[8.8,7.7]],"lb":[[9,10]],"is":[[11.11,12.12]],"ub":[[13,14]],"ls":[15,16],"ra":[[17.17,18.18]]}""".getBytes)
    }
    "serialize and deserialize case classes with immutable traversables" in {
      verifySerDeser(codecOfImmutableTraversables,
        ImmutableTraversables(List(ListSet("1")), Queue(Set[BigInt](4, 5, 6)),
          IndexedSeq(SortedSet(7, 8), SortedSet()), Stream(TreeSet(9.9)), Vector(Traversable(10L, 11L))),
        """{"l":[["1"]],"q":[[4,5,6]],"is":[[7,8],[]],"s":[[9.9]],"v":[[10,11]]}""".getBytes)
    }
    "serialize and deserialize top-level traversables" in {
      val codecOfImmutableTraversables = make[mutable.Set[List[BigDecimal]]](CodecMakerConfig())
      verifySerDeser(codecOfImmutableTraversables,
        mutable.Set(List[BigDecimal](1.1, 2.2), List[BigDecimal](3.3)),
        """[[3.3],[1.1,2.2]]""".getBytes)
      verifySer(codecOfImmutableTraversables, null, "null".getBytes)
    }
    "serialize and deserialize stringified top-level traversables" in {
      val codecOfImmutableTraversables = make[mutable.Set[List[BigDecimal]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfImmutableTraversables,
        mutable.Set(List[BigDecimal](1.1, 2.2), List[BigDecimal](3.3)),
        """[["3.3"],["1.1","2.2"]]""".getBytes)
      verifySer(codecOfImmutableTraversables, null, "null".getBytes)
    }
    "serialize and deserialize case classes with mutable maps" in {
      verifySerDeser(codecOfMutableMaps,
        MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
          mutable.Map(1.1f -> mutable.ListMap(BigInt(2) -> "2")),
          mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
        """{"hm":{"true":{"1.1":1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes)
    }
    "serialize and deserialize case classes with immutable maps" in {
      verifySerDeser(make[ImmutableMaps](CodecMakerConfig()),
        ImmutableMaps(Map(1 -> 1.1), HashMap("2" -> ListMap('V' -> 2), "3" -> ListMap('X' -> 3)),
          SortedMap(4L -> TreeMap(4.toByte -> 4.4f), 5L -> TreeMap.empty[Byte, Float])),
        """{"m":{"1":1.1},"hm":{"2":{"V":2},"3":{"X":3}},"sm":{"4":{"4":4.4},"5":{}}}""".getBytes)
    }
    "serialize and deserialize top-level maps" in {
      val codecOfMaps = make[mutable.LinkedHashMap[Int, Map[Char, Boolean]]](CodecMakerConfig())
      verifySerDeser(codecOfMaps,
        mutable.LinkedHashMap(1 -> Map('V' -> true, 'X' -> false), 2 -> Map.empty[Char, Boolean]),
        """{"1":{"V":true,"X":false},"2":{}}""".getBytes)
      verifySer(codecOfMaps, null, "null".getBytes)
    }
    "serialize and deserialize stringified top-level maps" in {
      val codecOfMaps = make[mutable.LinkedHashMap[Int, Map[Char, Boolean]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfMaps,
        mutable.LinkedHashMap(1 -> Map('V' -> true, 'X' -> false), 2 -> Map.empty[Char, Boolean]),
        """{"1":{"V":"true","X":"false"},"2":{}}""".getBytes)
      verifySer(codecOfMaps, null, "null".getBytes)
    }
    "throw parse exception in case of JSON object is not properly started/closed or with leading/trailing comma" in {
      val immutableMaps = ImmutableMaps(Map(1 -> 1.1), HashMap.empty, SortedMap.empty)
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":["1":1.1},"hm":{},"sm":{}}""".getBytes)
      }.getMessage.contains("expected '{' or null, offset: 0x00000005"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":{,"1":1.1},"hm":{},"sm":{}}""".getBytes)
      }.getMessage.contains("expected '\"', offset: 0x00000006"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":{"1":1.1],"hm":{},"sm":{}""".getBytes)
      }.getMessage.contains("expected '}' or ',', offset: 0x0000000d"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfImmutableMaps, immutableMaps, """{"m":{"1":1.1,},"hm":{},"sm":{}""".getBytes)
      }.getMessage.contains("expected '\"', offset: 0x0000000e"))
    }
    "throw parse exception in case of illegal keys found during deserialization of maps" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfMutableMaps,
          MutableMaps(null, mutable.Map(1.1f -> mutable.ListMap(null.asInstanceOf[BigInt] -> "2")), null),
          """{"m":{"1.1":{"null":"2"}}""".getBytes)
      }.getMessage.contains("illegal number, offset: 0x0000000f"))
    }
    "don't serialize null keys for maps" in {
      assert(intercept[IOException] {
        verifySer(codecOfMutableMaps,
          MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(null.asInstanceOf[BigDecimal] -> 1)),
            mutable.Map(1.1f -> mutable.ListMap(BigInt(2) -> "2")),
            mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
          """{"hm":{"true":{null:1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes)
      }.getMessage.contains("key cannot be null"))
      assert(intercept[IOException] {
        verifySer(codecOfMutableMaps,
          MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
            mutable.Map(1.1f -> mutable.ListMap(null.asInstanceOf[BigInt] -> "2")),
            mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
          """{"hm":{"true":{"1.1":1}},"m":{"1.1":{null:"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes)
      }.getMessage.contains("key cannot be null"))
      assert(intercept[IOException] {
        verifySerDeser(codecOfImmutableMaps,
          ImmutableMaps(Map(1 -> 1.1), HashMap(null.asInstanceOf[String] -> ListMap(2.toChar -> 2), "3" -> ListMap(3.toChar -> 3)),
            SortedMap(4L -> TreeMap(4.toByte -> 4.4f), 5L -> TreeMap.empty[Byte, Float])),
          """{"m":{"1":1.1},"hm":{null:{"2":2},"3":{"3":3}},"sm":{"4":{"4":4.4},"5":{}}}""".getBytes)
      }.getMessage.contains("key cannot be null"))
    }
    "serialize and deserialize case classes with mutable long maps" in {
      verifySerDeser(make[MutableLongMaps](CodecMakerConfig()),
        MutableLongMaps(mutable.LongMap(1L -> 1.1), mutable.LongMap(3L -> "33")),
        """{"lm1":{"1":1.1},"lm2":{"3":"33"}}""".getBytes)
    }
    "serialize and deserialize case classes with immutable int and long maps" in {
      verifySerDeser(make[ImmutableIntLongMaps](CodecMakerConfig()),
        ImmutableIntLongMaps(IntMap(1 -> 1.1, 2 -> 2.2), LongMap(3L -> "33")),
        """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""".getBytes)
    }
    "serialize and deserialize case classes with mutable & immutable bitsets" in {
      verifySerDeser(make[BitSets](CodecMakerConfig()), BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6)),
        """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes)
    }
    "serialize and deserialize top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets = make[mutable.LongMap[IntMap[mutable.BitSet]]](CodecMakerConfig())
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        mutable.LongMap(1L -> IntMap(2 -> mutable.BitSet(4, 5, 6), 3 -> mutable.BitSet.empty)),
        """{"1":{"2":[4,5,6],"3":[]}}""".getBytes)
      verifySer(codecOfIntLongMapsAndBitSets, null, "null".getBytes)
    }
    "serialize and deserialize stringified top-level int/long maps & bitsets" in {
      val codecOfIntLongMapsAndBitSets =
        make[mutable.LongMap[IntMap[mutable.BitSet]]](CodecMakerConfig(isStringified = true))
      verifySerDeser(codecOfIntLongMapsAndBitSets,
        mutable.LongMap(1L -> IntMap(2 -> mutable.BitSet(4, 5, 6), 3 -> mutable.BitSet.empty)),
        """{"1":{"2":["4","5","6"],"3":[]}}""".getBytes)
      verifySer(codecOfIntLongMapsAndBitSets, null, "null".getBytes)
    }
    "serialize and deserialize with keys defined as is by fields" in {
      verifySerDeser(make[CamelAndSnakeCases](CodecMakerConfig()),
        CamelAndSnakeCases("VVV", "WWW", "YYY", "ZZZ"),
        """{"camelCase":"VVV","snake_case":"WWW","camel1":"YYY","snake_1":"ZZZ"}""".getBytes)
    }
    "serialize and deserialize with keys enforced to camelCase and throw parse exception when they are missing" in {
      val codecOfCamelAndSnakeCases = make[CamelAndSnakeCases](CodecMakerConfig(JsonCodecMaker.enforceCamelCase))
      verifySerDeser(codecOfCamelAndSnakeCases,
        CamelAndSnakeCases("VVV", "WWW", "YYY", "ZZZ"),
        """{"camelCase":"VVV","snakeCase":"WWW","camel1":"YYY","snake1":"ZZZ"}""".getBytes)
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelAndSnakeCases("VVV", "WWW", "YYY", "ZZZ"),
          """{"camel_case":"VVV","snake_case":"WWW","camel_1":"YYY","snake_1":"ZZZ"}""".getBytes)
      }.getMessage.contains("missing required field(s) \"camelCase\", \"snakeCase\", \"camel1\", \"snake1\", offset: 0x00000046"))
    }
    "serialize and deserialize with keys enforced to snake_case and throw parse exception when they are missing" in {
      val codecOfCamelAndSnakeCases = make[CamelAndSnakeCases](CodecMakerConfig(JsonCodecMaker.enforce_snake_case))
      verifySerDeser(codecOfCamelAndSnakeCases,
        CamelAndSnakeCases("VVV", "WWW", "YYY", "ZZZ"),
        """{"camel_case":"VVV","snake_case":"WWW","camel_1":"YYY","snake_1":"ZZZ"}""".getBytes)
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfCamelAndSnakeCases,
          CamelAndSnakeCases("VVV", "WWW", "YYY", "ZZZ"),
          """{"camelCase":"VVV","snakeCase":"WWW","camel1":"YYY","snake1":"ZZZ"}""".getBytes)
      }.getMessage.contains("missing required field(s) \"camel_case\", \"snake_case\", \"camel_1\", \"snake_1\", offset: 0x00000042"))
    }
    "serialize and deserialize with keys overridden by annotation and throw parse exception when they are missing" in {
      verifySerDeser(codecOfNameOverridden, NameOverridden(oldName = "VVV"), """{"new_name":"VVV"}""".getBytes)
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfNameOverridden, NameOverridden(oldName = "VVV"), """{"oldName":"VVV"}""".getBytes)
      }.getMessage.contains("missing required field(s) \"new_name\", offset: 0x00000010"))
    }
    "serialize and deserialize fields that stringified by annotation" in {
      verifySerDeser(codecOfStringified, stringified, """{"i":"1","bi":"2","l1":["1"],"l2":[2]}""".getBytes)
    }
    "throw parse exception when stringified fields have non-string values" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStringified, stringified, """{"i":1,"bi":"2","l1":["1"],"l2":[2]}""".getBytes)
      }.getMessage.contains("expected '\"', offset: 0x00000005"))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfStringified, stringified, """{"i":"1","bi":2,"l1":[1],"l2":[2]}""".getBytes)
      }.getMessage.contains("expected '\"' or null, offset: 0x0000000e"))
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
          |}""".stripMargin.getBytes,
        WriterConfig(indentionStep = 2))
    }
    "deserialize JSON with tabs & line returns" in {
      verifyDeser(codecOfIndented, indented,
        "{\r\t\"s\":\t\"VVV\",\r\t\"bd\":\t1.1,\r\t\"l\":\t[\r\t\t1,\r\t\t2,\r\t\t3\r\t],\r\t\"m\":\t{\r\t\t\"S\":\t-90.0,\r\t\t\"N\":\t90.0,\r\t\t\"W\":\t-180.0,\r\t\t\"E\":\t180.0\r\t}\r}".getBytes)
    }
    "serialize and deserialize UTF-8 keys and values of case classes without hex encoding" in {
      verifySerDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ"),
        """{"გასაღები":"ვვვ"}""".getBytes(UTF_8))
    }
    "serialize and deserialize UTF-8 keys and values of case classes with hex encoding" in {
      verifyDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t\\/\"}".getBytes(UTF_8))
      verifySer(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t/\"}".getBytes(UTF_8),
        WriterConfig(escapeUnicode = true))
    }
    "deserialize but don't serialize default values of case classes that defined for fields" in {
      verifySer(codecOfDefaults, defaults, "{}".getBytes)
      verifySer(codecOfDefaults, defaults.copy(s = null, bi = null, oc = None, l = Nil, a = null),
        """{"s":null,"bi":null}""".getBytes)
      val parsedObj = JsonReader.read(codecOfDefaults, "{}".getBytes)
      parsedObj.s shouldBe defaults.s
      parsedObj.i shouldBe defaults.i
      parsedObj.bi shouldBe defaults.bi
      parsedObj.l shouldBe defaults.l
      parsedObj.oc shouldBe defaults.oc
      parsedObj.a.deep shouldBe defaults.a.deep
      parsedObj.ml shouldBe defaults.ml
      parsedObj.mm shouldBe defaults.mm
      parsedObj.ms shouldBe defaults.ms
      parsedObj.mb shouldBe defaults.mb
    }
    "deserialize values of mutable collections in case of null defaults are defined" in {
      val json = """{"ml":{"1":1},"mm":{"2":2},"ms":[3],"mb":[4]}""".getBytes
      val parsedObj = JsonReader.read(codecOfDefaults, json)
      parsedObj.ml shouldBe mutable.LongMap[Int](1L -> 1)
      parsedObj.mm shouldBe mutable.Map[Int, Int](2 -> 2)
      parsedObj.ms shouldBe mutable.BitSet(3)
      parsedObj.mb shouldBe mutable.Buffer[Int](4)
    }
    "don't serialize and deserialize transient and non constructor defined fields of case classes" in {
      verifySerDeser(make[Transient](CodecMakerConfig()), Transient(required = "VVV"), """{"required":"VVV"}""".getBytes)
    }
    "don't serialize case class fields with 'None' values" in {
      verifySer(make[NullAndNoneValues](CodecMakerConfig()),
        NullAndNoneValues("VVV", null, null, null, NullAndNoneValues(null, null, null, null, null, None), None),
        """{"str":"VVV","bi":null,"bd":null,"lt":null,"nv":{"str":null,"bi":null,"bd":null,"lt":null,"nv":null}}""".getBytes)
      verifySer(make[List[NullAndNoneValues]](CodecMakerConfig()),
        List(NullAndNoneValues("VVV", null, null, null, null, None)),
        """[{"str":"VVV","bi":null,"bd":null,"lt":null,"nv":null}]""".getBytes)
    }
    "don't serialize case class fields with empty collections" in {
      verifySer(make[EmptyTraversables](CodecMakerConfig()), EmptyTraversables(List(), Set(), List()), """{}""".getBytes)
    }
    "throw parse exception in case of unknown case class fields" in {
      verifyDeser(make[Unknown](CodecMakerConfig()), Unknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes)
    }
    "throw parse exception for unknown case class fields if skipping of them wasn't allowed in materialize call" in {
      assert(intercept[JsonParseException] {
        verifyDeser(make[Unknown](CodecMakerConfig(skipUnexpectedFields = false)), Unknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes)
      }.getMessage.contains("unexpected field: \"x\", offset: 0x00000004"))
    }
    "deserialize null values of case class fields for standard types" in {
      verifyDeser(codecOfStandardTypes, StandardTypes(null, null, null),
        """{"s":null,"bi":null,"bd":null}""".getBytes)
    }
    "deserialize null values for standard types of traversable values" in {
      verifyDeser(codecOfImmutableTraversables,
        ImmutableTraversables(List(ListSet(null)), Queue(Set[BigInt](4, null, 6)),
          IndexedSeq(), Stream(TreeSet()), Vector(Traversable())),
        """{"l":[[null]],"q":[[4,null,6]],"is":null,"s":[null],"v":[null]}""".getBytes)
    }
    "deserialize null values for standard types of map values" in {
      verifyDeser(codecOfMutableMaps,
        MutableMaps(mutable.HashMap(),
          mutable.Map(1.1f -> mutable.ListMap(BigInt(2) -> null.asInstanceOf[String])),
          mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(), 2.2 -> mutable.LinkedHashMap())),
        """{"hm":null,"m":{"1.1":{"2":null}},"ohm":{"1.1":null,"2.2":null}}""".getBytes)
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
            |"r00":0,"r01":1,"r02":2,"r03":3,"r04":4,"r05":5,"r06":6,"r07":7,"r08":8,
            |"r10":10,"r11":11,"r12":12,"r13":13,"r14":14,"r15":15,"r16":16,"r17":17,"r18":18,
            |"r20":20,"r21":21,"r22":22,"r23":23,"r24":24,"r25":25,"r26":26,"r27":27,"r28":28,
            |"r30":30,"r31":31,"r32":32,"r33":33,"r34":34,"r35":35,"r36":36,"r37":37,"r38":38,
            |"r40":40,"r41":41,"r42":42,"r43":43,"r44":44,"r45":45,"r46":46,"r47":47,"r48":48,
            |"r50":50,"r51":51,"r52":52,"r53":53,"r54":54,"r55":55,"r56":56,"r57":57,"r58":58,
            |"r60":60,"r61":61,"r62":62,"r63":63,"r64":64,"r65":65,"r66":66,"r67":67,"r68":68,
            |"r70":70,"r71":71,"r72":72,"r73":73,"r74":74,"r75":75,"r76":76,"r77":77,"r78":78,
            |"r80":80,"r81":81,"r82":82,"r83":83,"r84":84,"r85":85,"r86":86,"r87":87,"r88":88,
            |"r90":90,"r91":91,"r92":92,"r93":93,"r94":94,"r95":95,"r96":96,"r97":97,"r98":98
            |}""".stripMargin.getBytes)
      }.getMessage.contains("""missing required field(s) "r09", "r19", "r29", "r39", "r49", "r59", "r69", "r79", "r89", "r99", offset: 0x0000032c"""))
    }
    "serialize and deserialize ADTs using ASCII discriminator field & value" in {
      verifySerDeser(codecOfADTList,
        List(A(1), B("VVV"), null, C(1, "VVV"), D),
        """[{"type":"A","a":1},{"type":"B","a":"VVV"},null,{"type":"C","a":1,"b":"VVV"},{"type":"D"}]""".getBytes)
      val longStr = new String(Array.fill(100000)('W'))
      verifyDeser(codecOfADTList,
        List(C(2, longStr), C(1, "VVV")),
        s"""[{"a":2,"b":"$longStr","type":"C"},{"a":1,"type":"C","b":"VVV"}]""".getBytes)
      verifySerDeser(make[List[AdtBase]](CodecMakerConfig(discriminatorFieldName = "t")),
        List(C(2, "WWW"), C(1, "VVV")),
        s"""[{"t":"C","a":2,"b":"WWW"},{"t":"C","a":1,"b":"VVV"}]""".getBytes)
    }
    "serialize and deserialize ADTs using non-ASCII discriminator field & value w/ reusage of case classes w/o ADTs" in {
      verifySerDeser(make[List[База]](CodecMakerConfig(discriminatorFieldName = "тип", skipUnexpectedFields = false)),
        List(А(Б(null)), Б(А(null))),
        """[{"тип":"А","б":{"а":null}},{"тип":"Б","а":{"б":null}}]""".getBytes)
    }
    "throw parse exception in case of missing discriminator field or illegal value of discriminator field" in {
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(A(1)), """[{"a":1}]""".getBytes)
      }.getMessage.contains("""missing required field "type", offset: 0x00000007"""))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(A(1)), """[{"a":1,"type":"AAA"}]""".getBytes)
      }.getMessage.contains("""illegal value of discriminator field "type", offset: 0x00000013"""))
      assert(intercept[JsonParseException] {
        verifyDeser(codecOfADTList, List(A(1)), """[{"a":1,"type":123}]""".getBytes)
      }.getMessage.contains("""expected '"', offset: 0x0000000f"""))
    }
    "serialize and deserialize when the root codec defined as an impicit val" in {
      implicit val implicitRootCodec: JsonCodec[Int] = make[Int](CodecMakerConfig())
      verifySerDeser(implicitRootCodec, 1, "1".getBytes)
    }
  }
  "JsonCodec.enforceCamelCase" should {
    "transform snake_case names to camelCase" in {
      JsonCodecMaker.enforceCamelCase("o_o") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("o_ooo_") shouldBe "oOoo"
      JsonCodecMaker.enforceCamelCase("O_OOO_111") shouldBe "oOoo111"
    }
    "leave camelCase names as is" in {
      JsonCodecMaker.enforceCamelCase("oO") shouldBe "oO"
      JsonCodecMaker.enforceCamelCase("oOoo") shouldBe "oOoo"
      JsonCodecMaker.enforceCamelCase("OOoo111") shouldBe "OOoo111"
    }
  }
  "JsonCodec.enforce_snake_case" should {
    "transform camelCase names to snake_case" in {
      JsonCodecMaker.enforce_snake_case("oO") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("oOoo") shouldBe "o_ooo"
      JsonCodecMaker.enforce_snake_case("oOoo111") shouldBe "o_ooo_111"
    }
    "enforce lower case for snake_case names as is" in {
      JsonCodecMaker.enforce_snake_case("o_O") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o_ooo_") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case("O_OOO_111") shouldBe "o_ooo_111"
    }
  }
  "JsonCodec.simpleClassName" should {
    "shorten full class name to simple class name" in {
      JsonCodecMaker.simpleClassName("com.github.plohkotnyuk.jsoniter_scala.Test") shouldBe "Test"
      JsonCodecMaker.simpleClassName("JsonCodecMakerSpec.this.Test") shouldBe "Test"
      JsonCodecMaker.simpleClassName(".Test") shouldBe "Test"
      JsonCodecMaker.simpleClassName("Test") shouldBe "Test"
    }
  }

  def verifySerDeser[T](codec: JsonCodec[T], obj: T, json: Array[Byte], cfg: WriterConfig = WriterConfig()): Unit = {
    verifySer(codec, obj, json, cfg)
    verifyDeser(codec, obj, json)
  }

  def verifySer[T](codec: JsonCodec[T], obj: T, json: Array[Byte], cfg: WriterConfig = WriterConfig()): Unit = {
    val baos = new ByteArrayOutputStream
    JsonWriter.write(codec, obj, baos, cfg)
    toString(baos.toByteArray) shouldBe toString(json)
    toString(JsonWriter.write(codec, obj, cfg)) shouldBe toString(json)
  }

  def verifyDeser[T](codec: JsonCodec[T], obj: T, json: Array[Byte]): Unit = {
    JsonReader.read(codec, new ByteArrayInputStream(json)) shouldBe obj
    JsonReader.read(codec, json) shouldBe obj
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}