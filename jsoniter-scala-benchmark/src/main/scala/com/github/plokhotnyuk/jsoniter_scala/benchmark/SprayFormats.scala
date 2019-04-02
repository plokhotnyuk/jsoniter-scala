package com.github.plokhotnyuk.jsoniter_scala.benchmark

import spray.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

// Based on the code found: https://github.com/spray/spray-json/issues/200
class EnumJsonFormat[T <: scala.Enumeration](e: T) extends RootJsonFormat[T#Value] {
  override def read(json: JsValue): T#Value =
    e.values.iterator.find { ev =>
      json.isInstanceOf[JsString] && json.asInstanceOf[JsString].value == ev.toString
    }.getOrElse(deserializationError(s"No value found in enum $e for $json"))

  override def write(ev: T#Value): JsValue = JsString(ev.toString)
}

object SprayFormats extends DefaultJsonProtocol {
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
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingReqFields] = jsonFormat2(MissingReqFields)
  implicit val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = jsonFormat1(NestedStructs)
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormat8(Primitives)
  implicit val suitEnumJsonFormat: EnumJsonFormat[SuitEnum.type] = new EnumJsonFormat(SuitEnum)

  implicit def arrayBufferFormat[T :JsonFormat]: RootJsonFormat[ArrayBuffer[T]] =
    new RootJsonFormat[mutable.ArrayBuffer[T]] {
      def read(value: JsValue): mutable.ArrayBuffer[T] =
        if (!value.isInstanceOf[JsArray]) deserializationError(s"Expected List as JsArray, but got $value")
        else {
          val es = value.asInstanceOf[JsArray].elements
          new mutable.ArrayBuffer[T](es.size) {
            es.foreach(e => this += e.convertTo[T])
          }
        }

      def write(buf: mutable.ArrayBuffer[T]) = JsArray(buf.map(_.toJson).toVector)
    }
}
