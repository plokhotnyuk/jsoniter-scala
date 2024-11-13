package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Objects, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException
import java.util.concurrent.ThreadLocalRandom
import scala.annotation.switch
import scala.util.control.NonFatal
import scala.util.hashing.MurmurHash3

case class UserId(id: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

case class UserIds(l: List[String]) extends AnyVal

object Alias {
  type I = Int

  type S = String

  case class UserId(id: S) extends AnyVal

  case class OrderId(value: I) extends AnyVal
}

object Generic {
  case class UserId[S](id: S) extends AnyVal

  case class OrderId[I](value: I) extends AnyVal
}

object UserId2 {
  type Opaque = Base with Tag

  type Base = Any {
    type Hack
  }

  trait Tag

  object Opaque {
    def apply(value: String): Opaque = value.asInstanceOf[Opaque]

    def unapply(userId: Opaque): Option[String] = Option(userId).map(_.value)
  }

  final implicit class Ops(private val userId: Opaque) extends AnyVal {
    def value: String = userId.asInstanceOf[String]
  }
}

object OrderId2 {
  type Opaque = Base with Tag

  type Base = Any {
    type Hack
  }

  trait Tag

  object Opaque {
    def apply(value: Int): Opaque = value.asInstanceOf[Opaque]

    def unapply(orderId: Opaque): Option[Int] = Option(orderId).map(_.value)
  }

  final implicit class Ops(private val orderId: Opaque) extends AnyVal {
    def value: Int = orderId.asInstanceOf[Int]
  }
}

case class Id[A](id: A) extends AnyVal

sealed trait Weapon extends Product with Serializable

object Weapon {
  case object Axe extends Weapon

  case object Sword extends Weapon
}

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

case class JavaEnums(l: Level, il: Levels.InnerLevel)

case class JavaTypes(uuid: UUID)

object LocationType extends Enumeration {
  type LocationType = Value

  val IP, GPS: LocationType = Value

  def extra(name: String): LocationType = Value(nextId, name)
}

case class Enums(lt: LocationType.LocationType)

case class Enums2(@stringified lt: LocationType.LocationType)

case class Options(
  os: Option[String],
  obi: Option[BigInt],
  osi: Option[Set[Int]],
  ol: Option[Long],
  ojl: Option[java.lang.Long],
  ost: Option[StandardTypes])

case class Tuples(t1: (Int, Double, List[Char]), t2: (String, BigInt, Option[LocationType.LocationType]))

case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

case class Iterators(s: Iterator[Iterator[String]], i: Iterator[Int])

case class MutableMaps(
  hm: collection.mutable.HashMap[Boolean, collection.mutable.AnyRefMap[BigDecimal, Int]],
  m: collection.mutable.Map[Float, collection.mutable.ListMap[BigInt, String]],
  ohm: collection.mutable.OpenHashMap[Double, collection.mutable.LinkedHashMap[Short, Double]])

case class ImmutableMaps(
  m: Map[Int, Double],
  hm: collection.immutable.HashMap[String, collection.immutable.ListMap[Char, BigInt]],
  sm: collection.immutable.SortedMap[Long, collection.immutable.TreeMap[Byte, Float]])

case class EmptyMaps(im: collection.immutable.Map[String, Int], mm: collection.mutable.Map[Long, Int])

case class MutableIterables(
  ml: collection.mutable.Seq[collection.mutable.SortedSet[String]],
  ab: collection.mutable.ArrayBuffer[collection.mutable.Set[BigInt]],
  as: collection.mutable.ArraySeq[collection.mutable.LinkedHashSet[Int]],
  b: collection.mutable.Buffer[collection.mutable.HashSet[Double]],
  lb: collection.mutable.ListBuffer[collection.mutable.TreeSet[Long]],
  is: collection.mutable.IndexedSeq[collection.mutable.ArrayStack[Float]],
  ub: collection.mutable.UnrolledBuffer[collection.mutable.Iterable[Short]])

case class EmptyIterables(l: List[String], a: collection.mutable.ArrayBuffer[Int])

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

case class Stringified(
  @stringified i: Int,
  @stringified bi: BigInt,
  @stringified o: Option[Int],
  @stringified l: List[Int])

trait TopProperty extends Any

trait Property extends Any with TopProperty

case class DoubleProperty(value: Double) extends AnyVal with Property

case class StringProperty(value: String) extends AnyVal with Property

case class Defaults(
  st: String = "VVV",
  i: Int = 1,
  bi: BigInt = -1,
  oc: Option[Char] = Some('X'),
  l: List[Int] = collection.immutable.List(0),
  a: Array[Array[Long]] = Array(Array(1L, 2L), Array(3L, 4L)),
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

case class IterablesWithDefaults(l: List[Int] = _root_.scala.collection.immutable.Nil, s: Set[Option[String]] = Set())

sealed trait AdtBase extends Product with Serializable

sealed abstract class Inner extends AdtBase {
  def a: Int
}

case class AAA(a: Int) extends Inner

case class BBB(a: BigInt) extends AdtBase

case class CCC(a: Int, b: String) extends Inner

case object DDD extends AdtBase

sealed trait GADT2[A] extends Product with Serializable

object GADT2 {
  case class IsDir(path: String) extends GADT2[_root_.scala.Boolean]

  case class Exists(path: String) extends GADT2[_root_.scala.Boolean]

  case class ReadBytes(path: String) extends GADT2[_root_.scala.Array[_root_.scala.Byte]]

  case class CopyOver(src: Seq[_root_.scala.Byte], path: String) extends GADT2[Int]
}

sealed trait Fruit[T <: Fruit[T]] extends Product with Serializable

final case class Apple(family: String) extends Fruit[Apple]

final case class Orange(color: Int) extends Fruit[Orange]

object KingDom {
  sealed trait Human

  object Human {
    final case class Subject(name: String) extends Human

    final case class King(name: String, reignOver: Iterable[Human]) extends Human
  }
}

case class Kind(name: String) extends AnyVal {
  type ValueType
}

object Kind {
  type Aux[V] = Kind { type ValueType = V }

  private[this] val codecOfKind: JsonValueCodec[Kind] = make[Kind]

  def of[V](name: String): Kind.Aux[V] = new Kind(name).asInstanceOf[Kind.Aux[V]]

  implicit def codecOfKindAux[V]: JsonValueCodec[Kind.Aux[V]] = codecOfKind.asInstanceOf[JsonValueCodec[Kind.Aux[V]]]
}

object Demo { // See https://github.com/plokhotnyuk/jsoniter-scala/issues/1004
  sealed trait Status[+E, +A]

  final case class Error[E](error: E) extends Status[E, Nothing]

  final case class Fatal(reason: String) extends Status[Nothing, Nothing]

  final case class Success[A](a: A) extends Status[Nothing, A]

  final case class Timeout() extends Status[Nothing, Nothing]

  implicit def statusCodec[A, E](implicit ec: JsonValueCodec[E], ac: JsonValueCodec[A]): JsonValueCodec[Status[E, A]] =
    JsonCodecMaker.make
}

sealed abstract class Version(val value: String)

object Version {
  case object Current extends Version("8.10")

  case object `8.09` extends Version("8.9")
}

trait Aggregate {
  type Props

  implicit def propsCodec: JsonValueCodec[Props]
}

trait Events { self: Aggregate =>
  case class MyEvent(props: Props)

  // Works here
  implicit val myEventCodec: JsonValueCodec[MyEvent] = make
}

object Person extends Aggregate with Events {
  case class Props(name: String, age: Int)

  implicit val propsCodec: JsonValueCodec[Props] = make

  // FIXME: Doesn't work here
  // Scala 3: No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for 'Events.this.Props'.
  // Scala 2: Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'Events.this.Props' or provide a custom implicitly accessible codec for it.
  // val myEventCodec: JsonValueCodec[MyEvent] = make
}

class JsonCodecMakerSpec extends VerifyingSpec {
  import com.github.plokhotnyuk.jsoniter_scala.macros.NamespacePollutions._

  //given CodecMakerConfig.PrintCodec with {}
  //implicit val printCodec: CodecMakerConfig.PrintCodec = new CodecMakerConfig.PrintCodec {}
  val codecOfPrimitives: JsonValueCodec[Primitives] = make
  val codecOfStandardTypes: JsonValueCodec[StandardTypes] = make
  val codecOfJavaEnums: JsonValueCodec[JavaEnums] = make
  val codecOfJavaTypes: JsonValueCodec[JavaTypes] = make
  val codecOfEnums1: JsonValueCodec[Enums] = make
  val codecOfEnums2: JsonValueCodec[Enums] = make(CodecMakerConfig.withUseScalaEnumValueId(true))
  val codecOfEnums3: JsonValueCodec[Enums2] = make(CodecMakerConfig.withUseScalaEnumValueId(true))
  val codecOfOptions: JsonValueCodec[Options] = make
  val codecOfTuples: JsonValueCodec[Tuples] = make
  val codecOfArrays: JsonValueCodec[Arrays] = make
  val codecOfIterators: JsonValueCodec[Iterators] = make
  val codecOfMutableMaps: JsonValueCodec[MutableMaps] = make
  val codecOfImmutableMaps: JsonValueCodec[ImmutableMaps] = make
  val codecOfNameOverridden: JsonValueCodec[NameOverridden] = make
  val codecOfRecursive: JsonValueCodec[Recursive] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
  val codecOfStringified: JsonValueCodec[Stringified] = make
  val codecOfADTList1: JsonValueCodec[List[AdtBase]] = make
  val codecOfADTList2: JsonValueCodec[List[AdtBase]] = make(CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None))
  val codecOfADTList3: JsonValueCodec[List[AdtBase]] = make(CodecMakerConfig.withRequireDiscriminatorFirst(false).withBigIntDigitsLimit(20001))

  "JsonCodecMaker.make generate codecs which" should {
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
      verifySerDeser(make[Char], 'V', """"V"""")
      verifySerDeser(make[Double], 1.1, "1.1")
      verifySerDeser(make[Float], 2.2f, "2.2")
    }
    "serialize and deserialize stringified top-level primitives" in {
      verifySerDeser(make[_root_.scala.Byte](CodecMakerConfig.withIsStringified(true)), 1.toByte, """"1"""")
      verifySerDeser(make[_root_.scala.Short](CodecMakerConfig.withIsStringified(true)), 2.toShort, """"2"""")
      verifySerDeser(make[Int](CodecMakerConfig.withIsStringified(true)), 3, """"3"""")
      verifySerDeser(make[Long](CodecMakerConfig.withIsStringified(true)), 4L, """"4"""")
      verifySerDeser(make[_root_.scala.Boolean](CodecMakerConfig.withIsStringified(true)), true, """"true"""")
      verifySerDeser(make[Double](CodecMakerConfig.withIsStringified(true)), 1.1, """"1.1"""")
      verifySerDeser(make[Float](CodecMakerConfig.withIsStringified(true)), 2.2f, """"2.2"""")
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
    "serialize and deserialize case classes with Java primitives" in {
      case class BoxedPrimitives(
        b: _root_.java.lang.Byte,
        s: _root_.java.lang.Short,
        i: _root_.java.lang.Integer,
        l: _root_.java.lang.Long,
        bl: _root_.java.lang.Boolean,
        ch: _root_.java.lang.Character,
        dbl: _root_.java.lang.Double,
        f: _root_.java.lang.Float)

      verifySerDeser(make[BoxedPrimitives],
        BoxedPrimitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"V","dbl":1.1,"f":2.2}""")
    }
    "serialize and deserialize top-level Java primitives" in {
      verifySerDeser(make[_root_.java.lang.Byte], _root_.java.lang.Byte.valueOf(1.toByte), "1")
      verifySerDeser(make[_root_.java.lang.Short], _root_.java.lang.Short.valueOf(2.toShort), "2")
      verifySerDeser(make[_root_.java.lang.Integer], _root_.java.lang.Integer.valueOf(3), "3")
      verifySerDeser(make[_root_.java.lang.Long], _root_.java.lang.Long.valueOf(4L), "4")
      verifySerDeser(make[_root_.java.lang.Boolean], _root_.java.lang.Boolean.valueOf(true), "true")
      verifySerDeser(make[_root_.java.lang.Character], _root_.java.lang.Character.valueOf('V'), """"V"""")
      verifySerDeser(make[_root_.java.lang.Double], _root_.java.lang.Double.valueOf(1.1), "1.1")
      verifySerDeser(make[_root_.java.lang.Float], _root_.java.lang.Float.valueOf(2.2f), "2.2")
    }
    "serialize and deserialize stringifeid top-level Java primitives" in {
      verifySerDeser(make[_root_.java.lang.Byte](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Byte.valueOf(1.toByte), """"1"""")
      verifySerDeser(make[_root_.java.lang.Short](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Short.valueOf(2.toShort), """"2"""")
      verifySerDeser(make[_root_.java.lang.Integer](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Integer.valueOf(3), """"3"""")
      verifySerDeser(make[_root_.java.lang.Long](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Long.valueOf(4L), """"4"""")
      verifySerDeser(make[_root_.java.lang.Boolean](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Boolean.valueOf(true), """"true"""")
      verifySerDeser(make[_root_.java.lang.Double](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Double.valueOf(1.1), """"1.1"""")
      verifySerDeser(make[_root_.java.lang.Float](CodecMakerConfig.withIsStringified(true)),
        _root_.java.lang.Float.valueOf(2.2f), """"2.2"""")
    }
    "serialize and deserialize Java primitives as map keys and values" in {
      verifySerDeser(make[Map[_root_.java.lang.Byte, _root_.java.lang.Byte]],
        Map(_root_.java.lang.Byte.valueOf(1.toByte) -> _root_.java.lang.Byte.valueOf(11.toByte),
          _root_.java.lang.Byte.valueOf(2.toByte) -> _root_.java.lang.Byte.valueOf(12.toByte)),
        """{"1":11,"2":12}""")
      verifySerDeser(make[Map[_root_.java.lang.Short, _root_.java.lang.Short]],
        Map(_root_.java.lang.Short.valueOf(1.toShort) -> _root_.java.lang.Short.valueOf(11.toShort),
          _root_.java.lang.Short.valueOf(2.toShort) -> _root_.java.lang.Short.valueOf(12.toShort)),
        """{"1":11,"2":12}""")
      verifySerDeser(make[Map[_root_.java.lang.Integer, _root_.java.lang.Integer]],
        Map(_root_.java.lang.Integer.valueOf(1) -> _root_.java.lang.Integer.valueOf(11),
          _root_.java.lang.Integer.valueOf(2) -> _root_.java.lang.Integer.valueOf(12)),
        """{"1":11,"2":12}""")
      verifySerDeser(make[Map[_root_.java.lang.Long, _root_.java.lang.Long]],
        Map(_root_.java.lang.Long.valueOf(1) -> _root_.java.lang.Long.valueOf(11),
          _root_.java.lang.Long.valueOf(2) -> _root_.java.lang.Long.valueOf(12)),
        """{"1":11,"2":12}""")
      verifySerDeser(make[Map[_root_.java.lang.Boolean, _root_.java.lang.Boolean]],
        Map(_root_.java.lang.Boolean.valueOf(false) -> _root_.java.lang.Boolean.valueOf(false),
          _root_.java.lang.Boolean.valueOf(true) -> _root_.java.lang.Boolean.valueOf(true)),
        """{"false":false,"true":true}""")
      verifySerDeser(make[Map[_root_.java.lang.Double, _root_.java.lang.Double]],
        Map(_root_.java.lang.Double.valueOf(1.0) -> _root_.java.lang.Double.valueOf(11.0),
          _root_.java.lang.Double.valueOf(2.0) -> _root_.java.lang.Double.valueOf(12.0)),
        """{"1.0":11.0,"2.0":12.0}""")
      verifySerDeser(make[Map[_root_.java.lang.Float, _root_.java.lang.Float]],
        Map(_root_.java.lang.Float.valueOf(1.0f) -> _root_.java.lang.Float.valueOf(11.0f),
          _root_.java.lang.Float.valueOf(2.0f) -> _root_.java.lang.Float.valueOf(12.0f)),
        """{"1.0":11.0,"2.0":12.0}""")
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
        BigInt("123456789012345678901234567890"), """"123456789012345678901234567890"""")
      verifySerDeser(make[BigDecimal](CodecMakerConfig.withIsStringified(true)),
        BigDecimal("1234567890.12345678901234567890"), """"1234567890.12345678901234567890"""")
    }
    "deserialize duplicated key for case classes when checking of field duplication is disabled" in {
      verifyDeser(make[StandardTypes](CodecMakerConfig.withCheckFieldDuplication(false)),
        StandardTypes("VVV", BigInt(1), BigDecimal(2.0)),
        s"""{"s":"XXX","s":"VVV","bi":10,"bi":1,"bd":20.0,"bd":2.0}""")
    }
    "throw parse exception in case of duplicated key for case classes was detected" in {
      verifyDeserError(codecOfStandardTypes, s"""{"s":"XXX","s":"VVV","bi":10,"bi":1,"bd":20.0,"bd":2.0}""",
        """duplicated field "s", offset: 0x0000000e""")
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
      verifyDeserError(codecOfStandardTypes, """{,"s":"VVV","bi":1,"bd":2}""", """expected '"', offset: 0x00000001""")
      verifyDeserError(codecOfStandardTypes, """{"s":"VVV","bi":1,"bd":2]""", "expected '}' or ',', offset: 0x00000018")
      verifyDeserError(codecOfStandardTypes, """{"s":"VVV","bi":1,"bd":2,}""", """expected '"', offset: 0x00000019""")
    }
    "throw exception in attempt to serialize null values" in {
      verifySerError[StandardTypes](codecOfStandardTypes, null, "", null, WriterConfig)
    }
    "serialize and deserialize Scala classes which has a primary constructor with 'var' or 'var' parameters only" in {
      class NonCaseClass(val id: Int, var name: String) {
        override def hashCode(): Int = id * 31 + Objects.hashCode(name)

        override def equals(obj: Any): _root_.scala.Boolean = obj match {
          case c: NonCaseClass => id == c.id && Objects.equals(name, c.name)
          case _ => false
        }

        override def toString: String = s"NonCaseClass(id=$id,name=$name)"
      }

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
      verifySerDeser(make[UUID], new UUID(0, 0), """"00000000-0000-0000-0000-000000000000"""")
    }
    "serialize and deserialize Java types as key in maps" in {
      verifySerDeser(make[Map[UUID, Int]], Map(new UUID(0, 0) -> 0),
        """{"00000000-0000-0000-0000-000000000000":0}""")
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
      verifyDeserError(codecOfJavaEnums, """{"l":null,"il":"HIGH"}""", """expected '"', offset: 0x00000005""")
      verifyDeserError(codecOfJavaEnums, """{"l":"LO","il":"HIGH"}""", """illegal enum value "LO", offset: 0x00000008""")
    }
    "serialize and deserialize top-level Java enumerations" in {
      verifySerDeser(make[Level], Level.HIGH, """"HIGH"""")
    }
    "serialize and deserialize Java enumerations as key in maps" in {
      verifySerDeser(make[Map[Level, Int]], Map(Level.HIGH -> 0), """{"HIGH":0}""")
    }
    "serialize and deserialize option types using a custom codec to handle missing fields and 'null' values differently" in {
      case class Ex(opt: Option[String] = _root_.scala.Some("hiya"), level: Option[Int] = _root_.scala.Some(10))

      object CustomOptionCodecs {
        implicit val intCodec: JsonValueCodec[Int] = make[Int]
        implicit val stringCodec: JsonValueCodec[String] = make[String]

        implicit def optionCodec[A](implicit aCodec: JsonValueCodec[A]): JsonValueCodec[Option[A]] =
          new JsonValueCodec[Option[A]] {
            override def decodeValue(in: JsonReader, default: Option[A]): Option[A] =
              if (in.isNextToken('n')) in.readNullOrError(_root_.scala.None, "expected 'null' or JSON value")
              else {
                in.rollbackToken()
                _root_.scala.Some(aCodec.decodeValue(in, aCodec.nullValue))
              }

            override def encodeValue(x: Option[A], out: JsonWriter): _root_.scala.Unit =
              if (x eq _root_.scala.None) out.writeNull()
              else aCodec.encodeValue(x.get, out)

            override def nullValue: Option[A] = _root_.scala.None
          }
      }

      import CustomOptionCodecs._

      val codecOfEx = make[Ex](CodecMakerConfig.withTransientNone(false))
      verifySerDeser(codecOfEx, Ex(_root_.scala.None, _root_.scala.None), """{"opt":null,"level":null}""")
      verifySerDeser(codecOfEx, Ex(_root_.scala.Some("hiya"), _root_.scala.Some(10)), """{}""")
      verifySerDeser(codecOfEx, Ex(_root_.scala.Some("pakikisama"), _root_.scala.Some(5)), """{"opt":"pakikisama","level":5}""")
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
          case _ => out.encodeError("illegal enum value: null")
        }
      }
      verifySerDeser(make[Map[Level, Int]], Map(Level.HIGH -> 100), """{"1":100}""")
      implicit val levelOrdering: Ordering[Level] = new Ordering[Level] {
        override def compare(x: Level, y: Level): Int = y.ordinal - x.ordinal
      }
      verifySerDeser(make[_root_.scala.collection.immutable.TreeMap[Level, Int]],
        _root_.scala.collection.immutable.TreeMap[Level, Int](Level.HIGH -> 100, Level.LOW -> 10), """{"0":10,"1":100}""")
      verifyDeserByCheck(make[_root_.scala.collection.immutable.TreeMap[Level, Int]], """{"0":10,"1":100}""",
        check = (actual: _root_.scala.collection.immutable.TreeMap[Level, Int]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.immutable.SortedMap[Level, Int]],
        _root_.scala.collection.immutable.SortedMap[Level, Int](Level.HIGH -> 100, Level.LOW -> 10), """{"0":10,"1":100}""")
      verifyDeserByCheck(make[_root_.scala.collection.immutable.SortedMap[Level, Int]], """{"0":10,"1":100}""",
        check = (actual: _root_.scala.collection.immutable.SortedMap[Level, Int]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.mutable.TreeMap[Level, Int]],
        _root_.scala.collection.mutable.TreeMap[Level, Int](Level.HIGH -> 100, Level.LOW -> 10), """{"0":10,"1":100}""")
      verifyDeserByCheck(make[_root_.scala.collection.mutable.TreeMap[Level, Int]], """{"0":10,"1":100}""",
        check = (actual: _root_.scala.collection.mutable.TreeMap[Level, Int]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.mutable.SortedMap[Level, Int]],
        _root_.scala.collection.mutable.SortedMap[Level, Int](Level.HIGH -> 100, Level.LOW -> 10), """{"0":10,"1":100}""")
      verifyDeserByCheck(make[_root_.scala.collection.mutable.SortedMap[Level, Int]], """{"0":10,"1":100}""",
        check = (actual: _root_.scala.collection.mutable.SortedMap[Level, Int]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.SortedMap[Level, Int]],
        _root_.scala.collection.SortedMap[Level, Int](Level.HIGH -> 100, Level.LOW -> 10), """{"0":10,"1":100}""")
      verifyDeserByCheck(make[_root_.scala.collection.SortedMap[Level, Int]], """{"0":10,"1":100}""",
        check = (actual: _root_.scala.collection.SortedMap[Level, Int]) => actual.ordering shouldBe levelOrdering)
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
          case _ => out.encodeError("illegal enum value: null")
        }

        override def nullValue: Level = null.asInstanceOf[Level]
      }
      implicit val levelOrdering: Ordering[Level] = new Ordering[Level] {
        override def compare(x: Level, y: Level): Int = y.ordinal - x.ordinal
      }
      verifySerDeser(make[_root_.scala.collection.immutable.TreeSet[Level]],
        _root_.scala.collection.immutable.TreeSet[Level](Level.HIGH, Level.LOW), """[0,1]""")
      verifyDeserByCheck(make[_root_.scala.collection.immutable.TreeSet[Level]], """[0,1]""",
        check = (actual: _root_.scala.collection.immutable.TreeSet[Level]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.immutable.SortedSet[Level]],
        _root_.scala.collection.immutable.SortedSet[Level](Level.HIGH, Level.LOW), """[0,1]""")
      verifyDeserByCheck(make[_root_.scala.collection.immutable.SortedSet[Level]], """[0,1]""",
        check = (actual: _root_.scala.collection.immutable.SortedSet[Level]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.mutable.TreeSet[Level]],
        _root_.scala.collection.mutable.TreeSet[Level](Level.HIGH, Level.LOW), """[0,1]""")
      verifyDeserByCheck(make[_root_.scala.collection.mutable.TreeSet[Level]], """[0,1]""",
        check = (actual: _root_.scala.collection.mutable.TreeSet[Level]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.mutable.SortedSet[Level]],
        _root_.scala.collection.mutable.SortedSet[Level](Level.HIGH, Level.LOW), """[0,1]""")
      verifyDeserByCheck(make[_root_.scala.collection.mutable.SortedSet[Level]], """[0,1]""",
        check = (actual: _root_.scala.collection.mutable.SortedSet[Level]) => actual.ordering shouldBe levelOrdering)
      verifySerDeser(make[_root_.scala.collection.SortedSet[Level]],
        _root_.scala.collection.SortedSet[Level](Level.HIGH, Level.LOW), """[0,1]""")
      verifyDeserByCheck(make[_root_.scala.collection.SortedSet[Level]], """[0,1]""",
        check = (actual: _root_.scala.collection.SortedSet[Level]) => actual.ordering shouldBe levelOrdering)
    }
    "serialize and deserialize enumerations" in {
      verifySerDeser(codecOfEnums1, Enums(LocationType.GPS), """{"lt":"GPS"}""")
      verifySerDeser(codecOfEnums2, Enums(LocationType.GPS), """{"lt":1}""")
      verifySerDeser(codecOfEnums3, Enums2(LocationType.GPS), """{"lt":"1"}""")
      verifySerDeser(codecOfEnums1, Enums(LocationType.extra("Galileo1")), """{"lt":"Galileo1"}""")
      verifySerDeser(codecOfEnums2, Enums(LocationType.extra("Galileo2")), """{"lt":3}""")
      verifySerDeser(codecOfEnums3, Enums2(LocationType.extra("Galileo3")), """{"lt":"4"}""")
    }
    "throw parse exception in case of illegal value of enumeration" in {
      verifyDeserError(codecOfEnums1, """{"lt":null}""", """expected '"', offset: 0x00000006""")
      verifyDeserError(codecOfEnums1, """{"lt":"GLONASS"}""", """illegal enum value "GLONASS", offset: 0x0000000e""")
      verifyDeserError(codecOfEnums2, """{"lt":null}""", "expected digit, offset: 0x00000006")
      verifyDeserError(codecOfEnums2, """{"lt":5}""", "illegal enum value 5, offset: 0x00000006")
    }
    "serialize and deserialize top-level enumerations" in {
      verifySerDeser(make[LocationType.LocationType], LocationType.GPS, """"GPS"""")
      verifySerDeser(make[LocationType.LocationType](CodecMakerConfig.withUseScalaEnumValueId(true)),
        LocationType.GPS, "1")
      verifySerDeser(make[LocationType.LocationType](CodecMakerConfig.withUseScalaEnumValueId(true).withIsStringified(true)),
        LocationType.GPS, """"1"""")
    }
    "serialize and deserialize enumerations as key in maps" in {
      verifySerDeser(make[Map[LocationType.LocationType, Int]], Map(LocationType.GPS -> 0), """{"GPS":0}""")
      verifySerDeser(make[Map[LocationType.LocationType, Int]](CodecMakerConfig.withUseScalaEnumValueId(true)),
        Map(LocationType.GPS -> 0), """{"1":0}""")
    }
    "deserialize int from floating point numbers using custom codecs" in {
      implicit val customCodecOfInt: JsonValueCodec[Int] = new JsonValueCodec[Int] {
        def nullValue: Int = 0

        def decodeValue(in: JsonReader, default: Int): Int = {
          val d = in.readDouble()
          val i = d.toInt
          if (d == i) i
          else in.decodeError("illegal number")
        }

        def encodeValue(x: Int, out: JsonWriter): _root_.scala.Unit = out.writeVal(x)
      }
      val codecOfIntList = make[List[Int]]
      verifyDeser(codecOfIntList, List(1, 123, 1234567), """[1.0,123000e-3,1.234567E+6]""")
      verifyDeserError(codecOfIntList, """[1.23456789E+6]""", "illegal number, offset: 0x0000000d")
      verifySer(codecOfIntList, List(1, 123, 1234567), """[1,123,1234567]""")
    }
    "serialize and deserialize outer types using custom value codecs for long type" in {
      implicit val customCodecOfLong: JsonValueCodec[Long] = new JsonValueCodec[Long] {
        def nullValue: Long = 0

        def decodeValue(in: JsonReader, default: Long): Long =
          if (in.isNextToken('"')) {
            in.rollbackToken()
            in.readStringAsLong()
          } else {
            in.rollbackToken()
            in.readLong()
          }

        def encodeValue(x: Long, out: JsonWriter): _root_.scala.Unit =
          if (x > 4503599627370496L || x < -4503599627370496L) out.writeValAsString(x)
          else out.writeVal(x)
      }
      val codecOfLongList = make[List[Long]]
      verifyDeser(codecOfLongList, List(1L, 4503599627370496L, 4503599627370497L),
        """["001",4503599627370496,"4503599627370497"]""")
      verifySer(codecOfLongList, List(1L, 4503599627370496L, 4503599627370497L),
        """[1,4503599627370496,"4503599627370497"]""")
    }
    "serialize and deserialize outer types using custom value codecs for boolean type" in {
      implicit val customCodecOfBoolean: JsonValueCodec[_root_.scala.Boolean] = new JsonValueCodec[_root_.scala.Boolean] {
        def nullValue: _root_.scala.Boolean = false

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
      verifyDeser(codecOfFlags, Flags(f1 = true, f2 = false), """{"f1":true,"f2":"False"}""")
      verifySer(codecOfFlags, Flags(f1 = true, f2 = false), """{"f1":"TRUE","f2":"FALSE"}""")
      verifyDeserError(codecOfFlags, """{"f1":"XALSE","f2":true}""", "illegal boolean, offset: 0x0000000c")
      verifyDeserError(codecOfFlags, """{"f1":xalse,"f2":true}""", "illegal boolean, offset: 0x00000006")
    }
    "serialize and deserialize outer types using custom value codecs for double type" in {
      implicit val customCodecOfDouble: JsonValueCodec[Double] = new JsonValueCodec[Double] {
        def nullValue: Double = 0.0f

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
        """["-Infinity","Infinity",0.0,1.0E10]""")
      verifyDeserError(codecOfDoubleArray, """["Inf","-Inf"]""", "illegal double, offset: 0x00000005")
    }
    "serialize zoned date time values into a custom format" in {
      implicit val customCodecOfZonedDateTime: JsonValueCodec[ZonedDateTime] = new JsonValueCodec[ZonedDateTime] {
        private[this] val standardCodec: JsonValueCodec[ZonedDateTime] = JsonCodecMaker.make[ZonedDateTime]

        def nullValue: ZonedDateTime = null

        def decodeValue(in: JsonReader, default: ZonedDateTime): ZonedDateTime = in.readZonedDateTime(default)

        def encodeValue(x: ZonedDateTime, out: JsonWriter): _root_.scala.Unit =
          if (x.getSecond != 0 || x.getNano != 0) out.writeVal(x)
          else {
            val buf = writeToArrayReentrant(x)(standardCodec)
            val len = buf.length
            val newBuf = new Array[_root_.scala.Byte](len + 3)
            var pos = 0
            while ({ // copy up to `:` separator between hours and minutes
              val b = buf(pos)
              newBuf(pos) = b
              pos += 1
              b != ':'
            }) ()
            newBuf(pos) = buf(pos) // copy minutes
            newBuf(pos + 1) = buf(pos + 1)
            pos += 2
            newBuf(pos) = ':'  // set zero seconds
            newBuf(pos + 1) = '0'
            newBuf(pos + 2) = '0'
            while (pos < len) { // copy rest of the value
              newBuf(pos + 3) = buf(pos)
              pos += 1
            }
            out.writeRawVal(newBuf)
          }
      }
      verifySerDeser(make[Array[ZonedDateTime]],
        _root_.scala.Array(ZonedDateTime.parse("2020-04-10T10:07:00Z"), ZonedDateTime.parse("2020-04-10T10:07:10Z")),
        """["2020-04-10T10:07:00Z","2020-04-10T10:07:10Z"]""")
    }
    "parse offset date time values with escaped characters using a custom codec" in {
      implicit val customCodecOfOffsetDateTime: JsonValueCodec[OffsetDateTime] = new JsonValueCodec[OffsetDateTime] {
        private[this] val defaultCodec: JsonValueCodec[OffsetDateTime] = JsonCodecMaker.make[OffsetDateTime]
        private[this] val maxLen = 44 // should be enough for the longest offset date time value
        private[this] val pool = new ThreadLocal[_root_.scala.Array[_root_.scala.Byte]] {
          override def initialValue(): _root_.scala.Array[_root_.scala.Byte] =
            new _root_.scala.Array[_root_.scala.Byte](maxLen + 2)
        }

        def nullValue: OffsetDateTime = null

        def decodeValue(in: JsonReader, default: OffsetDateTime): OffsetDateTime = {
          val buf = pool.get
          val s = in.readString(null)
          val len = s.length
          if (len <= maxLen && {
            buf(0) = '"'
            var bits, i = 0
            while (i < len) {
              val ch = s.charAt(i)
              buf(i + 1) = ch.toByte
              bits |= ch
              i += 1
            }
            buf(i + 1) = '"'
            bits < 0x80
          }) {
            try {
              return readFromSubArrayReentrant(buf, 0, len + 2, ReaderConfig)(defaultCodec)
            } catch {
              case NonFatal(_) => ()
            }
          }
          in.decodeError("illegal offset date time")
        }

        def encodeValue(x: OffsetDateTime, out: JsonWriter): _root_.scala.Unit = out.writeVal(x)
      }
      val codecOfArrayOfOffsetDateTimes = make[Array[OffsetDateTime]]
      verifyDeser(codecOfArrayOfOffsetDateTimes,
        _root_.scala.Array(OffsetDateTime.parse("2020-01-01T12:34:56.789+08:00")),
        """["2020-01-01T12:34:56.789\u002B08:00"]""")
      verifyDeserError(codecOfArrayOfOffsetDateTimes,
        """["x020-01-01T12:34:56.789-08:00"]""", "illegal offset date time, offset: 0x0000001f")
      verifyDeserError(codecOfArrayOfOffsetDateTimes,
        """["2020-01-01ї12:34:56.789-08:00"]""", "illegal offset date time, offset: 0x00000020")
    }
    "serialize and deserialize ADTs with case object values using a custom codec" in {
      implicit val codecOfVersion: JsonValueCodec[Version] = new JsonValueCodec[Version] {
        def nullValue: Version = null.asInstanceOf[Version]

        def encodeValue(x: Version, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.value)

        def decodeValue(in: JsonReader, default: Version): Version =
          in.readString(null) match {
            case "8.10" => Version.Current
            case "8.9" => Version.`8.09`
            case _ => in.decodeError("illegal version")
          }
      }

      case class App(name: String, version: Version)

      val codecOfApp = make[App]
      verifySerDeser(codecOfApp, App("Skype", Version.`Current`), """{"name":"Skype","version":"8.10"}""")
      verifyDeserError(codecOfApp, """{"name":"Skype","version":"9.0"}""", "illegal version, offset: 0x0000001e")
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
        def nullValue: Bar = null.asInstanceOf[Bar]

        def encodeValue(x: Bar, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.asInstanceOf[Int])

        def decodeValue(in: JsonReader, default: Bar): Bar = in.readInt().asInstanceOf[Bar]
      }
      verifySerDeser(make[Baz], Baz(42.asInstanceOf[Bar]), """{"bar":42}""")

      case class OpaqueTypes(uid: UserId2.Opaque, oid: OrderId2.Opaque)

      implicit val customCodecOfUserId2: JsonValueCodec[UserId2.Opaque] = new JsonValueCodec[UserId2.Opaque] {
        def nullValue: UserId2.Opaque = null.asInstanceOf[UserId2.Opaque]

        def encodeValue(x: UserId2.Opaque, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.value)

        def decodeValue(in: JsonReader, default: UserId2.Opaque): UserId2.Opaque = UserId2.Opaque(in.readString(default.value))
      }
      implicit val customCodecOfOrderId2: JsonValueCodec[OrderId2.Opaque] = new JsonValueCodec[OrderId2.Opaque] {
        def nullValue: OrderId2.Opaque = null.asInstanceOf[OrderId2.Opaque]

        def encodeValue(x: OrderId2.Opaque, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.value)

        def decodeValue(in: JsonReader, default: OrderId2.Opaque): OrderId2.Opaque = OrderId2.Opaque(in.readInt())
      }
      verifySerDeser(make[OpaqueTypes],
        OpaqueTypes(UserId2.Opaque("123abc"), OrderId2.Opaque(123123)), """{"uid":"123abc","oid":123123}""")
    }
    "serialize and deserialize non-sealed trait using a custom value codec" in {
      implicit val customCodecOfProperty: JsonValueCodec[Property] =
        new JsonValueCodec[Property] {
          def nullValue: Property = null

          def decodeValue(in: JsonReader, default: Property): Property = {
            val t = in.nextToken()
            in.rollbackToken()
            if (t == '"') StringProperty(in.readString(null))
            else DoubleProperty(in.readDouble())
          }

          def encodeValue(x: Property, out: JsonWriter): _root_.scala.Unit = x match {
            case DoubleProperty(d) => out.writeVal(d)
            case StringProperty(s) => out.writeVal(s)
          }
        }
      verifySerDeser(make[Map[String, Property]],
        Map("a" -> DoubleProperty(4.0), "b" -> StringProperty("bar")), """{"a":4.0,"b":"bar"}""")
    }
    "serialize and deserialize using custom value codecs for `Either` type" in {
      implicit val customCodecOfEither1: JsonValueCodec[Either[String, Int]] =
        new JsonValueCodec[Either[String, Int]] {
          def nullValue: Either[String, Int] = null

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
      verifySerDeser(make[List[Either[String, Int]]], List(Right(1), Left("VVV")), """[1,"VVV"]""")
      implicit val customCodecOfEither2: JsonValueCodec[Either[String, StandardTypes]] =
        new JsonValueCodec[Either[String, StandardTypes]] {
          def nullValue: Either[String, StandardTypes] = null

          def decodeValue(in: JsonReader, default: Either[String, StandardTypes]): Either[String, StandardTypes] =
            (in.nextToken(): @switch) match {
              case '{' =>
                in.rollbackToken()
                Right(codecOfStandardTypes.decodeValue(in, codecOfStandardTypes.nullValue))
              case '"' =>
                in.rollbackToken()
                Left(in.readString(null))
              case _ =>
                in.decodeError("""expected '{' or '"'""")
            }

          def encodeValue(x: Either[String, StandardTypes], out: JsonWriter): _root_.scala.Unit =
            x match {
              case Left(s) => out.writeVal(s)
              case Right(st) => codecOfStandardTypes.encodeValue(st, out)
            }
        }

      case class OuterTypes(s: String, st: Either[String, StandardTypes] = Left("error"))

      val codecOfOuterTypes = make[OuterTypes]
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Right(StandardTypes("VVV", 1, 1.1))),
        """{"s":"X","st":{"s":"VVV","bi":1,"bd":1.1}}""")
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("fatal error")), """{"s":"X","st":"fatal error"}""")
      verifySerDeser(codecOfOuterTypes, OuterTypes("X", Left("error")), """{"s":"X"}""") // st matches with default value
      verifySerDeser(codecOfOuterTypes, OuterTypes("X"), """{"s":"X"}""")
    }
    "serialize and deserialize using custom value codecs for enums" in {
      implicit object codecOfLocationType extends JsonValueCodec[LocationType.LocationType] {
        def nullValue: LocationType.LocationType = null

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
          def nullValue: StandardTypes = null

          def decodeValue(in: JsonReader, default: StandardTypes): StandardTypes = (in.nextToken(): @switch) match {
            case '{' =>
              in.rollbackToken()
              codecOfStandardTypes.decodeValue(in, codecOfStandardTypes.nullValue)
            case '"' =>
              in.rollbackToken()
              readFromStringReentrant(in.readString(null))(codecOfStandardTypes)
            case _ =>
              in.decodeError("""expected '{' or '"'""")
          }

          def encodeValue(x: StandardTypes, out: JsonWriter): _root_.scala.Unit = x.s match {
            case "VVV" => codecOfStandardTypes.encodeValue(x, out)
            case "XXX" => out.writeVal(writeToStringReentrant(x)(codecOfStandardTypes))
          }
        }
      verifySerDeser(customCodecOfStandardTypes, StandardTypes("VVV", 1, 1.1), """{"s":"VVV","bi":1,"bd":1.1}""")
      verifySerDeser(customCodecOfStandardTypes, StandardTypes("XXX", 1, 1.1), """"{\"s\":\"XXX\",\"bi\":1,\"bd\":1.1}"""")
    }
    "serialize and deserialize sequences of tuples as JSON object with duplicated keys using a custom value codec" in {
      val codecOfSeqOfTuples: JsonValueCodec[Seq[(String, Int)]] = new JsonValueCodec[Seq[(String, Int)]] {
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
        def apply(s: String) = new RawVal(s)

        implicit val codec: JsonValueCodec[RawVal] = new JsonValueCodec[RawVal] {
          override def decodeValue(in: JsonReader, default: RawVal): RawVal = new RawVal(in.readRawValAsBytes())

          override def encodeValue(x: RawVal, out: JsonWriter): _root_.scala.Unit = out.writeRawVal(x.bs)

          override val nullValue: RawVal = new RawVal(new _root_.scala.Array[_root_.scala.Byte](0))
        }

        private case class Nested(xx: _root_.java.lang.Boolean, yy: _root_.java.lang.Boolean)

        private case class TopLevel(y: Nested)

        private case object TopLevel {
          implicit val codec: JsonValueCodec[TopLevel] = JsonCodecMaker.make
        }
      }

      case class RawVal private(bs: _root_.scala.Array[_root_.scala.Byte]) {
        def this(s: String) = this(s.getBytes(UTF_8))

        override lazy val hashCode: Int = MurmurHash3.arrayHash(bs)

        override def equals(obj: Any): _root_.scala.Boolean = obj match {
          case that: RawVal => _root_.java.util.Arrays.equals(bs, that.bs)
          case _ => false
        }

        lazy val isValid: _root_.java.lang.Boolean = try {
          val topLevel = readFromArray[RawVal.TopLevel](bs)
          topLevel.y.xx & !topLevel.y.yy
        } catch {
          case NonFatal(_) => false
        }
      }

      case class Message(param1: String, param2: String, payload: RawVal, param3: String)

      val rawVal = RawVal("""{"x":[-1.0,1,4.0E20],"y":{"xx":true,"yy":false,"zz":null},"z":"Z"}""")
      rawVal.isValid shouldBe true
      verifySerDeser(make[Message],
        Message("A", "B", rawVal, "C"),
        """{"param1":"A","param2":"B","payload":{"x":[-1.0,1,4.0E20],"y":{"xx":true,"yy":false,"zz":null},"z":"Z"},"param3":"C"}""")
    }
    "serialize and deserialize nested options without loss of information" in {
      case class NestedOptions(x: Option[Option[Option[String]]])

      val codecOfNestedOptions = make[NestedOptions]
      verifySerDeser(codecOfNestedOptions, NestedOptions(_root_.scala.None), """{}""")
      verifyDeser(codecOfNestedOptions, NestedOptions(_root_.scala.None), """{"x":null}""")
      verifySerDeser(codecOfNestedOptions, NestedOptions(_root_.scala.Some(_root_.scala.Some(_root_.scala.Some("VVV")))),
        """{"x":{"type":"Some","value":{"type":"Some","value":"VVV"}}}""")
      verifySerDeser(codecOfNestedOptions, NestedOptions(_root_.scala.Some(_root_.scala.None)),
        """{"x":{"type":"None"}}""")
      verifySerDeser(codecOfNestedOptions, NestedOptions(_root_.scala.Some(_root_.scala.Some(_root_.scala.None))),
        """{"x":{"type":"Some","value":{"type":"None"}}}""")
    }
    "serialize and deserialize Option[Option[_]] to distinguish `null` field values and missing fields" in {
      case class Model(field1: String, field2: Option[Option[String]])

      verifySerDeser(make[List[Model]](CodecMakerConfig.withSkipNestedOptionValues(true)),
        List(Model("VVV", _root_.scala.Some(_root_.scala.Some("WWW"))), Model("VVV", _root_.scala.None), Model("VVV", _root_.scala.Some(_root_.scala.None))),
        """[{"field1":"VVV","field2":"WWW"},{"field1":"VVV"},{"field1":"VVV","field2":null}]""")
    }
    "serialize and deserialize Nullable[_] to distinguish `null` field values and missing fields using a custom value codec" in {
      sealed trait Nullable[+A]

      case class Value[A](a: A) extends Nullable[A]

      case object Missing extends Nullable[Nothing]

      case object NullValue extends Nullable[Nothing]

      object Nullable {
        def codec[A](valueCodec: JsonValueCodec[A]): JsonValueCodec[Nullable[A]] = new JsonValueCodec[Nullable[A]] {
          override def decodeValue(in: JsonReader, default: Nullable[A]): Nullable[A] =
            if (in.isNextToken('n')) in.readNullOrError(NullValue, "expected `null` value")
            else {
              in.rollbackToken()
              new Value(valueCodec.decodeValue(in, null.asInstanceOf[A]))
            }

          override def encodeValue(x: Nullable[A], out: JsonWriter): _root_.scala.Unit =
            x match {
              case v: Value[A] => valueCodec.encodeValue(v.a, out)
              case NullValue => out.writeNull()
              case Missing => out.encodeError("cannot serialize `Missing` out of a class instance")
            }

          override def nullValue: Nullable[A] = Missing
        }

        implicit val stringCodec: JsonValueCodec[Nullable[String]] = codec(make[String])
      }

      case class Model(field1: String, field2: Nullable[String] = Missing)

      verifySerDeser(make[List[Model]], List(Model("VVV", Value("WWW")), Model("VVV", Missing), Model("VVV", NullValue)),
        """[{"field1":"VVV","field2":"WWW"},{"field1":"VVV"},{"field1":"VVV","field2":null}]""")
    }
    "serialize and deserialize case classes as JSON arrays using a custom value codec" in {
      case class Obj(keyValues: Seq[KeyValue])

      case class KeyValue(key: String, value: String)

      implicit val codecOfKeyValue: JsonValueCodec[KeyValue] = new JsonValueCodec[KeyValue] {
        override def decodeValue(in: JsonReader, default: KeyValue): KeyValue =
          if (in.isNextToken('[')) {
            val k = in.readString(null)
            if (!in.isNextToken(',')) in.commaError()
            val v = in.readString(null)
            if (!in.isNextToken(']')) in.arrayEndError()
            new KeyValue(k, v)
          } else in.readNullOrTokenError(default, '[')

        override def encodeValue(x: KeyValue, out: JsonWriter): _root_.scala.Unit = {
          out.writeArrayStart()
          out.writeVal(x.key)
          out.writeVal(x.value)
          out.writeArrayEnd()
        }

        override def nullValue: KeyValue = null
      }
      verifySerDeser(make[Obj], Obj(Seq(KeyValue("a", "1"), KeyValue("b", "2"), KeyValue("c", "3"))),
        """{"keyValues":[["a","1"],["b","2"],["c","3"]]}""")
    }
    "serialize and deserialize JSON objects to case classes with maps using a custom value codec" in {
      case class Lang(id: String, content: String)

      case class Doc(id: String, tags: List[String], langs: Map[String, Lang])

      def customDocCodec(supportedLangs: Set[String]): JsonValueCodec[Doc] = new JsonValueCodec[Doc] {
        private[this] val langCodec: JsonValueCodec[Lang] = JsonCodecMaker.make
        private[this] val listOfStringCodec: JsonValueCodec[List[String]] = JsonCodecMaker.make

        override def decodeValue(in: JsonReader, default: Doc): Doc =  if (in.isNextToken('{')) {
          var _id: String = null
          var _tags: List[String] = _root_.scala.Nil
          var _langs: Map[String, Lang] = Map.empty
          var p0 = 0x3
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            var s: String = null
            while ((s eq null) || in.isNextToken(',')) {
              s = in.readKeyAsString()
              if (s == "id") {
                if ((p0 & 0x1) != 0) p0 ^= 0x1
                else duplicatedKeyError(in, s)
                _id = in.readString(_id)
              } else if (s == "tags") {
                if ((p0 & 0x2) != 0) p0 ^= 0x2
                else duplicatedKeyError(in, s)
                _tags = listOfStringCodec.decodeValue(in, _tags)
              } else if (supportedLangs.contains(s)) {
                if (_langs.contains(s)) duplicatedKeyError(in, s)
                _langs = _langs.updated(s, langCodec.decodeValue(in, langCodec.nullValue))
              } else in.skip()
            }
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          }
          if ((p0 & 0x1) != 0) in.requiredFieldError("id")
          new Doc(id = _id, tags = _tags, langs = _langs)
        } else in.readNullOrTokenError(default, '{')

        override def encodeValue(x: Doc, out: JsonWriter): _root_.scala.Unit = {
          out.writeObjectStart()
          out.writeNonEscapedAsciiKey("id")
          out.writeVal(x.id)
          out.writeNonEscapedAsciiKey("tags")
          listOfStringCodec.encodeValue(x.tags, out)
          x.langs.foreach { kv =>
            out.writeNonEscapedAsciiKey(kv._1)
            langCodec.encodeValue(kv._2, out)
          }
          out.writeObjectEnd()
        }

        override def nullValue: Doc = null

        private[this] def duplicatedKeyError(in: JsonReader, fieldName: String): Throwable =
          in.decodeError("duplicated field \"" + fieldName + "\" ")
      }

      val json = """{"id":"1","tags":["a","b"],"en":{"id":"en-1","content":"English text"},"de":{"id":"de-1","content":"German text"}}"""
      verifySerDeser(customDocCodec(Set("en", "de")),
        Doc("1", List("a", "b"), Map("en" -> Lang("en-1", "English text"), "de" -> Lang("de-1", "German text"))),
        json)
    }
    "serialize and deserialize case classes with refined type fields" in {
      case class Preference[V](key: String, kind: Kind.Aux[V], value: V)

      verifySerDeser(make[Preference[LocalDate]],
        Preference[LocalDate]("VVV", Kind.of[LocalDate]("LocalDate"), LocalDate.of(2022, 7, 18)),
        """{"key":"VVV","kind":"LocalDate","value":"2022-07-18"}""")
      verifySerDeser(make[Preference[Double]],
        Preference[Double]("WWW", Kind.of[Double]("Double"), 1.2),
        """{"key":"WWW","kind":"Double","value":1.2}""")
    }
    "serialize and deserialize generic options using an implicit function" in {
      implicit def make[A : JsonValueCodec]: JsonValueCodec[Option[A]] = JsonCodecMaker.make

      implicit val aCodec: JsonValueCodec[String] = JsonCodecMaker.make
      verifySerDeser(implicitly[JsonValueCodec[Option[String]]], Option("VVV"), """"VVV"""")
      verifySerDeser(implicitly[JsonValueCodec[Option[String]]], _root_.scala.None, """null""")
    }
    "serialize and deserialize generic lists using an implicit function" in {
      implicit def make[A : JsonValueCodec]: JsonValueCodec[List[A]] = JsonCodecMaker.make

      implicit val aCodec: JsonValueCodec[String] = JsonCodecMaker.make
      verifySerDeser(implicitly[JsonValueCodec[List[String]]], List("VVV", "WWW"), """["VVV","WWW"]""")
    }
    "serialize and deserialize generic classes using an implicit function" in {
      case class GenDoc[A, B, C](a: A, opt: Option[B], list: List[C])

      object GenDoc {
        implicit def make[A : JsonValueCodec, B : JsonValueCodec, C : JsonValueCodec]: JsonValueCodec[GenDoc[A, B, C]] =
          JsonCodecMaker.make
      }

      implicit val aCodec: JsonValueCodec[_root_.scala.Boolean] = JsonCodecMaker.make
      implicit val bCodec: JsonValueCodec[String] = JsonCodecMaker.make
      implicit val cCodec: JsonValueCodec[Int] = JsonCodecMaker.make
      verifySerDeser(implicitly[JsonValueCodec[GenDoc[_root_.scala.Boolean, String, Int]]],
        GenDoc(true, _root_.scala.Some("VVV"), List(1, 2, 3)), """{"a":true,"opt":"VVV","list":[1,2,3]}""")
    }
    "don't generate codecs for generic classes and an implicit function with a missing codec" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class GenDoc[A, B, C](a: A, opt: Option[B], list: List[C])
          |
          |object GenDoc {
          |  implicit def make[A, B : JsonValueCodec, C : JsonValueCodec]: JsonValueCodec[GenDoc[A, B, C]] =
          |    JsonCodecMaker.make
          |}
          |""".stripMargin
      }).getMessage.contains(if (ScalaVersionCheck.isScala2) {
        "Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'A' or provide a custom implicitly accessible codec for it."
      } else {
        "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for 'A'."
      }))
    }
    "serialize and deserialize case classes with self-type" in {
      import Person._

      verifySerDeser(myEventCodec, MyEvent(Props("John", 42)), """{"props":{"name":"John","age":42}}""")
    }
    "serialize and deserialize case classes with value classes" in {
      case class ValueClassTypes(uid: UserId, oid: OrderId)

      verifySerDeser(make[ValueClassTypes],
        ValueClassTypes(UserId("123abc"), OrderId(123123)), """{"uid":"123abc","oid":123123}""")
    }
    "serialize and deserialize collection fields of value classes" in {
      verifySerDeser(make[UserIds], UserIds(List("VVV", "WWW")), """["VVV","WWW"]""")
    }
    "serialize and deserialize top-level value classes" in {
      verifySerDeser(make[UserId], UserId("123abc"), """"123abc"""")
      verifySerDeser(make[OrderId], OrderId(123123), "123123")
    }
    "serialize and deserialize arrays of value classes" in {
      verifySerDeser(make[Array[UserId]], _root_.scala.Array(UserId("123abc"), UserId("123def")), """["123abc","123def"]""")
      verifySerDeser(make[Array[OrderId]], _root_.scala.Array(OrderId(123123), OrderId(123456)), "[123123,123456]")
    }
    "serialize and deserialize stringified top-level value classes" in {
      verifySerDeser(make[OrderId](CodecMakerConfig.withIsStringified(true)), OrderId(123123), """"123123"""")
    }
    "serialize and deserialize value classes with type aliases" in {
      verifySerDeser(make[Alias.UserId], Alias.UserId("123abc"), """"123abc"""")
      verifySerDeser(make[Alias.OrderId], Alias.OrderId(123123), "123123")
    }
    "serialize and deserialize value classes with generic type values" in {
      verifySerDeser(make[Generic.UserId[String]], Generic.UserId[String]("123abc"), """"123abc"""")
      verifySerDeser(make[Generic.OrderId[Int]], Generic.OrderId[Int](123123), "123123")
    }    
    "serialize and deserialize case classes with one value classes when turned on inlining of one value classes" in {
      case class UserId(value: String)

      case class OrderId(value: Int)

      case class OneValueClassTypes(uid: UserId, oid: OrderId)

      verifySerDeser(make[OneValueClassTypes](CodecMakerConfig.withInlineOneValueClasses(true)),
        OneValueClassTypes(UserId("123abc"), OrderId(123123)), """{"uid":"123abc","oid":123123}""")
    }
    "serialize and deserialize top-level value one value classes when turned on inlining of one value classes" in {
      case class UserId(value: String)

      case class OrderId(value: Int)

      verifySerDeser(make[UserId](CodecMakerConfig.withInlineOneValueClasses(true)), UserId("123abc"), """"123abc"""")
      verifySerDeser(make[OrderId](CodecMakerConfig.withInlineOneValueClasses(true)), OrderId(123123), "123123")
    }
    "serialize and deserialize arrays of one value classes when turned on inlining of one value classes" in {
      case class UserId(value: String)

      case class OrderId(value: Int)

      verifySerDeser(make[Array[UserId]](CodecMakerConfig.withInlineOneValueClasses(true)),
        _root_.scala.Array(UserId("123abc"), UserId("123def")), """["123abc","123def"]""")
      verifySerDeser(make[Array[OrderId]](CodecMakerConfig.withInlineOneValueClasses(true)),
        _root_.scala.Array(OrderId(123123), OrderId(123456)), "[123123,123456]")
    }
    "serialize and deserialize a collection field of one value classes" in {
      case class UserIds(l: List[String])

      verifySerDeser(make[UserIds](CodecMakerConfig.withInlineOneValueClasses(true)),
        UserIds(List("VVV", "WWW")), """["VVV","WWW"]""")
    }
    "serialize and deserialize a generic type field of one value classes" in {
      case class UserIds[T](l: T)

      verifySerDeser(make[UserIds[List[String]]](CodecMakerConfig.withInlineOneValueClasses(true)),
        UserIds(List("VVV", "WWW")), """["VVV","WWW"]""")
    }
    "serialize and deserialize a higher kind type field of one value classes" in {
      case class UserIds[F[_], A](l: F[A])

      verifySerDeser(make[UserIds[List, String]](CodecMakerConfig.withInlineOneValueClasses(true)),
        UserIds(List("VVV", "WWW")), """["VVV","WWW"]""")
    }
    "serialize and deserialize case classes with options" in {
      verifySerDeser(codecOfOptions,
        Options(Option("VVV"), Option(BigInt(4)), Option(Set()), Option(1L), Option(_root_.java.lang.Long.valueOf(2L)),
          Option(StandardTypes("WWW", BigInt(2), BigDecimal(3.3)))),
        """{"os":"VVV","obi":4,"osi":[],"ol":1,"ojl":2,"ost":{"s":"WWW","bi":2,"bd":3.3}}""")
      verifySerDeser(codecOfOptions,
        Options(_root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None,
          _root_.scala.None),
        """{}""")
    }
    "serialize and deserialize top-level options using null for None values" in {
      val codecOfOptionOfInt = make[Option[Int]]
      verifySerDeser(codecOfOptionOfInt, _root_.scala.None, "null")
      verifySerDeser(codecOfOptionOfInt, _root_.scala.Some(1), "1")

      case class Inner(a: Int, b: _root_.scala.Boolean, c: String)

      val codecOfOptionOfInner = make[Option[Inner]]
      verifySerDeser(codecOfOptionOfInner, _root_.scala.None, "null")
      verifySerDeser(codecOfOptionOfInner, _root_.scala.Some(Inner(1, b = true, "VVV")), """{"a":1,"b":true,"c":"VVV"}""")
    }
    "serialize and deserialize options in collections using null for None values" in {
      verifySerDeser(make[Array[Option[Int]]], _root_.scala.Array(_root_.scala.None, _root_.scala.Some(1)), "[null,1]")
      verifySerDeser(make[List[Option[Int]]], List(_root_.scala.Some(1), _root_.scala.None), "[1,null]")
      verifySerDeser(make[Set[Option[Int]]], Set(_root_.scala.Some(1), _root_.scala.None), "[1,null]")
      verifySerDeser(make[Map[String, Option[Int]]], Map("VVV" -> _root_.scala.None, "WWW" -> _root_.scala.Some(1)),
        """{"VVV":null,"WWW":1}""")
    }
    "don't generate codecs when options are used as keys in maps" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[Map[Option[Int], Option[String]]]"
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[_]' defined for 'Option[Int]'."
        else "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[_ >: scala.Nothing <: scala.Any]' defined for 'scala.Option[scala.Int]'."
      })
    }
    "serialize case classes with empty options as null when the transientNone flag is off" in {
      verifySerDeser(make[Options](CodecMakerConfig.withTransientNone(false)),
        Options(Option("VVV"), Option(BigInt(4)), Option(Set()), Option(1L), Option(_root_.java.lang.Long.valueOf(2L)),
          Option(StandardTypes("WWW", BigInt(2), BigDecimal(3.3)))),
        """{"os":"VVV","obi":4,"osi":[],"ol":1,"ojl":2,"ost":{"s":"WWW","bi":2,"bd":3.3}}""")
      verifySerDeser(make[Options](CodecMakerConfig.withTransientNone(false)),
        Options(_root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None, _root_.scala.None,
          _root_.scala.None),
        """{"os":null,"obi":null,"osi":null,"ol":null,"ojl":null,"ost":null}""")
    }
    "serialize and deserialize top-level options" in {
      val codecOfStringOption = make[Option[String]]
      verifySerDeser(codecOfStringOption, _root_.scala.Some("VVV"), """"VVV"""")
      verifySerDeser(codecOfStringOption, _root_.scala.None, "null")
    }
    "serialize and deserialize stringified top-level numeric options" in {
      val codecOfStringifiedOption = make[Option[BigInt]](CodecMakerConfig.withIsStringified(true))
      verifySerDeser(codecOfStringifiedOption, _root_.scala.Some(BigInt(123)), """"123"""")
      verifySerDeser(codecOfStringifiedOption, _root_.scala.None, "null")
    }
    "throw parse exception in case of unexpected value for option" in {
      verifyDeserError(make[Option[String]], """no!!!""", "expected value or null, offset: 0x00000001")
    }
    "serialize and deserialize case classes with tuples" in {
      verifySerDeser(codecOfTuples, Tuples((1, 2.2, List('V')), ("VVV", 3, _root_.scala.Some(LocationType.GPS))),
        """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS"]}""")
    }
    "throw parse exception in case of unexpected number of JSON array values" in {
      verifyDeserError(codecOfTuples, """{"t1":[1,2.2],"t2":["VVV",3,"GPS"]}""", "expected ',', offset: 0x0000000c")
      verifyDeserError(codecOfTuples, """{"t1":[1,2.2,["V"]],"t2":["VVV",3,"GPS","XXX"]}""",
        "expected ']', offset: 0x00000027")
    }
    "serialize and deserialize top-level tuples" in {
      verifySerDeser(make[(String, Int)], ("VVV", 1), """["VVV",1]""")
    }
    "serialize and deserialize stringified top-level numeric tuples" in {
      verifySerDeser(make[(Long, Float, BigDecimal)](CodecMakerConfig.withIsStringified(true)),
        (1L, 2.2f, BigDecimal(3.3)), """["1","2.2","3.3"]""")
    }
    "serialize and deserialize tuples with type aliases" in {
      type I = Int
      type S = String

      case class Tuples(t1: (S, I), t2: (I, S))

      verifySerDeser(make[Tuples], Tuples(("VVV", 1), (2, "WWW")),
        """{"t1":["VVV",1],"t2":[2,"WWW"]}""")
      verifySerDeser(make[(S, I)], ("VVV", 1), """["VVV",1]""")
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
      val arrays = Arrays(_root_.scala.Array(_root_.scala.Array[Int](), _root_.scala.Array[Int]()), _root_.scala.Array[BigInt]())
      verifySer(codecOfArrays, arrays, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "serialize fields of case classes with empty arrays when transientEmpty is off" in {
      val json = """{"aa":[[],[]],"a":[]}"""
      val arrays = Arrays(_root_.scala.Array(_root_.scala.Array[Int](), _root_.scala.Array[Int]()), _root_.scala.Array[BigInt]())
      verifySer(make[Arrays](CodecMakerConfig.withTransientEmpty(false)), arrays, json)
      val parsedObj = readFromArray(json.getBytes(UTF_8))(codecOfArrays)
      parsedObj.aa shouldBe arrays.aa
      parsedObj.a shouldBe arrays.a
    }
    "throw parse exception for missing array field when the requireCollectionFields flag is on" in {
      val codecOfArrays1 = makeWithRequiredCollectionFields[Arrays]
      verifyDeserError(codecOfArrays1, "{}", """missing required field "aa", offset: 0x00000001""")
      verifyDeserError(codecOfArrays1, """{"aa":[[],[]]}""", """missing required field "a", offset: 0x0000000d""")
      val codecOfArrays2 = makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[Arrays]
      verifyDeserError(codecOfArrays2, "{}", """missing required field "aa", offset: 0x00000001""")
      verifyDeserError(codecOfArrays2, """{"aa":[[],[]]}""", """missing required field "a", offset: 0x0000000d""")
    }
    "throw parse exception in case of JSON array is not properly started/closed or with leading/trailing comma" in {
      verifyDeserError(codecOfArrays, """{"aa":[{1,2,3]],"a":[]}""", "expected '[' or null, offset: 0x00000007")
      verifyDeserError(codecOfArrays, """{"aa":[[,1,2,3]],"a":[]}""", "illegal number, offset: 0x00000008")
      verifyDeserError(codecOfArrays, """{"aa":[[1,2,3}],"a":[]}""", "expected ']' or ',', offset: 0x0000000d")
      verifyDeserError(codecOfArrays, """{"aa":[[1,2,3,]],"a":[]}""", "illegal number, offset: 0x0000000e")
    }
    "serialize and deserialize case classes with Iterators" in {
      val obj = readFromString("""{"s":[["1","2","3"]],"i":[4,5,6]}""")(codecOfIterators)
      writeToString(obj)(codecOfIterators) shouldBe """{"s":[["1","2","3"]],"i":[4,5,6]}"""
    }
    "serialize and deserialize top-level Iterators" in {
      val codecOfIteratorOfIteratorOfInt = make[Iterator[Iterator[Int]]]
      val obj = readFromString("""[[1,2,3],[4,5,6]]""")(codecOfIteratorOfIteratorOfInt)
      writeToString(obj)(codecOfIteratorOfIteratorOfInt) shouldBe """[[1,2,3],[4,5,6]]"""
    }
    "don't serialize fields of case classes with empty Iterators" in {
      val obj = readFromString("""{"s":[[]],"i":[]}""")(codecOfIterators)
      writeToString(obj)(codecOfIterators) shouldBe """{"s":[[]]}"""
    }
    "serialize fields of case classes with empty Iterators when transientEmpty is off" in {
      val obj = readFromString("""{"s":[[]],"i":[]}""")(codecOfIterators)
      writeToString(obj)(make[Iterators](CodecMakerConfig.withTransientEmpty(false))) shouldBe """{"s":[[]],"i":[]}"""
    }
    "serialize and deserialize case classes with Iterables" in {
      case class Iterables(
        s: _root_.scala.collection.Set[_root_.scala.collection.SortedSet[String]],
        is: _root_.scala.collection.IndexedSeq[_root_.scala.collection.Seq[Float]],
        i: _root_.scala.collection.Iterable[Int])

      val codecOfIterables = make[Iterables]
      verifySerDeser(codecOfIterables,
        Iterables(_root_.scala.collection.Set(_root_.scala.collection.SortedSet("1", "2", "3")),
          _root_.scala.collection.IndexedSeq(_root_.scala.collection.Seq(1.1f, 2.2f, 3.3f)),
          _root_.scala.collection.Iterable(4, 5, 6)),
        """{"s":[["1","2","3"]],"is":[[1.1,2.2,3.3]],"i":[4,5,6]}""")
      verifySer(codecOfIterables,
        Iterables(_root_.scala.collection.immutable.Set(_root_.scala.collection.immutable.SortedSet("1", "2", "3")),
          _root_.scala.  collection.immutable.IndexedSeq(_root_.scala.collection.immutable.Seq(1.1f, 2.2f, 3.3f)),
          _root_.scala.collection.immutable.Iterable(4, 5, 6)),
        """{"s":[["1","2","3"]],"is":[[1.1,2.2,3.3]],"i":[4,5,6]}""")
      verifySer(codecOfIterables,
        Iterables(_root_.scala.collection.mutable.Set(_root_.scala.collection.mutable.SortedSet("1", "2", "3")),
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
      case class ImmutableIterables(
        l: List[_root_.scala.collection.immutable.ListSet[String]],
        nl: ::[::[Int]],
        q: _root_.scala.collection.immutable.Queue[Set[BigInt]],
        is: IndexedSeq[_root_.scala.collection.immutable.SortedSet[Int]],
        s: Stream[_root_.scala.collection.immutable.TreeSet[Double]],
        v: Vector[Iterable[Long]])

      val codecOfImmutableIterables = make[ImmutableIterables]
      verifySerDeser(codecOfImmutableIterables,
        ImmutableIterables(List(_root_.scala.collection.immutable.ListSet("1")),
          ::(::(2, ::(3, _root_.scala.Nil)), _root_.scala.Nil), _root_.scala.collection.immutable.Queue(Set[BigInt](4)),
          IndexedSeq(_root_.scala.collection.immutable.SortedSet(5, 6, 7), _root_.scala.collection.immutable.SortedSet()),
          Stream(_root_.scala.collection.immutable.TreeSet(8.9)), Vector(Iterable(10L, 11L))),
        """{"l":[["1"]],"nl":[[2,3]],"q":[[4]],"is":[[5,6,7],[]],"s":[[8.9]],"v":[[10,11]]}""")
      verifyDeserError(codecOfImmutableIterables,
        """{"l":[["1"]],"nl":[[]],"q":[[4]],"is":[[5,6,7],[]],"s":[[8.9]],"v":[[10,11]]}""",
        "expected non-empty JSON array")
      verifyDeserError(codecOfImmutableIterables,
        """{"l":[["1"]],"nl":[],"q":[[4]],"is":[[5,6,7],[]],"s":[[8.9]],"v":[[10,11]]}""",
        "expected non-empty JSON array")
      verifyDeserError(codecOfImmutableIterables,
        """{"l":[["1"]],"nl":null,"q":[[4]],"is":[[5,6,7],[]],"s":[[8.9]],"v":[[10,11]]}""",
        "expected non-empty JSON array")
    }
    "serialize and deserialize top-level ::" in {
      val codecOfNonEmptyListOfInts = make[::[Int]]
      verifySerDeser(codecOfNonEmptyListOfInts, ::(1, ::(2, ::(3, _root_.scala.Nil))), "[1,2,3]")
      verifyDeserError(codecOfNonEmptyListOfInts, "[]", "expected non-empty JSON array")
      verifyDeserError(codecOfNonEmptyListOfInts, "null", "expected non-empty JSON array")
    }
    "serialize and deserialize case class fields with empty iterables when transientEmpty is off" in {
      verifySerDeser(make[EmptyIterables](CodecMakerConfig.withTransientEmpty(false)),
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":[],"a":[]}""")
      verifySerDeser(make[EmptyIterables](CodecMakerConfig.withTransientEmpty(false)),
        EmptyIterables(List("VVV"), _root_.scala.collection.mutable.ArrayBuffer(1)), """{"l":["VVV"],"a":[1]}""")
    }
    "throw parse exception for missing collection field when the requireCollectionFields flag is on" in {
      verifyDeserError(makeWithRequiredCollectionFields[EmptyIterables],
        "{}", """missing required field "l", offset: 0x00000001""")

      case class NestedIterables(lessi: List[Either[String, Set[Int]]])

      val codecOfNestedIterables = make[NestedIterables](CodecMakerConfig.withFieldNameMapper {
        case "b" => "value"
        case "a" => "value"
      }.withRequireCollectionFields(true).withTransientEmpty(false))
      verifyDeserError(codecOfNestedIterables, """{"lessi":[{"type":"Left"}]}""",
        """missing required field "value", offset: 0x00000018""")
      verifyDeserError(codecOfNestedIterables, """{"lessi":[{"type":"Right"}]}""",
        """missing required field "value", offset: 0x00000019""")
    }
    "serialize and deserialize case classes with collection fields that has default values when the requireCollectionFields flag is on" in {
      verifySerDeser(makeWithRequiredCollectionFields[IterablesWithDefaults], IterablesWithDefaults(), "{}")
      verifySerDeser(make[IterablesWithDefaults](CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)),
        IterablesWithDefaults(), "{}")
    }
    "serialize and deserialize case classes with collection fields that has default values when the requireDefaultFields flag is on" in {
      verifySerDeser(makeWithRequiredDefaultFields[IterablesWithDefaults], IterablesWithDefaults(), "{}")
      verifySerDeser(make[IterablesWithDefaults](CodecMakerConfig.withRequireDefaultFields(true).withTransientDefault(false)),
        IterablesWithDefaults(), "{}")
    }
    "throw parse exception for missing collection with default value when both the requireCollectionFields and the requireDefaultFields flags are on" in {
      val codecOfDefaults2 = make[IterablesWithDefaults](CodecMakerConfig
        .withRequireDefaultFields(true).withTransientDefault(false).withRequireCollectionFields(true).withTransientEmpty(false))
      verifyDeserError(codecOfDefaults2, "{}", """missing required field "l", offset: 0x00000001""")
      verifyDeserError(codecOfDefaults2, """{"l":[1,2,3]}""", """missing required field "s", offset: 0x0000000c""")
    }
    "serialize and deserialize case classes with empty Iterables when the requireCollectionFields flag is on and transientEmpty is off" in {
      verifySerDeser(makeWithRequiredCollectionFields[EmptyIterables],
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":[],"a":[]}""")
    }
    "deserialize null values as empty Iterables for fields with collection types" in {
      verifyDeser(make[EmptyIterables],
        EmptyIterables(List(), _root_.scala.collection.mutable.ArrayBuffer()), """{"l":null,"a":null}""")
      verifyDeser(makeWithRequiredCollectionFields[EmptyIterables],
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
      verifyDeserError(makeWithRequiredCollectionFields[EmptyMaps],
        "{}", """missing required field "im", offset: 0x00000001""")
    }
    "serialize and deserialize case classes with empty maps when the requireCollectionFields flag is on and transientEmpty is off" in {
      verifySerDeser(makeWithRequiredCollectionFields[EmptyMaps],
        EmptyMaps(Map(), _root_.scala.collection.mutable.Map()), """{"im":{},"mm":{}}""")
    }
    "deserialize null values as empty maps for fields with map types" in {
      verifyDeser(make[EmptyMaps],
        EmptyMaps(Map(), _root_.scala.collection.mutable.Map()), """{"im":null,"mm":null}""")
      verifyDeser(makeWithRequiredCollectionFields[EmptyMaps],
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
        """expected '"', offset: 0x00000006""")
      verifyDeserError(codecOfImmutableMaps, """{"m":{"1":1.1],"hm":{},"sm":{}""",
        "expected '}' or ',', offset: 0x0000000d")
      verifyDeserError(codecOfImmutableMaps, """{"m":{"1":1.1,},"hm":{},"sm":{}""",
        """expected '"', offset: 0x0000000e""")
    }
    "throw parse exception in case of JSON array of JSON arrays is not properly started/closed or with leading/trailing comma" in {
      val codecOfMapAsArray = make[_root_.scala.collection.Map[Int, _root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true))
      verifyDeserError(codecOfMapAsArray, """{[1,true]]""", """expected '[' or null, offset: 0x00000000""")
      verifyDeserError(codecOfMapAsArray, """[,[1,true]]""", """expected '[', offset: 0x00000001""")
      verifyDeserError(codecOfMapAsArray, """[[1,true,]]""", """expected ']', offset: 0x00000008""")
      verifyDeserError(codecOfMapAsArray, """[[1,true]}""", """expected ']' or ',', offset: 0x00000009""")
      verifyDeserError(codecOfMapAsArray, """[[1,true],]""", """expected '[', offset: 0x0000000a""")
      val codecOfMapAsArray2 = make[_root_.scala.collection.Map[Int, _root_.scala.Boolean]](CodecMakerConfig.withMapAsArray(true).withMapMaxInsertNumber(Int.MaxValue))
      verifyDeserError(codecOfMapAsArray2, """{[1,true]]""", """expected '[' or null, offset: 0x00000000""")
      verifyDeserError(codecOfMapAsArray2, """[,[1,true]]""", """expected '[', offset: 0x00000001""")
      verifyDeserError(codecOfMapAsArray2, """[[1,true,]]""", """expected ']', offset: 0x00000008""")
      verifyDeserError(codecOfMapAsArray2, """[[1,true]}""", """expected ']' or ',', offset: 0x00000009""")
      verifyDeserError(codecOfMapAsArray2, """[[1,true],]""", """expected '[', offset: 0x0000000a""")
    }
    "throw parse exception in case of illegal keys found during deserialization of maps" in {
      verifyDeserError(codecOfMutableMaps, """{"m":{"1.1":{"null":"2"}}""", "illegal number, offset: 0x0000000e")
    }
    "serialize and deserialize case classes with mutable long maps" in {
      case class MutableLongMaps(
        lm1: _root_.scala.collection.mutable.LongMap[Double],
        lm2: _root_.scala.collection.mutable.LongMap[String])

      verifySerDeser(make[MutableLongMaps],
        MutableLongMaps(_root_.scala.collection.mutable.LongMap(1L -> 1.1),
          _root_.scala.collection.mutable.LongMap(3L -> "33")), """{"lm1":{"1":1.1},"lm2":{"3":"33"}}""")
    }
    "serialize and deserialize case classes with immutable int and long maps" in {
      case class ImmutableIntLongMaps(
        im: _root_.scala.collection.immutable.IntMap[Double],
        lm: _root_.scala.collection.immutable.LongMap[String])

      verifySerDeser(make[ImmutableIntLongMaps],
        ImmutableIntLongMaps(_root_.scala.collection.immutable.IntMap(1 -> 1.1, 2 -> 2.2),
          _root_.scala.collection.immutable.LongMap(3L -> "33")), """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""")
    }
    "serialize and deserialize case classes with mutable & immutable bitsets" in {
      case class BitSets(
        bs: _root_.scala.collection.BitSet,
        ibs: _root_.scala.collection.immutable.BitSet,
        mbs: _root_.scala.collection.mutable.BitSet)

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
        "JsonCodecMaker.make[Map[_root_.java.util.Date, String]]"
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) {
          "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[_]' defined for 'java.util.Date'."
        } else {
          "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[_ >: scala.Nothing <: scala.Any]' defined for '_root_.java.util.Date'."
        }
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
        """missing required field "camelCase", offset: 0x0000006e""")
      verifyDeserError(codecOfEnforcedCamelCase,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        """missing required field "camelCase", offset: 0x0000006e""")
      verifyDeserError(codecOfEnforcedCamelCase,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        """missing required field "camelCase", offset: 0x00000066""")
    }
    "serialize and deserialize with keys enforced to snake_case and throw parse exception when they are missing" in {
      val codec_of_enforced_snake_case =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case))
      verifySerDeser(codec_of_enforced_snake_case, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""")
      verifyDeserError(codec_of_enforced_snake_case,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        """missing required field "camel_case", offset: 0x00000066""")
      verifyDeserError(codec_of_enforced_snake_case,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        """missing required field "camel_case", offset: 0x0000006e""")
      verifyDeserError(codec_of_enforced_snake_case,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        """missing required field "camel_case", offset: 0x00000066""")
    }
    "serialize and deserialize with keys enforced to snake_case2 and throw parse exception when they are missing" in {
      val codec_of_enforced_snake_case2 =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case2))
      verifySerDeser(codec_of_enforced_snake_case2, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""")
      verifyDeserError(codec_of_enforced_snake_case2,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        """missing required field "camel_case", offset: 0x00000066""")
      verifyDeserError(codec_of_enforced_snake_case2,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        """missing required field "camel_case", offset: 0x0000006e""")
      verifyDeserError(codec_of_enforced_snake_case2,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        """missing required field "camel_case", offset: 0x00000066""")
    }
    "serialize and deserialize with keys enforced to kebab-case and throw parse exception when they are missing" in {
      val `codec-of-enforced-kebab-case` =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.`enforce-kebab-case`))
      verifySerDeser(`codec-of-enforced-kebab-case`, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""")
      verifyDeserError(`codec-of-enforced-kebab-case`,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        """missing required field "camel-case", offset: 0x00000066""")
      verifyDeserError(`codec-of-enforced-kebab-case`,
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""",
        """missing required field "camel-case", offset: 0x0000006e""")
      verifyDeserError(`codec-of-enforced-kebab-case`,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        """missing required field "camel-case", offset: 0x00000066""")
    }
    "serialize and deserialize with keys enforced to kebab-case2 and throw parse exception when they are missing" in {
      val `codec-of-enforced-kebab-case2` =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.`enforce-kebab-case2`))
      verifySerDeser(`codec-of-enforced-kebab-case2`, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""")
      verifyDeserError(`codec-of-enforced-kebab-case2`,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        """missing required field "camel-case", offset: 0x00000066""")
      verifyDeserError(`codec-of-enforced-kebab-case2`,
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""",
        """missing required field "camel-case", offset: 0x0000006e""")
      verifyDeserError(`codec-of-enforced-kebab-case2`,
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""",
        """missing required field "camel-case", offset: 0x00000066""")
    }
    "serialize and deserialize with keys enforced to PascalCase and throw parse exception when they are missing" in {
      val CodecOfEnforcedPascalCase =
        make[CamelPascalSnakeKebabCases](CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.EnforcePascalCase))
      verifySerDeser(CodecOfEnforcedPascalCase, CamelPascalSnakeKebabCases(1, 2, 3, 4, 5, 6, 7, 8),
        """{"CamelCase":1,"PascalCase":2,"SnakeCase":3,"KebabCase":4,"Camel1":5,"Pascal1":6,"Snake1":7,"Kebab1":8}""")
      verifyDeserError(CodecOfEnforcedPascalCase,
        """{"camelCase":1,"pascalCase":2,"snakeCase":3,"kebabCase":4,"camel1":5,"pascal1":6,"snake1":7,"kebab1":8}""",
        """missing required field "CamelCase", offset: 0x00000066""")
      verifyDeserError(CodecOfEnforcedPascalCase,
        """{"camel_case":1,"pascal_case":2,"snake_case":3,"kebab_case":4,"camel_1":5,"pascal_1":6,"snake_1":7,"kebab_1":8}""",
        """missing required field "CamelCase", offset: 0x0000006e""")
      verifyDeserError(CodecOfEnforcedPascalCase,
        """{"camel-case":1,"pascal-case":2,"snake-case":3,"kebab-case":4,"camel-1":5,"pascal-1":6,"snake-1":7,"kebab-1":8}""",
        """missing required field "CamelCase", offset: 0x0000006e""")
    }
    "serialize and deserialize with keys overridden by annotation and throw parse exception when they are missing" in {
      verifySerDeser(codecOfNameOverridden, NameOverridden(oldName = "VVV"), """{"new_name":"VVV"}""")
      verifyDeserError(codecOfNameOverridden, """{"oldName":"VVV"}""",
        """missing required field "new_name", offset: 0x00000010""")
    }
    "don't generate codecs for case classes with field that have duplicated @named annotation" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedNamed(@named("x") @named("y") z: Int)
          |
          |JsonCodecMaker.make[DuplicatedNamed]""".stripMargin
      }).getMessage.contains {
        "Duplicated 'com.github.plokhotnyuk.jsoniter_scala.macros.named' defined for 'z' of 'DuplicatedNamed'."
      })
    }
    "don't generate codecs for ADT leaf case classes that have duplicated @named annotation" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait Z
          |
          |@named("x") @named("y") case class DuplicatedNamed(z: Int) extends Z
          |
          |JsonCodecMaker.make[Z]""".stripMargin
      }).getMessage.contains {
        "Duplicated 'com.github.plokhotnyuk.jsoniter_scala.macros.named' defined for 'DuplicatedNamed'."
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
          |
          |JsonCodecMaker.make[DuplicatedJsonName]""".stripMargin
      }).getMessage.contains(expectedError))
      assert(intercept[TestFailedException](assertCompiles {
        """case class DuplicatedJsonName(y: Int, z: Int)
          |
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig.withFieldNameMapper { case _ => "x" })""".stripMargin
      }).getMessage.contains(expectedError))
    }
    "serialize and deserialize fields that stringified by annotation" in {
      verifySerDeser(codecOfStringified, Stringified(1, 2, Option(1), List(2)),
        """{"i":"1","bi":"2","o":"1","l":["2"]}""")
    }
    "throw parse exception when stringified fields have non-string values" in {
      verifyDeserError(codecOfStringified, """{"i":1,"bi":"2","o":"1","l":["2"]}""",
        """expected '"', offset: 0x00000005""")
      verifyDeserError(codecOfStringified, """{"i":"1","bi":2,"o":"1","l":["2"]}""",
        """expected '"', offset: 0x0000000e""")
      verifyDeserError(codecOfStringified, """{"i":"1","bi":"2","o":1,"l":["2"]}""",
        """expected '"', offset: 0x00000016""")
      verifyDeserError(codecOfStringified, """{"i":"1","bi":"2","o":"1","l":[2]}""",
        """expected '"', offset: 0x0000001f""")
    }
    "serialize and deserialize recursive types if it was allowed" in {
      verifySerDeser(make[Recursive](CodecMakerConfig.withAllowRecursiveTypes(true)),
        Recursive("VVV", 1.1, List(1, 2, 3), Map('S' -> Recursive("WWW", 2.2, List(4, 5, 6), Map()))),
        """{"s":"VVV","bd":1.1,"l":[1,2,3],"m":{"S":{"s":"WWW","bd":2.2,"l":[4,5,6]}}}""")
      verifySerDeser(make[KingDom.Human](CodecMakerConfig.withAllowRecursiveTypes(true)),
        KingDom.Human.King(name = "John", reignOver = Seq(KingDom.Human.Subject("Joanna"))),
        """{"type":"King","name":"John","reignOver":[{"type":"Subject","name":"Joanna"}]}""")
    }
    "don't generate codecs for recursive types by default" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class Recursive(r: Recursive)
          |
          |JsonCodecMaker.make[Recursive]""".stripMargin
      }).getMessage.contains {
        """Recursive type(s) detected: 'Recursive'. Please consider using a custom implicitly accessible codec for this
          |type to control the level of recursion or turn on the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.allowRecursiveTypes' for the trusted input
          |that will not exceed the thread stack size.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """case class NonRecursive(r1: Recursive1)
          |
          |case class Recursive1(r2: Recursive2)
          |
          |case class Recursive2(r3: Recursive3)
          |
          |case class Recursive3(r1: Recursive1)
          |
          |JsonCodecMaker.make[NonRecursive]""".stripMargin
      }).getMessage.contains {
        """Recursive type(s) detected: 'Recursive1', 'Recursive2', 'Recursive3'. Please consider using a custom
          |implicitly accessible codec for this type to control the level of recursion or turn on the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.allowRecursiveTypes' for the trusted input
          |that will not exceed the thread stack size.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """case class HigherKindedType[F[_]](f: F[Int], fs: F[HigherKindedType[F]])
          |
          |JsonCodecMaker.make[HigherKindedType[Option]]""".stripMargin
      }).getMessage.contains(
        s"""Recursive type(s) detected: ${
          if (ScalaVersionCheck.isScala2) "'HigherKindedType[Option]', 'Option[HigherKindedType[Option]]'"
          else "'HigherKindedType[[A >: scala.Nothing <: scala.Any] => scala.Option[A]]', 'scala.Option[HigherKindedType[[A >: scala.Nothing <: scala.Any] => scala.Option[A]]]'"
        }. Please consider
          |using a custom implicitly accessible codec for this type to control the level of recursion or turn on the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.allowRecursiveTypes' for the trusted input
          |that will not exceed the thread stack size.""".stripMargin.replace('\n', ' ')
      ))
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
    "throw the stack overflow error in case of serialization of a cyclic graph" in {
      case class Cyclic(var opt: Option[Cyclic])

      val cyclic = Cyclic(_root_.scala.None)
      cyclic.opt = _root_.scala.Some(cyclic)
      val codecOfCyclic = make[Cyclic](CodecMakerConfig.withAllowRecursiveTypes(true))
      val len = 10000000
      val cfg = WriterConfig.withPreferredBufSize(1)
      TestUtils.assertStackOverflow(verifyDirectByteBufferSer(codecOfCyclic, cyclic, len, cfg, ""))
      TestUtils.assertStackOverflow(verifyHeapByteBufferSer(codecOfCyclic, cyclic, len, cfg, ""))
      TestUtils.assertStackOverflow(verifyOutputStreamSer(codecOfCyclic, cyclic, cfg, ""))
      TestUtils.assertStackOverflow(verifyArraySer(codecOfCyclic, cyclic, cfg, ""))
    }
    "serialize and deserialize UTF-8 keys and values of case classes with and without hex encoding" in {
      case class UTF8KeysAndValues(გასაღები: String)

      val codecOfUTF8KeysAndValues: JsonValueCodec[UTF8KeysAndValues] = make
      verifySerDeser(codecOfUTF8KeysAndValues, UTF8KeysAndValues("ვვვ"), """{"გასაღები":"ვვვ"}""")
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
    "throw parse exception for missing field with default value when the requireDefaultFields flag is on" in {
      val codecOfDefaults1 = makeWithRequiredDefaultFields[Defaults]
      verifyDeserError(codecOfDefaults1, "{}", """missing required field "st", offset: 0x00000001""")
      verifyDeserError(codecOfDefaults1, """{"st":"VVV"}""", """missing required field "i", offset: 0x0000000b""")
      val codecOfDefaults2 = make[Defaults](CodecMakerConfig.withRequireDefaultFields(true).withTransientDefault(false))
      verifyDeserError(codecOfDefaults2, "{}", """missing required field "st", offset: 0x00000001""")
      verifyDeserError(codecOfDefaults2, """{"st":"VVV"}""", """missing required field "i", offset: 0x0000000b""")
    }
    "serialize default values of case classes that defined for fields when the transientDefault flag is off" in {
      verifySer(make[Defaults](CodecMakerConfig.withTransientDefault(false)), Defaults(),
        """{"st":"VVV","i":1,"bi":-1,"oc":"X","l":[0],"a":[[1,2],[3,4]],"ab":[1,2],"m":{"1":true},"mm":{"VVV":1},"im":{"1":"VVV"},"lm":{"1":2},"s":["VVV"],"ms":[1],"bs":[1],"mbs":[1]}""")
      verifySer(makeWithRequiredDefaultFields[Defaults], Defaults(),
        """{"st":"VVV","i":1,"bi":-1,"oc":"X","l":[0],"a":[[1,2],[3,4]],"ab":[1,2],"m":{"1":true},"mm":{"VVV":1},"im":{"1":"VVV"},"lm":{"1":2},"s":["VVV"],"ms":[1],"bs":[1],"mbs":[1]}""")
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
      val codecOfDefaults2: JsonValueCodec[Defaults2] = make
      verifyDeser(codecOfDefaults2, Defaults2(), """{}""")
      verifyDeser(codecOfDefaults2, Defaults2(),
        """{"st":null,"bi":null,"l":null,"oc":null,"e":null,"ab":null,"m":null,"mm":null,"im":null,"lm":null,"s":null,"ms":null,"bs":null,"mbs":null}""".stripMargin)
      verifyDeser(codecOfDefaults2, Defaults2(),
        """{"l":[],"ab":[],"m":{},"mm":{},"im":{},"lm":{},"s":[],"ms":[],"bs":[],"mbs":[]}""")
    }
    "deserialize new default values of case classes without memorization" in {
      case class Defaults3(u: UUID = new UUID(ThreadLocalRandom.current().nextLong(), ThreadLocalRandom.current().nextLong()),
                           i: Int = ThreadLocalRandom.current().nextInt())

      val codecOfDefaults3: JsonValueCodec[Defaults3] = make
      val acc = Set.newBuilder[Defaults3]
      verifyDeserByCheck(codecOfDefaults3, """{}""", (x: Defaults3) => acc += x)
      acc.result().size shouldBe 4
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
        if (ScalaVersionCheck.isScala2) "polymorphic expression cannot be instantiated to expected type"
        else "Polymorphic expression cannot be instantiated to expected type"
      })
    }
    "don't serialize and deserialize transient and non constructor defined fields of case classes" in {
      case class Transient(@_root_.com.github.plokhotnyuk.jsoniter_scala.macros.transient transient: String = "default",
                           required: String) {
        val ignored: String = s"$required-$transient" // always transient
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
    "don't serialize and deserialize scala.transient defined fields of case classes when scala transient support is on" in {
      case class ScalaTransient(@_root_.scala.transient transient: String = "default", required: String) {
        val ignored: String = s"$required-$transient" // always transient
      }

      val codecOfScalaTransient = make[ScalaTransient](CodecMakerConfig.withScalaTransientSupport(true))
      verifySer(codecOfScalaTransient, ScalaTransient(required = "VVV"), """{"required":"VVV"}""")
      verifyDeser(codecOfScalaTransient, ScalaTransient(required = "VVV"), """{"transient":"XXX","required":"VVV"}""")
      verifySer(codecOfScalaTransient, ScalaTransient(required = "VVV", transient = "non-default"), """{"required":"VVV"}""")
      val codecOfScalaTransient2 = make[ScalaTransient](CodecMakerConfig.withScalaTransientSupport(true).withTransientDefault(false))
      verifySer(codecOfScalaTransient2, ScalaTransient(required = "VVV"), """{"required":"VVV"}""")
      verifyDeser(codecOfScalaTransient2, ScalaTransient(required = "VVV"), """{"transient":"XXX","required":"VVV"}""")
      verifySer(codecOfScalaTransient2, ScalaTransient(required = "VVV", transient = "non-default"), """{"required":"VVV"}""")
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
        """{"x":1,"y":[1,2],"z":{"a",3}}""", """unexpected field "x", offset: 0x00000004""")
    }
    "throw parse exception in case of null values for required fields of case classes are provided" in {
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
    "serialize and deserialize case class ADTs using discriminator" in {
      verifySerDeser(codecOfADTList1, List(AAA(1), BBB(BigInt(1)), CCC(1, "VVV"), DDD),
        """[{"type":"AAA","a":1},{"type":"BBB","a":1},{"type":"CCC","a":1,"b":"VVV"},{"type":"DDD"}]""")
      verifySerDeser(codecOfADTList2, List(AAA(1), BBB(BigInt(1)), CCC(1, "VVV"), DDD),
        """[{"AAA":{"a":1}},{"BBB":{"a":1}},{"CCC":{"a":1,"b":"VVV"}},"DDD"]""")
      verifySerDeser(make[List[AdtBase]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("t"))),
        List(CCC(2, "WWW"), CCC(1, "VVV")), """[{"t":"CCC","a":2,"b":"WWW"},{"t":"CCC","a":1,"b":"VVV"}]""")
    }
    "deserialize case class ADTs using discriminator in different positions" in {
      verifyDeser(make[List[AdtBase]](CodecMakerConfig.withRequireDiscriminatorFirst(false)),
        List(AAA(1), BBB(BigInt(1)), CCC(1, "VVV"), DDD),
        """[{"type":"AAA","a":1},{"a":1,"type":"BBB"},{"a":1,"type":"CCC","b":"VVV"},{"type":"DDD"}]""")
      verifyDeser(make[List[AdtBase]](CodecMakerConfig
        .withRequireDiscriminatorFirst(false)
        .withDiscriminatorFieldName(_root_.scala.Some("t"))),
        List(CCC(2, "WWW"), CCC(1, "VVV")), """[{"a":2,"b":"WWW","t":"CCC"},{"a":1,"t":"CCC","b":"VVV"}]""")
    }
    "serialize and deserialize case object ADTs without discriminator" in {
      verifySerDeser(makeWithoutDiscriminator[List[Weapon]], List(Weapon.Axe, Weapon.Sword), """["Axe","Sword"]""")
      verifySerDeser(make[List[Weapon]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        List(Weapon.Axe, Weapon.Sword), """["Axe","Sword"]""")
    }
    "serialize and deserialize product types without discriminators if their codecs are derived not from the base ADT type" in {
      verifySerDeser(make[AAA], AAA(1), """{"a":1}""")
      verifySerDeser(make[BBB], BBB(BigInt(1)), """{"a":1}""")
      verifySerDeser(make[CCC], CCC(1, "VVV"), """{"a":1,"b":"VVV"}""")
      verifySerDeser(make[DDD.type], DDD, """{}""")
    }
    "serialize and deserialize product types with enforced discriminators even if their codecs are derived not from the base ADT type" in {
      verifySerDeser(make[AAA](CodecMakerConfig.withAlwaysEmitDiscriminator(true)), AAA(1), """{"type":"AAA","a":1}""")
      verifySerDeser(make[BBB](CodecMakerConfig.withAlwaysEmitDiscriminator(true)), BBB(BigInt(1)),
        """{"type":"BBB","a":1}""")
      verifySerDeser(make[CCC](CodecMakerConfig.withAlwaysEmitDiscriminator(true)), CCC(1, "VVV"),
        """{"type":"CCC","a":1,"b":"VVV"}""")
      verifySerDeser(make[DDD.type](CodecMakerConfig.withAlwaysEmitDiscriminator(true)), DDD, """{"type":"DDD"}""")
    }
    "don't generate codecs for CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None).withAlwaysEmitDiscriminator(true) compile-time configuration" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait A
          |
          |case class B(y: Int) extends A
          |
          |JsonCodecMaker.make[B](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None).withAlwaysEmitDiscriminator(true))""".stripMargin
      }).getMessage.contains {
        "'discriminatorFieldName' should not be 'None' when 'alwaysEmitDiscriminator' is 'true'"
      })
    }
    "deserialize ADTs with extra fields" in {
      sealed trait Base

      case class A(x: Int) extends Base

      case class B() extends Base

      case object C extends Base

      val baseCodec1: JsonValueCodec[Base] = make
      verifyDeser(baseCodec1, A(1), """{"type":"A","x":1,"extra":"should be ignored"}""")
      verifyDeser(baseCodec1, B(), """{"type":"B","extra":"should be ignored"}""")
      verifyDeser(baseCodec1, C, """{"type":"C","extra":"should be ignored"}""")
      val baseCodec2: JsonValueCodec[Base] = makeWithoutDiscriminator
      verifyDeser(baseCodec2, A(1), """{"A":{"x":1,"extra":"should be ignored"}}""")
      verifyDeser(baseCodec2, B(), """{"B":{"extra":"should be ignored"}}""")
      verifyDeser(baseCodec2, C, """"C"""")
    }
    "serialize and deserialize ADTs with circe-like default formatting" in {
      sealed trait Enum

      object EnumValue extends Enum

      sealed trait Base

      case class AbbA(x: Int = 1, xs: List[Int], oX: Option[Enum]) extends Base

      val baseCodec1: JsonValueCodec[Base] = makeCirceLike
      verifySerDeser(baseCodec1, AbbA(2, List(1, 2, 3), _root_.scala.Some(EnumValue)), """{"AbbA":{"x":2,"xs":[1,2,3],"oX":{"EnumValue":{}}}}""")
      verifySerDeser(baseCodec1, AbbA(1, List(), _root_.scala.None), """{"AbbA":{"x":1,"xs":[],"oX":null}}""")
      val baseCodec2: JsonValueCodec[Base] = makeCirceLikeSnakeCased
      verifySerDeser(baseCodec2, AbbA(2, List(1, 2, 3), _root_.scala.Some(EnumValue)), """{"abb_a":{"x":2,"xs":[1,2,3],"o_x":{"enum_value":{}}}}""")
      verifySerDeser(baseCodec2, AbbA(1, List(), _root_.scala.None), """{"abb_a":{"x":1,"xs":[],"o_x":null}}""")
    }
    "serialize and deserialize ADTs with circe-like encoding for Scala objects" in {
      sealed trait Enum

      object EnumValue1 extends Enum

      object EnumValue2 extends Enum

      val enumCodec: JsonValueCodec[List[Enum]] =
        make(CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None).withCirceLikeObjectEncoding(true))
      verifySerDeser(enumCodec, List(EnumValue1, EnumValue2), """[{"EnumValue1":{}},{"EnumValue2":{}}]""")
    }
    "don't generate codecs for ADT when circeLikeObjectEncoding is true and discriminatorFieldName is non empty" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait Enum
          |
          |object EnumValue extends Enum
          |
          |JsonCodecMaker.make[List[Enum]](CodecMakerConfig.withCirceLikeObjectEncoding(true))""".stripMargin
      }).getMessage.contains {
        """'discriminatorFieldName' should be 'None' when 'circeLikeObjectEncoding' is 'true'"""
      })
    }
    "deserialize and throw non-implemented error for serialization with decodingOnly" in {
      val decodingOnlyCodec = make[Int](CodecMakerConfig.withDecodingOnly(true))
      verifyDeser(decodingOnlyCodec, 1, "1")
      verifySerError(decodingOnlyCodec, 1, "1", "an implementation is missing")
    }
    "serialize and throw non-implemented error for deserialization with encodingOnly" in {
      val encodingOnlyCodec = make[Int](CodecMakerConfig.withEncodingOnly(true))
      verifyDeserError(encodingOnlyCodec, "1", "an implementation is missing")
      verifySer(encodingOnlyCodec, 1, "1")
    }
    "don't generate codecs when decodingOnly and encodingOnly are true simultaneously" in {
      assert(intercept[TestFailedException](assertCompiles {
        """JsonCodecMaker.make[Int](CodecMakerConfig.withDecodingOnly(true).withEncodingOnly(true))""".stripMargin
      }).getMessage.contains {
        """'decodingOnly' and 'encodingOnly' cannot be 'true' simultaneously"""
      })
    }
    "deserialize ADTs when discriminator field was serialized in far away last position and configuration allows to parse it" in {
      val longStr = "W" * 100000
      verifyDeser(codecOfADTList3, List(CCC(2, longStr), CCC(1, "VVV")),
        s"""[{"a":2,"b":"$longStr","type":"CCC"},{"a":1,"type":"CCC","b":"VVV"}]""")
      val longBigInt = BigInt("9" * 20000)
      verifyDeser(codecOfADTList3, List(BBB(longBigInt), BBB(BigInt(1))),
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

      verifySerDeser(make[List[Base]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("t")).withSkipUnexpectedFields(false)),
        List(A(B("x")), B("x")), """[{"t":"A","b":{"c":"x"}},{"t":"B","c":"x"}]""")
      verifySerDeser(makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[List[Base]],
        List(A(B("x")), B("x")), """[{"name":"A","b":{"c":"x"}},{"name":"B","c":"x"}]""")
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
    "serialize and deserialize ADTs using custom values of the discriminator field set by the named annotation" in {
      sealed abstract class Base extends Product with Serializable

      @named("X") final case class A(b: B) extends Base

      @named("Y") final case class B(c: String) extends Base

      verifySerDeser(make[List[Base]](CodecMakerConfig.withSkipUnexpectedFields(false)),
        List(A(B("x")), B("x")), """[{"type":"X","b":{"c":"x"}},{"type":"Y","c":"x"}]""")
    }
    "serialize and deserialize ADTs with dots in simple names of leaf classes/objects using a workaround with named annotations" in {
      sealed abstract class Version(val value: String)

      @named("8.10") case object `8.10` extends Version("8.10")

      @named("8.09") case object `8.09` extends Version("8.9")

      verifySerDeser(makeWithoutDiscriminator[List[Version]], List(`8.09`, `8.10`), """["8.09","8.10"]""")
    }
    "serialize and deserialize ADTs using non-ASCII characters for the discriminator field name and it's values" in {
      sealed abstract class База extends Product with Serializable

      case class А(б: Б) extends База

      case class Б(с: String) extends База

      verifySerDeser(make[List[База]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("тип")).withSkipUnexpectedFields(false)),
        List(А(Б("x")), Б("x")), """[{"тип":"А","б":{"с":"x"}},{"тип":"Б","с":"x"}]""")
    }
    "serialize and deserialize ADTs with Scala operators in names" in {
      sealed trait TimeZone extends Product with Serializable

      case object `US/Alaska` extends TimeZone

      case object `Europe/Paris` extends TimeZone

      verifySerDeser(make[List[TimeZone]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("zoneId"))),
        List(`US/Alaska`, `Europe/Paris`),
        """[{"zoneId":"US/Alaska"},{"zoneId":"Europe/Paris"}]""")
    }
    "serialize and deserialize ADTs with deeply nested hierarchies" in {
      sealed trait Base extends Product with Serializable

      sealed trait Base2 extends Base

      sealed trait Base3 extends Base2

      sealed abstract class Base4 extends Base3

      sealed abstract class Base5 extends Base4

      final case class A(a: Int) extends Base3

      final case class B(b: String) extends Base5

      verifySerDeser(make[List[Base]], List(A(1), B("VVV")), """[{"type":"A","a":1},{"type":"B","b":"VVV"}]""")
    }
    "serialize and deserialize ADTs with leaves that have mixed traits that extends the same base" in {
      sealed trait Base extends Product with Serializable

      sealed trait Base2 extends Base

      final case class A(a: Int) extends Base with Base2

      final case class B(b: String) extends Base with Base2

      verifySerDeser(make[List[Base]], List(A(1), B("VVV")), """[{"type":"A","a":1},{"type":"B","b":"VVV"}]""")
    }
    "serialize and deserialize ADTs with leaves that have a mixed trait hierarchy that makes a diamond" in {
      sealed trait Base extends Product with Serializable

      sealed trait Base1 extends Base

      sealed trait Base2 extends Base

      final case class A(a: Int) extends Base1 with Base2

      final case class B(b: String) extends Base1 with Base2

      verifySerDeser(make[List[Base]], List(A(1), B("VVV")), """[{"type":"A","a":1},{"type":"B","b":"VVV"}]""")
    }
    "deserialize ADTs with a custom handler of unknown type" in {
      def adtCodecWithUnknownKindHandler[A](knownKinds: Set[String], codec: JsonValueCodec[A],
                                            handler: String => A): JsonValueCodec[A] =
        new JsonValueCodec[A] {
          override def decodeValue(in: JsonReader, default: A): A = {
            in.setMark()
            if (in.isNextToken('{')) {
              in.skipToKey("kind")
              val kind = in.readString(null)
              in.rollbackToMark()
              if (knownKinds(kind)) codec.decodeValue(in, default)
              else {
                in.skip()
                handler(kind)
              }
            } else in.readNullOrTokenError(default, '{')
          }

          override def encodeValue(x: A, out: JsonWriter): _root_.scala.Unit = codec.encodeValue(x, out)

          override def nullValue: A = null.asInstanceOf[A]
        }

      object Schema {
        sealed trait Event {
          def kind: String
        }

        case class CreateEvent() extends Event {
          override def kind: String = "CreateEvent"
        }

        case class DeleteEvent() extends Event {
          override def kind: String = "DeleteEvent"
        }

        case class UnknownEvent(unknownKind: String) extends Event {
          override def kind: String = unknownKind
        }

        implicit val codec: JsonValueCodec[Event] = adtCodecWithUnknownKindHandler(
          Set("CreateEvent", "DeleteEvent"),
          JsonCodecMaker.make[Event](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("kind"))),
          UnknownEvent.apply)
      }

      verifyDeser(make[List[Schema.Event]],
        List(Schema.CreateEvent(), Schema.DeleteEvent(), Schema.UnknownEvent("UpdateEvent")),
        """[{"kind":"CreateEvent"},{"kind":"DeleteEvent"},{"kind":"UpdateEvent"}]""")
    }
    "serialize and deserialize case class that have a field named as discriminator" in {
      case class Foo(hint: String)

      verifySerDeser(JsonCodecMaker.make[Foo](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("hint"))),
        Foo("a"), """{"hint":"a"}""")
    }
    "deserialize in case of duplicated discriminator field when checking for field duplication is disabled" in {
      verifyDeser(make[List[AdtBase]](CodecMakerConfig.withCheckFieldDuplication(false)),
        List(AAA(1)), """[{"type":"AAA","a":1,"type":"XXX"}]""")
    }
    "throw parse exception in case of duplicated discriminator field" in {
      verifyDeserError(codecOfADTList1, """[{"type":"AAA","a":1,"type":"AAA"}]""",
        """duplicated field "type", offset: 0x0000001b""")
    }
    "throw parse exception in case of missing discriminator field" in {
      verifyDeserError(codecOfADTList1, """[{"a":1}]""", """expected key: "type", offset: 0x00000005""")
      verifyDeserError(codecOfADTList3, """[{"a":1}]""", """missing required field "type", offset: 0x00000007""")
    }
    "throw parse exception in case of illegal value or position of discriminator field" in {
      verifyDeserError(codecOfADTList1, """[{"a":1,"type":"aaa"}]""", """expected key: "type", offset: 0x00000005""")
      verifyDeserError(codecOfADTList3, """[{"a":1,"type":"aaa"}]""",
        """illegal value of discriminator field "type", offset: 0x00000013""")
      verifyDeserError(codecOfADTList3, """[{"a":1,"type":123}]""", """expected '"', offset: 0x0000000f""")
    }
    "throw parse exception in case of illegal or missing discriminator" in {
      verifyDeserError(codecOfADTList2, """[null]""", """expected '"' or '{', offset: 0x00000001""")
      verifyDeserError(codecOfADTList2, """[true]""", """expected '"' or '{', offset: 0x00000001""")
      verifyDeserError(codecOfADTList2, """[{{"a":1}}]""", """expected '"', offset: 0x00000002""")
      verifyDeserError(codecOfADTList2, """[{"aaa":{"a":1}}]""", """illegal discriminator, offset: 0x00000007""")
    }
    "throw exception in attempt to serialize null values for ADTs" in {
      verifySerError[List[AdtBase]](codecOfADTList1, List[AdtBase](null), "", null, WriterConfig)
      verifySerError[List[AdtBase]](codecOfADTList2, List[AdtBase](null), "", null, WriterConfig)
      verifySerError[List[AdtBase]](codecOfADTList3, List[AdtBase](null), "", null, WriterConfig)
    }
    "don't generate codecs for non sealed traits or abstract classes as an ADT base" in {
      assert(intercept[TestFailedException](assertCompiles {
        """trait X1799
          |
          |case class A1799(i: Int) extends X1799
          |
          |case object B1799 extends X1799
          |
          |JsonCodecMaker.make[X1799]""".stripMargin
      }).getMessage.contains {
        """Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'X1799' or
          |provide a custom implicitly accessible codec for it.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """abstract class X
          |
          |case class A(i: Int) extends X
          |
          |case object B extends X
          |
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'X' or
          |provide a custom implicitly accessible codec for it.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for ADTs that have intermediate non-sealed traits or abstract classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |
          |sealed abstract class AX extends X
          |
          |abstract class BX extends X
          |
          |case class A(i: Int) extends AX
          |
          |case object B extends BX
          |
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed intermediate traits or abstract classes are supported. Please consider using of them for ADT
          |with base 'X' or provide a custom implicitly accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X
          |
          |sealed trait AX extends X
          |
          |trait BX extends X
          |
          |case class A(i: Int) extends AX
          |
          |case object B extends BX
          |
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Only sealed intermediate traits or abstract classes are supported. Please consider using of them for ADT
          |with base 'X' or provide a custom implicitly accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for ADT bases without leaf classes" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X1859 extends Product with Serializable
          |
          |JsonCodecMaker.make[X1859]""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X1859'. Please add them or provide a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed abstract class X extends Product with Serializable
          |
          |JsonCodecMaker.make[X]""".stripMargin
      }).getMessage.contains {
        """Cannot find leaf classes for ADT base 'X'. Please add them or provide a custom implicitly
          |accessible codec for the ADT base.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for case objects which are mapped to the same discriminator value" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X extends Product with Serializable
          |
          |case object A extends X
          |
          |case object B extends X
          |
          |JsonCodecMaker.make[X](CodecMakerConfig.withAdtLeafClassNameMapper(_ => "Z"))""".stripMargin
      }).getMessage.contains {
        """Duplicated discriminator defined for ADT base 'X': 'Z'. Values for leaf classes of ADT that are returned by
          |the 'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.adtLeafClassNameMapper' function
          |should be unique.""".stripMargin.replace('\n', ' ')
      })
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait Data extends Product with Serializable
          |
          |case class Data1(i: Int, s: String) extends Data
          |
          |case object Data1 extends Data
          |
          |val c = make[Data]""".stripMargin
      }).getMessage.contains {
        """Duplicated discriminator defined for ADT base 'Data': 'Data1'. Values for leaf classes of ADT that are
          |returned by the 'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.adtLeafClassNameMapper'
          |function should be unique.""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs for case classes with fields that the same name as discriminator name" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait DuplicatedJsonName extends Product with Serializable
          |
          |case class A(x: Int) extends DuplicatedJsonName
          |
          |JsonCodecMaker.make[DuplicatedJsonName](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.Some("x")))""".stripMargin
      }).getMessage.contains {
        """Duplicated JSON key(s) defined for 'A': 'x'. Keys are derived from field names of the class that are mapped
          |by the 'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.fieldNameMapper' function or can be
          |overridden by 'com.github.plokhotnyuk.jsoniter_scala.macros.named' annotation(s). Result keys should be
          |unique and should not match with a key for the discriminator field that is specified by the
          |'com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.discriminatorFieldName' option.""".stripMargin.replace('\n', ' ')
      })
    }
    "serialize and deserialize ADTs with self-recursive (aka F-bounded) types without discriminators" in {
      case class Basket[T <: Fruit[T]](fruits: List[T])

      val oneFruit: Basket[Apple] = Basket(List(Apple("golden")))
      val twoFruits: Basket[Apple] = oneFruit.copy(fruits = oneFruit.fruits :+ Apple("red"))
      val message = intercept[TestFailedException](assertCompiles {
        """oneFruit.copy(fruits = oneFruit.fruits :+ Orange(0))"""
      }).getMessage
      if (ScalaVersionCheck.isScala2) {
        assert(message.contains("inferred type arguments [com.github.plokhotnyuk.jsoniter_scala.macros.Fruit[_ >: com.github.plokhotnyuk.jsoniter_scala.macros.Apple with com.github.plokhotnyuk.jsoniter_scala.macros.Orange <: com.github.plokhotnyuk.jsoniter_scala.macros.Fruit[_ >: com.github.plokhotnyuk.jsoniter_scala.macros.Apple with com.github.plokhotnyuk.jsoniter_scala.macros.Orange <: Product with Serializable]]] do not conform to method copy's type parameter bounds [T <: com.github.plokhotnyuk.jsoniter_scala.macros.Fruit[T]]") ||
          message.contains("inferred type arguments [com.github.plokhotnyuk.jsoniter_scala.macros.Fruit[_ >: com.github.plokhotnyuk.jsoniter_scala.macros.Apple with com.github.plokhotnyuk.jsoniter_scala.macros.Orange <: com.github.plokhotnyuk.jsoniter_scala.macros.Fruit[_ >: com.github.plokhotnyuk.jsoniter_scala.macros.Apple with com.github.plokhotnyuk.jsoniter_scala.macros.Orange <: Product with java.io.Serializable]]] do not conform to method copy's type parameter bounds [T <: com.github.plokhotnyuk.jsoniter_scala.macros.Fruit[T]]"))
      } else {
        assert(message.contains("Found:    com.github.plokhotnyuk.jsoniter_scala.macros.Orange\nRequired: com.github.plokhotnyuk.jsoniter_scala.macros.Apple"))
      }
      verifySerDeser(make[Basket[Apple]], twoFruits, """{"fruits":[{"family":"golden"},{"family":"red"}]}""")
      verifySerDeser(make[Basket[Orange]], Basket(List(Orange(1), Orange(2))),
        """{"fruits":[{"color":1},{"color":2}]}""")
    }
    "serialize and deserialize dependent codecs which use implicit val" in {
      implicit val intCodec: JsonValueCodec[Int] = make(CodecMakerConfig.withIsStringified(true))
      verifySerDeser(make[List[Int]], List(1, 2), """["1","2"]""")
    }
    "serialize and deserialize dependent codecs which use implicit lazy val to don't depend on order of definition" in {
      verifySerDeser(make[List[Int]], List(1, 2), """["1","2"]""")
      implicit lazy val intCodec: JsonValueCodec[Int] = make(CodecMakerConfig.withIsStringified(true))
    }
    "serialize and deserialize case classes with Java time types" in {
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
      verifySerDeser(make[DayOfWeek], DayOfWeek.FRIDAY, """"FRIDAY"""")
      verifySerDeser(make[Duration], Duration.parse("PT10H30M"), """"PT10H30M"""")
      verifySerDeser(make[Instant],
        Instant.parse("2007-12-03T10:15:30.001Z"), """"2007-12-03T10:15:30.001Z"""")
      verifySerDeser(make[LocalDate], LocalDate.parse("2007-12-03"), """"2007-12-03"""")
      verifySerDeser(make[LocalDateTime],
        LocalDateTime.parse("2007-12-03T10:15:30"), """"2007-12-03T10:15:30"""")
      verifySerDeser(make[LocalTime], LocalTime.parse("10:15:30"), """"10:15:30"""")
      verifySerDeser(make[Month], Month.APRIL, """"APRIL"""")
      verifySerDeser(make[MonthDay], MonthDay.parse("--12-03"), """"--12-03"""")
      verifySerDeser(make[OffsetDateTime],
        OffsetDateTime.parse("2007-12-03T10:15:30+01:00"), """"2007-12-03T10:15:30+01:00"""")
      verifySerDeser(make[OffsetTime], OffsetTime.parse("10:15:30+01:00"), """"10:15:30+01:00"""")
      verifySerDeser(make[Period], Period.parse("P1Y2M25D"), """"P1Y2M25D"""")
      verifySerDeser(make[Year], Year.parse("2007"), """"2007"""")
      verifySerDeser(make[YearMonth], YearMonth.parse("2007-12"), """"2007-12"""")
      verifySerDeser(make[ZonedDateTime],
        ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"), """"2007-12-03T10:15:30+01:00[Europe/Paris]"""")
      verifySerDeser(make[ZoneId], ZoneId.of("Europe/Paris"), """"Europe/Paris"""")
      verifySerDeser(make[ZoneOffset], ZoneOffset.of("+01:00"), """"+01:00"""")
    }
    "serialize and deserialize Java time types as key in maps" in {
      verifySerDeser(make[Map[DayOfWeek, Int]],
        Map(DayOfWeek.FRIDAY -> 0), """{"FRIDAY":0}""")
      verifySerDeser(make[Map[Duration, Int]],
        Map(Duration.parse("PT10H30M") -> 0), """{"PT10H30M":0}""")
      verifySerDeser(make[Map[Instant, Int]],
        Map(Instant.parse("2007-12-03T10:15:30.001Z") -> 0), """{"2007-12-03T10:15:30.001Z":0}""")
      verifySerDeser(make[Map[LocalDate, Int]],
        Map(LocalDate.parse("2007-12-03") -> 0), """{"2007-12-03":0}""")
      verifySerDeser(make[Map[LocalDateTime, Int]],
        Map(LocalDateTime.parse("2007-12-03T10:15:30") -> 0), """{"2007-12-03T10:15:30":0}""")
      verifySerDeser(make[Map[LocalTime, Int]],
        Map(LocalTime.parse("10:15:30") -> 0), """{"10:15:30":0}""")
      verifySerDeser(make[Map[Month, Int]],
        Map(Month.APRIL -> 0), """{"APRIL":0}""")
      verifySerDeser(make[Map[MonthDay, Int]],
        Map(MonthDay.parse("--12-03") -> 0), """{"--12-03":0}""")
      verifySerDeser(make[Map[OffsetDateTime, Int]],
        Map(OffsetDateTime.parse("2007-12-03T10:15:30+01:00") -> 0), """{"2007-12-03T10:15:30+01:00":0}""")
      verifySerDeser(make[Map[OffsetTime, Int]],
        Map(OffsetTime.parse("10:15:30+01:00") -> 0), """{"10:15:30+01:00":0}""")
      verifySerDeser(make[Map[Period, Int]], Map(Period.parse("P1Y2M25D") -> 0), """{"P1Y2M25D":0}""")
      verifySerDeser(make[Map[Year, Int]], Map(Year.parse("2007") -> 0), """{"2007":0}""")
      verifySerDeser(make[Map[YearMonth, Int]],
        Map(YearMonth.parse("2007-12") -> 0), """{"2007-12":0}""")
      verifySerDeser(make[Map[ZonedDateTime, Int]],
        Map(ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]") -> 0),
        """{"2007-12-03T10:15:30+01:00[Europe/Paris]":0}""")
      verifySerDeser(make[Map[ZoneId, Int]],
        Map(ZoneId.of("Europe/Paris") -> 0), """{"Europe/Paris":0}""")
      verifySerDeser(make[Map[ZoneOffset, Int]],
        Map(ZoneOffset.of("+01:00") -> 0), """{"+01:00":0}""")
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

      verifySerDeser(make[Map[I, S]], Map(1 -> "VVV"), """{"1":"VVV"}""")
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
        FirstOrderType[Int, String](1, "VVV", _root_.scala.Some(1), List("WWW")),
        """{"a":1,"b":"VVV","oa":1,"bs":["WWW"]}""")
      verifySerDeser(make[FirstOrderType[Id[Int], Id[String]]],
        FirstOrderType[Id[Int], Id[String]](Id[Int](1), Id[String]("VVV"), _root_.scala.Some(Id[Int](2)), List(Id[String]("WWW"))),
        """{"a":1,"b":"VVV","oa":2,"bs":["WWW"]}""")
    }
    "don't generate codecs for first-order types that are specified using 'Any' type parameter" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class FirstOrder[A](a: A)
          |
          |JsonCodecMaker.make[FirstOrder[_]]""".stripMargin
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) {
          """Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'Any' or
            |provide a custom implicitly accessible codec for it.""".stripMargin.replace('\n', ' ')
        } else {
          """Type bounds are not supported for type 'FirstOrder[_ >: scala.Nothing <: scala.Any]' with field type
            |for a '_ >: scala.Nothing <: scala.Any'""".stripMargin.replace('\n', ' ')
        }
      })
    }
    "serialize and deserialize arrays of generic types" in {
      sealed trait GADT[A] extends Product with Serializable

      case object Foo extends GADT[_root_.scala.Boolean]

      case object Bar extends GADT[_root_.scala.Unit]

      case object Baz extends GADT[Int]

      case object Qux extends GADT[String]

      verifySerDeser(makeWithoutDiscriminator[Array[GADT[_]]],
        _root_.scala.Array[GADT[_]](Foo, Bar, Baz, Qux), """["Foo","Bar","Baz","Qux"]""")
      verifySerDeser(make[Array[GADT[_]]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        _root_.scala.Array[GADT[_]](Foo, Bar, Baz, Qux), """["Foo","Bar","Baz","Qux"]""")
      verifySerDeser(make[Array[GADT2[_]]],
        _root_.scala.Array[GADT2[_]](GADT2.Exists("WWW"), GADT2.ReadBytes("QQQ"), GADT2.CopyOver("AAA".getBytes.toSeq, "OOO")),
        """[{"type":"Exists","path":"WWW"},{"type":"ReadBytes","path":"QQQ"},{"type":"CopyOver","src":[65,65,65],"path":"OOO"}]""")
    }
    "serialize and deserialize higher-kinded types" in {
      sealed trait Foo[A[_]] extends Product with Serializable

      case class Bar[A[_]](a: A[Int]) extends Foo[A]

      case class Baz[A[_]](a: A[String]) extends Foo[A]

      val codecOfFooForOption = make[Foo[Option]]
      verifySerDeser(codecOfFooForOption, Bar[Option](_root_.scala.Some(1)), """{"type":"Bar","a":1}""")
      verifySerDeser(codecOfFooForOption, Baz[Option](_root_.scala.Some("VVV")), """{"type":"Baz","a":"VVV"}""")
    }
    "serialize and deserialize case classes with an auxiliary constructor using primary one" in {
      case class AuxiliaryConstructor(i: Int, s: String = "") {
        def this(s: String) = this(0, s)
      }

      val codecOfAuxiliaryConstructor = make[AuxiliaryConstructor]
      verifySerDeser(codecOfAuxiliaryConstructor, new AuxiliaryConstructor("VVV"),"""{"i":0,"s":"VVV"}""")
      verifySerDeser(codecOfAuxiliaryConstructor, AuxiliaryConstructor(1),"""{"i":1}""")
    }
    "serialize and deserialize case classes with private primary constructor if it can be accessed" in {
      object PrivatePrimaryConstructor {
        implicit val codec: JsonValueCodec[PrivatePrimaryConstructor] = make

        def apply(s: String) = new PrivatePrimaryConstructor(s)
      }

      case class PrivatePrimaryConstructor private(i: Int) {
        def this(s: String) = this(s.toInt)
      }

      verifySerDeser(PrivatePrimaryConstructor.codec, PrivatePrimaryConstructor("1"), """{"i":1}""")
    }
    "don't generate codecs for classes without a primary constructor" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[_root_.scala.concurrent.duration.Duration]"
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) "Cannot find a primary constructor for 'Infinite.this.<local child>'"
        else "Local child symbols are not supported"
      })
    }
    "generate codecs for case classes with multiple parameter lists in a primary constructor without default values" in {
      case class MultiListOfArgs(i: Int)(val l: Long)(var s: String)

      verifySerDeser(make[MultiListOfArgs], new MultiListOfArgs(1)(2)("VVV"), """{"i":1,"l":2,"s":"VVV"}""")
    }
    "generate codecs for case classes with multiple parameter lists in a primary constructor with depended default values when the requireDefaultFields is on" in {
      case class MultiListOfArgs(i: Int = 1)(val l: Long = i - 1)(var s: String = l.toString)

      verifySerDeser(makeWithRequiredDefaultFields[MultiListOfArgs],
        new MultiListOfArgs(1)(2)("VVV"), """{"i":1,"l":2,"s":"VVV"}""")
      verifySerDeser(make[MultiListOfArgs](CodecMakerConfig.withRequireDefaultFields(true).withTransientDefault(false)),
        new MultiListOfArgs(1)(2)("VVV"), """{"i":1,"l":2,"s":"VVV"}""")
    }
    "don't generate codecs for case classes with non public parameters of the primary constructor" in {
      assert(intercept[TestFailedException](assertCompiles {
        """case class MultiListOfArgsWithNonPublicParam(i: Int)(l: Long)
          |
          |JsonCodecMaker.make[MultiListOfArgsWithNonPublicParam]""".stripMargin
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) "'l' parameter of 'MultiListOfArgsWithNonPublicParam' should be defined as 'val' or 'var' in the primary constructor."
        else "Field 'l' in class 'MultiListOfArgsWithNonPublicParam' is private. It should be defined as 'val' or 'var' in the primary constructor."
      })
    }
    "don't generate codecs for classes with parameters in a primary constructor that have no accessor for read" in {
      assert(intercept[TestFailedException](assertCompiles {
        """class ParamHasNoAccessor(val i: Int, a: String)
          |
          |JsonCodecMaker.make[ParamHasNoAccessor]""".stripMargin
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) "'a' parameter of 'ParamHasNoAccessor' should be defined as 'val' or 'var' in the primary constructor."
        else "Field 'a' in class 'ParamHasNoAccessor' is private. It should be defined as 'val' or 'var' in the primary constructor."
      })
    }
    "don't generate codecs when all generic type parameters cannot be resolved" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait Foo[F[_]] extends Product with Serializable
          |
          |case class FooImpl[F[_], A](fa: F[A], as: Vector[A]) extends Foo[F]
          |
          |sealed trait Bar[A] extends Product with Serializable
          |
          |case object Baz extends Bar[Int]
          |
          |case object Qux extends Bar[String]
          |
          |val v = FooImpl[Bar, String](Qux, Vector.empty[String])
          |val c = JsonCodecMaker.make[Foo[Bar]]""".stripMargin
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) "Cannot resolve generic type(s) for `FooImpl[F,A]`. Please provide a custom implicitly accessible codec for it."
        else "Type parameter A of class FooImpl can't be deduced from type arguments of Foo[[A >: scala.Nothing <: scala.Any] => Bar[A]]. Please provide a custom implicitly accessible codec for it."
      })
    }
    "don't generate codecs when 'AnyVal' or one value classes with 'CodecMakerConfig.withInlineOneValueClasses(true)' are leaf types of the ADT base" in {
      assert(intercept[TestFailedException](assertCompiles {
        """sealed trait X extends Any
          |
          |case class D(value: Double) extends X
          |
          |JsonCodecMaker.make[X](CodecMakerConfig.withInlineOneValueClasses(true))""".stripMargin
      }).getMessage.contains {
        "'AnyVal' and one value classes with 'CodecMakerConfig.withInlineOneValueClasses(true)' are not supported as leaf classes for ADT with base 'X'."
      })
    }
    "don't generate codecs that cannot parse own output" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[Arrays](CodecMakerConfig.withRequireCollectionFields(true))"
      }).getMessage.contains {
        "'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously"
      })
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[Arrays](CodecMakerConfig.withRequireDefaultFields(true))"
      }).getMessage.contains {
        "'requireDefaultFields' and 'transientDefault' cannot be 'true' simultaneously"
      })
    }
    "don't generate codecs for unsupported classes like java.util.Date" in {
      assert(intercept[TestFailedException](assertCompiles {
        "JsonCodecMaker.make[_root_.java.util.Date]"
      }).getMessage.contains {
        if (ScalaVersionCheck.isScala2) "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_]' defined for 'java.util.Date'."
        else "No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for '_root_.java.util.Date'."
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
    "transform camelCase names to snake_case with separated non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case("oO") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("oOoo") shouldBe "o_ooo"
      JsonCodecMaker.enforce_snake_case("oOoo111") shouldBe "o_ooo_111"
      JsonCodecMaker.enforce_snake_case("oOoo$") shouldBe "o_ooo_$"
    }
    "transform PascalCase names to snake_case with separated non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case("Oo") shouldBe "oo"
      JsonCodecMaker.enforce_snake_case("Ooo111") shouldBe "ooo_111"
      JsonCodecMaker.enforce_snake_case("OOOoo111") shouldBe "oo_ooo_111"
      JsonCodecMaker.enforce_snake_case("OOOoo$") shouldBe "oo_ooo_$"
    }
    "transform kebab-case names to snake_case with separated non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case("o-o") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o-ooo-") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case("o-ooo111") shouldBe "o_ooo_111"
      JsonCodecMaker.enforce_snake_case("o-ooo-111") shouldBe "o_ooo_111"
      JsonCodecMaker.enforce_snake_case("o-ooo-$") shouldBe "o_ooo_$"
    }
    "transform snake_case names to snake_case with separated non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case("o_o") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case("o_ooo_") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case("o_ooo111") shouldBe "o_ooo_111"
      JsonCodecMaker.enforce_snake_case("o_ooo_111") shouldBe "o_ooo_111"
      JsonCodecMaker.enforce_snake_case("o_ooo$") shouldBe "o_ooo_$"
    }
  }
  "JsonCodecMaker.enforce_snake_case2" should {
    "transform camelCase names to snake_case2 with joined non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case2("oO") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case2("oOoo") shouldBe "o_ooo"
      JsonCodecMaker.enforce_snake_case2("oOoo111") shouldBe "o_ooo111"
      JsonCodecMaker.enforce_snake_case2("oOoo$") shouldBe "o_ooo$"
    }
    "transform PascalCase names to snake_case2 with joined non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case2("Oo") shouldBe "oo"
      JsonCodecMaker.enforce_snake_case2("Ooo111") shouldBe "ooo111"
      JsonCodecMaker.enforce_snake_case2("OOOoo111") shouldBe "oo_ooo111"
      JsonCodecMaker.enforce_snake_case2("OOOoo$") shouldBe "oo_ooo$"
    }
    "transform kebab-case names to snake_case2 with joined non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case2("o-o") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case2("o-ooo-") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case2("o-ooo111") shouldBe "o_ooo111"
      JsonCodecMaker.enforce_snake_case2("o-ooo-111") shouldBe "o_ooo111"
      JsonCodecMaker.enforce_snake_case2("o-ooo-$") shouldBe "o_ooo$"
    }
    "transform snake_case names to snake_case2 with joined non-alphabetic" in {
      JsonCodecMaker.enforce_snake_case2("o_o") shouldBe "o_o"
      JsonCodecMaker.enforce_snake_case2("o_ooo_") shouldBe "o_ooo_"
      JsonCodecMaker.enforce_snake_case2("o_ooo111") shouldBe "o_ooo111"
      JsonCodecMaker.enforce_snake_case2("o_ooo_111") shouldBe "o_ooo111"
      JsonCodecMaker.enforce_snake_case2("o_ooo_$") shouldBe "o_ooo$"
    }
  }
  "JsonCodecMaker.enforce-kebab-case" should {
    "transform camelCase names to kebab-case with separated non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case`("oO") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("oOoo") shouldBe "o-ooo"
      JsonCodecMaker.`enforce-kebab-case`("oOoo111") shouldBe "o-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("oOoo$") shouldBe "o-ooo-$"
    }
    "transform PascalCase names to kebab-case with separated non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case`("Oo") shouldBe "oo"
      JsonCodecMaker.`enforce-kebab-case`("Ooo111") shouldBe "ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("OOOoo111") shouldBe "oo-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("OOOoo$") shouldBe "oo-ooo-$"
    }
    "transform snake_case names to kebab-case with separated non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case`("o_o") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("o_ooo_") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case`("o_ooo111") shouldBe "o-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("o_ooo_111") shouldBe "o-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("o_ooo_$") shouldBe "o-ooo-$"
    }
    "transform kebab-case names to kebab-case with separated non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case`("o-o") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case`("o-ooo-") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case`("o-ooo111") shouldBe "o-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("o-ooo-111") shouldBe "o-ooo-111"
      JsonCodecMaker.`enforce-kebab-case`("o-ooo$") shouldBe "o-ooo-$"
    }
  }
  "JsonCodecMaker.enforce-kebab-case2" should {
    "transform camelCase names to kebab-case2 with joined non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case2`("oO") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case2`("oOoo") shouldBe "o-ooo"
      JsonCodecMaker.`enforce-kebab-case2`("oOoo111") shouldBe "o-ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("oOoo$") shouldBe "o-ooo$"
    }
    "transform PascalCase names to kebab-case2 with joined non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case2`("Oo") shouldBe "oo"
      JsonCodecMaker.`enforce-kebab-case2`("Ooo111") shouldBe "ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("OOOoo111") shouldBe "oo-ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("OOOoo$") shouldBe "oo-ooo$"
    }
    "transform snake_case names to kebab-case2 with joined non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case2`("o_o") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case2`("o_ooo_") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case2`("o_ooo111") shouldBe "o-ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("o_ooo_111") shouldBe "o-ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("o_ooo_$") shouldBe "o-ooo$"
    }
    "transform kebab-case names to kebab-case2 with joined non-alphabetic" in {
      JsonCodecMaker.`enforce-kebab-case2`("o-o") shouldBe "o-o"
      JsonCodecMaker.`enforce-kebab-case2`("o-ooo-") shouldBe "o-ooo-"
      JsonCodecMaker.`enforce-kebab-case2`("o-ooo111") shouldBe "o-ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("o-ooo-111") shouldBe "o-ooo111"
      JsonCodecMaker.`enforce-kebab-case2`("o-ooo-$") shouldBe "o-ooo$"
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
