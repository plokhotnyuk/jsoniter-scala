package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Objects, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException
import scala.annotation.switch
import scala.util.hashing.MurmurHash3

enum TrafficLight:
  case Red, Yellow, Green

enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)

enum MediaType(name: String):
  case `text/json` extends MediaType("text/json")
  case `text/html` extends MediaType("text/html")
  case `application/jpeg` extends MediaType("application/jpeg")

enum ColorADT(val rgb: Int):
  case Red   extends ColorADT(0xFF0000)
  case Green extends ColorADT(0x00FF00)
  case Blue  extends ColorADT(0x0000FF)
  case Mix(mix: Int) extends ColorADT(mix)

enum Planet(mass: Double, radius: Double) {
  private final val G = 6.67300E-11

  def surfaceGravity: Double = G * mass / (radius * radius)

  def surfaceWeight(otherMass: Double): Double = otherMass * surfaceGravity

  case Mercury extends Planet(3.303e+23, 2.4397e6)
  case Venus   extends Planet(4.869e+24, 6.0518e6)
  case Earth   extends Planet(5.976e+24, 6.37814e6)
  case Mars    extends Planet(6.421e+23, 3.3972e6)
  case Jupiter extends Planet(1.9e+27,   7.1492e7)
  case Saturn  extends Planet(5.688e+26, 6.0268e7)
  case Uranus  extends Planet(8.686e+25, 2.5559e7)
  case Neptune extends Planet(1.024e+26, 2.4746e7)
}

enum GEnum[A]:
  case IsDir(path: String) extends GEnum[Boolean]
  case Exists(path: String) extends GEnum[Boolean]
  case ReadBytes(path: String) extends GEnum[Seq[Byte]]
  case CopyOver(src: Seq[Byte], path: String) extends GEnum[Int]

enum FruitEnum[T <: FruitEnum[T]]:
  case Apple(family: String) extends FruitEnum[Apple]
  case Orange(color: Int) extends FruitEnum[Orange]

case class FruitEnumBasket[T <: FruitEnum[T]](fruits: List[T])

enum FooEnum[A[_]]:
  case Bar[A[_]](a: A[Int]) extends FooEnum[A]
  case Baz[A[_]](a: A[String]) extends FooEnum[A]

class JsonCodecMakerNewEnumSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize Scala3 enums" in {
      verifySerDeser(make[List[TrafficLight]],
        List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green),
        """[{"type":"Red"},{"type":"Yellow"},{"type":"Green"}]""")
      verifySerDeser(make[List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(None)),
        List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["Red","Yellow","Green"]""")
      verifySerDeser(make[List[TrafficLight]](CodecMakerConfig
        .withAdtLeafClassNameMapper(x => JsonCodecMaker.simpleClassName(x) match {
          case "Red" => "🟥"
          case "Yellow" => "🟨"
          case "Green" => "🟩"
        }).withDiscriminatorFieldName(None)),
        List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["🟥","🟨","🟩"]""".stripMargin)
    }
    "serialize and deserialize Scala3 enums with parameters" in {
      verifySerDeser(make[List[Color]](CodecMakerConfig.withDiscriminatorFieldName(None)),
        List(Color.Red, Color.Green, Color.Blue), """["Red","Green","Blue"]""")
    }
    "serialize and deserialize Scala3 enums with multiple parameters" in {
      verifySerDeser(make[List[Planet]](CodecMakerConfig.withDiscriminatorFieldName(None)),
        List(Planet.Mercury, Planet.Mars), """["Mercury","Mars"]""")
    }
    "serialize and deserialize Scala3 enums using a custom codec" in {
      implicit val codecOfMediaType: JsonValueCodec[MediaType] = new JsonValueCodec[MediaType] {
        override val nullValue: MediaType = null

        override def decodeValue(in: JsonReader, default: MediaType): MediaType = in.readByte() match {
          case 0 => MediaType.`text/json`
          case 1 => MediaType.`text/html`
          case 2 => MediaType.`application/jpeg`
          case x => in.decodeError(s"unexpected number value: $x")
        }

        override def encodeValue(x: MediaType, out: JsonWriter): Unit = out.writeVal(x.ordinal)
      }

      verifySerDeser[List[MediaType]](make[List[MediaType]],
        List(MediaType.`text/json`, MediaType.`text/html`, MediaType.`application/jpeg`), """[0,1,2]""")
    }
    "serialize and deserialize Scala3 enum ADTs" in {
      verifySerDeser(make[List[ColorADT]], List(ColorADT.Red, ColorADT.Green, ColorADT.Mix(0)),
        """[{"type":"Red"},{"type":"Green"},{"type":"Mix","mix":0}]""")
      verifySerDeser(make[List[ColorADT]](CodecMakerConfig.withDiscriminatorFieldName(None)),
        List(ColorADT.Red, ColorADT.Green, ColorADT.Mix(0)), """["Red","Green",{"Mix":{"mix":0}}]""")
    }
    "serialize and deserialize Scala3 generic enum ADTs" in {
      verifySerDeser(make[Array[GEnum[_]]],
        Array[GEnum[_]](GEnum.Exists("WWW"), GEnum.ReadBytes("QQQ"), GEnum.CopyOver("AAA".getBytes.toSeq, "OOO")),
        """[{"type":"Exists","path":"WWW"},{"type":"ReadBytes","path":"QQQ"},{"type":"CopyOver","src":[65,65,65],"path":"OOO"}]""")
    }
    "serialize and deserialize enum ADTs with self-recursive (aka F-bounded) types" in {
      val oneFruit: FruitEnumBasket[FruitEnum.Apple] = FruitEnumBasket(List(FruitEnum.Apple("golden")))
      val twoFruits: FruitEnumBasket[FruitEnum.Apple] = oneFruit.copy(fruits = oneFruit.fruits :+ FruitEnum.Apple("red"))
      val message = intercept[TestFailedException](assertCompiles {
        """oneFruit.copy(fruits = oneFruit.fruits :+ FruitEnum.Orange(0))"""
      }).getMessage
      assert(message.contains("Found:    com.github.plokhotnyuk.jsoniter_scala.macros.FruitEnum.Orange\nRequired: com.github.plokhotnyuk.jsoniter_scala.macros.FruitEnum.Apple"))
      verifySerDeser(make[FruitEnumBasket[FruitEnum.Apple]], twoFruits,
        """{"fruits":[{"family":"golden"},{"family":"red"}]}""")
      verifySerDeser(make[FruitEnumBasket[FruitEnum.Orange]], FruitEnumBasket(List(FruitEnum.Orange(1), FruitEnum.Orange(2))),
        """{"fruits":[{"color":1},{"color":2}]}""")
    }
    "serialize and deserialize higher-kinded enum ADTs" in {
      verifySerDeser(make[List[FooEnum[Option]]], List(FooEnum.Bar[Option](Some(1)), FooEnum.Baz[Option](Some("VVV"))),
        """[{"type":"Bar","a":1},{"type":"Baz","a":"VVV"}]""")
    }
    "serialize and deserialize Scala3 enum ADTs defined with `derives` keyword" in {
      trait DefaultJsonValueCodec[A] extends JsonValueCodec[A]

      object DefaultJsonValueCodec {
        inline def derived[A]: DefaultJsonValueCodec[A] = new DefaultJsonValueCodec[A] {
          private val impl = JsonCodecMaker.make[A](CodecMakerConfig.withDiscriminatorFieldName(Some("name")))
          export impl._
        }
      }

      enum TestEnum derives DefaultJsonValueCodec:
        case Value1
        case Value2(string: String)

      verifySerDeser(summon[JsonValueCodec[TestEnum]], TestEnum.Value2("VVV"), """{"name":"Value2","string":"VVV"}""")
    }
  }
}