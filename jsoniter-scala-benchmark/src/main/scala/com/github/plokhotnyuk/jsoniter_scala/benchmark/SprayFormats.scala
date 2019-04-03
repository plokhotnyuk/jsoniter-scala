package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.util.UUID

import spray.json.{RootJsonFormat, _}

import scala.collection.immutable.Map
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
  // Based on the Cat/Dog sample: https://gist.github.com/jrudolph/f2d0825aac74ed81c92a
  val adtBaseJsonFormat: RootJsonFormat[ADTBase] = {
    implicit lazy val xjf: RootJsonFormat[X] = jsonFormat1(X)
    implicit lazy val yjf: RootJsonFormat[Y] = jsonFormat1(Y)
    implicit lazy val zjf: RootJsonFormat[Z] = jsonFormat2(Z)
    implicit lazy val ajf: RootJsonFormat[ADTBase] = new RootJsonFormat[ADTBase] {
      override def read(json: JsValue): ADTBase = json.asJsObject.getFields("type") match {
        case Seq(JsString("X")) => json.convertTo[X]
        case Seq(JsString("Y")) => json.convertTo[Y]
        case Seq(JsString("Z")) => json.convertTo[Z]
        case _ => deserializationError(s"Cannot deserialize ADTBase")
      }

      override def write(obj: ADTBase): JsValue = JsObject((obj match {
        case x: X => x.toJson
        case y: Y => y.toJson
        case z: Z => z.toJson
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }
    ajf
  }
  implicit val anyRefsJsonFormat: RootJsonFormat[AnyRefs] = jsonFormat3(AnyRefs)
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = {
    // Based on the following "horrible hack": https://github.com/spray/spray-json/issues/38#issuecomment-11708058
    case class AnyValJsonFormat[T <: AnyVal{def a : V}, V](construct: V => T)(implicit jf: JsonFormat[V]) extends JsonFormat[T] {
      import scala.language.reflectiveCalls

      override def read(json: JsValue): T = construct(jf.read(json))

      override def write(obj: T): JsValue = jf.write(obj.a)
    }

    implicit val jf1: JsonFormat[ByteVal] = AnyValJsonFormat(ByteVal)
    implicit val jf2: JsonFormat[ShortVal] = AnyValJsonFormat(ShortVal)
    implicit val jf3: JsonFormat[IntVal] = AnyValJsonFormat(IntVal)
    implicit val jf4: JsonFormat[LongVal] = AnyValJsonFormat(LongVal)
    implicit val jf5: JsonFormat[BooleanVal] = AnyValJsonFormat(BooleanVal)
    implicit val jf6: JsonFormat[CharVal] = AnyValJsonFormat(CharVal)
    implicit val jf7: JsonFormat[DoubleVal] = AnyValJsonFormat(DoubleVal)
    implicit val jf8: JsonFormat[FloatVal] = AnyValJsonFormat(FloatVal)
    jsonFormat8(AnyVals)
  }
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
  implicit val suitEnumADTJsonFormat: RootJsonFormat[SuitADT] = new RootJsonFormat[SuitADT] {
    private[this] val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)

    override def read(json: JsValue): SuitADT = Try(suite(json.asInstanceOf[JsString].value))
      .getOrElse(deserializationError(s"No value found in Suit enum for $json"))

    override def write(ev: SuitADT): JsValue = JsString(ev.toString)
  }
  implicit val suitEnumJsonFormat: EnumJsonFormat[SuitEnum.type] = new EnumJsonFormat(SuitEnum)
  implicit val suitJavaEnumJsonFormat: RootJsonFormat[Suit] = new RootJsonFormat[Suit] {
    override def read(json: JsValue): Suit = Try(Suit.valueOf(json.asInstanceOf[JsString].value))
      .getOrElse(deserializationError(s"No value found in Suit enum for $json"))

    override def write(ev: Suit): JsValue = JsString(ev.name)
  }
  implicit val uuidJsonFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)

    def read(json: JsValue): UUID = Try(UUID.fromString(json.asInstanceOf[JsString].value))
      .getOrElse(deserializationError("Expected hexadecimal UUID string"))
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
