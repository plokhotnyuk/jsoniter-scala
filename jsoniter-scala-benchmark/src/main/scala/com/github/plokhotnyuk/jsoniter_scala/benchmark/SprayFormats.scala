package com.github.plokhotnyuk.jsoniter_scala.benchmark

import spray.json.{RootJsonFormat, _}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

// Based on the code found: https://github.com/spray/spray-json/issues/200
class EnumJsonFormat[T <: scala.Enumeration](e: T) extends RootJsonFormat[T#Value] {
  override def read(json: JsValue): T#Value =
    e.values.iterator.find { ev =>
      json.isInstanceOf[JsString] && json.asInstanceOf[JsString].value == ev.toString
    }.getOrElse(deserializationError(s"No value found in enum $e for $json"))

  override def write(ev: T#Value): JsValue = JsString(ev.toString)
}

object SprayFormats extends DefaultJsonProtocol {
  val jsonParserSettings: JsonParserSettings = JsonParserSettings.default
    .withMaxDepth(Int.MaxValue).withMaxNumberCharacters(Int.MaxValue) /*WARNING: don't do this for open-systems*/
  implicit val anyRefsJsonFormat: RootJsonFormat[AnyRefs] = jsonFormat3(AnyRefs)
/* Spray-JSON throws java.lang.ExceptionInInitializerError
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = {
    implicit val jf1: RootJsonFormat[ByteVal] = jsonFormat1(ByteVal)
    implicit val jf2: RootJsonFormat[ShortVal] = jsonFormat1(ShortVal)
    implicit val jf3: RootJsonFormat[IntVal] = jsonFormat1(IntVal)
    implicit val jf4: RootJsonFormat[LongVal] = jsonFormat1(LongVal)
    implicit val jf5: RootJsonFormat[BooleanVal] = jsonFormat1(BooleanVal)
    implicit val jf6: RootJsonFormat[CharVal] = jsonFormat1(CharVal)
    implicit val jf7: RootJsonFormat[DoubleVal] = jsonFormat1(DoubleVal)
    implicit val jf8: RootJsonFormat[FloatVal] = jsonFormat1(FloatVal)
    jsonFormat8(AnyVals)
  }
*/
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormat2(ExtractFields)
  implicit val googleMapsAPIJsonFormat: RootJsonFormat[DistanceMatrix] = {
    implicit val jf1: RootJsonFormat[Value] = jsonFormat2(Value)
    implicit val jf2: RootJsonFormat[Elements] = jsonFormat3(Elements)
    implicit val jf3: RootJsonFormat[Rows] = jsonFormat1(Rows)
    jsonFormat4(DistanceMatrix)
  }
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingReqFields] = jsonFormat2(MissingReqFields)
  implicit val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = jsonFormat1(NestedStructs)
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormat8(Primitives)
  implicit val suitEnumJsonFormat: EnumJsonFormat[SuitEnum.type] = new EnumJsonFormat(SuitEnum)
  implicit val suitJavaEnumJsonFormat: RootJsonFormat[Suit] = new RootJsonFormat[Suit] {
    override def read(json: JsValue): Suit = Try(Suit.valueOf(json.asInstanceOf[JsString].value))
      .getOrElse(deserializationError(s"No value found in Suit enum for $json"))

    override def write(ev: Suit): JsValue = JsString(ev.name)
  }

  implicit def arrayBufferFormat[T :JsonFormat]: RootJsonFormat[ArrayBuffer[T]] =
    new RootJsonFormat[mutable.ArrayBuffer[T]] {
      def read(value: JsValue): mutable.ArrayBuffer[T] =
        if (!value.isInstanceOf[JsArray]) deserializationError(s"Expected List as JsArray, but got $value")
        else {
          val es = value.asInstanceOf[JsArray].elements
          val buf = new mutable.ArrayBuffer[T](es.size)
          es.foreach(e => buf += e.convertTo[T])
          buf
        }

      def write(buf: mutable.ArrayBuffer[T]) = JsArray(buf.map(_.toJson).toVector)
    }
}
