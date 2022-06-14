package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import zio.json.JsonDecoder.{JsonError, UnsafeJson}
import zio.json._
import zio.json.internal._
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.ClassTag

object ZioJSONEncoderDecoders extends ZioJSONScalaJsEncoderDecoders {
  implicit val (adtE5r: JsonEncoder[ADTBase], adtD5r: JsonDecoder[ADTBase]) = {
    implicit val c1: JsonCodec[ADTBase] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (geoJsonE5r: JsonEncoder[GeoJSON.GeoJSON], geoJsonD5r: JsonDecoder[GeoJSON.GeoJSON]) = {
    implicit val c1: JsonCodec[GeoJSON.SimpleGeometry] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GeoJSON.Geometry] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GeoJSON.SimpleGeoJSON] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[GeoJSON.GeoJSON] = DeriveJsonCodec.gen
    (c4.encoder, c4.decoder)
  }
  implicit val (nestedStructsE5r: JsonEncoder[NestedStructs], nestedStructsD5r: JsonDecoder[NestedStructs]) = {
    implicit lazy val c1: JsonCodec[NestedStructs] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (openRTBBidRequestE5r: JsonEncoder[OpenRTB.BidRequest], openRTBBidRequestD5r: JsonDecoder[OpenRTB.BidRequest]) = {
    implicit val c1: JsonCodec[OpenRTB.Segment] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[OpenRTB.Format] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[OpenRTB.Deal] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[OpenRTB.Metric] = DeriveJsonCodec.gen
    implicit val c5: JsonCodec[OpenRTB.Banner] = DeriveJsonCodec.gen
    implicit val c6: JsonCodec[OpenRTB.Audio] = DeriveJsonCodec.gen
    implicit val c7: JsonCodec[OpenRTB.Video] = DeriveJsonCodec.gen
    implicit val c8: JsonCodec[OpenRTB.Native] = DeriveJsonCodec.gen
    implicit val c9: JsonCodec[OpenRTB.Pmp] = DeriveJsonCodec.gen
    implicit val c10: JsonCodec[OpenRTB.Producer] = DeriveJsonCodec.gen
    implicit val c11: JsonCodec[OpenRTB.Data] = DeriveJsonCodec.gen
    implicit val c12: JsonCodec[OpenRTB.Content] = DeriveJsonCodec.gen
    implicit val c13: JsonCodec[OpenRTB.Publisher] = DeriveJsonCodec.gen
    implicit val c14: JsonCodec[OpenRTB.Geo] = DeriveJsonCodec.gen
    implicit val c15: JsonCodec[OpenRTB.Imp] = DeriveJsonCodec.gen
    implicit val c16: JsonCodec[OpenRTB.Site] = DeriveJsonCodec.gen
    implicit val c17: JsonCodec[OpenRTB.App] = DeriveJsonCodec.gen
    implicit val c18: JsonCodec[OpenRTB.Device] = DeriveJsonCodec.gen
    implicit val c19: JsonCodec[OpenRTB.User] = DeriveJsonCodec.gen
    implicit val c20: JsonCodec[OpenRTB.Source] = DeriveJsonCodec.gen
    implicit val c21: JsonCodec[OpenRTB.Reqs] = DeriveJsonCodec.gen
    val c22: JsonCodec[OpenRTB.BidRequest] = DeriveJsonCodec.gen
    (c22.encoder, c22.decoder)
  }
  implicit val (twitterAPIE5r: JsonEncoder[TwitterAPI.Tweet], twitterAPID5r: JsonDecoder[TwitterAPI.Tweet]) = {
    implicit val c1: JsonCodec[TwitterAPI.UserMentions] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[TwitterAPI.Urls] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[TwitterAPI.Entities] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[TwitterAPI.Url] = DeriveJsonCodec.gen
    implicit val c5: JsonCodec[TwitterAPI.UserEntities] = DeriveJsonCodec.gen
    implicit val c6: JsonCodec[TwitterAPI.User] = DeriveJsonCodec.gen
    implicit val c7: JsonCodec[TwitterAPI.RetweetedStatus] = DeriveJsonCodec.gen
    val c8: JsonCodec[TwitterAPI.Tweet] = DeriveJsonCodec.gen
    (c8.encoder, c8.decoder)
  }
}

object ZioJSONScalaJsEncoderDecoders extends ZioJSONScalaJsEncoderDecoders

trait ZioJSONScalaJsEncoderDecoders {
  implicit val (anyValsE5r: JsonEncoder[AnyVals], anyValsD5r: JsonDecoder[AnyVals]) = {
    implicit val c1: JsonCodec[ByteVal] = JsonCodec.byte.transform(ByteVal.apply, _.a)
    implicit val c2: JsonCodec[ShortVal] = JsonCodec.short.transform(ShortVal.apply, _.a)
    implicit val c3: JsonCodec[IntVal] = JsonCodec.int.transform(IntVal.apply, _.a)
    implicit val c4: JsonCodec[LongVal] = JsonCodec.long.transform(LongVal.apply, _.a)
    implicit val c5: JsonCodec[BooleanVal] = JsonCodec.boolean.transform(BooleanVal.apply, _.a)
    implicit val c6: JsonCodec[CharVal] = JsonCodec.char.transform(CharVal.apply, _.a)
    implicit val c7: JsonCodec[DoubleVal] = JsonCodec.double.transform(DoubleVal.apply, _.a)
    implicit val c8: JsonCodec[FloatVal] = JsonCodec.float.transform(FloatVal.apply, _.a)
    val c9: JsonCodec[AnyVals] = DeriveJsonCodec.gen
    (c9.encoder, c9.decoder)
  }
  val base64C3c: JsonCodec[Array[Byte]] = new JsonCodec[Array[Byte]](
    (a: Array[Byte], _: Option[Int], out: Write) => {
      out.write('"')
      out.write(Base64.getEncoder.encodeToString(a))
      out.write('"')
    },
    (trace: List[JsonError], in: RetractReader) => Base64.getDecoder.decode(Lexer.string(trace, in).toString))
  implicit val (extractFieldsE5r: JsonEncoder[ExtractFields], extractFieldsD5r: JsonDecoder[ExtractFields]) = {
    val c1: JsonCodec[ExtractFields] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (gitHubActionsAPIE5r: JsonEncoder[GitHubActionsAPI.Response], gitHubActionsAPID5r: JsonDecoder[GitHubActionsAPI.Response]) = {
    implicit val e1: JsonEncoder[Boolean] = (a: Boolean, _: Option[Int], out: Write) =>
      out.write(if (a) "\"true\"" else "\"false\"")
    implicit val d1: JsonDecoder[Boolean] = (trace: List[JsonError], in: RetractReader) => {
      Lexer.char(trace, in, '"')
      val x = Lexer.boolean(trace, in)
      Lexer.char(trace, in, '"')
      x
    }
    implicit val c2: JsonCodec[GitHubActionsAPI.Artifact] = DeriveJsonCodec.gen
    val c3: JsonCodec[GitHubActionsAPI.Response] = DeriveJsonCodec.gen
    (c3.encoder, c3.decoder)
  }
  implicit val (googleMapsAPIE5r: JsonEncoder[GoogleMapsAPI.DistanceMatrix], googleMapsAPID5r: JsonDecoder[GoogleMapsAPI.DistanceMatrix]) = {
    implicit val c1: JsonCodec[GoogleMapsAPI.Value] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GoogleMapsAPI.Elements] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GoogleMapsAPI.Rows] = DeriveJsonCodec.gen
    val c4: JsonCodec[GoogleMapsAPI.DistanceMatrix] = DeriveJsonCodec.gen
    (c4.encoder, c4.decoder)
  }
  implicit val (missingRequiredFieldsE5r: JsonEncoder[MissingRequiredFields], missingRequiredFieldsD5r: JsonDecoder[MissingRequiredFields]) = {
    val c1: JsonCodec[MissingRequiredFields] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (primitivesE5r: JsonEncoder[Primitives], primitivesD5r: JsonDecoder[Primitives]) = {
    val c1: JsonCodec[Primitives] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (arrayOfEnumADTsE5r: JsonEncoder[Array[SuitADT]], arrayOfEnumADTsD5r: JsonDecoder[Array[SuitADT]]) =
    (JsonEncoder.array[SuitADT]({ (a: SuitADT, _: Option[Int], out: Write) =>
      out.write('"')
      out.write(a.toString)
      out.write('"')
    }, ClassTag(classOf[SuitADT])), JsonDecoder.array[SuitADT](new JsonDecoder[SuitADT] {
      private[this] val suite = Map(
        "Hearts" -> Hearts,
        "Spades" -> Spades,
        "Diamonds" -> Diamonds,
        "Clubs" -> Clubs)

      override def unsafeDecode(trace: List[JsonError], in: RetractReader): SuitADT =
        suite.getOrElse(Lexer.string(trace, in).toString, throwError("SuitADT", trace))
    }, ClassTag(classOf[SuitADT])))
  implicit val (arrayOfEnumsE5r: JsonEncoder[Array[SuitEnum]], arrayOfEnumsD5r: JsonDecoder[Array[SuitEnum]]) =
    (JsonEncoder.array[SuitEnum]({ (a: SuitEnum, _: Option[Int], out: Write) =>
      out.write('"')
      out.write(a.toString)
      out.write('"')
    }, ClassTag(classOf[SuitEnum])), JsonDecoder.array[SuitEnum](new JsonDecoder[SuitEnum] {
      private[this] val ec = new ConcurrentHashMap[String, SuitEnum]

      override def unsafeDecode(trace: List[JsonError], in: RetractReader): SuitEnum = {
        val s = Lexer.string(trace, in).toString
        var v = ec.get(s)
        if (v eq null) {
          v = SuitEnum.values.iterator.find(_.toString == s).getOrElse(throwError("SuitEnum", trace))
          ec.put(s, v)
        }
        v
      }
    }, ClassTag(classOf[SuitEnum])))

  private[this] def throwError(msg: String, trace: List[JsonError]): Nothing =
    throw UnsafeJson(JsonError.Message(msg) :: trace)
}