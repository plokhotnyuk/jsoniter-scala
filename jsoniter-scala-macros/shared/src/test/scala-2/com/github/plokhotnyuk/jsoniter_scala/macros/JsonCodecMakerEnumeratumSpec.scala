/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.macros

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

sealed abstract class MyEnum(override val entryName: String) extends enumeratum.EnumEntry

object MyEnum extends enumeratum.Enum[MyEnum] {
  case object MyEnumAsText extends MyEnum("text")

  case object MyEnumAsCheckbox extends MyEnum("checkbox")

  val values = findValues
}

class JsonCodecMakerEnumeratumSpec extends VerifyingSpec {
  import com.github.plokhotnyuk.jsoniter_scala.core._
  import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
  import com.github.plokhotnyuk.jsoniter_scala.macros.NamespacePollutions._

  "Value codecs for Enumeratum enum" should {
    "serialize and deserialize when derived by macros" in {
      verifySerDeser(make[_root_.scala.List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        _root_.scala.List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["Red","Yellow","Green"]""")
      verifySerDeser(make[_root_.scala.List[TrafficLight]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)
        .withAdtLeafClassNameMapper(x => simpleClassName(x).toLowerCase)),
        _root_.scala.List(TrafficLight.Red, TrafficLight.Yellow, TrafficLight.Green), """["red","yellow","green"]""")
      verifySerDeser(make[_root_.scala.List[MyEnum]](CodecMakerConfig
        .withDiscriminatorFieldName(_root_.scala.None)
        .withAdtLeafClassNameMapper(x => simpleClassName(x) match {
          case "MyEnumAsText" => "text"
          case "MyEnumAsCheckbox" => "checkbox"
        })), _root_.scala.List(MyEnum.MyEnumAsText, MyEnum.MyEnumAsCheckbox), """["text","checkbox"]""")
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

      verifySerDeser(make[_root_.scala.List[MediaType]],
        _root_.scala.List(MediaType.`text/json`, MediaType.`text/html`, MediaType.`application/jpeg`), """[1,2,3]""")
    }
  }
  "Key codecs for Enumeratum enum" should {
/* FIXME: add generation of key codes for simple value enums
    "serialize and deserialize when derived by macros" in {
      verifySerDeser[_root_.scala.collection.Map[TrafficLight, _root_.scala.Boolean]](make[_root_.scala.collection.Map[TrafficLight, _root_.scala.Boolean]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)),
        _root_.scala.collection.Map(TrafficLight.Red -> true, TrafficLight.Yellow -> false, TrafficLight.Green -> true),
        """{"Red":true,"Yellow":false,"Green":true}""")
      verifySerDeser[_root_.scala.collection.Map[TrafficLight, _root_.scala.Boolean]](make[_root_.scala.collection.Map[TrafficLight, _root_.scala.Boolean]](CodecMakerConfig.withDiscriminatorFieldName(_root_.scala.None)
        .withAdtLeafClassNameMapper(x => simpleClassName(x).toLowerCase)),
        _root_.scala.collection.Map(TrafficLight.Red -> true, TrafficLight.Yellow -> false, TrafficLight.Green -> true),
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

      verifySerDeser[_root_.scala.collection.Map[MediaType, _root_.scala.Boolean]](make[_root_.scala.collection.Map[MediaType, _root_.scala.Boolean]],
        _root_.scala.collection.Map(MediaType.`text/json` -> true, MediaType.`text/html` -> false, MediaType.`application/jpeg` -> false),
        """{"1":true,"2":false,"3":false}""")
    }
  }
}