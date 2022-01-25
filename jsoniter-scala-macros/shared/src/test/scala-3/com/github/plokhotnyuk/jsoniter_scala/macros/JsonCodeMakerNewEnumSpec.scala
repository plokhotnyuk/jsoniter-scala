package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Objects, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException
import scala.annotation.switch
import scala.util.hashing.MurmurHash3

enum TrafficLight {
  case Red, Yellow, Green
}

enum MediaType(val value: Long, name: String) {
  case `text/json` extends MediaType(1L, "text/json")
  case `text/html` extends MediaType(2L, "text/html")
  case `application/jpeg` extends MediaType(3L, "application/jpeg")
}

enum Color(val rgb: Int):
  case Red   extends Color(0xFF0000)
  case Green extends Color(0x00FF00)
  case Blue  extends Color(0x0000FF)

enum ColorADT(val rgb: Int):
  case Red   extends ColorADT(0xFF0000)
  case Green extends ColorADT(0x00FF00)
  case Blue  extends ColorADT(0x0000FF)
  case Mix(mix: Int) extends ColorADT(mix)

enum Planet(mass: Double, radius: Double):
  private final val G = 6.67300E-11

  def surfaceGravity = G * mass / (radius * radius)

  def surfaceWeight(otherMass: Double) = otherMass * surfaceGravity

  case Mercury extends Planet(3.303e+23, 2.4397e6)
  case Venus   extends Planet(4.869e+24, 6.0518e6)
  case Earth   extends Planet(5.976e+24, 6.37814e6)
  case Mars    extends Planet(6.421e+23, 3.3972e6)
  case Jupiter extends Planet(1.9e+27,   7.1492e7)
  case Saturn  extends Planet(5.688e+26, 6.0268e7)
  case Uranus  extends Planet(8.686e+25, 2.5559e7)
  case Neptune extends Planet(1.024e+26, 2.4746e7)
end Planet

// TODO:
//   Enum ADT with type parameters
//   ordinal flag (create config param)

class JsonCodecMakerEnumSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize Scala3 enums" in {
      verifySerDeser(make[List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(None)),
        List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["Red","Yellow","Green"]""")

      implicit val codecOfMediaType: JsonValueCodec[MediaType] = new JsonValueCodec[MediaType] {
        override val nullValue: MediaType = null

        override def decodeValue(in: JsonReader, default: MediaType): MediaType = in.readLong() match {
          case 1L => MediaType.`text/json`
          case 2L => MediaType.`text/html`
          case 3L => MediaType.`application/jpeg`
          case x => in.decodeError(s"unexpected number value: $x")
        }

        override def encodeValue(x: MediaType, out: JsonWriter): _root_.scala.Unit = out.writeVal(x.value)
      }

      verifySerDeser[List[MediaType]](make[List[MediaType]],
        List(MediaType.`text/json`, MediaType.`text/html`, MediaType.`application/jpeg`), """[1,2,3]""")
    }
    "serialize and deserialize Scala3 enums with parameters" in {
      verifySerDeser(make[List[Color]](CodecMakerConfig),
        List(Color.Red, Color.Red, Color.Green, Color.Blue), """["Red","Red","Green","Blue"]""")
    }
    "serialize and deserialize Scala3 enums with multiple parameters" in {
      verifySerDeser(make[List[Planet]](CodecMakerConfig),
        List(Planet.Mercury, Planet.Mars), """["Mercury","Mars"]""")
    }
    "serialize and deserialize Scala3 enums ADT" in {
      verifySerDeser(make[List[ColorADT]](CodecMakerConfig),
        List(ColorADT.Red, ColorADT.Green, ColorADT.Mix(0)), """["Red","Green",{"type":"Mix","mix":0}]""")
    }
    "serialize and deserialize Scala3 enums ADT defined with `derives` keyword" in {
      sealed trait DefaultJsonValueCodec[A] extends JsonValueCodec[A]

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