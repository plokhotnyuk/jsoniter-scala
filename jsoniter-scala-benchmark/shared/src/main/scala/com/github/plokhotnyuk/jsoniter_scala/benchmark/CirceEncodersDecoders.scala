package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import io.circe.Decoder._
import io.circe.Encoder._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import java.time.Instant
import java.util.Base64
import scala.collection.immutable.{BitSet, IntMap, Map}
import scala.collection.mutable

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true, predictSize = true)
  val prettyPrinter: Printer = Printer.spaces2.copy(dropNullValues = true, reuseWriters = true, predictSize = true)
  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
  implicit val adtC3c: Codec[ADTBase] = {
    implicit val c1: Codec[X] = deriveCodec
    implicit val c2: Codec[Y] = deriveCodec
    implicit val c3: Codec[Z] = deriveCodec
    Codec.from(Decoder.instance(c =>
      c.downField("type").as[String].flatMap {
        case "X" => c.as[X]
        case "Y" => c.as[Y]
        case "Z" => c.as[Z]
      }), Encoder.instance {
      case x: X => x.asJson.mapObject(_.+:("type" -> "X".asJson))
      case y: Y => y.asJson.mapObject(_.+:("type" -> "Y".asJson))
      case z: Z => z.asJson.mapObject(_.+:("type" -> "Z".asJson))
    })
  }
  implicit val anyValsC3c: Codec[AnyVals] = {
    implicit val c1: Codec[ByteVal] = Codec.from(decodeByte.map(ByteVal.apply), encodeByte.contramap(_.a))
    implicit val c2: Codec[ShortVal] = Codec.from(decodeShort.map(ShortVal.apply), encodeShort.contramap(_.a))
    implicit val c3: Codec[IntVal] = Codec.from(decodeInt.map(IntVal.apply), encodeInt.contramap(_.a))
    implicit val c4: Codec[LongVal] = Codec.from(decodeLong.map(LongVal.apply), encodeLong.contramap(_.a))
    implicit val c5: Codec[BooleanVal] = Codec.from(decodeBoolean.map(BooleanVal.apply), encodeBoolean.contramap(_.a))
    implicit val c6: Codec[CharVal] = Codec.from(decodeChar.map(CharVal.apply), encodeChar.contramap(_.a))
    implicit val c7: Codec[DoubleVal] = Codec.from(decodeDouble.map(DoubleVal.apply), encodeDouble.contramap(_.a))
    implicit val c8: Codec[FloatVal] = Codec.from(decodeFloat.map(FloatVal.apply), encodeFloat.contramap(_.a))
    deriveCodec
  }
  val base64C3c: Codec[Array[Byte]] =
    Codec.from(Decoder.decodeString.map[Array[Byte]](Base64.getDecoder.decode),
      Encoder.encodeString.contramap[Array[Byte]](Base64.getEncoder.encodeToString))
  implicit val bidRequestC3c: Codec[OpenRTB.BidRequest] = {
    implicit val c21: Codec[OpenRTB.Segment] =
      Codec.from(deriveDecoder, (x: OpenRTB.Segment) => Util.toJObject(
        ("id", x.id.asJson),
        ("name", x.name.asJson),
        ("value", x.value.asJson)
      ))
    implicit val c20: Codec[OpenRTB.Data] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[Option[String]]
        name <- c.downField("name").as[Option[String]]
        segment <- c.downField("segment").as[Option[List[OpenRTB.Segment]]]
      } yield {
        new OpenRTB.Data(id, name, segment.getOrElse(Nil))
      }, (x: OpenRTB.Data) => Util.toJObject(
        ("id", x.id.asJson),
        ("name", x.name.asJson),
        ("segment", x.segment.asJson)
      ))
    implicit val c19: Codec[OpenRTB.Geo] =
      Codec.from(deriveDecoder, (x: OpenRTB.Geo) => Util.toJObject(
        ("lat", x.lat.asJson),
        ("lon", x.lon.asJson),
        ("type", x.`type`.asJson),
        ("accuracy", x.accuracy.asJson),
        ("lastfix", x.lastfix.asJson),
        ("ipservice", x.ipservice.asJson),
        ("country", x.country.asJson),
        ("region", x.region.asJson),
        ("regionfips104", x.regionfips104.asJson),
        ("metro", x.metro.asJson),
        ("city", x.city.asJson),
        ("zip", x.zip.asJson),
        ("utcoffset", x.utcoffset.asJson),
      ))
    implicit val c18: Codec[OpenRTB.User] =
      Codec.from(deriveDecoder, (x: OpenRTB.User) => Util.toJObject(
        ("id", x.id.asJson),
        ("buyeruid", x.buyeruid.asJson),
        ("yob", x.yob.asJson),
        ("gender", x.gender.asJson),
        ("keywords", x.keywords.asJson),
        ("customdata", x.customdata.asJson),
        ("geo", x.geo.asJson),
        ("data", x.data.asJson),
      ))
    implicit val c17: Codec[OpenRTB.Device] =
      Codec.from(deriveDecoder, (x: OpenRTB.Device) => Util.toJObject(
        ("ua", x.ua.asJson),
        ("geo", x.geo.asJson),
        ("dnt", x.dnt.asJson),
        ("lmt", x.lmt.asJson),
        ("ip", x.ip.asJson),
        ("devicetype", x.devicetype.asJson),
        ("make", x.make.asJson),
        ("model", x.model.asJson),
        ("os", x.os.asJson),
        ("osv", x.osv.asJson),
        ("hwv", x.hwv.asJson),
        ("h", x.h.asJson),
        ("w", x.w.asJson),
        ("ppi", x.ppi.asJson),
        ("pxratio", x.pxratio.asJson),
        ("js", x.js.asJson),
        ("geofetch", x.geofetch.asJson),
        ("flashver", x.flashver.asJson),
        ("language", x.language.asJson),
        ("carrier", x.carrier.asJson),
        ("mccmnc", x.mccmnc.asJson),
        ("connectiontype", x.connectiontype.asJson),
        ("ifa", x.ifa.asJson),
        ("didsha1", x.didsha1.asJson),
        ("didmd5", x.didmd5.asJson),
        ("dpidsha1", x.dpidsha1.asJson),
        ("dpidmd5", x.dpidmd5.asJson),
        ("macsha1", x.macsha1.asJson),
        ("macmd5", x.macmd5.asJson),
      ))
    implicit val c16: Codec[OpenRTB.Producer] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[Option[String]]
        name <- c.downField("name").as[Option[String]]
        cat <- c.downField("cat").as[Option[List[String]]]
        domain <- c.downField("domain").as[Option[String]]
      } yield {
        new OpenRTB.Producer(id, name, cat.getOrElse(Nil), domain)
      }, (x: OpenRTB.Producer) => Util.toJObject(
        ("id", x.id.asJson),
        ("name", x.name.asJson),
        ("cat", x.cat.asJson),
        ("domain", x.domain.asJson)
      ))
    implicit val c15: Codec[OpenRTB.Content] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[Option[String]]
        episode <- c.downField("episode").as[Option[Int]]
        title <- c.downField("title").as[Option[String]]
        series <- c.downField("series").as[Option[String]]
        season <- c.downField("season").as[Option[String]]
        artist <- c.downField("artist").as[Option[String]]
        genre <- c.downField("genre").as[Option[String]]
        album <- c.downField("album").as[Option[String]]
        isrc <- c.downField("isrc").as[Option[String]]
        producer <- c.downField("producer").as[Option[OpenRTB.Producer]]
        url <- c.downField("url").as[Option[String]]
        cat <- c.downField("cat").as[Option[List[String]]]
        prodq <- c.downField("prodq").as[Option[Int]]
        videoquality <- c.downField("videoquality").as[Option[Int]]
        context <- c.downField("context").as[Option[Int]]
        contentrating <- c.downField("contentrating").as[Option[String]]
        userrating <- c.downField("userrating").as[Option[String]]
        qagmediarating <- c.downField("qagmediarating").as[Option[Int]]
        keywords <- c.downField("keywords").as[Option[String]]
        livestream <- c.downField("livestream").as[Option[Int]]
        sourcerelationship <- c.downField("sourcerelationship").as[Option[Int]]
        len <- c.downField("len").as[Option[Int]]
        language <- c.downField("language").as[Option[String]]
        embeddable <- c.downField("embeddable").as[Option[Int]]
        data <- c.downField("data").as[Option[OpenRTB.Data]]
      } yield {
        new OpenRTB.Content(id, episode, title, series, season, artist, genre, album, isrc, producer, url,
          cat.getOrElse(Nil), prodq, videoquality, context, contentrating, userrating, qagmediarating, keywords,
          livestream, sourcerelationship, len, language, embeddable, data)
      }, (x: OpenRTB.Content) => Util.toJObject(
        ("id", x.id.asJson),
        ("episode", x.episode.asJson),
        ("title", x.title.asJson),
        ("series", x.series.asJson),
        ("season", x.season.asJson),
        ("artist", x.artist.asJson),
        ("genre", x.genre.asJson),
        ("album", x.album.asJson),
        ("isrc", x.isrc.asJson),
        ("producer", x.producer.asJson),
        ("url", x.url.asJson),
        ("cat", x.cat.asJson),
        ("prodq", x.prodq.asJson),
        ("videoquality", x.videoquality.asJson),
        ("context", x.context.asJson),
        ("contentrating", x.contentrating.asJson),
        ("userrating", x.userrating.asJson),
        ("qagmediarating", x.qagmediarating.asJson),
        ("keywords", x.keywords.asJson),
        ("livestream", x.livestream.asJson),
        ("sourcerelationship", x.sourcerelationship.asJson),
        ("len", x.len.asJson),
        ("language", x.language.asJson),
        ("embeddable", x.embeddable.asJson),
        ("data", x.data.asJson),
      ))
    implicit val c14: Codec[OpenRTB.Publisher] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[Option[String]]
        name <- c.downField("name").as[Option[String]]
        cat <- c.downField("cat").as[Option[List[String]]]
        domain <- c.downField("domain").as[Option[String]]
      } yield {
        new OpenRTB.Publisher(id, name, cat.getOrElse(Nil), domain)
      }, (x: OpenRTB.Publisher) => Util.toJObject(
        ("id", x.id.asJson),
        ("name", x.name.asJson),
        ("cat", x.cat.asJson),
        ("domain", x.domain.asJson)
      ))
    implicit val c13: Codec[OpenRTB.App] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[Option[String]]
        name <- c.downField("name").as[Option[String]]
        bundle <- c.downField("bundle").as[Option[String]]
        domain <- c.downField("domain").as[Option[String]]
        storeurl <- c.downField("storeurl").as[Option[String]]
        cat <- c.downField("cat").as[Option[List[String]]]
        sectioncat <- c.downField("sectioncat").as[Option[List[String]]]
        pagecat <- c.downField("pagecat").as[Option[List[String]]]
        ver <- c.downField("ver").as[Option[String]]
        privacypolicy <- c.downField("privacypolicy").as[Option[Int]]
        paid <- c.downField("paid").as[Option[Int]]
        publisher <- c.downField("publisher").as[Option[OpenRTB.Publisher]]
        content <- c.downField("content").as[Option[OpenRTB.Content]]
        keywords <- c.downField("keywords").as[Option[String]]
      } yield {
        new OpenRTB.App(id, name, bundle, domain, storeurl, cat.getOrElse(Nil), sectioncat.getOrElse(Nil),
          pagecat.getOrElse(Nil), ver, privacypolicy, paid, publisher, content, keywords)
      }, (x: OpenRTB.App) => Util.toJObject(
        ("id", x.id.asJson),
        ("name", x.name.asJson),
        ("bundle", x.bundle.asJson),
        ("domain", x.domain.asJson),
        ("storeurl", x.storeurl.asJson),
        ("cat", x.cat.asJson),
        ("sectioncat", x.sectioncat.asJson),
        ("pagecat", x.pagecat.asJson),
        ("ver", x.ver.asJson),
        ("privacypolicy", x.privacypolicy.asJson),
        ("paid", x.paid.asJson),
        ("publisher", x.publisher.asJson),
        ("content", x.content.asJson),
        ("keywords", x.keywords.asJson)
      ))
    implicit val c12: Codec[OpenRTB.Site] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[Option[String]]
        name <- c.downField("name").as[Option[String]]
        domain <- c.downField("domain").as[Option[String]]
        cat <- c.downField("cat").as[Option[List[String]]]
        sectioncat <- c.downField("sectioncat").as[Option[List[String]]]
        pagecat <- c.downField("pagecat").as[Option[List[String]]]
        page <- c.downField("page").as[Option[String]]
        ref <- c.downField("ref").as[Option[String]]
        search <- c.downField("search").as[Option[String]]
        mobile <- c.downField("mobile").as[Option[Int]]
        privacypolicy <- c.downField("privacypolicy").as[Option[Int]]
        publisher <- c.downField("publisher").as[Option[OpenRTB.Publisher]]
        content <- c.downField("content").as[Option[OpenRTB.Content]]
        keywords <- c.downField("keywords").as[Option[String]]
      } yield {
        new OpenRTB.Site(id, name, domain, cat.getOrElse(Nil), sectioncat.getOrElse(Nil), pagecat.getOrElse(Nil), page,
          ref, search, mobile, privacypolicy, publisher, content, keywords)
      }, (x: OpenRTB.Site) => Util.toJObject(
        ("id", x.id.asJson),
        ("name", x.name.asJson),
        ("domain", x.domain.asJson),
        ("cat", x.cat.asJson),
        ("sectioncat", x.sectioncat.asJson),
        ("pagecat", x.pagecat.asJson),
        ("page", x.page.asJson),
        ("ref", x.ref.asJson),
        ("search", x.search.asJson),
        ("mobile", x.mobile.asJson),
        ("privacypolicy", x.privacypolicy.asJson),
        ("publisher", x.publisher.asJson),
        ("content", x.content.asJson),
        ("keywords", x.keywords.asJson)
      ))
    implicit val c11: Codec[OpenRTB.Deal] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[String]
        bidfloor <- c.downField("bidfloor").as[Option[Double]]
        bidfloorcur <- c.downField("bidfloorcur").as[Option[String]]
        at <- c.downField("at").as[Option[Int]]
        wseat <- c.downField("wseat").as[Option[List[String]]]
        wadomain <- c.downField("wadomain").as[Option[List[String]]]
      } yield {
        new OpenRTB.Deal(id, bidfloor.getOrElse(0.0), bidfloorcur.getOrElse("USD"), at, wseat.getOrElse(Nil),
          wadomain.getOrElse(Nil))
      }, (x: OpenRTB.Deal) => Util.toJObject(
        ("id", Json.fromString(x.id)),
        ("bidfloor", toJson(x.bidfloor, 0.0)),
        ("bidfloorcur", toJson(x.bidfloorcur, "USD")),
        ("at", x.at.asJson),
        ("wseat", x.wseat.asJson),
        ("wadomain", x.wadomain.asJson)
      ))
    implicit val c10: Codec[OpenRTB.Pmp] =
      Codec.from((c: HCursor) => for {
        private_auction <- c.downField("private_auction").as[Option[Int]]
        deals <- c.downField("deals").as[Option[List[OpenRTB.Deal]]]
      } yield {
        new OpenRTB.Pmp(private_auction.getOrElse(0), deals.getOrElse(Nil))
      }, (x: OpenRTB.Pmp) => Util.toJObject(
        ("private_auction", toJson(x.private_auction, 0)),
        ("deals", x.deals.asJson)
      ))
    implicit val c9: Codec[OpenRTB.Format] =
      Codec.from(deriveDecoder, (x: OpenRTB.Format) => Util.toJObject(
        ("w", x.w.asJson),
        ("h", x.h.asJson),
        ("wratio", x.wratio.asJson),
        ("hratio", x.hratio.asJson),
        ("wmin", x.wmin.asJson)
      ))
    implicit val c8: Codec[OpenRTB.Native] =
      Codec.from((c: HCursor) => for {
        request <- c.downField("request").as[String]
        ver <- c.downField("ver").as[Option[String]]
        api <- c.downField("api").as[Option[List[Int]]]
        battr <- c.downField("battr").as[Option[List[Int]]]
      } yield {
        new OpenRTB.Native(request, ver, api.getOrElse(Nil), battr.getOrElse(Nil))
      }, (x: OpenRTB.Native) => Util.toJObject(
        ("request", Json.fromString(x.request)),
        ("ver", x.ver.asJson),
        ("api", x.api.asJson),
        ("battr", x.battr.asJson)
      ))
    implicit val c7: Codec[OpenRTB.Banner] =
      Codec.from((c: HCursor) => for {
        format <- c.downField("format").as[Option[OpenRTB.Format]]
        w <- c.downField("w").as[Option[Int]]
        h <- c.downField("h").as[Option[Int]]
        wmax <- c.downField("wmax").as[Option[Int]]
        hmax <- c.downField("hmax").as[Option[Int]]
        wmin <- c.downField("wmin").as[Option[Int]]
        hmin <- c.downField("hmin").as[Option[Int]]
        btype <- c.downField("btype").as[Option[List[Int]]]
        battr <- c.downField("battr").as[Option[List[Int]]]
        pos <- c.downField("pos").as[Option[Int]]
        mimes <- c.downField("mimes").as[Option[List[String]]]
        topframe <- c.downField("topframe").as[Option[Int]]
        expdir <- c.downField("expdir").as[Option[List[Int]]]
        api <- c.downField("api").as[Option[List[Int]]]
        id <- c.downField("id").as[Option[String]]
        vcm <- c.downField("vcm").as[Option[Int]]
      } yield {
        new OpenRTB.Banner(format, w, h, wmax, hmax, wmin, hmin, btype.getOrElse(Nil), battr.getOrElse(Nil), pos,
          mimes.getOrElse(Nil), topframe, expdir.getOrElse(Nil), api.getOrElse(Nil), id, vcm)
      }, (x: OpenRTB.Banner) => Util.toJObject(
        ("format", x.format.asJson),
        ("w", x.w.asJson),
        ("h", x.h.asJson),
        ("wmax", x.wmax.asJson),
        ("hmax", x.hmax.asJson),
        ("wmin", x.wmin.asJson),
        ("hmin", x.hmin.asJson),
        ("btype", x.btype.asJson),
        ("battr", x.battr.asJson),
        ("pos", x.pos.asJson),
        ("mimes", x.mimes.asJson),
        ("topframe", x.topframe.asJson),
        ("expdir", x.expdir.asJson),
        ("api", x.api.asJson),
        ("id", x.id.asJson),
        ("vcm", x.vcm.asJson)
      ))
    implicit val c6: Codec[OpenRTB.Audio] =
      Codec.from((c: HCursor) => for {
        mimes <- c.downField("mimes").as[Option[List[String]]]
        minduration <- c.downField("minduration").as[Option[Int]]
        maxduration <- c.downField("maxduration").as[Option[Int]]
        protocols <- c.downField("protocols").as[Option[List[Int]]]
        startdelay <- c.downField("startdelay").as[Option[Int]]
        sequence <- c.downField("sequence").as[Option[Int]]
        battr <- c.downField("battr").as[Option[List[Int]]]
        maxextended <- c.downField("maxextended").as[Option[Int]]
        minbitrate <- c.downField("minbitrate").as[Option[Int]]
        maxbitrate <- c.downField("maxbitrate").as[Option[Int]]
        delivery <- c.downField("delivery").as[Option[List[Int]]]
        companionad <- c.downField("companionad").as[Option[List[OpenRTB.Banner]]]
        api <- c.downField("api").as[Option[List[Int]]]
        companiontype <- c.downField("companiontype").as[Option[List[Int]]]
        maxseq <- c.downField("maxseq").as[Option[Int]]
        feed <- c.downField("feed").as[Option[Int]]
        stitched <- c.downField("stitched").as[Option[Int]]
        nvol <- c.downField("nvol").as[Option[Int]]
      } yield {
        new OpenRTB.Audio(mimes.getOrElse(Nil), minduration, maxduration, protocols.getOrElse(Nil), startdelay,
          sequence, battr.getOrElse(Nil), maxextended, minbitrate, maxbitrate, delivery.getOrElse(Nil),
          companionad.getOrElse(Nil), api.getOrElse(Nil), companiontype.getOrElse(Nil), maxseq, feed, stitched, nvol)
      }, (x: OpenRTB.Audio) => Util.toJObject(
        ("mimes", x.mimes.asJson),
        ("minduration", x.minduration.asJson),
        ("maxduration", x.maxduration.asJson),
        ("protocols", x.protocols.asJson),
        ("startdelay", x.startdelay.asJson),
        ("sequence", x.sequence.asJson),
        ("battr", x.battr.asJson),
        ("maxextended", x.maxextended.asJson),
        ("minbitrate", x.minbitrate.asJson),
        ("maxbitrate", x.maxbitrate.asJson),
        ("delivery", x.delivery.asJson),
        ("companionad", x.companionad.asJson),
        ("api", x.api.asJson),
        ("companiontype", x.companiontype.asJson),
        ("maxseq", x.maxseq.asJson),
        ("feed", x.feed.asJson),
        ("stitched", x.stitched.asJson),
        ("nvol", x.nvol.asJson),
      ))
    implicit val c5: Codec[OpenRTB.Video] =
      Codec.from((c: HCursor) => for {
        mimes <- c.downField("mimes").as[Option[List[String]]]
        minduration <- c.downField("minduration").as[Option[Int]]
        maxduration <- c.downField("maxduration").as[Option[Int]]
        protocols <- c.downField("protocols").as[Option[List[Int]]]
        protocol <- c.downField("protocol").as[Option[Int]]
        w <- c.downField("w").as[Option[Int]]
        h <- c.downField("h").as[Option[Int]]
        startdelay <- c.downField("startdelay").as[Option[Int]]
        placement <- c.downField("placement").as[Option[Int]]
        linearity <- c.downField("linearity").as[Option[Int]]
        skip <- c.downField("skip").as[Option[Int]]
        skipmin <- c.downField("skipmin").as[Option[Int]]
        skipafter <- c.downField("skipafter").as[Option[Int]]
        sequence <- c.downField("sequence").as[Option[Int]]
        battr <- c.downField("battr").as[Option[List[Int]]]
        maxextended <- c.downField("maxextended").as[Option[Int]]
        minbitrate <- c.downField("minbitrate").as[Option[Int]]
        maxbitrate <- c.downField("maxbitrate").as[Option[Int]]
        boxingallowed <- c.downField("boxingallowed").as[Option[Int]]
        playbackmethod <- c.downField("playbackmethod").as[Option[List[Int]]]
        playbackend <- c.downField("playbackend").as[Option[Int]]
        delivery <- c.downField("delivery").as[Option[List[Int]]]
        pos <- c.downField("pos").as[Option[Int]]
        companionad <- c.downField("companionad").as[Option[List[OpenRTB.Banner]]]
        api <- c.downField("api").as[Option[List[Int]]]
        companiontype <- c.downField("companiontype").as[Option[List[Int]]]
      } yield {
        new OpenRTB.Video(mimes.getOrElse(Nil), minduration, maxduration, protocols.getOrElse(Nil), protocol, w, h,
          startdelay, placement, linearity, skip, skipmin.getOrElse(0), skipafter.getOrElse(0), sequence,
          battr.getOrElse(Nil), maxextended, minbitrate, maxbitrate, boxingallowed.getOrElse(1),
          playbackmethod.getOrElse(Nil), playbackend, delivery.getOrElse(Nil), pos, companionad.getOrElse(Nil),
          api.getOrElse(Nil), companiontype.getOrElse(Nil))
      }, (x: OpenRTB.Video) => Util.toJObject(
        ("mimes", x.mimes.asJson),
        ("minduration", x.minduration.asJson),
        ("maxduration", x.maxduration.asJson),
        ("protocols", x.protocols.asJson),
        ("protocol", x.protocol.asJson),
        ("w", x.w.asJson),
        ("h", x.h.asJson),
        ("startdelay", x.startdelay.asJson),
        ("placement", x.placement.asJson),
        ("linearity", x.linearity.asJson),
        ("skip", x.skip.asJson),
        ("skipmin", toJson(x.skipmin, 0)),
        ("skipafter", toJson(x.skipafter, 0)),
        ("sequence", x.sequence.asJson),
        ("battr", x.battr.asJson),
        ("maxextended", x.maxextended.asJson),
        ("minbitrate", x.minbitrate.asJson),
        ("maxbitrate", x.maxbitrate.asJson),
        ("boxingallowed", toJson(x.boxingallowed, 1)),
        ("playbackmethod", x.playbackmethod.asJson),
        ("playbackend", x.playbackend.asJson),
        ("delivery", x.delivery.asJson),
        ("pos", x.pos.asJson),
        ("companionad", x.companionad.asJson),
        ("api", x.api.asJson),
        ("companiontype", x.companiontype.asJson)
      ))
    implicit val c4: Codec[OpenRTB.Metric] =
      Codec.from(deriveDecoder, (x: OpenRTB.Metric) => Util.toJObject(
        ("type", Json.fromString(x.`type`)),
        ("value", x.value.asJson),
        ("vendor", x.vendor.asJson)
      ))
    implicit val c3: Codec[OpenRTB.Imp] =
      Codec.from((c: HCursor) => for {
        id <- c.downField("id").as[String]
        metric <- c.downField("metric").as[Option[List[OpenRTB.Metric]]]
        banner <- c.downField("banner").as[Option[OpenRTB.Banner]]
        video <- c.downField("video").as[Option[OpenRTB.Video]]
        audio <- c.downField("audio").as[Option[OpenRTB.Audio]]
        native <- c.downField("native").as[Option[OpenRTB.Native]]
        pmp <- c.downField("pmp").as[Option[OpenRTB.Pmp]]
        displaymanager <- c.downField("displaymanager").as[Option[String]]
        displaymanagerver <- c.downField("displaymanagerver").as[Option[String]]
        instl <- c.downField("instl").as[Option[Int]]
        tagid <- c.downField("tagid").as[Option[String]]
        bidfloor <- c.downField("bidfloor").as[Option[Double]]
        bidfloorcur <- c.downField("bidfloorcur").as[Option[String]]
        clickbrowser <- c.downField("clickbrowser").as[Option[Int]]
        secure <- c.downField("secure").as[Option[Int]]
        iframebuster <- c.downField("iframebuster").as[Option[List[String]]]
        exp <- c.downField("exp").as[Option[Int]]
      } yield {
        new OpenRTB.Imp(id, metric.getOrElse(Nil), banner, video, audio, native, pmp, displaymanager, displaymanagerver,
          instl.getOrElse(0), tagid, bidfloor.getOrElse(0.0), bidfloorcur.getOrElse("USD"), clickbrowser,
          secure.getOrElse(0), iframebuster.getOrElse(Nil), exp)
      }, (x: OpenRTB.Imp) => Util.toJObject(
        ("id", Json.fromString(x.id)),
        ("metric", x.metric.asJson),
        ("banner", x.banner.asJson),
        ("video", x.video.asJson),
        ("audio", x.audio.asJson),
        ("native", x.native.asJson),
        ("pmp", x.pmp.asJson),
        ("displaymanager", x.displaymanager.asJson),
        ("displaymanagerver", x.displaymanagerver.asJson),
        ("instl", toJson(x.instl, 0)),
        ("tagid", x.tagid.asJson),
        ("bidfloor", toJson(x.bidfloor, 0.0)),
        ("bidfloorcur", toJson(x.bidfloorcur, "USD")),
        ("secure", toJson(x.secure, 0)),
        ("iframebuster", x.iframebuster.asJson),
        ("exp", x.exp.asJson)
      ))
    implicit val c2: Codec[OpenRTB.Reqs] = deriveCodec
    implicit val c1: Codec[OpenRTB.Source] =
      Codec.from(deriveDecoder, (x: OpenRTB.Source) => Util.toJObject(
        ("fd", x.fd.asJson),
        ("tid", x.tid.asJson),
        ("pchain", x.pchain.asJson)
      ))
    Codec.from((c: HCursor) => for {
      id <- c.downField("id").as[String]
      imp <- c.downField("imp").as[Option[List[OpenRTB.Imp]]]
      site <- c.downField("site").as[Option[OpenRTB.Site]]
      app <- c.downField("app").as[Option[OpenRTB.App]]
      device <- c.downField("device").as[Option[OpenRTB.Device]]
      user <- c.downField("user").as[Option[OpenRTB.User]]
      test <- c.downField("test").as[Option[Int]]
      at <- c.downField("at").as[Option[Int]]
      tmax <- c.downField("tmax").as[Option[Int]]
      wset <- c.downField("wset").as[Option[List[String]]]
      bset <- c.downField("bset").as[Option[List[String]]]
      allimps <- c.downField("allimps").as[Option[Int]]
      cur <- c.downField("cur").as[Option[List[String]]]
      wlang <- c.downField("wlang").as[Option[List[String]]]
      bcat <- c.downField("bcat").as[Option[List[String]]]
      badv <- c.downField("badv").as[Option[List[String]]]
      bapp <- c.downField("bapp").as[Option[List[String]]]
      source <- c.downField("source").as[Option[OpenRTB.Source]]
      reqs <- c.downField("reqs").as[Option[OpenRTB.Reqs]]
    } yield {
      new OpenRTB.BidRequest(id, imp.getOrElse(Nil), site, app, device, user, test.getOrElse(0), at.getOrElse(2), tmax,
        wset.getOrElse(Nil), bset.getOrElse(Nil), allimps.getOrElse(0), cur.getOrElse(Nil), wlang.getOrElse(Nil),
        bcat.getOrElse(Nil), badv.getOrElse(Nil), bapp.getOrElse(Nil), source, reqs)
    }, (x: OpenRTB.BidRequest) => Util.toJObject(
      ("id", Json.fromString(x.id)),
      ("imp", x.imp.asJson),
      ("site", x.site.asJson),
      ("app", x.app.asJson),
      ("device", x.device.asJson),
      ("user", x.user.asJson),
      ("test", toJson(x.test, 0)),
      ("at", toJson(x.at, 2)),
      ("tmax", x.tmax.asJson),
      ("wset", x.wset.asJson),
      ("bset", x.bset.asJson),
      ("allimps", toJson(x.allimps, 0)),
      ("cur", x.cur.asJson),
      ("wlang", x.wlang.asJson),
      ("bcat", x.bcat.asJson),
      ("badv", x.badv.asJson),
      ("bapp", x.bapp.asJson),
      ("source", x.source.asJson),
      ("reqs", x.reqs.asJson)
    ))
  }
  implicit val bigIntE5r: Encoder[BigInt] = encodeJsonNumber
    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val bitSetC3c: Codec[BitSet] =
    Codec.from(Decoder.decodeArray[Int].map(arr => BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is an unsafe option for open systems */))),
      Encoder.encodeSet[Int].contramapArray((m: BitSet) => m))
  implicit val mutableBitSetC3c: Codec[mutable.BitSet] =
    Codec.from(Decoder.decodeArray[Int].map(arr => mutable.BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is an unsafe option for open systems */))),
      Encoder.encodeSeq[Int].contramapArray((m: mutable.BitSet) => m.toVector))
  implicit val distanceMatrixC3c: Codec[GoogleMapsAPI.DistanceMatrix] = {
    implicit val c1: Codec[GoogleMapsAPI.Value] = deriveCodec
    implicit val c2: Codec[GoogleMapsAPI.Elements] = deriveCodec
    implicit val c3: Codec[GoogleMapsAPI.Rows] = deriveCodec
    deriveCodec
  }
  implicit val gitHubActionsAPIC3c: Codec[GitHubActionsAPI.Response] = {
    implicit val c1: Codec[GitHubActionsAPI.Artifact] =
      Codec.forProduct9("id", "node_id", "name", "size_in_bytes", "url", "archive_download_url",
        "expired", "created_at", "expires_at") {
        (id: Long, node_id: String, name: String, size_in_bytes: Long, url: String, archive_download_url: String,
         expired: String, created_at: Instant, expires_at: Instant) =>
          new GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url,
            expired.toBoolean, created_at, expires_at)
      } { a =>
        (a.id, a.node_id, a.name, a.size_in_bytes, a.url, a.archive_download_url,
          a.expired.toString, a.created_at, a.expires_at)
      }
    deriveCodec
  }
  implicit val extractFieldsC3c: Codec[ExtractFields] = deriveCodec
  implicit val geoJSONC3c: Codec[GeoJSON.GeoJSON] = {
    implicit val c1: Codec[GeoJSON.Point] = deriveCodec
    implicit val c2: Codec[GeoJSON.MultiPoint] = deriveCodec
    implicit val c3: Codec[GeoJSON.LineString] = deriveCodec
    implicit val c4: Codec[GeoJSON.MultiLineString] = deriveCodec
    implicit val c5: Codec[GeoJSON.Polygon] = deriveCodec
    implicit val c6: Codec[GeoJSON.MultiPolygon] = deriveCodec
    implicit val c7: Codec[GeoJSON.SimpleGeometry] = Codec.from(Decoder.instance(c =>
      c.downField("type").as[String].flatMap {
        case "Point" => c.as[GeoJSON.Point]
        case "MultiPoint" => c.as[GeoJSON.MultiPoint]
        case "LineString" => c.as[GeoJSON.LineString]
        case "MultiLineString" => c.as[GeoJSON.MultiLineString]
        case "Polygon" => c.as[GeoJSON.Polygon]
        case "MultiPolygon" => c.as[GeoJSON.MultiPolygon]
      }), Encoder.instance {
        case x: GeoJSON.Point => x.asJson.mapObject(_.+:("type" -> "Point".asJson))
        case x: GeoJSON.MultiPoint => x.asJson.mapObject(_.+:("type" -> "MultiPoint".asJson))
        case x: GeoJSON.LineString => x.asJson.mapObject(_.+:("type" -> "LineString".asJson))
        case x: GeoJSON.MultiLineString => x.asJson.mapObject(_.+:("type" -> "MultiLineString".asJson))
        case x: GeoJSON.Polygon => x.asJson.mapObject(_.+:("type" -> "Polygon".asJson))
        case x: GeoJSON.MultiPolygon => x.asJson.mapObject(_.+:("type" -> "MultiPolygon".asJson))
      })
    implicit val c8: Codec[GeoJSON.GeometryCollection] = deriveCodec
    implicit val c9: Codec[GeoJSON.Geometry] = Codec.from(Decoder.instance(c =>
      c.downField("type").as[String].flatMap {
        case "Point" => c.as[GeoJSON.Point]
        case "MultiPoint" => c.as[GeoJSON.MultiPoint]
        case "LineString" => c.as[GeoJSON.LineString]
        case "MultiLineString" => c.as[GeoJSON.MultiLineString]
        case "Polygon" => c.as[GeoJSON.Polygon]
        case "MultiPolygon" => c.as[GeoJSON.MultiPolygon]
        case "GeometryCollection" => c.as[GeoJSON.GeometryCollection]
      }), Encoder.instance {
        case x: GeoJSON.Point => x.asJson.mapObject(_.+:("type" -> "Point".asJson))
        case x: GeoJSON.MultiPoint => x.asJson.mapObject(_.+:("type" -> "MultiPoint".asJson))
        case x: GeoJSON.LineString => x.asJson.mapObject(_.+:("type" -> "LineString".asJson))
        case x: GeoJSON.MultiLineString => x.asJson.mapObject(_.+:("type" -> "MultiLineString".asJson))
        case x: GeoJSON.Polygon => x.asJson.mapObject(_.+:("type" -> "Polygon".asJson))
        case x: GeoJSON.MultiPolygon => x.asJson.mapObject(_.+:("type" -> "MultiPolygon".asJson))
        case x: GeoJSON.GeometryCollection => x.asJson.mapObject(_.+:("type" -> "GeometryCollection".asJson))
      })
    implicit val c10: Codec[GeoJSON.Feature] = deriveCodec
    implicit val c11: Codec[GeoJSON.SimpleGeoJSON] = Codec.from(Decoder.instance(c =>
      c.downField("type").as[String].flatMap {
        case "Feature" => c.as[GeoJSON.Feature]
      }), Encoder.instance {
      case x: GeoJSON.Feature => x.asJson.mapObject(_.+:("type" -> "Feature".asJson))
    })
    implicit val c12: Codec[GeoJSON.FeatureCollection] = deriveCodec
    Codec.from(Decoder.instance(c =>
      c.downField("type").as[String].flatMap {
        case "Feature" => c.as[GeoJSON.Feature]
        case "FeatureCollection" => c.as[GeoJSON.FeatureCollection]
      }), Encoder.instance {
      case x: GeoJSON.Feature => x.asJson.mapObject(_.+:("type" -> "Feature".asJson))
      case x: GeoJSON.FeatureCollection => x.asJson.mapObject(_.+:("type" -> "FeatureCollection".asJson))
    })
  }
  implicit val intMapC3c: Codec[IntMap[Boolean]] =
    Codec.from(Decoder.decodeMap[Int, Boolean].map(_.foldLeft(IntMap.empty[Boolean])((m, p) => m.updated(p._1, p._2))),
      Encoder.encodeMap[Int, Boolean].contramapObject((m: IntMap[Boolean]) => m))
  implicit val longMapC3c: Codec[mutable.LongMap[Boolean]] =
    Codec.from(Decoder.decodeMap[Long, Boolean].map(_.foldLeft(new mutable.LongMap[Boolean]) { (m, p) =>
      m.update(p._1, p._2)
      m
    }), Encoder.encodeMapLike[Long, Boolean, mutable.Map].contramapObject((m: mutable.LongMap[Boolean]) => m))
  implicit val missingRequiredFieldsC3c: Codec[MissingRequiredFields] = deriveCodec
  implicit val nestedStructsC3c: Codec[NestedStructs] = deriveCodec
  implicit val suitC3c: Codec[Suit] = Codec.from(decodeString.emap { s =>
    try new Right(Suit.valueOf(s)) catch {
      case _: IllegalArgumentException => new Left("Suit")
    }
  }, encodeString.contramap[Suit](_.name))
  implicit val suitADTC3c: Codec[SuitADT] = Codec.from(decodeString.map {
    val m = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s => m.getOrElse(s, throw new IllegalArgumentException("SuitADT"))
  }, encodeString.contramap(_.toString))
  implicit val suitEnumC3c: Codec[SuitEnum] = Codec.codecForEnumeration(SuitEnum)
  implicit val primitivesC3c: Codec[Primitives] = deriveCodec
  implicit val tweetC3c: Codec[TwitterAPI.Tweet] = {
    implicit val c1: Codec[TwitterAPI.UserMentions] = deriveCodec
    implicit val c2: Codec[TwitterAPI.Urls] = deriveCodec
    implicit val c3: Codec[TwitterAPI.Entities] = deriveCodec
    implicit val c4: Codec[TwitterAPI.Url] = deriveCodec
    implicit val c5: Codec[TwitterAPI.UserEntities] = deriveCodec
    implicit val c6: Codec[TwitterAPI.User] = deriveCodec
    implicit val c7: Codec[TwitterAPI.RetweetedStatus] = deriveCodec
    deriveCodec
  }

  @inline
  private[this] def toJson[T: Encoder](x: T, d: T): Json =
    if (x == d) Json.Null
    else x.asJson
}