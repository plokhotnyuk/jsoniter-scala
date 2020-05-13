package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException

import scala.annotation.tailrec

case class JavaEnums(l: Level, il: Levels.InnerLevel)

class JsonCodecMakerJVMSpec extends VerifyingSpec {
  import NamespacePollutions._

  val codecOfJavaEnums: JsonValueCodec[JavaEnums] = make

  "JsonCodecMaker.make generate codes which" should {
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
    "serialize and deserialize recursive structures with an implicit codec using a minimal thread stack" in {
      case class Nested(n: Option[Nested] = _root_.scala.None)

      @tailrec
      def construct(d: Int = 1000000, n: Nested = Nested()): Nested =
        if (d <= 0) n
        else construct(d - 1, Nested(Some(n)))

      implicit val codecOfNestedStructs: JsonValueCodec[Nested] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
      val bytes = ("{" + "\"n\":{" * 1000000 + "}" * 1000000 + "}").getBytes
      val readStackTrace = AssertionUtils.assertStackOverflow(readFromArray[Nested](bytes))
      assert(readStackTrace.contains(".d0("))
      assert(!readStackTrace.contains(".d1("))
      assert(!readStackTrace.contains(".decodeValue("))
      val writeStackTrace = AssertionUtils.assertStackOverflow(writeToArray[Nested](construct()))
      assert(writeStackTrace.contains(".e0("))
      assert(!writeStackTrace.contains(".e1("))
      assert(!writeStackTrace.contains(".encodeValue("))
    }
  }
}
