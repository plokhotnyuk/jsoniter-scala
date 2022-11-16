package com.github.plokhotnyuk.jsoniter_scala.benchmark

import play.api.libs.functional.syntax._
import play.api.libs.json._
import java.time._
import java.util.Base64
import scala.collection.immutable.{BitSet, IntMap, Map, Seq}
import scala.collection.mutable
import scala.util.Try

object PlayJsonFormats {
  implicit val config = JsonConfiguration(typeNaming = JsonNaming { fullName =>
    fullName.substring(Math.max(fullName.lastIndexOf('.') + 1, 0))
  }, discriminator = "type")

  def stringFormat[A](name: String)(f: String => A): Format[A] = new Format[A] {
    override def reads(js: JsValue): JsResult[A] =
      Try(JsSuccess(f(js.asInstanceOf[JsString].value))).getOrElse(JsError(s"expected.${name}string"))

    override def writes(v: A): JsValue = JsString(v.toString)
  }

  implicit def mutableMapReads[A, B](implicit mapReads: Reads[Map[A, B]]): Reads[mutable.Map[A, B]] =
    Reads[mutable.Map[A, B]](js => JsSuccess(js.as[Map[A, B]].foldLeft(mutable.Map.empty[A, B]) {
      (m, p) => m += ((p._1, p._2))
    }))

  implicit def mutableLongMapFormat[A](implicit mapReads: Reads[Map[Long, A]],
                                       aWrites: Writes[A]): Format[mutable.LongMap[A]] =
    new Format[mutable.LongMap[A]] {
      override def reads(js: JsValue): JsResult[mutable.LongMap[A]] =
        JsSuccess(js.as[Map[Long, A]].foldLeft(mutable.LongMap.empty[A]) { (m, p) =>
          m.update(p._1, p._2)
          m
        })

      override def writes(v: mutable.LongMap[A]): JsValue =
        Json.toJsObject(v.foldLeft(mutable.LinkedHashMap.empty[String, JsValue]) {
          (m, p) => m += ((p._1.toString, aWrites.writes(p._2)))
        })
    }

  implicit def intMapFormat[A](implicit mapReads: Reads[Map[Int, A]], aWrites: Writes[A]): Format[IntMap[A]] =
    new Format[IntMap[A]] {
      override def reads(js: JsValue): JsResult[IntMap[A]] =
        JsSuccess(js.as[Map[Int, A]].foldLeft(IntMap.empty[A])((m, p) => m.updated(p._1, p._2)))

      override def writes(v: IntMap[A]): JsValue =
        Json.toJsObject(v.foldLeft(mutable.LinkedHashMap.empty[String, JsValue]) {
          (m, p) => m += ((p._1.toString, aWrites.writes(p._2)))
        })
    }

  // Allow case classes with Tuple2 types to be represented as a Json Array with 2 elements e.g. (Double, Double)
  // Borrowed from https://gist.github.com/alexanderjarvis/4595298
  implicit def tuple2Format[A, B](implicit aFormat: Format[A], bFormat: Format[B]): Format[(A, B)] =
    new Format[(A, B)] {
      override def reads(js: JsValue): JsResult[(A, B)] = Try(readsUnsafe(js)).getOrElse(JsError("expected.jsarray"))

      override def writes(tuple: (A, B)): JsValue = JsArray(Seq(aFormat.writes(tuple._1), bFormat.writes(tuple._2)))

      private[this] def readsUnsafe(js: JsValue): JsResult[(A, B)] = {
        val arr = js.asInstanceOf[JsArray]
        aFormat.reads(arr(0)).flatMap(a => bFormat.reads(arr(1)).map(b => (a, b)))
      }
    }

  val base64Format: Format[Array[Byte]] = new Format[Array[Byte]] {
    override def reads(js: JsValue): JsResult[Array[Byte]] =
      Try(JsSuccess(Base64.getDecoder.decode(js.asInstanceOf[JsString].value)))
        .getOrElse(JsError(s"expected.base64string"))

    override def writes(v: Array[Byte]): JsValue = JsString(Base64.getEncoder.encodeToString(v))
  }

  implicit val charFormat: Format[Char] = stringFormat("char") { case s if s.length == 1 => s.charAt(0) }
  implicit val missingReqFieldsFormat: Format[MissingRequiredFields] = Json.format
  implicit val nestedStructsFormat: Format[NestedStructs] = Json.format
  implicit val anyValsFormat: Format[AnyVals] = {
    implicit val v1: Format[ByteVal] = implicitly[Format[Byte]].inmap(ByteVal.apply, _.a)
    implicit val v2: Format[ShortVal] = implicitly[Format[Short]].inmap(ShortVal.apply, _.a)
    implicit val v3: Format[IntVal] = implicitly[Format[Int]].inmap(IntVal.apply, _.a)
    implicit val v4: Format[LongVal] = implicitly[Format[Long]].inmap(LongVal.apply, _.a)
    implicit val v5: Format[BooleanVal] = implicitly[Format[Boolean]].inmap(BooleanVal.apply, _.a)
    implicit val v6: Format[CharVal] = charFormat.inmap(CharVal.apply, _.a)
    implicit val v7: Format[DoubleVal] = implicitly[Format[Double]].inmap(DoubleVal.apply, _.a)
    implicit val v8: Format[FloatVal] = implicitly[Format[Float]].inmap(FloatVal.apply, _.a)
    Json.format
  }
  implicit val bitSetFormat: Format[BitSet] = Format(
    Reads(js => JsSuccess(BitSet.fromBitMaskNoCopy(BitMask.toBitMask(js.as[Array[Int]], Int.MaxValue /* WARNING: It is an unsafe option for open systems */)))),
    Writes((es: BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  implicit val mutableBitSetFormat: Format[mutable.BitSet] = Format(
    Reads(js => JsSuccess(mutable.BitSet.fromBitMaskNoCopy(BitMask.toBitMask(js.as[Array[Int]], Int.MaxValue /* WARNING: It is an unsafe option for open systems */)))),
    Writes((es: mutable.BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  implicit val primitivesFormat: Format[Primitives] = Json.format
  implicit val extractFieldsFormat: Format[ExtractFields] = Json.format
  val adtFormat: Format[ADTBase] = {
    implicit lazy val v1: Format[X] = Json.format
    implicit lazy val v2: Format[Y] = Json.format
    implicit lazy val v3: Format[Z] = Json.format
    implicit lazy val v4: Format[ADTBase] = Json.format
    v4
  }
  val geoJSONFormat: Format[GeoJSON.GeoJSON] = {
    implicit val v1: Format[GeoJSON.Point] =
      (__ \ "coordinates").format[(Double, Double)].inmap(GeoJSON.Point.apply, _.coordinates)
    implicit lazy val v2: Format[GeoJSON.MultiPoint] = Json.format
    implicit lazy val v3: Format[GeoJSON.LineString] = Json.format
    implicit lazy val v4: Format[GeoJSON.MultiLineString] = Json.format
    implicit lazy val v5: Format[GeoJSON.Polygon] = Json.format
    implicit lazy val v6: Format[GeoJSON.MultiPolygon] = Json.format
    implicit lazy val v7: Format[GeoJSON.SimpleGeometry] = Json.format
    implicit lazy val v8: Format[GeoJSON.GeometryCollection] = Json.format
    implicit lazy val v9: Format[GeoJSON.Geometry] = Json.format
    implicit lazy val v10: Format[GeoJSON.Feature] = Json.format
    implicit lazy val v11: Format[GeoJSON.SimpleGeoJSON] = Json.format
    implicit lazy val v12: Format[GeoJSON.FeatureCollection] = Json.format
    implicit lazy val v13: Format[GeoJSON.GeoJSON] = Json.format
    v13
  }
  implicit val gitHubActionsAPIFormat: Format[GitHubActionsAPI.Response] = {
    implicit val v1: Format[Boolean] = stringFormat[Boolean]("boolean") { s =>
      "true" == s || "false" != s && sys.error("")
    }
    implicit val v2: Format[GitHubActionsAPI.Artifact] = Json.format
    Json.format
  }
  implicit val googleMapsAPIFormat: Format[GoogleMapsAPI.DistanceMatrix] = {
    implicit val v1: Format[GoogleMapsAPI.Value] = Json.format
    implicit val v2: Format[GoogleMapsAPI.Elements] = Json.format
    implicit val v3: Format[GoogleMapsAPI.Rows] = Json.format
    Json.format
  }
  implicit val openRTBBidRequestFormat: Format[OpenRTB.BidRequest] = {
    implicit val v1: Format[OpenRTB.Segment] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        name <- (__ \ "name").readNullable[String]
        value <- (__ \ "value").readNullable[String]
      } yield OpenRTB.Segment(id, name, value)
    }, (x: OpenRTB.Segment) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "name" -> Json.toJson(x.name),
        "value" -> Json.toJson(x.value)
      )
    })
    implicit val v2: Format[OpenRTB.Format] = Format({
      for {
        w <- (__ \ "w").readNullable[Int]
        h <- (__ \ "h").readNullable[Int]
        wratio <- (__ \ "wratio").readNullable[Int]
        hratio <- (__ \ "hratio").readNullable[Int]
        wmin <- (__ \ "wmin").readNullable[Int]
      } yield OpenRTB.Format(w, h, wratio, hratio, wmin)
    }, (x: OpenRTB.Format) => {
      toJsObject(
        "w" -> Json.toJson(x.w),
        "h" -> Json.toJson(x.h),
        "wratio" -> Json.toJson(x.wratio),
        "hratio" -> Json.toJson(x.hratio),
        "wmin" -> Json.toJson(x.wmin)
      )
    })
    implicit val v3: Format[OpenRTB.Deal] = Format({
      for {
        id <- (__ \ "id").read[String]
        bidfloor <- (__ \ "bidfloor").readWithDefault[Double](0.0)
        bidfloorcur <- (__ \ "bidfloorcur").readWithDefault[String]("USD")
        at <- (__ \ "at").readNullable[Int]
        wseat <- (__ \ "wseat").readWithDefault[List[String]](Nil)
        wadomain <- (__ \ "wadomain").readWithDefault[List[String]](Nil)
      } yield OpenRTB.Deal(id, bidfloor, bidfloorcur, at, wseat, wadomain)
    }, (x: OpenRTB.Deal) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "bidfloor" -> toJson(x.bidfloor, 0.0),
        "bidfloorcur" -> toJson(x.bidfloorcur, "USD"),
        "at" -> Json.toJson(x.at),
        "wseat" -> Json.toJson(x.wseat),
        "wadomain" -> Json.toJson(x.wadomain)
      )
    })
    implicit val v4: Format[OpenRTB.Metric] = Format({
      for {
        type_ <- (__ \ "type").read[String]
        value <- (__ \ "value").read[Double]
        vendor <- (__ \ "vendor").readNullable[String]
      } yield OpenRTB.Metric(type_, value, vendor)
    }, (x: OpenRTB.Metric) => {
      toJsObject(
        "type" -> Json.toJson(x.`type`),
        "value" -> Json.toJson(x.value),
        "vendor" -> Json.toJson(x.vendor)
      )
    })
    implicit val v5: Format[OpenRTB.Banner] = Format({
      for {
        format <- (__ \ "format").readNullable[OpenRTB.Format]
        w <- (__ \ "w").readNullable[Int]
        h <- (__ \ "h").readNullable[Int]
        wmax <- (__ \ "wmax").readNullable[Int]
        hmax <- (__ \ "hmax").readNullable[Int]
        wmin <- (__ \ "wmin").readNullable[Int]
        hmin <- (__ \ "hmin").readNullable[Int]
        btype <- (__ \ "btype").readWithDefault[List[Int]](Nil)
        battr <- (__ \ "battr").readWithDefault[List[Int]](Nil)
        pos <- (__ \ "pos").readNullable[Int]
        mimes <- (__ \ "mimes").readWithDefault[List[String]](Nil)
        topframe <- (__ \ "topframe").readNullable[Int]
        expdir <- (__ \ "expdir").readWithDefault[List[Int]](Nil)
        api <- (__ \ "api").readWithDefault[List[Int]](Nil)
        id <- (__ \ "id").readNullable[String]
        vcm <- (__ \ "vcm").readNullable[Int]
      } yield OpenRTB.Banner(format, w, h, wmax, hmax, wmin, hmin, btype, battr, pos, mimes, topframe, expdir, api, id,
        vcm)
    }, (x: OpenRTB.Banner) => {
      toJsObject(
        "format" -> Json.toJson(x.format),
        "w" -> Json.toJson(x.w),
        "h" -> Json.toJson(x.h),
        "wmax" -> Json.toJson(x.wmax),
        "hmax" -> Json.toJson(x.hmax),
        "wmin" -> Json.toJson(x.wmin),
        "hmin" -> Json.toJson(x.hmin),
        "btype" -> Json.toJson(x.btype),
        "battr" -> Json.toJson(x.battr),
        "pos" -> Json.toJson(x.pos),
        "mimes" -> Json.toJson(x.mimes),
        "topframe" -> Json.toJson(x.topframe),
        "expdir" -> Json.toJson(x.expdir),
        "api" -> Json.toJson(x.api),
        "id" -> Json.toJson(x.id),
        "vcm" -> Json.toJson(x.vcm),
      )
    })
    implicit val v6: Format[OpenRTB.Audio] = Format({
      for {
        mimes <- (__ \ "mimes").readWithDefault[List[String]](Nil)
        minduration <- (__ \ "minduration").readNullable[Int]
        maxduration <- (__ \ "maxduration").readNullable[Int]
        protocols <- (__ \ "protocols").readWithDefault[List[Int]](Nil)
        startdelay <- (__ \ "startdelay").readNullable[Int]
        sequence <- (__ \ "sequence").readNullable[Int]
        battr <- (__ \ "battr").readWithDefault[List[Int]](Nil)
        maxextended <- (__ \ "maxextended").readNullable[Int]
        minbitrate <- (__ \ "minbitrate").readNullable[Int]
        maxbitrate <- (__ \ "maxbitrate").readNullable[Int]
        delivery <- (__ \ "delivery").readWithDefault[List[Int]](Nil)
        companionad <- (__ \ "companionad").readWithDefault[List[OpenRTB.Banner]](Nil)
        api <- (__ \ "api").readWithDefault[List[Int]](Nil)
        companiontype <- (__ \ "companiontype").readWithDefault[List[Int]](Nil)
        maxseq <- (__ \ "maxseq").readNullable[Int]
        feed <- (__ \ "feed").readNullable[Int]
        stitched <- (__ \ "stitched").readNullable[Int]
        nvol <- (__ \ "nvol").readNullable[Int]
      } yield OpenRTB.Audio(mimes, minduration, maxduration, protocols, startdelay, sequence, battr, maxextended,
        minbitrate, maxbitrate, delivery, companionad, api, companiontype, maxseq, feed, stitched, nvol)
    }, (x: OpenRTB.Audio) => {
      toJsObject(
        "mimes" -> Json.toJson(x.mimes),
        "minduration" -> Json.toJson(x.minduration),
        "maxduration" -> Json.toJson(x.maxduration),
        "protocols" -> Json.toJson(x.protocols),
        "sequence" -> Json.toJson(x.sequence),
        "battr" -> Json.toJson(x.battr),
        "maxextended" -> Json.toJson(x.maxextended),
        "minbitrate" -> Json.toJson(x.minbitrate),
        "maxbitrate" -> Json.toJson(x.maxbitrate),
        "delivery" -> Json.toJson(x.delivery),
        "companionad" -> Json.toJson(x.companionad),
        "api" -> Json.toJson(x.api),
        "companiontype" -> Json.toJson(x.companiontype),
        "maxseq" -> Json.toJson(x.maxseq),
        "feed" -> Json.toJson(x.feed),
        "stitched" -> Json.toJson(x.stitched),
        "nvol" -> Json.toJson(x.nvol)
      )
    })
    implicit val v7: Format[OpenRTB.Video] = Format({
      for {
        mimes <- (__ \ "mimes").readWithDefault[List[String]](Nil)
        minduration <- (__ \ "minduration").readNullable[Int]
        maxduration <- (__ \ "maxduration").readNullable[Int]
        protocols <- (__ \ "protocols").readWithDefault[List[Int]](Nil)
        protocol <- (__ \ "protocol").readNullable[Int]
        w <- (__ \ "w").readNullable[Int]
        h <- (__ \ "h").readNullable[Int]
        startdelay <- (__ \ "startdelay").readNullable[Int]
        placement <- (__ \ "placement").readNullable[Int]
        linearity <- (__ \ "linearity").readNullable[Int]
        skip <- (__ \ "skip").readNullable[Int]
        skipmin <- (__ \ "skipmin").readWithDefault[Int](0)
        skipafter <- (__ \ "skipafter").readWithDefault[Int](0)
        sequence <- (__ \ "sequence").readNullable[Int]
        battr <- (__ \ "battr").readWithDefault[List[Int]](Nil)
        maxextended <- (__ \ "maxextended").readNullable[Int]
        minbitrate <- (__ \ "minbitrate").readNullable[Int]
        maxbitrate <- (__ \ "maxbitrate").readNullable[Int]
        boxingallowed <- (__ \ "boxingallowed").readWithDefault[Int](1)
        playbackmethod <- (__ \ "playbackmethod").readWithDefault[List[Int]](Nil)
        playbackend <- (__ \ "playbackend").readNullable[Int]
        delivery <- (__ \ "delivery").readWithDefault[List[Int]](Nil)
        pos <- (__ \ "pos").readNullable[Int]
        companionad <- (__ \ "companionad").readWithDefault[List[OpenRTB.Banner]](Nil)
        api <- (__ \ "api").readWithDefault[List[Int]](Nil)
        companiontype <- (__ \ "companiontype").readWithDefault[List[Int]](Nil)
      } yield OpenRTB.Video(mimes, minduration, maxduration, protocols, protocol, w, h, startdelay, placement,
        linearity, skip, skipmin, skipafter, sequence, battr, maxextended, minbitrate, maxbitrate, boxingallowed,
        playbackmethod, playbackend, delivery, pos, companionad, api, companiontype)
    }, (x: OpenRTB.Video) => {
      toJsObject(
        "mimes" -> Json.toJson(x.mimes),
        "minduration" -> Json.toJson(x.minduration),
        "maxduration" -> Json.toJson(x.maxduration),
        "protocols" -> Json.toJson(x.protocols),
        "protocol" -> Json.toJson(x.protocol),
        "w" -> Json.toJson(x.w),
        "h" -> Json.toJson(x.h),
        "startdelay" -> Json.toJson(x.startdelay),
        "placement" -> Json.toJson(x.placement),
        "linearity" -> Json.toJson(x.linearity),
        "skip" -> Json.toJson(x.skip),
        "skipmin" -> Json.toJson(x.skipmin),
        "skipafter" -> Json.toJson(x.skipafter),
        "sequence" -> Json.toJson(x.sequence),
        "battr" -> Json.toJson(x.battr),
        "maxextended" -> Json.toJson(x.maxextended),
        "minbitrate" -> Json.toJson(x.minbitrate),
        "maxbitrate" -> Json.toJson(x.maxbitrate),
        "boxingallowed" -> Json.toJson(x.boxingallowed),
        "playbackmethod" -> Json.toJson(x.playbackmethod),
        "playbackend" -> Json.toJson(x.playbackend),
        "delivery" -> Json.toJson(x.delivery),
        "pos" -> Json.toJson(x.pos),
        "companionad" -> Json.toJson(x.companionad),
        "api" -> Json.toJson(x.api),
        "companiontype" -> Json.toJson(x.companiontype)
      )
    })
    implicit val v8: Format[OpenRTB.Native] = Format({
      for {
        request <- (__ \ "request").read[String]
        ver <- (__ \ "ver").readNullable[String]
        api <- (__ \ "api").readWithDefault[List[Int]](Nil)
        battr <- (__ \ "battr").readWithDefault[List[Int]](Nil)
      } yield OpenRTB.Native(request, ver, api, battr)
    }, (x: OpenRTB.Native) => {
      toJsObject(
        "request" -> Json.toJson(x.request),
        "ver" -> Json.toJson(x.ver),
        "api" -> Json.toJson(x.api),
        "battr" -> Json.toJson(x.battr)
      )
    })
    implicit val v9: Format[OpenRTB.Pmp] = Format({
      for {
        private_auction <- (__ \ "private_auction").readWithDefault[Int](0)
        deals <- (__ \ "deals").readWithDefault[List[OpenRTB.Deal]](Nil)
      } yield OpenRTB.Pmp(private_auction, deals)
    }, (x: OpenRTB.Pmp) => {
      toJsObject(
        "private_auction" -> toJson(x.private_auction, 0),
        "deals" -> Json.toJson(x.deals)
      )
    })
    implicit val v10: Format[OpenRTB.Producer] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        name <- (__ \ "name").readNullable[String]
        cat <- (__ \ "cat").readWithDefault[List[String]](Nil)
        domain <- (__ \ "domain").readNullable[String]
      } yield OpenRTB.Producer(id, name, cat, domain)
    }, (x: OpenRTB.Producer) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "name" -> Json.toJson(x.name),
        "cat" -> Json.toJson(x.cat),
        "domain" -> Json.toJson(x.domain)
      )
    })
    implicit val v11: Format[OpenRTB.Data] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        name <- (__ \ "name").readNullable[String]
        segment <- (__ \ "segment").readWithDefault[List[OpenRTB.Segment]](Nil)
      } yield OpenRTB.Data(id, name, segment)
    }, (x: OpenRTB.Data) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "name" -> Json.toJson(x.name),
        "segment" -> Json.toJson(x.segment)
      )
    })
    implicit val v12: Format[OpenRTB.Content] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        episode <- (__ \ "episode").readNullable[Int]
        title <- (__ \ "title").readNullable[String]
        series <- (__ \ "series").readNullable[String]
        season <- (__ \ "season").readNullable[String]
        artist <- (__ \ "artist").readNullable[String]
        genre <- (__ \ "genre").readNullable[String]
        album <- (__ \ "album").readNullable[String]
        isrc <- (__ \ "isrc").readNullable[String]
        producer <- (__ \ "producer").readNullable[OpenRTB.Producer]
        url <- (__ \ "url").readNullable[String]
        cat <- (__ \ "cat").readWithDefault[List[String]](Nil)
        prodq <- (__ \ "prodq").readNullable[Int]
        videoquality <- (__ \ "videoquality").readNullable[Int]
        context <- (__ \ "context").readNullable[Int]
        contentrating <- (__ \ "contentrating").readNullable[String]
        userrating <- (__ \ "userrating").readNullable[String]
        qagmediarating <- (__ \ "qagmediarating").readNullable[Int]
        keywords <- (__ \ "keywords").readNullable[String]
        livestream <- (__ \ "livestream").readNullable[Int]
        sourcerelationship <- (__ \ "sourcerelationship").readNullable[Int]
        len <- (__ \ "len").readNullable[Int]
        language <- (__ \ "language").readNullable[String]
        embeddable <- (__ \ "embeddable").readNullable[Int]
        data <- (__ \ "data").readNullable[OpenRTB.Data]
      } yield OpenRTB.Content(id, episode, title, series, season, artist, genre, album, isrc, producer, url, cat, prodq,
        videoquality, context, contentrating, userrating, qagmediarating, keywords, livestream, sourcerelationship, len,
        language, embeddable, data)
    }, (x: OpenRTB.Content) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "episode" -> Json.toJson(x.episode),
        "title" -> Json.toJson(x.title),
        "series" -> Json.toJson(x.series),
        "season" -> Json.toJson(x.season),
        "artist" -> Json.toJson(x.artist),
        "genre" -> Json.toJson(x.genre),
        "album" -> Json.toJson(x.album),
        "isrc" -> Json.toJson(x.isrc),
        "url" -> Json.toJson(x.url),
        "cat" -> Json.toJson(x.cat),
        "prodq" -> Json.toJson(x.prodq),
        "videoquality" -> Json.toJson(x.videoquality),
        "context" -> Json.toJson(x.context),
        "contentrating" -> Json.toJson(x.contentrating),
        "userrating" -> Json.toJson(x.userrating),
        "qagmediarating" -> Json.toJson(x.qagmediarating),
        "keywords" -> Json.toJson(x.keywords),
        "livestream" -> Json.toJson(x.livestream),
        "sourcerelationship" -> Json.toJson(x.sourcerelationship),
        "len" -> Json.toJson(x.len),
        "language" -> Json.toJson(x.language),
        "embeddable" -> Json.toJson(x.embeddable),
        "data" -> Json.toJson(x.data),
      )
    })
    implicit val v13: Format[OpenRTB.Publisher] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        name <- (__ \ "name").readNullable[String]
        cat <- (__ \ "cat").readWithDefault[List[String]](Nil)
        domain <- (__ \ "domain").readNullable[String]
      } yield OpenRTB.Publisher(id, name, cat, domain)
    }, (x: OpenRTB.Publisher) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "name" -> Json.toJson(x.name),
        "cat" -> Json.toJson(x.cat),
        "domain" -> Json.toJson(x.domain)
      )
    })
    implicit val v14: Format[OpenRTB.Geo] = Format({
      for {
        lat <- (__ \ "lat").readNullable[Double]
        lon <- (__ \ "lon").readNullable[Double]
        type_ <- (__ \ "`type`").readNullable[Int]
        accuracy <- (__ \ "accuracy").readNullable[Int]
        lastfix <- (__ \ "lastfix").readNullable[Int]
        ipservice <- (__ \ "ipservice").readNullable[Int]
        country <- (__ \ "country").readNullable[String]
        region <- (__ \ "region").readNullable[String]
        regionfips104 <- (__ \ "regionfips104").readNullable[String]
        metro <- (__ \ "metro").readNullable[String]
        city <- (__ \ "city").readNullable[String]
        zip <- (__ \ "zip").readNullable[String]
        utcoffset <- (__ \ "utcoffset").readNullable[String]
      } yield OpenRTB.Geo(lat, lon, type_, accuracy, lastfix, ipservice, country, region, regionfips104, metro, city,
        zip, utcoffset)
    }, (x: OpenRTB.Geo) => {
      toJsObject(
        "lat" -> Json.toJson(x.lat),
        "lon" -> Json.toJson(x.lon),
        "type" -> Json.toJson(x.`type`),
        "accuracy" -> Json.toJson(x.accuracy),
        "lastfix" -> Json.toJson(x.lastfix),
        "ipservice" -> Json.toJson(x.ipservice),
        "country" -> Json.toJson(x.country),
        "region" -> Json.toJson(x.region),
        "regionfips104" -> Json.toJson(x.regionfips104),
        "metro" -> Json.toJson(x.metro),
        "city" -> Json.toJson(x.city),
        "zip" -> Json.toJson(x.zip),
        "utcoffset" -> Json.toJson(x.utcoffset)
      )
    })
    implicit val v15: Format[OpenRTB.Imp] = Format({
      for {
        id <- (__ \ "id").read[String]
        metric <- (__ \ "metric").readWithDefault[List[OpenRTB.Metric]](Nil)
        banner <- (__ \ "banner").readNullable[OpenRTB.Banner]
        video <- (__ \ "video").readNullable[OpenRTB.Video]
        audio <- (__ \ "audio").readNullable[OpenRTB.Audio]
        native <- (__ \ "native").readNullable[OpenRTB.Native]
        pmp <- (__ \ "pmp").readNullable[OpenRTB.Pmp]
        displaymanager <- (__ \ "displaymanager").readNullable[String]
        displaymanagerver <- (__ \ "displaymanagerver").readNullable[String]
        instl <- (__ \ "instl").readWithDefault[Int](0)
        tagid <- (__ \ "tagid").readNullable[String]
        bidfloor <- (__ \ "bidfloor").readWithDefault[Double](0.0)
        bidfloorcur <- (__ \ "bidfloorcur").readWithDefault[String]("USD")
        clickbrowser <- (__ \ "clickbrowser").readNullable[Int]
        secure <- (__ \ "secure").readWithDefault[Int](0)
        iframebuster <- (__ \ "iframebuster").readWithDefault[List[String]](Nil)
        exp <- (__ \ "exp").readNullable[Int]
      } yield OpenRTB.Imp(id, metric, banner, video, audio, native, pmp, displaymanager, displaymanagerver, instl,
        tagid, bidfloor, bidfloorcur, clickbrowser, secure, iframebuster, exp)
    }, (x: OpenRTB.Imp) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "metric" -> Json.toJson(x.metric),
        "banner" -> Json.toJson(x.banner),
        "video" -> Json.toJson(x.video),
        "audio" -> Json.toJson(x.audio),
        "native" -> Json.toJson(x.native),
        "pmp" -> Json.toJson(x.pmp),
        "displaymanager" -> Json.toJson(x.displaymanager),
        "displaymanagerver" -> Json.toJson(x.displaymanagerver),
        "instl" -> toJson(x.instl, 0),
        "tagid" -> Json.toJson(x.tagid),
        "bidfloor" -> toJson(x.bidfloor, 0.0),
        "bidfloorcur" -> toJson(x.bidfloorcur, "USD"),
        "clickbrowser" -> Json.toJson(x.clickbrowser),
        "secure" -> toJson(x.secure, 0),
        "iframebuster" -> Json.toJson(x.iframebuster),
        "exp" -> Json.toJson(x.exp)
      )
    })
    implicit val v16: Format[OpenRTB.Site] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        name <- (__ \ "name").readNullable[String]
        domain <- (__ \ "domain").readNullable[String]
        cat <- (__ \ "cat").readWithDefault[List[String]](Nil)
        sectioncat <- (__ \ "sectioncat").readWithDefault[List[String]](Nil)
        pagecat <- (__ \ "pagecat").readWithDefault[List[String]](Nil)
        page <- (__ \ "page").readNullable[String]
        ref <- (__ \ "ref").readNullable[String]
        search <- (__ \ "search").readNullable[String]
        mobile <- (__ \ "mobile").readNullable[Int]
        privacypolicy <- (__ \ "ver").readNullable[Int]
        publisher <- (__ \ "publisher").readNullable[OpenRTB.Publisher]
        content <- (__ \ "content").readNullable[OpenRTB.Content]
        keywords <- (__ \ "keywords").readNullable[String]
      } yield OpenRTB.Site(id, name, domain, cat, sectioncat, pagecat, page, ref, search, mobile, privacypolicy,
        publisher, content, keywords)
    }, (x: OpenRTB.Site) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "name" -> Json.toJson(x.name),
        "domain" -> Json.toJson(x.domain),
        "cat" -> Json.toJson(x.cat),
        "sectioncat" -> Json.toJson(x.sectioncat),
        "pagecat" -> Json.toJson(x.pagecat),
        "page" -> Json.toJson(x.page),
        "ref" -> Json.toJson(x.ref),
        "search" -> Json.toJson(x.search),
        "mobile" -> Json.toJson(x.mobile),
        "privacypolicy" -> Json.toJson(x.privacypolicy),
        "publisher" -> Json.toJson(x.publisher),
        "content" -> Json.toJson(x.content),
        "keywords" -> Json.toJson(x.keywords)
      )
    })
    implicit val v17: Format[OpenRTB.App] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        name <- (__ \ "name").readNullable[String]
        bundle <- (__ \ "bundle").readNullable[String]
        domain <- (__ \ "domain").readNullable[String]
        storeurl <- (__ \ "storeurl").readNullable[String]
        cat <- (__ \ "cat").readWithDefault[List[String]](Nil)
        sectioncat <- (__ \ "sectioncat").readWithDefault[List[String]](Nil)
        pagecat <- (__ \ "pagecat").readWithDefault[List[String]](Nil)
        ver <- (__ \ "ver").readNullable[String]
        privacypolicy <- (__ \ "privacypolicy").readNullable[Int]
        paid <- (__ \ "paid").readNullable[Int]
        publisher <- (__ \ "publisher").readNullable[OpenRTB.Publisher]
        content <- (__ \ "content").readNullable[OpenRTB.Content]
        keywords <- (__ \ "keywords").readNullable[String]
      } yield OpenRTB.App(id, name, bundle, domain, storeurl, cat, sectioncat, pagecat, ver, privacypolicy, paid,
        publisher, content, keywords)
    }, (x: OpenRTB.App) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "name" -> Json.toJson(x.name),
        "bundle" -> Json.toJson(x.bundle),
        "domain" -> Json.toJson(x.domain),
        "storeurl" -> Json.toJson(x.storeurl),
        "cat" -> Json.toJson(x.cat),
        "sectioncat" -> Json.toJson(x.sectioncat),
        "pagecat" -> Json.toJson(x.pagecat),
        "ver" -> Json.toJson(x.ver),
        "privacypolicy" -> Json.toJson(x.privacypolicy),
        "paid" -> Json.toJson(x.paid),
        "publisher" -> Json.toJson(x.publisher),
        "content" -> Json.toJson(x.content),
        "keywords" -> Json.toJson(x.keywords)
      )
    })
    implicit val v18: Format[OpenRTB.Device] = Format({
      for {
        ua <- (__ \ "ua").readNullable[String]
        geo <- (__ \ "geo").readNullable[OpenRTB.Geo]
        dnt <- (__ \ "dnt").readNullable[Int]
        lmt <- (__ \ "lmt").readNullable[Int]
        ip <- (__ \ "ip").readNullable[String]
        devicetype <- (__ \ "devicetype").readNullable[Int]
        make <- (__ \ "make").readNullable[String]
        model <- (__ \ "model").readNullable[String]
        os <- (__ \ "os").readNullable[String]
        osv <- (__ \ "osv").readNullable[String]
        hwv <- (__ \ "hwv").readNullable[String]
        h <- (__ \ "h").readNullable[Int]
        w <- (__ \ "w").readNullable[Int]
        ppi <- (__ \ "ppi").readNullable[Int]
        pxratio <- (__ \ "pxratio").readNullable[Double]
        js <- (__ \ "js").readNullable[Int]
        geofetch <- (__ \ "geofetch").readNullable[Int]
        flashver <- (__ \ "flashver").readNullable[String]
        language <- (__ \ "language").readNullable[String]
        carrier <- (__ \ "carrier").readNullable[String]
        mccmnc <- (__ \ "mccmnc").readNullable[String]
        connectiontype <- (__ \ "connectiontype").readNullable[Int]
        ifa <- (__ \ "ifa").readNullable[String]
        didsha1 <- (__ \ "didsha1").readNullable[String]
        didmd5 <- (__ \ "didmd5").readNullable[String]
        dpidsha1 <- (__ \ "dpidsha1").readNullable[String]
        dpidmd5 <- (__ \ "dpidmd5").readNullable[String]
        macsha1 <- (__ \ "macsha1").readNullable[String]
        macmd5 <- (__ \ "macmd5").readNullable[String]
      } yield OpenRTB.Device(ua, geo, dnt, lmt, ip, devicetype, make, model, os, osv, hwv, h, w, ppi, pxratio, js,
        geofetch, flashver, language, carrier, mccmnc, connectiontype, ifa, didsha1, didmd5, dpidsha1, dpidmd5, macsha1,
        macmd5)
    }, (x: OpenRTB.Device) => {
      toJsObject(
        "ua" -> Json.toJson(x.ua),
        "geo" -> Json.toJson(x.geo),
        "dnt" -> Json.toJson(x.dnt),
        "lmt" -> Json.toJson(x.lmt),
        "ip" -> Json.toJson(x.ip),
        "devicetype" -> Json.toJson(x.devicetype),
        "make" -> Json.toJson(x.make),
        "model" -> Json.toJson(x.model),
        "os" -> Json.toJson(x.os),
        "osv" -> Json.toJson(x.osv),
        "hwv" -> Json.toJson(x.hwv),
        "h" -> Json.toJson(x.h),
        "w" -> Json.toJson(x.w),
        "ppi" -> Json.toJson(x.ppi),
        "pxratio" -> Json.toJson(x.pxratio),
        "js" -> Json.toJson(x.js),
        "geofetch" -> Json.toJson(x.geofetch),
        "flashver" -> Json.toJson(x.flashver),
        "language" -> Json.toJson(x.language),
        "carrier" -> Json.toJson(x.carrier),
        "mccmnc" -> Json.toJson(x.mccmnc),
        "connectiontype" -> Json.toJson(x.connectiontype),
        "ifa" -> Json.toJson(x.ifa),
        "didsha1" -> Json.toJson(x.didsha1),
        "didmd5" -> Json.toJson(x.didmd5),
        "dpidsha1" -> Json.toJson(x.dpidsha1),
        "dpidmd5" -> Json.toJson(x.dpidmd5),
        "macsha1" -> Json.toJson(x.macsha1),
        "macmd5" -> Json.toJson(x.macmd5)
      )
    })
    implicit val v19: Format[OpenRTB.User] = Format({
      for {
        id <- (__ \ "id").readNullable[String]
        buyeruid <- (__ \ "buyeruid").readNullable[String]
        yob <- (__ \ "yob").readNullable[Int]
        gender <- (__ \ "gender").readNullable[String]
        keywords <- (__ \ "keywords").readNullable[String]
        customdata <- (__ \ "customdata").readNullable[String]
        geo <- (__ \ "geo").readNullable[OpenRTB.Geo]
        data <- (__ \ "data").readNullable[OpenRTB.Data]
      } yield OpenRTB.User(id, buyeruid, yob, gender, keywords, customdata, geo, data)
    }, (x: OpenRTB.User) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "buyeruid" -> Json.toJson(x.buyeruid),
        "yob" -> Json.toJson(x.yob),
        "gender" -> Json.toJson(x.gender),
        "keywords" -> Json.toJson(x.keywords),
        "customdata" -> Json.toJson(x.customdata),
        "geo" -> Json.toJson(x.geo),
        "data" -> Json.toJson(x.data)
      )
    })
    implicit val v20: Format[OpenRTB.Source] = Format({
      for {
        fd <- (__ \ "fd").readNullable[Int]
        tid <- (__ \ "tid").readNullable[String]
        pchain <- (__ \ "pchain").readNullable[String]
      } yield OpenRTB.Source(fd, tid, pchain)
    }, (x: OpenRTB.Source) => {
      toJsObject(
        "fd" -> Json.toJson(x.fd),
        "tid" -> Json.toJson(x.tid),
        "pchain" -> Json.toJson(x.pchain)
      )
    })
    implicit val v21: Format[OpenRTB.Reqs] = Json.format
    Format({
      for {
        id <- (__ \ "id").read[String]
        imp <- (__ \ "imp").readWithDefault[List[OpenRTB.Imp]](Nil)
        site <- (__ \ "site").readNullable[OpenRTB.Site]
        app <- (__ \ "app").readNullable[OpenRTB.App]
        device <- (__ \ "device").readNullable[OpenRTB.Device]
        user <- (__ \ "user").readNullable[OpenRTB.User]
        test <- (__ \ "test").readWithDefault[Int](0)
        at <- (__ \ "at").readWithDefault[Int](2)
        tmax <- (__ \ "tmax").readNullable[Int]
        wset <- (__ \ "wset").readWithDefault[List[String]](Nil)
        bset <- (__ \ "bset").readWithDefault[List[String]](Nil)
        allimps <- (__ \ "allimps").readWithDefault[Int](0)
        cur <- (__ \ "cur").readWithDefault[List[String]](Nil)
        wlang <- (__ \ "wlang").readWithDefault[List[String]](Nil)
        bcat <- (__ \ "bcat").readWithDefault[List[String]](Nil)
        badv <- (__ \ "badv").readWithDefault[List[String]](Nil)
        bapp <- (__ \ "bapp").readWithDefault[List[String]](Nil)
        source <- (__ \ "source").readNullable[OpenRTB.Source]
        reqs <- (__ \ "reqs").readNullable[OpenRTB.Reqs]
      } yield OpenRTB.BidRequest(id, imp, site, app, device, user, test, at, tmax, wset, bset, allimps, cur, wlang,
        bcat, badv, bapp, source, reqs)
    }, (x: OpenRTB.BidRequest) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "imp" -> Json.toJson(x.imp),
        "site" -> Json.toJson(x.site),
        "app" -> Json.toJson(x.app),
        "device" -> Json.toJson(x.device),
        "user" -> Json.toJson(x.user),
        "test" -> toJson(x.test, 0),
        "at" -> toJson(x.at, 2),
        "tmax" -> Json.toJson(x.tmax),
        "wset" -> Json.toJson(x.wset),
        "bset" -> Json.toJson(x.bset),
        "allimps" -> toJson(x.allimps, 0),
        "cur" -> Json.toJson(x.cur),
        "wlang" -> Json.toJson(x.wlang),
        "bcat" -> Json.toJson(x.bcat),
        "badv" -> Json.toJson(x.badv),
        "bapp" -> Json.toJson(x.bapp),
        "source" -> Json.toJson(x.source),
        "reqs" -> Json.toJson(x.reqs)
      )
    })
  }
  implicit val twitterFormat: Format[TwitterAPI.Tweet] = {
    implicit val v1: Format[TwitterAPI.Urls] = Format({
      for {
        url <- (__ \ "url").read[String]
        expanded_url <- (__ \ "expanded_url").read[String]
        display_url <- (__ \ "display_url").read[String]
        indices <- (__ \ "indices").readWithDefault[Seq[Int]](Nil)
      } yield TwitterAPI.Urls(url, expanded_url, display_url, indices)
    }, (x: TwitterAPI.Urls) => {
      toJsObject(
        "url" -> Json.toJson(x.url),
        "expanded_url" -> Json.toJson(x.expanded_url),
        "display_url" -> Json.toJson(x.display_url),
        "indices" -> Json.toJson(x.indices),
      )
    })
    implicit val v2: Format[TwitterAPI.Url] = Format({
      (__ \ "urls").readWithDefault[Seq[TwitterAPI.Urls]](Nil).map(TwitterAPI.Url.apply)
    }, (x: TwitterAPI.Url) => toJsObject("urls" -> Json.toJson(x.urls)))
    implicit val v3: Format[TwitterAPI.UserEntities] = Format({
      for {
        url <- (__ \ "url").read[TwitterAPI.Url]
        description <- (__ \ "description").read[TwitterAPI.Url]
      } yield TwitterAPI.UserEntities(url, description)
    }, (x: TwitterAPI.UserEntities) => {
      toJsObject(
        "url" -> Json.toJson(x.url),
        "description" -> Json.toJson(x.description),
      )
    })
    implicit val v4: Format[TwitterAPI.UserMentions] = Format({
      for {
        screen_name <- (__ \ "screen_name").read[String]
        name <- (__ \ "name").read[String]
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        indices <- (__ \ "indices").readWithDefault[Seq[Int]](Nil)
      } yield TwitterAPI.UserMentions(screen_name, name, id, id_str, indices)
    }, (x: TwitterAPI.UserMentions) => {
      toJsObject(
        "screen_name" -> Json.toJson(x.screen_name),
        "name" -> Json.toJson(x.name),
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "indices" -> Json.toJson(x.indices),
      )
    })
    implicit val v5: Format[TwitterAPI.Entities] = Format({
      for {
        hashtags <- (__ \ "hashtags").readWithDefault[Seq[String]](Nil)
        symbols <- (__ \ "symbols").readWithDefault[Seq[String]](Nil)
        user_mentions <- (__ \ "user_mentions").readWithDefault[Seq[TwitterAPI.UserMentions]](Nil)
        urls <- (__ \ "urls").readWithDefault[Seq[TwitterAPI.Urls]](Nil)
      } yield TwitterAPI.Entities(hashtags, symbols, user_mentions, urls)
    }, (x: TwitterAPI.Entities) => {
      toJsObject(
        "hashtags" -> Json.toJson(x.hashtags),
        "symbols" -> Json.toJson(x.symbols),
        "user_mentions" -> Json.toJson(x.user_mentions),
        "urls" -> Json.toJson(x.urls),
      )
    })
    implicit val v6: Format[TwitterAPI.User] = Format({
      for {
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        name <- (__ \ "name").read[String]
        screen_name <- (__ \ "screen_name").read[String]
        location <- (__ \ "location").read[String]
        description <- (__ \ "description").read[String]
        url <- (__ \ "url").read[String]
        entities <- (__ \ "entities").read[TwitterAPI.UserEntities]
        protected_ <- (__ \ "protected").read[Boolean]
        followers_count <- (__ \ "followers_count").read[Int]
        friends_count <- (__ \ "friends_count").read[Int]
        listed_count <- (__ \ "listed_count").read[Int]
        created_at <- (__ \ "created_at").read[String]
        favourites_count <- (__ \ "favourites_count").read[Int]
        utc_offset <- (__ \ "utc_offset").read[Int]
        time_zone <- (__ \ "time_zone").read[String]
        geo_enabled <- (__ \ "geo_enabled").read[Boolean]
        verified <- (__ \ "verified").read[Boolean]
        statuses_count <- (__ \ "statuses_count").read[Int]
        lang <- (__ \ "lang").read[String]
        contributors_enabled <- (__ \ "contributors_enabled").read[Boolean]
        is_translator <- (__ \ "is_translator").read[Boolean]
        is_translation_enabled <- (__ \ "is_translation_enabled").read[Boolean]
        profile_background_color <- (__ \ "profile_background_color").read[String]
        profile_background_image_url <- (__ \ "profile_background_image_url").read[String]
        profile_background_image_url_https <- (__ \ "profile_background_image_url_https").read[String]
        profile_background_tile <- (__ \ "profile_background_tile").read[Boolean]
        profile_image_url <- (__ \ "profile_image_url").read[String]
        profile_image_url_https <- (__ \ "profile_image_url_https").read[String]
        profile_banner_url <- (__ \ "profile_banner_url").read[String]
        profile_link_color <- (__ \ "profile_link_color").read[String]
        profile_sidebar_border_color <- (__ \ "profile_sidebar_border_color").read[String]
        profile_sidebar_fill_color <- (__ \ "profile_sidebar_fill_color").read[String]
        profile_text_color <- (__ \ "profile_text_color").read[String]
        profile_use_background_image <- (__ \ "profile_use_background_image").read[Boolean]
        has_extended_profile <- (__ \ "has_extended_profile").read[Boolean]
        default_profile <- (__ \ "default_profile").read[Boolean]
        default_profile_image <- (__ \ "default_profile_image").read[Boolean]
        following <- (__ \ "following").read[Boolean]
        follow_request_sent <- (__ \ "follow_request_sent").read[Boolean]
        notifications <- (__ \ "notifications").read[Boolean]
        translator_type <- (__ \ "translator_type").read[String]
      } yield TwitterAPI.User(id, id_str, name, screen_name, location, description, url, entities, protected_,
        followers_count, friends_count, listed_count, created_at, favourites_count, utc_offset, time_zone, geo_enabled,
        verified, statuses_count, lang, contributors_enabled, is_translator, is_translation_enabled,
        profile_background_color, profile_background_image_url, profile_background_image_url_https,
        profile_background_tile, profile_image_url, profile_image_url_https, profile_banner_url, profile_link_color,
        profile_sidebar_border_color, profile_sidebar_fill_color, profile_text_color, profile_use_background_image,
        has_extended_profile, default_profile, default_profile_image, following, follow_request_sent, notifications,
        translator_type)
    }, (x: TwitterAPI.User) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "name" -> Json.toJson(x.name),
        "screen_name" -> Json.toJson(x.screen_name),
        "location" -> Json.toJson(x.location),
        "description" -> Json.toJson(x.description),
        "url" -> Json.toJson(x.url),
        "entities" -> Json.toJson(x.entities),
        "protected" -> Json.toJson(x.`protected`),
        "followers_count" -> Json.toJson(x.followers_count),
        "friends_count" -> Json.toJson(x.friends_count),
        "listed_count" -> Json.toJson(x.listed_count),
        "created_at" -> Json.toJson(x.created_at),
        "favourites_count" -> Json.toJson(x.favourites_count),
        "utc_offset" -> Json.toJson(x.utc_offset),
        "time_zone" -> Json.toJson(x.time_zone),
        "geo_enabled" -> Json.toJson(x.geo_enabled),
        "verified" -> Json.toJson(x.verified),
        "statuses_count" -> Json.toJson(x.statuses_count),
        "lang" -> Json.toJson(x.lang),
        "contributors_enabled" -> Json.toJson(x.contributors_enabled),
        "is_translator" -> Json.toJson(x.is_translator),
        "is_translation_enabled" -> Json.toJson(x.is_translation_enabled),
        "profile_background_color" -> Json.toJson(x.profile_background_color),
        "profile_background_image_url" -> Json.toJson(x.profile_background_image_url),
        "profile_background_image_url_https" -> Json.toJson(x.profile_background_image_url_https),
        "profile_background_tile" -> Json.toJson(x.profile_background_tile),
        "profile_image_url" -> Json.toJson(x.profile_image_url),
        "profile_image_url_https" -> Json.toJson(x.profile_image_url_https),
        "profile_banner_url" -> Json.toJson(x.profile_banner_url),
        "profile_link_color" -> Json.toJson(x.profile_link_color),
        "profile_sidebar_border_color" -> Json.toJson(x.profile_sidebar_border_color),
        "profile_sidebar_fill_color" -> Json.toJson(x.profile_sidebar_fill_color),
        "profile_text_color" -> Json.toJson(x.profile_text_color),
        "profile_use_background_image" -> Json.toJson(x.profile_use_background_image),
        "has_extended_profile" -> Json.toJson(x.has_extended_profile),
        "default_profile" -> Json.toJson(x.default_profile),
        "default_profile_image" -> Json.toJson(x.default_profile_image),
        "following" -> Json.toJson(x.following),
        "follow_request_sent" -> Json.toJson(x.follow_request_sent),
        "notifications" -> Json.toJson(x.notifications),
        "translator_type" -> Json.toJson(x.translator_type)
      )
    })
    implicit val v7: Format[TwitterAPI.RetweetedStatus] = Format({
      for {
        created_at <- (__ \ "created_at").read[String]
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        text <- (__ \ "text").read[String]
        truncated <- (__ \ "truncated").read[Boolean]
        entities <- (__ \ "entities").read[TwitterAPI.Entities]
        source <- (__ \ "source").read[String]
        in_reply_to_status_id <- (__ \ "in_reply_to_status_id").readNullableWithDefault[String](None)
        in_reply_to_status_id_str <- (__ \ "in_reply_to_status_id_str").readNullableWithDefault[String](None)
        in_reply_to_user_id <- (__ \ "in_reply_to_user_id").readNullableWithDefault[String](None)
        in_reply_to_user_id_str <- (__ \ "in_reply_to_user_id_str").readNullableWithDefault[String](None)
        in_reply_to_screen_name <- (__ \ "in_reply_to_screen_name").readNullableWithDefault[String](None)
        user <- (__ \ "user").read[TwitterAPI.User]
        geo <- (__ \ "geo").readNullableWithDefault[String](None)
        coordinates <- (__ \ "coordinates").readNullableWithDefault[String](None)
        place <- (__ \ "place").readNullableWithDefault[String](None)
        contributors <- (__ \ "contributors").readNullableWithDefault[String](None)
        is_quote_status <- (__ \ "is_quote_status").read[Boolean]
        retweet_count <- (__ \ "retweet_count").read[Int]
        favorite_count <- (__ \ "favorite_count").read[Int]
        favorited <- (__ \ "favorited").read[Boolean]
        retweeted <- (__ \ "retweeted").read[Boolean]
        possibly_sensitive <- (__ \ "possibly_sensitive").read[Boolean]
        lang <- (__ \ "lang").read[String]
      } yield TwitterAPI.RetweetedStatus(created_at, id, id_str, text, truncated, entities, source,
        in_reply_to_status_id, in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str,
        in_reply_to_screen_name, user, geo, coordinates, place, contributors, is_quote_status, retweet_count,
        favorite_count, favorited, retweeted, possibly_sensitive, lang)
    }, (x: TwitterAPI.RetweetedStatus) => {
      toJsObject(
        "created_at" -> Json.toJson(x.created_at),
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "text" -> Json.toJson(x.text),
        "truncated" -> Json.toJson(x.truncated),
        "entities" -> Json.toJson(x.entities),
        "source" -> Json.toJson(x.source),
        "in_reply_to_status_id" -> Json.toJson(x.in_reply_to_status_id),
        "in_reply_to_status_id_str" -> Json.toJson(x.in_reply_to_status_id_str),
        "in_reply_to_user_id" -> Json.toJson(x.in_reply_to_user_id),
        "in_reply_to_user_id_str" -> Json.toJson(x.in_reply_to_user_id_str),
        "in_reply_to_screen_name" -> Json.toJson(x.in_reply_to_screen_name),
        "user" -> Json.toJson(x.user),
        "coordinates" -> Json.toJson(x.coordinates),
        "place" -> Json.toJson(x.place),
        "contributors" -> Json.toJson(x.contributors),
        "is_quote_status" -> Json.toJson(x.is_quote_status),
        "retweet_count" -> Json.toJson(x.retweet_count),
        "favorite_count" -> Json.toJson(x.favorite_count),
        "favorited" -> Json.toJson(x.favorited),
        "retweeted" -> Json.toJson(x.retweeted),
        "possibly_sensitive" -> Json.toJson(x.possibly_sensitive),
        "lang" -> Json.toJson(x.lang)
      )
    })
    Format({
      for {
        created_at <- (__ \ "created_at").read[String]
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        text <- (__ \ "text").read[String]
        truncated <- (__ \ "truncated").read[Boolean]
        entities <- (__ \ "entities").read[TwitterAPI.Entities]
        source <- (__ \ "source").read[String]
        in_reply_to_status_id <- (__ \ "in_reply_to_status_id").readNullableWithDefault[String](None)
        in_reply_to_status_id_str <- (__ \ "in_reply_to_status_id_str").readNullableWithDefault[String](None)
        in_reply_to_user_id <- (__ \ "in_reply_to_user_id").readNullableWithDefault[String](None)
        in_reply_to_user_id_str <- (__ \ "in_reply_to_user_id_str").readNullableWithDefault[String](None)
        in_reply_to_screen_name <- (__ \ "in_reply_to_screen_name").readNullableWithDefault[String](None)
        user <- (__ \ "user").read[TwitterAPI.User]
        geo <- (__ \ "geo").readNullableWithDefault[String](None)
        coordinates <- (__ \ "coordinates").readNullableWithDefault[String](None)
        place <- (__ \ "place").readNullableWithDefault[String](None)
        contributors <- (__ \ "contributors").readNullableWithDefault[String](None)
        retweeted_status <- (__ \ "retweeted_status").read[TwitterAPI.RetweetedStatus]
        is_quote_status <- (__ \ "is_quote_status").read[Boolean]
        retweet_count <- (__ \ "retweet_count").read[Int]
        favorite_count <- (__ \ "favorite_count").read[Int]
        favorited <- (__ \ "favorited").read[Boolean]
        retweeted <- (__ \ "retweeted").read[Boolean]
        possibly_sensitive <- (__ \ "possibly_sensitive").read[Boolean]
        lang <- (__ \ "lang").read[String]
      } yield TwitterAPI.Tweet(created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
        in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name, user, geo,
        coordinates, place, contributors, retweeted_status, is_quote_status, retweet_count, favorite_count, favorited,
        retweeted, possibly_sensitive, lang)
    }, (x: TwitterAPI.Tweet) => {
      toJsObject(
        "created_at" -> Json.toJson(x.created_at),
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "text" -> Json.toJson(x.text),
        "truncated" -> Json.toJson(x.truncated),
        "entities" -> Json.toJson(x.entities),
        "source" -> Json.toJson(x.source),
        "in_reply_to_status_id" -> Json.toJson(x.in_reply_to_status_id),
        "in_reply_to_status_id_str" -> Json.toJson(x.in_reply_to_status_id_str),
        "in_reply_to_user_id" -> Json.toJson(x.in_reply_to_user_id),
        "in_reply_to_user_id_str" -> Json.toJson(x.in_reply_to_user_id_str),
        "in_reply_to_screen_name" -> Json.toJson(x.in_reply_to_screen_name),
        "user" -> Json.toJson(x.user),
        "coordinates" -> Json.toJson(x.coordinates),
        "place" -> Json.toJson(x.place),
        "contributors" -> Json.toJson(x.contributors),
        "retweeted_status" -> Json.toJson(x.retweeted_status),
        "is_quote_status" -> Json.toJson(x.is_quote_status),
        "retweet_count" -> Json.toJson(x.retweet_count),
        "favorite_count" -> Json.toJson(x.favorite_count),
        "favorited" -> Json.toJson(x.favorited),
        "retweeted" -> Json.toJson(x.retweeted),
        "possibly_sensitive" -> Json.toJson(x.possibly_sensitive),
        "lang" -> Json.toJson(x.lang)
      )
    })
  }
  implicit val enumFormat: Format[SuitEnum.SuitEnum] = Format(Reads.enumNameReads(SuitEnum), Writes.enumNameWrites)
  implicit val enumADTFormat: Format[SuitADT] = stringFormat("suitadt") {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    (s: String) => suite(s)
  }
  implicit val javaEnumFormat: Format[Suit] = stringFormat("suitenum")(Suit.valueOf)
  implicit val durationFormat: Format[Duration] = stringFormat("instant")(Duration.parse)
  implicit lazy val instantFormat: Format[Instant] = stringFormat("instant")(Instant.parse)
  implicit val localDateTimeFormat: Format[LocalDateTime] = stringFormat("localdatetime")(LocalDateTime.parse)
  implicit val localDateFormat: Format[LocalDate] = stringFormat("localdate")(LocalDate.parse)
  implicit val localTimeFormat: Format[LocalTime] = stringFormat("localtime")(LocalTime.parse)
  implicit val monthDayFormat: Format[MonthDay] = stringFormat("monthday")(MonthDay.parse)
  implicit val offsetDateTimeFormat: Format[OffsetDateTime] = stringFormat("offsetdatetime")(OffsetDateTime.parse)
  implicit val offsetTimeFormat: Format[OffsetTime] = stringFormat("offsettime")(OffsetTime.parse)
  implicit val periodFormat: Format[Period] = stringFormat("period")(Period.parse)
  implicit val yearFormat: Format[Year] = stringFormat("year")(Year.parse)
  implicit val yearMonthFormat: Format[YearMonth] = stringFormat("yearmonth")(YearMonth.parse)
  implicit val zoneOffsetFormat: Format[ZoneOffset] = stringFormat("zoneoffset")(ZoneOffset.of)
  implicit val zoneIdFormat: Format[ZoneId] = stringFormat("zoneid")(ZoneId.of)
  implicit val zonedDateTimeFormat: Format[ZonedDateTime] = stringFormat("zoneddatetime")(ZonedDateTime.parse)

  private[this] def toJson[T](x: T, d: T)(implicit tjs: Writes[T]): JsValue =
    if (x == d) JsNull
    else tjs.writes(x)

  private[this] def toJsObject(fields: (String, JsValue)*): JsObject = JsObject(fields.filterNot { case (_, v) =>
    (v eq JsNull) || (v.isInstanceOf[JsArray] && v.asInstanceOf[JsArray].value.isEmpty)
  })
}