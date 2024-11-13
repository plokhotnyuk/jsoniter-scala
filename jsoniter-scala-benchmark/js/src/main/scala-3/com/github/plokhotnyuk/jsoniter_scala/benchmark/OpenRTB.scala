package com.github.plokhotnyuk.jsoniter_scala.benchmark

object OpenRTB {
  case class BidRequest(
    id: String,
    imp: List[Imp] = Nil,
    site: Option[Site] = None,
    app: Option[App] = None,
    device: Option[Device] = None,
    user: Option[User] = None,
    test: Int = 0,
    at: Int = 2,
    tmax: Option[Int] = None,
    wset: List[String] = Nil,
    bset: List[String] = Nil,
    allimps: Int = 0,
    cur: List[String] = Nil,
    wlang: List[String] = Nil,
    bcat: List[String] = Nil,
    badv: List[String] = Nil,
    bapp: List[String] = Nil,
    source: Option[Source] = None,
    reqs: Option[Reqs] = None,
    /*ext: Option[BidRequestExt] = None*/)

  case class Source(
    fd: Option[Int] = None,
    tid: Option[String] = None,
    pchain: Option[String] = None,
    /*ext: Option[SourceExt] = None*/)

  case class Reqs(
    coppa: Int,
    /*ext: Option[ReqsExt] = None*/)

  case class Imp(
    id: String,
    metric: List[Metric] = Nil,
    banner: Option[Banner] = None,
    video: Option[Video] = None,
    audio: Option[Audio] = None,
    native: Option[Native] = None,
    pmp: Option[Pmp] = None,
    displaymanager: Option[String] = None,
    displaymanagerver: Option[String] = None,
    instl: Int = 0,
    tagid: Option[String] = None,
    bidfloor: Double = 0.0,
    bidfloorcur: String = "USD",
    clickbrowser: Option[Int] = None,
    secure: Int = 0,
    iframebuster: List[String] = Nil,
    exp: Option[Int] = None,
    /*ext: Option[ImpExt] = None*/)

  case class Metric(
    `type`: String,
    value: Double,
    vendor: Option[String] = None,
    /*ext: Option[MetricExt] = None*/)

  case class Banner(
    format: Option[Format] = None,
    w: Option[Int] = None,
    h: Option[Int] = None,
    wmax: Option[Int] = None,
    hmax: Option[Int] = None,
    wmin: Option[Int] = None,
    hmin: Option[Int] = None,
    btype: List[Int] = Nil,
    battr: List[Int] = Nil,
    pos: Option[Int] = None,
    mimes: List[String] = Nil,
    topframe: Option[Int] = None,
    expdir: List[Int] = Nil,
    api: List[Int] = Nil,
    id: Option[String] = None,
    vcm: Option[Int] = None,
    /*ext: Option[BannerExt] = None*/)

  case class Video(
    mimes: List[String] = Nil,
    minduration: Option[Int] = None,
    maxduration: Option[Int] = None,
    protocols: List[Int] = Nil,
    protocol: Option[Int] = None,
    w: Option[Int] = None,
    h: Option[Int] = None,
    startdelay: Option[Int] = None,
    placement: Option[Int] = None,
    linearity: Option[Int] = None,
    skip: Option[Int] = None,
    skipmin: Int = 0,
    skipafter: Int = 0,
    sequence: Option[Int] = None,
    battr: List[Int] = Nil,
    maxextended: Option[Int] = None,
    minbitrate: Option[Int] = None,
    maxbitrate: Option[Int] = None,
    boxingallowed: Int = 1,
    playbackmethod: List[Int] = Nil,
    playbackend: Option[Int] = None,
    delivery: List[Int] = Nil,
    pos: Option[Int] = None,
    companionad: List[Banner] = Nil,
    api: List[Int] = Nil,
    companiontype: List[Int] = Nil,
    /*ext: Option[VideoExt] = None*/)

  case class Audio(
    mimes: List[String] = Nil,
    minduration: Option[Int] = None,
    maxduration: Option[Int] = None,
    protocols: List[Int] = Nil,
    startdelay: Option[Int] = None,
    sequence: Option[Int] = None,
    battr: List[Int] = Nil,
    maxextended: Option[Int] = None,
    minbitrate: Option[Int] = None,
    maxbitrate: Option[Int] = None,
    delivery: List[Int] = Nil,
    companionad: List[Banner] = Nil,
    api: List[Int] = Nil,
    companiontype: List[Int] = Nil,
    maxseq: Option[Int] = None,
    feed: Option[Int] = None,
    stitched: Option[Int] = None,
    nvol: Option[Int] = None,
    /*ext: Option[AudioExt] = None*/)

  case class Native(
    request: String,
    ver: Option[String] = None,
    api: List[Int] = Nil,
    battr: List[Int] = Nil,
    /*ext: Option[NativeExt] = None*/)

  case class Format(
    w: Option[Int] = None,
    h: Option[Int] = None,
    wratio: Option[Int] = None,
    hratio: Option[Int] = None,
    wmin: Option[Int] = None,
    /*ext: Option[FormatExt] = None*/)

  case class Pmp(
    private_auction: Int = 0,
    deals: List[Deal] = Nil,
    /*ext: Option[PmpExt] = None*/)

  case class Deal(
    id: String,
    bidfloor: Double = 0.0,
    bidfloorcur: String = "USD",
    at: Option[Int] = None,
    wseat: List[String] = Nil,
    wadomain: List[String] = Nil,
    /*ext: Option[DealExt] = None*/)

  case class Site(
    id: Option[String] = None,
    name: Option[String] = None,
    domain: Option[String] = None,
    cat: List[String] = Nil,
    sectioncat: List[String] = Nil,
    pagecat: List[String] = Nil,
    page: Option[String] = None,
    ref: Option[String] = None,
    search: Option[String] = None,
    mobile: Option[Int] = None,
    privacypolicy: Option[Int] = None,
    publisher: Option[Publisher] = None,
    content: Option[Content] = None,
    keywords: Option[String] = None,
    /*ext: Option[SiteExt] = None*/)

  case class App(
    id: Option[String] = None,
    name: Option[String] = None,
    bundle: Option[String] = None,
    domain: Option[String] = None,
    storeurl: Option[String] = None,
    cat: List[String] = Nil,
    sectioncat: List[String] = Nil,
    pagecat: List[String] = Nil,
    ver: Option[String] = None,
    privacypolicy: Option[Int] = None,
    paid: Option[Int] = None,
    publisher: Option[Publisher] = None,
    content: Option[Content] = None,
    keywords: Option[String] = None,
    /*ext: Option[AppExt] = None*/)

  case class Publisher(
    id: Option[String] = None,
    name: Option[String] = None,
    cat: List[String] = Nil,
    domain: Option[String] = None,
    /*ext: Option[PublisherExt] = None*/)

  case class Content(
    id: Option[String] = None,
    episode: Option[Int] = None,
    title: Option[String] = None,
    series: Option[String] = None,
    season: Option[String] = None,
    artist: Option[String] = None,
    genre: Option[String] = None,
    album: Option[String] = None,
    isrc: Option[String] = None,
    producer: Option[Producer] = None,
    url: Option[String] = None,
    cat: List[String] = Nil,
    prodq: Option[Int] = None,
    videoquality: Option[Int] = None,
    context: Option[Int] = None,
    contentrating: Option[String] = None,
    userrating: Option[String] = None,
    qagmediarating: Option[Int] = None,
    keywords: Option[String] = None,
    livestream: Option[Int] = None,
    sourcerelationship: Option[Int] = None,
    len: Option[Int] = None,
    language: Option[String] = None,
    embeddable: Option[Int] = None,
    data: Option[Data] = None,
    /*ext: Option[ContentExt] = None*/)

  case class Producer(
    id: Option[String] = None,
    name: Option[String] = None,
    cat: List[String] = Nil,
    domain: Option[String] = None,
    /*ext: Option[ProducerExt] = None*/)

  case class Device(
    ua: Option[String] = None,
    geo: Option[Geo] = None,
    dnt: Option[Int] = None,
    lmt: Option[Int] = None,
    ip: Option[String] = None,
    devicetype: Option[Int] = None,
    make: Option[String] = None,
    model: Option[String] = None,
    os: Option[String] = None,
    osv: Option[String] = None,
    hwv: Option[String] = None,
    h: Option[Int] = None,
    w: Option[Int] = None,
    ppi: Option[Int] = None,
    pxratio: Option[Double] = None,
    js: Option[Int] = None,
    geofetch: Option[Int] = None,
    flashver: Option[String] = None,
    language: Option[String] = None,
    carrier: Option[String] = None,
    mccmnc: Option[String] = None,
    connectiontype: Option[Int] = None,
    ifa: Option[String] = None,
    didsha1: Option[String] = None,
    didmd5: Option[String] = None,
    dpidsha1: Option[String] = None,
    dpidmd5: Option[String] = None,
    macsha1: Option[String] = None,
    macmd5: Option[String] = None,
    /*ext: Option[DeviceExt] = None*/)

  case class Geo(
    lat: Option[Double] = None,
    lon: Option[Double] = None,
    `type`: Option[Int] = None,
    accuracy: Option[Int] = None,
    lastfix: Option[Int] = None,
    ipservice: Option[Int] = None,
    country: Option[String] = None,
    region: Option[String] = None,
    regionfips104: Option[String] = None,
    metro: Option[String] = None,
    city: Option[String] = None,
    zip: Option[String] = None,
    utcoffset: Option[String] = None,
    /*ext: Option[GeoExt] = None*/)

  case class User(
    id: Option[String] = None,
    buyeruid: Option[String] = None,
    yob: Option[Int] = None,
    gender: Option[String] = None,
    keywords: Option[String] = None,
    customdata: Option[String] = None,
    geo: Option[Geo] = None,
    data: Option[Data] = None,
    /*ext: Option[UserExt] = None*/)

  case class Data(
    id: Option[String] = None,
    name: Option[String] = None,
    segment: List[Segment] = Nil,
    /*ext: Option[DataExt] = None*/)

  case class Segment(
    id: Option[String] = None,
    name: Option[String] = None,
    value: Option[String] = None,
    /*ext: Option[DataExt] = None*/)
}
