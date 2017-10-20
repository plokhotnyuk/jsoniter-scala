package com.github.plokhotnyuk.jsoniter_scala

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}

import com.jsoniter.spi.{Config, JsonException, JsoniterSpi}
import Codec._
import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable._
import scala.collection.mutable

case class KeyOverridden(@key("new_key") oldKey: String) //FIXME: fail tests when move to end of source file

class CodecSpec extends WordSpec with Matchers {
  "Codec" should {
    "serialize and deserialize primitives" in {
      verifySerDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
    }
    "don't deserialize numbers that overflow primitive types" in {
      assert(intercept[JsonException] {
        verifyDeser(materialize[Primitives],
          Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
          """{"b":1000,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
      }.getMessage.contains("value is too large for byte"))
      assert(intercept[JsonException] {
        verifyDeser(materialize[Primitives],
          Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
          """{"b":1,"s":200000,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
      }.getMessage.contains("value is too large for short"))
      assert(intercept[JsonException] {
        verifyDeser(materialize[Primitives],
          Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
          """{"b":1,"s":2,"i":3000000000,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
      }.getMessage.contains("value is too large for int"))
/* FIXME parsing of Int.MinValue fails with "decode: missing required field(s)"
      verifyDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, -2147483648, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":-2147483648,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
*/
/* FIXME parsing of too big values for long types should throw exception
      assertThrows[JsonException] {
        verifyDeser(materialize[Primitives],
          Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
          """{"b":1,"s":2,"i":3,"l":40000000000000000000,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
      }
*/
/* FIXME parsing of invalid Boolean value fails with "decode: missing required field(s)"
      verifyDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":truee,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
      verifyDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":tru,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
*/
      assert(intercept[JsonException] {
        verifyDeser(materialize[Primitives],
          Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
          """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":1000000,"dbl":1.1,"f":2.2}""".getBytes)
      }.getMessage.contains("value is too large for char"))
    }
    "deserialize too big numbers as infinity for floating point types" in {
      verifyDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', Double.PositiveInfinity, Float.PositiveInfinity),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1e1000,"f":2.2e2000}""".getBytes)
      verifyDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', Double.NegativeInfinity, Float.NegativeInfinity),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":-1.1e1000,"f":-2.2e2000}""".getBytes)
    }
    "deserialize too small numbers as zero for floating point types" in {
      verifyDeser(materialize[Primitives],
        Primitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 0.0, 0.0f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1e-1000,"f":2.2e-2000}""".getBytes)
    }
    "serialize and deserialize boxed primitives" in {
      verifySerDeser(materialize[BoxedPrimitives],
        BoxedPrimitives(1.toByte, 2.toShort, 3, 4L, bl = true, 'V', 1.1, 2.2f),
        """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes)
    }
    "serialize and deserialize standard types" in {
      verifySerDeser(materialize[StandardTypes],
        StandardTypes("VVV", BigInt("123456789012345678901234567890"), BigDecimal("1234567890.12345678901234567890")),
        """{"s":"VVV","bi":123456789012345678901234567890,"bd":1234567890.12345678901234567890}""".getBytes)
    }
    "don't deserialize unexpected or invalid values" in {
      assert(intercept[JsonException] {
        verifyDeser(materialize[StandardTypes],
          StandardTypes(null, 1, 2),
          """{"s":n,"bi":1,"bd":2}""".getBytes)
      }.getMessage.contains("unexpected value"))
      assert(intercept[JsonException] {
        verifyDeser(materialize[StandardTypes],
          StandardTypes(null, 1, 2),
          """{"s":nu,"bi":1,"bd":2}""".getBytes)
      }.getMessage.contains("unexpected value"))
      assert(intercept[JsonException] {
        verifyDeser(materialize[StandardTypes],
          StandardTypes(null, 1, 2),
          """{"s":nul,"bi":1,"bd":2}""".getBytes)
      }.getMessage.contains("unexpected value"))
/* FIXME consider using FSM based parser to avoid such kind of failures
      assert(intercept[JsonException] {
        verifyDeser(materialize[StandardTypes],
          StandardTypes(null, 1, 2),
          """{"s":nulll,"bi":1,"bd":2}""".getBytes)
      }.getMessage.contains("unexpected value"))
*/
    }
    "don't deserialize invalid UTF-8 encoded strings" in {
      assert(intercept[JsonException] {
        val buf = """{"s":"VVV","bi":1,"bd":1.1}""".getBytes
        buf(6) = 0xF0.toByte
        verifyDeser(materialize[StandardTypes], StandardTypes("VVV", BigInt("1"), BigDecimal("1.1")), buf)
      }.getMessage.contains("malformed byte(s): 0xF0"))
    }
    "don't deserialize invalid UTF-8 encoded field names" in {
      assert(intercept[JsonException] {
        val buf = """{"s":"VVV","bi":1,"bd":1.1}""".getBytes
        buf(2) = 0xF0.toByte
        verifyDeser(materialize[StandardTypes], StandardTypes("VVV", BigInt("1"), BigDecimal("1.1")), buf)
      }.getMessage.contains("malformed byte(s): 0xF0"))
    }
    "don't deserialize invalid JSON escaped strings" in {
      assert(intercept[JsonException] {
        val buf = "{\"s\":\"\\udd1e\",\"bi\":1,\"bd\":1.1}".getBytes
        verifyDeser(materialize[StandardTypes], StandardTypes("VVV", BigInt("1"), BigDecimal("1.1")), buf)
      }.getMessage.contains("expect high surrogate character"))
    }
    "don't deserialize invalid JSON escaped field names" in {
      assert(intercept[JsonException] {
        val buf = "{\"\\udd1e\":\"VVV\",\"bi\":1,\"bd\":1.1}".getBytes
        verifyDeser(materialize[StandardTypes], StandardTypes("VVV", BigInt("1"), BigDecimal("1.1")), buf)
      }.getMessage.contains("expect high surrogate character"))
    }
    "serialize and deserialize enumerations" in {
      verifySerDeser(materialize[Enums], Enums(LocationType.GPS), """{"lt":1}""".getBytes)
      assert(intercept[JsonException] {
        verifyDeser(materialize[Enums], Enums(LocationType.GPS), """{"lt":-1}""".getBytes)
      }.getMessage.contains("invalid enum value: -1"))
    }
    "serialize and deserialize value classes" in {
      verifySerDeser(materialize[ValueClassTypes],
        ValueClassTypes(UserId("123abc"), OrderId(123123)),
        """{"uid":"123abc","oid":123123}""".getBytes)
    }
    "serialize and deserialize options" in {
      verifySerDeser(materialize[Options],
        Options(Option("VVV"), Option(BigInt(4)), Option(Set())),
        """{"os":"VVV","obi":4,"osi":[]}""".getBytes)
    }
    "serialize and deserialize arrays" in {
      val arrayCodec = materialize[Arrays]
      val json = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}""".getBytes
      val obj = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array(BigInt(7)))
      verifySer(arrayCodec, obj, json)
      val parsedObj = arrayCodec.read(json)
      parsedObj.aa.deep shouldBe obj.aa.deep
      parsedObj.a.deep shouldBe obj.a.deep
    }
    "serialize and deserialize mutable traversables" in {
      verifySerDeser(materialize[MutableTraversables],
        MutableTraversables(mutable.MutableList(mutable.SortedSet("1", "2", "3")),
          mutable.ArrayBuffer(mutable.Set(BigInt(4)), mutable.Set.empty[BigInt]),
          mutable.ArraySeq(mutable.LinkedHashSet(5, 6), mutable.LinkedHashSet.empty[Int]),
          mutable.Buffer(mutable.HashSet(7.7, 8.8)),
          mutable.ListBuffer(mutable.TreeSet(9L, 10L)),
          mutable.IndexedSeq(mutable.ArrayStack(11.11f, 12.12f)),
          mutable.UnrolledBuffer(mutable.Traversable(13.toShort, 14.toShort)),
          mutable.LinearSeq(15.toByte, 16.toByte),
          mutable.ResizableArray(mutable.Seq(17.17, 18.18))),
        """{"ml":[["1","2","3"]],"ab":[[4],[]],"as":[[5,6],[]],"b":[[8.8,7.7]],"lb":[[9,10]],"is":[[11.11,12.12]],"ub":[[13,14]],"ls":[15,16],"ra":[[17.17,18.18]]}""".getBytes)
    }
    "serialize and deserialize immutable traversables" in {
      verifySerDeser(materialize[ImmutableTraversables],
        ImmutableTraversables(List(ListSet("1")), Queue(Set(BigInt(4), BigInt(5), BigInt(6))),
          IndexedSeq(SortedSet(7, 8), SortedSet()), Stream(TreeSet(9.9)), Vector(Traversable(10L, 11L))),
        """{"l":[["1"]],"q":[[4,5,6]],"is":[[7,8],[]],"s":[[9.9]],"v":[[10,11]]}""".getBytes)
    }
    "serialize and deserialize mutable maps" in {
      verifySerDeser(materialize[MutableMaps],
        MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
          mutable.Map(1.1f -> mutable.WeakHashMap(BigInt(2) -> "2")),
          mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
        """{"hm":{"true":{"1.1":1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes)
    }
    "serialize and deserialize immutable maps" in {
      verifySerDeser(materialize[ImmutableMaps],
        ImmutableMaps(Map(1 -> 1.1), HashMap("2" -> ListMap(2 -> 2), "3" -> ListMap(3 -> 3)),
          SortedMap(4L -> TreeMap(4.4 -> 4.4f), 5L -> TreeMap.empty[Double, Float])),
        """{"m":{"1":1.1},"hm":{"2":{"2":2},"3":{"3":3}},"sm":{"4":{"4.4":4.4},"5":{}}}""".getBytes)
    }
    "don't serialize null keys for maps" in {
      assert(intercept[IOException] {
        verifySer(materialize[MutableMaps],
          MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(null.asInstanceOf[BigDecimal] -> 1)),
            mutable.Map(1.1f -> mutable.WeakHashMap(BigInt(2) -> "2")),
            mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
          """{"hm":{"true":{null:1}},"m":{"1.1":{"2":"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes)
      }.getMessage.contains("key cannot be null"))
      assert(intercept[IOException] {
        verifySer(materialize[MutableMaps],
          MutableMaps(mutable.HashMap(true -> mutable.AnyRefMap(BigDecimal(1.1) -> 1)),
            mutable.Map(1.1f -> mutable.WeakHashMap(null.asInstanceOf[BigInt] -> "2")),
            mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(3.toShort -> 3.3), 2.2 -> mutable.LinkedHashMap())),
          """{"hm":{"true":{"1.1":1}},"m":{"1.1":{null:"2"}},"ohm":{"1.1":{"3":3.3},"2.2":{}}}""".getBytes)
      }.getMessage.contains("key cannot be null"))
      assert(intercept[IOException] {
        verifySerDeser(materialize[ImmutableMaps],
          ImmutableMaps(Map(1 -> 1.1), HashMap(null.asInstanceOf[String] -> ListMap(2 -> 2), "3" -> ListMap(3 -> 3)),
            SortedMap(4L -> TreeMap(4.4 -> 4.4f), 5L -> TreeMap.empty[Double, Float])),
          """{"m":{"1":1.1},"hm":{null:{"2":2},"3":{"3":3}},"sm":{"4":{"4.4":4.4},"5":{}}}""".getBytes)
      }.getMessage.contains("key cannot be null"))
    }
    "serialize and deserialize mutable long maps" in {
      verifySerDeser(materialize[MutableLongMaps],
        MutableLongMaps(mutable.LongMap(1L -> 1.1), mutable.LongMap(3L -> "33")),
        """{"lm1":{"1":1.1},"lm2":{"3":"33"}}""".getBytes)
    }
    "serialize and deserialize immutable int and long maps" in {
      verifySerDeser(materialize[ImmutableIntLongMaps],
        ImmutableIntLongMaps(IntMap(1 -> 1.1, 2 -> 2.2), LongMap(3L -> "33")),
        """{"im":{"1":1.1,"2":2.2},"lm":{"3":"33"}}""".getBytes)
    }
    "serialize and deserialize mutable & immutable bitsets" in {
      verifySerDeser(materialize[BitSets], BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6)),
        """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes)
    }
    "serialize and deserialize with keys defined by fields" in {
      verifySerDeser(materialize[CamelAndSnakeCase],
        CamelAndSnakeCase("VVV", "XXX"),
        """{"camelCase":"VVV","snake_case":"XXX"}""".getBytes)
    }
    "serialize and deserialize indented JSON" in {
      withConfig(_.indentionStep(2)) {
        verifySerDeser(materialize[Indented], Indented("VVV", List("X", "Y", "Z")),
          """{
            |  "f1": "VVV",
            |  "f2": [
            |    "X",
            |    "Y",
            |    "Z"
            |  ]
            |}""".stripMargin.getBytes)
      }
    }
    "serialize and deserialize UTF-8 keys and values without hex encoding" in {
      verifySerDeser(materialize[UTF8KeysAndValues], UTF8KeysAndValues("ვვვ"),
        """{"გასაღები":"ვვვ"}""".getBytes("UTF-8"))
    }
    "serialize and deserialize UTF-8 keys and values with hex encoding" in {
      verifyDeser(materialize[UTF8KeysAndValues], UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
        "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t\\/\"}".getBytes("UTF-8"))
      withConfig(_.escapeUnicode(true)) {
        verifySer(materialize[UTF8KeysAndValues], UTF8KeysAndValues("ვვვ\b\f\n\r\t/"),
          "{\"\\u10d2\\u10d0\\u10e1\\u10d0\\u10e6\\u10d4\\u10d1\\u10d8\":\"\\u10d5\\u10d5\\u10d5\\b\\f\\n\\r\\t/\"}".getBytes("UTF-8"))
      }
    }
    "serialize and deserialize with keys overridden by annotation" in {
      verifySerDeser(materialize[KeyOverridden], KeyOverridden("VVV"), """{"new_key":"VVV"}""".getBytes)
    }
    "deserialize but don't serialize default values that defined for fields" in {
      val defaultsCodec = materialize[Defaults]
      val obj = Defaults()
      val json = """{}""".getBytes
      verifySer(defaultsCodec, obj, json)
      val parsedObj = defaultsCodec.read(json)
      parsedObj.s shouldBe obj.s
      parsedObj.i shouldBe obj.i
      parsedObj.bi shouldBe obj.bi
      parsedObj.a.deep shouldBe obj.a.deep
    }
    "don't serialize and deserialize transient and non constructor defined fields" in {
      verifySerDeser(materialize[Transient], Transient("VVV"), """{"required":"VVV"}""".getBytes)
    }
    "don't serialize fields with none values" in {
      verifySer(materialize[NullAndNoneValues],
        NullAndNoneValues("VVV", null, null, null, NullAndNoneValues(null, null, null, null, null, None), None),
        """{"str":"VVV","bi":null,"bd":null,"lt":null,"nv":{"str":null,"bi":null,"bd":null,"lt":null,"nv":null}}""".getBytes)
    }
    "don't serialize fields empty collections" in {
      verifySer(materialize[EmptyTraversables], EmptyTraversables(List(), Set(), List()), """{}""".getBytes)
    }
    "don't deserialize unknown fields" in {
      verifyDeser(materialize[Unknown], Unknown(), """{"x":1,"y":[1,2],"z":{"a",3}}""".getBytes)
    }
    "serialize and deserialize null" in {
      verifyDeser(materialize[Unknown], null, """null""".getBytes)
    }
    "deserialize null values for standard types" in {
      verifyDeser(materialize[StandardTypes],
        StandardTypes(null, null, null),
        """{"s":null,"bi":null,"bd":null}""".getBytes)
    }
    "deserialize null values for standard types (but empty values for container types) of traversable values" in {
      verifyDeser(materialize[ImmutableTraversables],
        ImmutableTraversables(List(ListSet(null)), Queue(Set(BigInt(4), null, BigInt(6))),
          IndexedSeq(), Stream(TreeSet()), Vector(Traversable())),
        """{"l":[[null]],"q":[[4,null,6]],"is":null,"s":[null],"v":[null]}""".getBytes)
    }
    "deserialize null values for standard types (but empty values for container types) of map values" in {
      verifyDeser(materialize[MutableMaps],
        MutableMaps(mutable.HashMap(),
          mutable.Map(1.1f -> mutable.WeakHashMap(BigInt(2) -> null.asInstanceOf[String])),
          mutable.OpenHashMap(1.1 -> mutable.LinkedHashMap(), 2.2 -> mutable.LinkedHashMap())),
        """{"hm":null,"m":{"1.1":{"2":null}},"ohm":{"1.1":null,"2.2":null}}""".getBytes)
    }
    "throw exception in case of missing required fields detected during deserialization" in {
      assert(intercept[Exception] {
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
        verifyDeser(materialize[Required], obj,
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
      }.getMessage.contains("""decode: missing required field(s) "r09", "r19", "r29", "r39", "r49", "r59", "r69", "r79", "r89", "r99""""))
    }
  }

  def verifySerDeser[A](codec: Codec[A], obj: A, json: Array[Byte]): Unit = {
    verifySer(codec, obj, json)
    verifyDeser(codec, obj, json)
  }

  def verifySer[A](codec: Codec[A], obj: A, json: Array[Byte]): Unit = {
    val baos = new ByteArrayOutputStream
    codec.write(obj, baos)
    toString(baos.toByteArray) shouldBe toString(json)
    toString(codec.write(obj)) shouldBe toString(json)
  }

  def verifyDeser[A](codec: Codec[A], obj: A, json: Array[Byte]): Unit = {
    //FIXME: Failing when launched by 'sbt test'
    //codec.read(new ByteArrayInputStream(json)) shouldBe obj
    codec.read(json) shouldBe obj
  }

  def withConfig(configBuilder: Config.Builder => Config.Builder)(f: => Unit): Unit = {
    val currentConfig = JsoniterSpi.getCurrentConfig
    JsoniterSpi.setCurrentConfig(configBuilder(new Config.Builder).build)
    try f finally JsoniterSpi.setCurrentConfig(currentConfig)
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, "UTF-8")
}

case class UserId(value: String) extends AnyVal

case class OrderId(value: Int) extends AnyVal

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

case class BoxedPrimitives(b: java.lang.Byte, s: java.lang.Short, i: java.lang.Integer, l: java.lang.Long,
                           bl: java.lang.Boolean, ch: java.lang.Character, dbl: java.lang.Double, f: java.lang.Float)

case class StandardTypes(s: String, bi: BigInt, bd: BigDecimal)

object LocationType extends Enumeration {
  type LocationType = Value
  val GPS: LocationType = Value(1)
  val IP: LocationType = Value(2)
  val UserProvided: LocationType = Value(3)
}
case class Enums(lt: LocationType.LocationType)

case class ValueClassTypes(uid: UserId, oid: OrderId)

case class Options(os: Option[String], obi: Option[BigInt], osi: Option[Set[Int]])

case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

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

case class MutableMaps(hm: mutable.HashMap[Boolean, mutable.AnyRefMap[BigDecimal, Int]],
                       m: mutable.Map[Float, mutable.WeakHashMap[BigInt, String]],
                       ohm: mutable.OpenHashMap[Double, mutable.LinkedHashMap[Short, Double]])

case class ImmutableMaps(m: Map[Int, Double], hm: HashMap[String, ListMap[Int, BigInt]],
                         sm: SortedMap[Long, TreeMap[Double, Float]])

case class MutableLongMaps(lm1: mutable.LongMap[Double], lm2: mutable.LongMap[String])

case class ImmutableIntLongMaps(im: IntMap[Double], lm: LongMap[String])

case class BitSets(bs: BitSet, mbs: mutable.BitSet)

case class CamelAndSnakeCase(camelCase: String, snake_case: String)

case class Indented(f1: String, f2: List[String])

case class UTF8KeysAndValues(გასაღები: String)

case class Defaults(s: String = "VVV", i: Int = 1, bi: BigInt = BigInt(-1), l: List[Int] = List(0),
                    a: Array[Array[Double]] = Array(Array(-1.0, 0.0), Array(1.0)))

case class Transient(required: String, @transient transient: String = "default") {
  val ignored: String = required + "_" + transient
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
