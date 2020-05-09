package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.transientDefault
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.OpenRTB._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weepickle.v1.implicits.dropDefault

import scala.reflect.io.Streamable

object OpenRTB {
  case class BidRequest (
    id: String,
    @transientDefault @dropDefault imp: List[Imp] = Nil,
    @transientDefault @dropDefault site: Option[Site] = None,
    @transientDefault @dropDefault app: Option[App] = None,
    @transientDefault @dropDefault device: Option[Device] = None,
    @transientDefault @dropDefault user: Option[User] = None,
    @transientDefault @dropDefault test: Int = 0,
    @transientDefault @dropDefault at: Int = 2,
    @transientDefault @dropDefault tmax: Option[Int] = None,
    @transientDefault @dropDefault wset: List[String] = Nil,
    @transientDefault @dropDefault bset: List[String] = Nil,
    @transientDefault @dropDefault allimps: Int = 0,
    @transientDefault @dropDefault cur: List[String] = Nil,
    @transientDefault @dropDefault wlang: List[String] = Nil,
    @transientDefault @dropDefault bcat: List[String] = Nil,
    @transientDefault @dropDefault badv: List[String] = Nil,
    @transientDefault @dropDefault bapp: List[String] = Nil,
    @transientDefault @dropDefault source: Option[Source] = None,
    @transientDefault @dropDefault reqs: Option[Reqs] = None,
    /*@transientDefault @dropDefault ext: Option[BidRequestExt] = None*/)

  case class Source(
    @transientDefault @dropDefault fd: Option[Int] = None,
    @transientDefault @dropDefault tid: Option[String] = None,
    @transientDefault @dropDefault pchain: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[SourceExt] = None*/)

  case class Reqs(
    coppa: Int,
    /*@transientDefault @dropDefault ext: Option[ReqsExt] = None*/)

  case class Imp(
    id: String,
    @transientDefault @dropDefault metric: List[Metric] = Nil,
    @transientDefault @dropDefault banner: Option[Banner] = None,
    @transientDefault @dropDefault video: Option[Video] = None,
    @transientDefault @dropDefault audio: Option[Audio] = None,
    @transientDefault @dropDefault native: Option[Native] = None,
    @transientDefault @dropDefault pmp: Option[Pmp] = None,
    @transientDefault @dropDefault displaymanager: Option[String] = None,
    @transientDefault @dropDefault displaymanagerver: Option[String] = None,
    @transientDefault @dropDefault instl: Int = 0,
    @transientDefault @dropDefault tagid: Option[String] = None,
    @transientDefault @dropDefault bidfloor: Double = 0.0,
    @transientDefault @dropDefault bidfloorcur: String = "USD",
    @transientDefault @dropDefault clickbrowser: Option[Int] = None,
    @transientDefault @dropDefault secure: Int = 0,
    @transientDefault @dropDefault iframebuster: List[String] = Nil,
    @transientDefault @dropDefault exp: Option[Int] = None,
    /*@transientDefault @dropDefault ext: Option[ImpExt] = None*/)

  case class Metric(
    `type`: String,
    value: Double,
    @transientDefault @dropDefault vendor: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[MetricExt] = None*/)

  case class Banner(
    @transientDefault @dropDefault format: Option[Format] = None,
    @transientDefault @dropDefault w: Option[Int] = None,
    @transientDefault @dropDefault h: Option[Int] = None,
    @transientDefault @dropDefault wmax: Option[Int] = None,
    @transientDefault @dropDefault hmax: Option[Int] = None,
    @transientDefault @dropDefault wmin: Option[Int] = None,
    @transientDefault @dropDefault hmin: Option[Int] = None,
    @transientDefault @dropDefault btype: List[Int] = Nil,
    @transientDefault @dropDefault battr: List[Int] = Nil,
    @transientDefault @dropDefault pos: Option[Int] = None,
    @transientDefault @dropDefault mimes: List[String] = Nil,
    @transientDefault @dropDefault topframe: Option[Int] = None,
    @transientDefault @dropDefault expdir: List[Int] = Nil,
    @transientDefault @dropDefault api: List[Int] = Nil,
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault vcm: Option[Int] = None,
    /*@transientDefault @dropDefault ext: Option[BannerExt] = None*/)

  case class Video(
    @transientDefault @dropDefault mimes: List[String] = Nil,
    @transientDefault @dropDefault minduration: Option[Int] = None,
    @transientDefault @dropDefault maxduration: Option[Int] = None,
    @transientDefault @dropDefault protocols: List[Int] = Nil,
    @transientDefault @dropDefault protocol: Option[Int] = None,
    @transientDefault @dropDefault w: Option[Int] = None,
    @transientDefault @dropDefault h: Option[Int] = None,
    @transientDefault @dropDefault startdelay: Option[Int] = None,
    @transientDefault @dropDefault placement: Option[Int] = None,
    @transientDefault @dropDefault linearity: Option[Int] = None,
    @transientDefault @dropDefault skip: Option[Int] = None,
    @transientDefault @dropDefault skipmin: Int = 0,
    @transientDefault @dropDefault skipafter: Int = 0,
    @transientDefault @dropDefault Listuence: Option[Int] = None,
    @transientDefault @dropDefault battr: List[Int] = Nil,
    @transientDefault @dropDefault maxextended: Option[Int] = None,
    @transientDefault @dropDefault minbitrate: Option[Int] = None,
    @transientDefault @dropDefault maxbitrate: Option[Int] = None,
    @transientDefault @dropDefault boxingallowed: Int = 1,
    @transientDefault @dropDefault playbackmethod: List[Int] = Nil,
    @transientDefault @dropDefault playbackend: Option[Int] = None,
    @transientDefault @dropDefault delivery: List[Int] = Nil,
    @transientDefault @dropDefault pos: Option[Int] = None,
    @transientDefault @dropDefault companionad: List[Banner] = Nil,
    @transientDefault @dropDefault api: List[Int] = Nil,
    @transientDefault @dropDefault companiontype: List[Int] = Nil,
    /*@transientDefault @dropDefault ext: Option[VideoExt] = None*/)

  case class Audio(
    @transientDefault @dropDefault mimes: List[String] = Nil,
    @transientDefault @dropDefault minduration: Option[Int] = None,
    @transientDefault @dropDefault maxduration: Option[Int] = None,
    @transientDefault @dropDefault protocols: List[Int] = Nil,
    @transientDefault @dropDefault startdelay: Option[Int] = None,
    @transientDefault @dropDefault Listuence: Option[Int] = None,
    @transientDefault @dropDefault battr: List[Int] = Nil,
    @transientDefault @dropDefault maxextended: Option[Int] = None,
    @transientDefault @dropDefault minbitrate: Option[Int] = None,
    @transientDefault @dropDefault maxbitrate: Option[Int] = None,
    @transientDefault @dropDefault delivery: List[Int] = Nil,
    @transientDefault @dropDefault companionad: List[Banner] = Nil,
    @transientDefault @dropDefault api: List[Int] = Nil,
    @transientDefault @dropDefault companiontype: List[Int] = Nil,
    @transientDefault @dropDefault maxList: Option[Int] = None,
    @transientDefault @dropDefault feed: Option[Int] = None,
    @transientDefault @dropDefault stitched: Option[Int] = None,
    @transientDefault @dropDefault nvol: Option[Int] = None,
    /*@transientDefault @dropDefault ext: Option[AudioExt] = None*/)

  case class Native(
    request: String,
    @transientDefault @dropDefault ver: Option[String] = None,
    @transientDefault @dropDefault api: List[Int] = Nil,
    @transientDefault @dropDefault battr: List[Int] = Nil,
    /*@transientDefault @dropDefault ext: Option[NativeExt] = None*/)

  case class Format(
    @transientDefault @dropDefault w: Option[Int] = None,
    @transientDefault @dropDefault h: Option[Int] = None,
    @transientDefault @dropDefault wratio: Option[Int] = None,
    @transientDefault @dropDefault hratio: Option[Int] = None,
    @transientDefault @dropDefault wmin: Option[Int] = None,
    /*@transientDefault @dropDefault ext: Option[FormatExt] = None*/)

  case class Pmp(
    @transientDefault @dropDefault private_auction: Int = 0,
    @transientDefault @dropDefault deals: List[Deal] = Nil,
    /*@transientDefault @dropDefault ext: Option[PmpExt] = None*/)

  case class Deal(
    id: String,
    @transientDefault @dropDefault bidfloor: Double = 0,
    @transientDefault @dropDefault bidfloorcur: String = "USD",
    @transientDefault @dropDefault at: Option[Int] = None,
    @transientDefault @dropDefault wseat: List[String] = Nil,
    @transientDefault @dropDefault wadomain: List[String] = Nil,
    /*@transientDefault @dropDefault ext: Option[DealExt] = None*/)

  case class Site(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault name: Option[String] = None,
    @transientDefault @dropDefault domain: Option[String] = None,
    @transientDefault @dropDefault cat: List[String] = Nil,
    @transientDefault @dropDefault sectioncat: List[String] = Nil,
    @transientDefault @dropDefault pagecat: List[String] = Nil,
    @transientDefault @dropDefault page: Option[String] = None,
    @transientDefault @dropDefault ref: Option[String] = None,
    @transientDefault @dropDefault search: Option[String] = None,
    @transientDefault @dropDefault mobile: Option[Int] = None,
    @transientDefault @dropDefault privacypolicy: Option[Int] = None,
    @transientDefault @dropDefault publisher: Option[Publisher] = None,
    @transientDefault @dropDefault content: Option[Content] = None,
    @transientDefault @dropDefault keywords: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[SiteExt] = None*/)

  case class App(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault name: Option[String] = None,
    @transientDefault @dropDefault bundle: Option[String] = None,
    @transientDefault @dropDefault domain: Option[String] = None,
    @transientDefault @dropDefault storeurl: Option[String] = None,
    @transientDefault @dropDefault cat: List[String] = Nil,
    @transientDefault @dropDefault sectioncat: List[String] = Nil,
    @transientDefault @dropDefault pagecat: List[String] = Nil,
    @transientDefault @dropDefault ver: Option[String] = None,
    @transientDefault @dropDefault privacypolicy: Option[Int] = None,
    @transientDefault @dropDefault paid: Option[Int] = None,
    @transientDefault @dropDefault publisher: Option[Publisher] = None,
    @transientDefault @dropDefault content: Option[Content] = None,
    @transientDefault @dropDefault keywords: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[AppExt] = None*/)

  case class Publisher(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault name: Option[String] = None,
    @transientDefault @dropDefault cat: List[String] = Nil,
    @transientDefault @dropDefault domain: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[PublisherExt] = None*/)

  case class Content(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault episode: Option[Int] = None,
    @transientDefault @dropDefault title: Option[String] = None,
    @transientDefault @dropDefault series: Option[String] = None,
    @transientDefault @dropDefault season: Option[String] = None,
    @transientDefault @dropDefault artist: Option[String] = None,
    @transientDefault @dropDefault genre: Option[String] = None,
    @transientDefault @dropDefault album: Option[String] = None,
    @transientDefault @dropDefault isrc: Option[String] = None,
    @transientDefault @dropDefault producer: Option[Producer] = None,
    @transientDefault @dropDefault url: Option[String] = None,
    @transientDefault @dropDefault cat: List[String] = Nil,
    @transientDefault @dropDefault prodq: Option[Int] = None,
    @transientDefault @dropDefault videoquality: Option[Int] = None,
    @transientDefault @dropDefault context: Option[Int] = None,
    @transientDefault @dropDefault contentrating: Option[String] = None,
    @transientDefault @dropDefault userrating: Option[String] = None,
    @transientDefault @dropDefault qagmediarating: Option[Int] = None,
    @transientDefault @dropDefault keywords: Option[String] = None,
    @transientDefault @dropDefault livestream: Option[Int] = None,
    @transientDefault @dropDefault sourcerelationship: Option[Int] = None,
    @transientDefault @dropDefault len: Option[Int] = None,
    @transientDefault @dropDefault language: Option[String] = None,
    @transientDefault @dropDefault embeddable: Option[Int] = None,
    @transientDefault @dropDefault data: Option[Data] = None,
    /*@transientDefault @dropDefault ext: Option[ContentExt] = None*/)

  case class Producer(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault name: Option[String] = None,
    @transientDefault @dropDefault cat: List[String] = Nil,
    @transientDefault @dropDefault domain: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[ProducerExt] = None*/)

  case class Device(
    @transientDefault @dropDefault ua: Option[String] = None,
    @transientDefault @dropDefault geo: Option[Geo] = None,
    @transientDefault @dropDefault dnt: Option[Int] = None,
    @transientDefault @dropDefault lmt: Option[Int] = None,
    @transientDefault @dropDefault ip: Option[String] = None,
    @transientDefault @dropDefault devicetype: Option[Int] = None,
    @transientDefault @dropDefault make: Option[String] = None,
    @transientDefault @dropDefault model: Option[String] = None,
    @transientDefault @dropDefault os: Option[String] = None,
    @transientDefault @dropDefault osv: Option[String] = None,
    @transientDefault @dropDefault hwv: Option[String] = None,
    @transientDefault @dropDefault h: Option[Int] = None,
    @transientDefault @dropDefault w: Option[Int] = None,
    @transientDefault @dropDefault ppi: Option[Int] = None,
    @transientDefault @dropDefault pxratio: Option[Double] = None,
    @transientDefault @dropDefault js: Option[Int] = None,
    @transientDefault @dropDefault geofetch: Option[Int] = None,
    @transientDefault @dropDefault flashver: Option[String] = None,
    @transientDefault @dropDefault language: Option[String] = None,
    @transientDefault @dropDefault carrier: Option[String] = None,
    @transientDefault @dropDefault mccmnc: Option[String] = None,
    @transientDefault @dropDefault connectiontype: Option[Int] = None,
    @transientDefault @dropDefault ifa: Option[String] = None,
    @transientDefault @dropDefault didsha1: Option[String] = None,
    @transientDefault @dropDefault didmd5: Option[String] = None,
    @transientDefault @dropDefault dpidsha1: Option[String] = None,
    @transientDefault @dropDefault dpidmd5: Option[String] = None,
    @transientDefault @dropDefault macsha1: Option[String] = None,
    @transientDefault @dropDefault macmd5: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[DeviceExt] = None*/)

  case class Geo(
    @transientDefault @dropDefault lat: Option[Double] = None,
    @transientDefault @dropDefault lon: Option[Double] = None,
    @transientDefault @dropDefault `type`: Option[Int] = None,
    @transientDefault @dropDefault accuracy: Option[Int] = None,
    @transientDefault @dropDefault lastfix: Option[Int] = None,
    @transientDefault @dropDefault ipservice: Option[Int] = None,
    @transientDefault @dropDefault country: Option[String] = None,
    @transientDefault @dropDefault region: Option[String] = None,
    @transientDefault @dropDefault regionfips104: Option[String] = None,
    @transientDefault @dropDefault metro: Option[String] = None,
    @transientDefault @dropDefault city: Option[String] = None,
    @transientDefault @dropDefault zip: Option[String] = None,
    @transientDefault @dropDefault utcoffset: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[GeoExt] = None*/)

  case class User(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault buyeruid: Option[String] = None,
    @transientDefault @dropDefault yob: Option[Int] = None,
    @transientDefault @dropDefault gender: Option[String] = None,
    @transientDefault @dropDefault keywords: Option[String] = None,
    @transientDefault @dropDefault customdata: Option[String] = None,
    @transientDefault @dropDefault geo: Option[Geo] = None,
    @transientDefault @dropDefault data: Option[Data] = None,
    /*@transientDefault @dropDefault ext: Option[UserExt] = None*/)

  case class Data(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault name: Option[String] = None,
    @transientDefault @dropDefault segment: List[Segment] = Nil,
    /*@transientDefault @dropDefault ext: Option[DataExt] = None*/)

  case class Segment(
    @transientDefault @dropDefault id: Option[String] = None,
    @transientDefault @dropDefault name: Option[String] = None,
    @transientDefault @dropDefault value: Option[String] = None,
    /*@transientDefault @dropDefault ext: Option[DataExt] = None*/)
}

abstract class OpenRTBBenchmark extends CommonParams {
  var jsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("openrtb_bidrequest.json"))
  var obj: BidRequest = readFromArray[BidRequest](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  var jsonString: String = new String(jsonBytes, UTF_8)
}