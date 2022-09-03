package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

sealed trait TrafficLight extends enumeratum.EnumEntry

object TrafficLight extends enumeratum.Enum[TrafficLight] {
  case object Red extends TrafficLight

  case object Yellow extends TrafficLight

  case object Green extends TrafficLight

  val values = findValues
}

sealed abstract class MediaType(val value: Long, name: String) extends enumeratum.values.LongEnumEntry

case object MediaType extends enumeratum.values.LongEnum[MediaType] {
  case object `text/json` extends MediaType(1L, "text/json")

  case object `text/html` extends MediaType(2L, "text/html")

  case object `application/jpeg` extends MediaType(3L, "application/jpeg")

  val values = findValues
}

class JsonCodecMakerEnumeratumSpec extends VerifyingSpec {
  "Value codecs for Enumeratum enum" should {
    "serialize and deserialize when derived by macros" in {
      verifySerDeser(make[List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["Red","Yellow","Green"]""")
      verifySerDeser(make[List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)
        .withAdtLeafClassNameMapper(x => simpleClassName(x).toLowerCase)),
        List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["red","yellow","green"]""")
    }
    "serialize and deserialize when injected by implicit vals as custom value codecs" in {
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

      verifySerDeser(make[List[MediaType]],
        List(MediaType.`text/json`, MediaType.`text/html`, MediaType.`application/jpeg`), """[1,2,3]""")
    }
  }
  "Key codecs for Enumeratum enum" should {
/* FIXME: add generation of key codes for simple value enums
    "serialize and deserialize when derived by macros" in {
      verifySerDeser[Map[TrafficLight, Boolean]](make[Map[TrafficLight, Boolean]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        Map(TrafficLight.Red -> true, TrafficLight.Yellow -> false, TrafficLight.Green -> true),
        """{"Red":true,"Yellow":false,"Green":true}""")
      verifySerDeser[Map[TrafficLight, Boolean]](make[Map[TrafficLight, Boolean]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)
        .withAdtLeafClassNameMapper(x => simpleClassName(x).toLowerCase)),
        Map(TrafficLight.Red -> true, TrafficLight.Yellow -> false, TrafficLight.Green -> true),
        """{"red":true,"yellow":false,"green":true}""")
    }
*/
    "serialize and deserialize when injected by implicit vals as custom value codecs" in {
      implicit val codecOfMediaType: JsonKeyCodec[MediaType] = new JsonKeyCodec[MediaType] {
        override def decodeKey(in: JsonReader): MediaType = in.readKeyAsLong() match {
          case 1L => MediaType.`text/json`
          case 2L => MediaType.`text/html`
          case 3L => MediaType.`application/jpeg`
          case x => in.decodeError(s"unexpected number key: $x")
        }

        override def encodeKey(x: MediaType, out: JsonWriter): _root_.scala.Unit = out.writeKey(x.value)
      }

      verifySerDeser[Map[MediaType, Boolean]](make[Map[MediaType, Boolean]],
        Map(MediaType.`text/json` -> true, MediaType.`text/html` -> false, MediaType.`application/jpeg` -> false),
        """{"1":true,"2":false,"3":false}""")
    }
  }
}