package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import zio.json.JsonDecoder.{JsonError, UnsafeJson}
import zio.json.internal.{Lexer, RetractReader, Write}
import zio.json.{DeriveJsonCodec, JsonCodec, JsonCodecConfiguration, JsonDecoder, JsonEncoder}
import java.util.Base64
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

object ZioJsonCodecs {
  implicit val config: JsonCodecConfiguration = JsonCodecConfiguration(explicitEmptyCollections = false)
  implicit val adtC3c: JsonCodec[ADTBase] = DeriveJsonCodec.gen
  implicit val geoJsonC3c: JsonCodec[GeoJSON.GeoJSON] = {
    implicit val c1: JsonCodec[GeoJSON.SimpleGeometry] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GeoJSON.Geometry] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GeoJSON.SimpleGeoJSON] = DeriveJsonCodec.gen
    DeriveJsonCodec.gen
  }
  implicit lazy val nestedStructsC3c: JsonCodec[NestedStructs] = DeriveJsonCodec.gen
  implicit val openRTBBidRequestC3c: JsonCodec[OpenRTB.BidRequest] = {
    def genOpenRTBBidRequestC3c1: JsonCodec[OpenRTB.BidRequest] = {
      implicit val c1: JsonCodec[OpenRTB.Segment] = DeriveJsonCodec.gen
      implicit val c2: JsonCodec[OpenRTB.Format] = DeriveJsonCodec.gen
      implicit val c3: JsonCodec[OpenRTB.Deal] = DeriveJsonCodec.gen
      implicit val c4: JsonCodec[OpenRTB.Metric] = DeriveJsonCodec.gen
      implicit val c5: JsonCodec[OpenRTB.Banner] = DeriveJsonCodec.gen
      implicit val c6: JsonCodec[OpenRTB.Audio] = DeriveJsonCodec.gen
      implicit val c7: JsonCodec[OpenRTB.Video] = DeriveJsonCodec.gen
      implicit val c8: JsonCodec[OpenRTB.Native] = DeriveJsonCodec.gen
      implicit val c9: JsonCodec[OpenRTB.Pmp] = DeriveJsonCodec.gen

      def genOpenRTBBidRequestC3c2: JsonCodec[OpenRTB.BidRequest] = {
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
        DeriveJsonCodec.gen
      }

      genOpenRTBBidRequestC3c2
    }

    genOpenRTBBidRequestC3c1
  }
  implicit val twitterAPIC3c: JsonCodec[TwitterAPI.Tweet] = {
    def genTwitterAPIC3c: JsonCodec[TwitterAPI.Tweet] = {
      implicit val c1: JsonCodec[TwitterAPI.UserMentions] = DeriveJsonCodec.gen
      implicit val c2: JsonCodec[TwitterAPI.Urls] = DeriveJsonCodec.gen
      implicit val c3: JsonCodec[TwitterAPI.Entities] = DeriveJsonCodec.gen
      implicit val c4: JsonCodec[TwitterAPI.Url] = DeriveJsonCodec.gen
      implicit val c5: JsonCodec[TwitterAPI.UserEntities] = DeriveJsonCodec.gen
      implicit val c6: JsonCodec[TwitterAPI.User] = DeriveJsonCodec.gen
      implicit val c7: JsonCodec[TwitterAPI.RetweetedStatus] = DeriveJsonCodec.gen
      DeriveJsonCodec.gen
    }

    genTwitterAPIC3c
  }
  implicit val anyValsC3cr: JsonCodec[AnyVals] = {
    implicit val c1: JsonCodec[ByteVal] = JsonCodec.byte.transform(ByteVal.apply, _.a)
    implicit val c2: JsonCodec[ShortVal] = JsonCodec.short.transform(ShortVal.apply, _.a)
    implicit val c3: JsonCodec[IntVal] = JsonCodec.int.transform(IntVal.apply, _.a)
    implicit val c4: JsonCodec[LongVal] = JsonCodec.long.transform(LongVal.apply, _.a)
    implicit val c5: JsonCodec[BooleanVal] = JsonCodec.boolean.transform(BooleanVal.apply, _.a)
    implicit val c6: JsonCodec[CharVal] = JsonCodec.char.transform(CharVal.apply, _.a)
    implicit val c7: JsonCodec[DoubleVal] = JsonCodec.double.transform(DoubleVal.apply, _.a)
    implicit val c8: JsonCodec[FloatVal] = JsonCodec.float.transform(FloatVal.apply, _.a)
    DeriveJsonCodec.gen
  }
  val base64C3c: JsonCodec[Array[Byte]] = new JsonCodec[Array[Byte]](
    (a: Array[Byte], _: Option[Int], out: Write) => {
      out.write('"')
      out.write(Base64.getEncoder.encodeToString(a))
      out.write('"')
    },
    (trace: List[JsonError], in: RetractReader) => Base64.getDecoder.decode(Lexer.string(trace, in).toString))
  implicit val extractFieldsC3c: JsonCodec[ExtractFields] = DeriveJsonCodec.gen
  implicit val gitHubActionsAPIC3c: JsonCodec[GitHubActionsAPI.Response] = {
    implicit val e1: JsonEncoder[Boolean] = (a: Boolean, _: Option[Int], out: Write) =>
      out.write(if (a) "\"true\"" else "\"false\"")
    implicit val d1: JsonDecoder[Boolean] = (trace: List[JsonError], in: RetractReader) => {
      Lexer.char(trace, in, '"')
      val x = Lexer.boolean(trace, in)
      Lexer.char(trace, in, '"')
      x
    }
    implicit val c2: JsonCodec[GitHubActionsAPI.Artifact] = DeriveJsonCodec.gen
    DeriveJsonCodec.gen
  }
  implicit val googleMapsAPIC3c: JsonCodec[GoogleMapsAPI.DistanceMatrix] = {
    implicit val c1: JsonCodec[GoogleMapsAPI.Value] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GoogleMapsAPI.Elements] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GoogleMapsAPI.Rows] = DeriveJsonCodec.gen
    DeriveJsonCodec.gen
  }
  implicit val missingRequiredFieldsC3c: JsonCodec[MissingRequiredFields] = DeriveJsonCodec.gen
  implicit val primitivesC3c: JsonCodec[Primitives] = DeriveJsonCodec.gen
  implicit val enumADTsC3c: JsonCodec[SuitADT] = new JsonCodec(new JsonEncoder[SuitADT] {
      override def unsafeEncode(a: SuitADT, indent: Option[Int], out: Write): Unit = {
        out.write('"')
        out.write(a.toString)
        out.write('"')
      }
    }, new JsonDecoder[SuitADT] {
      private[this] val m = Map(
        "Hearts" -> Hearts,
        "Spades" -> Spades,
        "Diamonds" -> Diamonds,
        "Clubs" -> Clubs)

      override def unsafeDecode(trace: List[JsonError], in: RetractReader): SuitADT =
        m.getOrElse(Lexer.string(trace, in).toString, throwError("SuitADT", trace))
    })
  implicit val arrayOfEnumsC3c: JsonCodec[Array[SuitEnum]] = new JsonCodec(
    JsonEncoder.array[SuitEnum]({ (a: SuitEnum, _: Option[Int], out: Write) =>
      out.write('"')
      out.write(a.toString)
      out.write('"')
    }, ClassTag(classOf[SuitEnum])), JsonDecoder.array[SuitEnum](new JsonDecoder[SuitEnum] {
      override def unsafeDecode(trace: List[JsonError], in: RetractReader): SuitEnum = {
        val s = Lexer.string(trace, in).toString
        var v: SuitEnum = null
        try v = SuitEnum.withName(s) catch {
          case _: NoSuchElementException =>
        }
        if (v eq null) throwError("SuitEnum", trace)
        v
      }
    }, ClassTag(classOf[SuitEnum])))
  implicit val arraySeqOfBooleansD5r: JsonDecoder[ArraySeq[Boolean]] =
    JsonDecoder.array[Boolean].map(ArraySeq.unsafeWrapArray)

  private[this] def throwError(msg: String, trace: List[JsonError]): Nothing =
    throw UnsafeJson(JsonError.Message(msg) :: trace)
}