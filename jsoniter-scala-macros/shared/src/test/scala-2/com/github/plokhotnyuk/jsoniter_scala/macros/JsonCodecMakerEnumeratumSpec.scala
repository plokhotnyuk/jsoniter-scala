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
  import NamespacePollutions._

  "JsonCodecMaker.make generates codecs which" should {
    "serialize and deserialize Enumeratum enums" in {
      verifySerDeser(make[List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
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
  }
}