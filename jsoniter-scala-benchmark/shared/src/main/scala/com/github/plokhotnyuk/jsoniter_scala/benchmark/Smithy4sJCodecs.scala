package com.github.plokhotnyuk.jsoniter_scala.benchmark

import smithy4s.http.json._
import smithy4s.{ByteArray, Schema, Timestamp}
import smithy4s.api.Discriminated
import smithy4s.schema.Schema._
import java.time.Instant
import java.util.UUID
import scala.collection.immutable.{ArraySeq, Seq}

object Smithy4sJCodecs {
  def toIndexedSeqOfIndexedSeq[A](xs: List[List[A]]): IndexedSeq[IndexedSeq[A]] =
    xs.foldLeft(IndexedSeq.newBuilder[IndexedSeq[A]])((a, x) => a.addOne(x.toIndexedSeq)).result()

  def toListOfList[A](xs: IndexedSeq[IndexedSeq[A]]): List[List[A]] =
    xs.foldLeft(List.newBuilder[List[A]])((a, x) => a.addOne(x.toList)).result()

  def toOptList[A](xs: List[A]): Option[List[A]] =
    if (xs.isEmpty) None
    else Some(xs)

  def toOpt[A](x: A, default: A): Option[A] =
    if (x == default) None
    else Some(x)

  val adtSchema: Schema[ADTBase] = recursive {
    val xAlt = struct(int.required[X]("a", _.a))(X.apply).oneOf[ADTBase]("X")
    val yAlt = struct(string.required[Y]("b", _.b))(Y.apply).oneOf[ADTBase]("Y")
    val zAlt = struct(adtSchema.required[Z]("l", _.l), adtSchema.required[Z]("r", _.r))(Z.apply).oneOf[ADTBase]("Z")
    union(xAlt, yAlt, zAlt) {
      case x: X => xAlt(x)
      case y: Y => yAlt(y)
      case z: Z => zAlt(z)
    }.addHints(Discriminated("type"))
  }
  implicit val adtJCodec: JCodec[ADTBase] = JCodec.deriveJCodecFromSchema(adtSchema)
  implicit val anyValsJCodec: JCodec[AnyVals] = JCodec.deriveJCodecFromSchema(struct(
    byte.required[AnyVals]("b", _.b.a),
    short.required[AnyVals]("s", _.s.a),
    int.required[AnyVals]("i", _.i.a),
    long.required[AnyVals]("l", _.l.a),
    boolean.required[AnyVals]("bl", _.bl.a),
    string.required[AnyVals]("ch", _.ch.a.toString),
    double.required[AnyVals]("dbl", _.dbl.a),
    float.required[AnyVals]("f", _.f.a)
  )((b, s, i, l, bl, st, dbl, f) => AnyVals(ByteVal(b), ShortVal(s), IntVal(i), LongVal(l), BooleanVal(bl),
    CharVal({
      if (st.length == 1) st.charAt(0)
      else sys.error("illegal char")
    }), DoubleVal(dbl), FloatVal(f))))
  implicit val arrayOfBigDecimalsJCodec: JCodec[Array[BigDecimal]] =
    JCodec.deriveJCodecFromSchema(bijection[List[BigDecimal], Array[BigDecimal]](list(bigdecimal), _.toArray, _.toList))
  implicit val arrayOfBigIntsJCodec: JCodec[Array[BigInt]] =
    JCodec.deriveJCodecFromSchema(bijection[List[BigInt], Array[BigInt]](list(bigint), _.toArray, _.toList))
  implicit val arrayOfBooleansJCodec: JCodec[Array[Boolean]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Boolean], Array[Boolean]](list(boolean), _.toArray, _.toList))
  implicit val arrayOfBytesJCodec: JCodec[Array[Byte]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Byte], Array[Byte]](list(byte), _.toArray, _.toList))
  implicit val arrayOfDoublesJCodec: JCodec[Array[Double]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Double], Array[Double]](list(double), _.toArray, _.toList))
  implicit val arrayOfFloatsJCodec: JCodec[Array[Float]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Float], Array[Float]](list(float), _.toArray, _.toList))
  implicit val arrayOfInstantsJCodec: JCodec[Array[Instant]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Timestamp], Array[Instant]](list(timestamp),
      _.map(PlatformUtils.toInstant).toArray, _.map(PlatformUtils.toTimestamp).toList))
  implicit val arrayOfIntsJCodec: JCodec[Array[Int]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Int], Array[Int]](list(int), _.toArray, _.toList))
  implicit val arrayOfLongsJCodec: JCodec[Array[Long]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Long], Array[Long]](list(long), _.toArray, _.toList))
  implicit val arrayOfShortsJCodec: JCodec[Array[Short]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Short], Array[Short]](list(short), _.toArray, _.toList))
  implicit val arrayOfUUIDsJCodec: JCodec[Array[UUID]] =
    JCodec.deriveJCodecFromSchema(bijection[List[UUID], Array[UUID]](list(uuid), _.toArray, _.toList))
  implicit val arraySeqOfBooleansJCodec: JCodec[ArraySeq[Boolean]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Boolean], ArraySeq[Boolean]](list(boolean),
      x => ArraySeq.unsafeWrapArray(x.toArray), _.toList))
  val base64JCodec: JCodec[Array[Byte]] =
    JCodec.deriveJCodecFromSchema(bijection[ByteArray, Array[Byte]](bytes, _.array, ByteArray.apply))
  val bigDecimalJCodec: JCodec[BigDecimal] = JCodec.deriveJCodecFromSchema(bigdecimal)
  val bigIntJCodec: JCodec[BigInt] = JCodec.deriveJCodecFromSchema(bigint)
  implicit val extractFieldsJCodec: JCodec[ExtractFields] = JCodec.deriveJCodecFromSchema(struct(
    string.required[ExtractFields]("s", _.s),
    int.required[ExtractFields]("i", _.i)
  )(ExtractFields.apply))
  implicit val geoJsonJCodec: JCodec[GeoJSON.GeoJSON] = JCodec.deriveJCodecFromSchema {
    val coordinatesSchema: Schema[(Double, Double)] =
      bijection[List[Double], (Double, Double)](list(double), {
        case (x: Double) :: (y: Double) :: Nil => (x, y)
        case _ => sys.error("expecting array of 2 numbers")
      }, x => x._1 :: x._2 :: Nil)
    val bboxSchema: Schema[(Double, Double, Double, Double)] =
      bijection[List[Double], (Double, Double, Double, Double)](list(double), {
        case (x1: Double) :: (y1: Double) :: (x2: Double) :: (y2: Double) :: Nil => (x1, y1, x2, y2)
        case _ => sys.error("expecting array of 4 numbers")
      }, x => x._1 :: x._2 :: x._3 :: x._4 :: Nil)
    val pointSchema: Schema[GeoJSON.Point] =
      struct(
        coordinatesSchema.required[GeoJSON.Point]("coordinates", _.coordinates),
      )(GeoJSON.Point.apply)
    val multiPointSchema: Schema[GeoJSON.MultiPoint] =
      struct(
        list(coordinatesSchema).required[GeoJSON.MultiPoint]("coordinates", _.coordinates.toList),
      )(coordinates => GeoJSON.MultiPoint(coordinates.toIndexedSeq))
    val lineStringSchema: Schema[GeoJSON.LineString] =
      struct(
        list(coordinatesSchema).required[GeoJSON.LineString]("coordinates", _.coordinates.toList),
      )(coordinates => GeoJSON.LineString(coordinates.toIndexedSeq))
    val multiLineStringSchema: Schema[GeoJSON.MultiLineString] =
      struct(
        list(list(coordinatesSchema)).required[GeoJSON.MultiLineString]("coordinates", x => toListOfList(x.coordinates)),
      )(coordinates => GeoJSON.MultiLineString(toIndexedSeqOfIndexedSeq(coordinates)))
    val polygonSchema: Schema[GeoJSON.Polygon] =
      struct(
        list(list(coordinatesSchema)).required[GeoJSON.Polygon]("coordinates", x => toListOfList(x.coordinates)),
      )(coordinates => GeoJSON.Polygon(toIndexedSeqOfIndexedSeq(coordinates)))
    val multiPolygonSchema: Schema[GeoJSON.MultiPolygon] =
      struct(
        list(list(list(coordinatesSchema)))
          .required[GeoJSON.MultiPolygon]("coordinates", _.coordinates.map(toListOfList).toList),
      )(coordinates => GeoJSON.MultiPolygon(coordinates.map(toIndexedSeqOfIndexedSeq).toIndexedSeq))
    val simpleGeometrySchema: Schema[GeoJSON.SimpleGeometry] =  {
      val pointAlt = pointSchema.oneOf[GeoJSON.SimpleGeometry]("Point")
      val multiPointAlt = multiPointSchema.oneOf[GeoJSON.SimpleGeometry]("MultiPoint")
      val lineStringAlt = lineStringSchema.oneOf[GeoJSON.SimpleGeometry]("LineString")
      val multiLineStringAlt = multiLineStringSchema.oneOf[GeoJSON.SimpleGeometry]("MultiLineString")
      val polygonAlt = polygonSchema.oneOf[GeoJSON.SimpleGeometry]("Polygon")
      val multiPolygonAlt = multiPolygonSchema.oneOf[GeoJSON.SimpleGeometry]("MultiPolygon")
      union(pointAlt, multiPointAlt, lineStringAlt, multiLineStringAlt, polygonAlt, multiPolygonAlt) {
        case x: GeoJSON.Point => pointAlt(x)
        case x: GeoJSON.MultiPoint => multiPointAlt(x)
        case x: GeoJSON.LineString => lineStringAlt(x)
        case x: GeoJSON.MultiLineString => multiLineStringAlt(x)
        case x: GeoJSON.Polygon => polygonAlt(x)
        case x: GeoJSON.MultiPolygon => multiPolygonAlt(x)
      }.addHints(Discriminated("type"))
    }
    val geometryCollectionSchema: Schema[GeoJSON.GeometryCollection] =
      struct(
        list(simpleGeometrySchema).required[GeoJSON.GeometryCollection]("geometries", _.geometries.toList),
      )(geometries => GeoJSON.GeometryCollection(geometries.toIndexedSeq))
    val geometrySchema: Schema[GeoJSON.Geometry] = {
      val pointAlt = pointSchema.oneOf[GeoJSON.Geometry]("Point")
      val multiPointAlt = multiPointSchema.oneOf[GeoJSON.Geometry]("MultiPoint")
      val lineStringAlt = lineStringSchema.oneOf[GeoJSON.Geometry]("LineString")
      val multiLineStringAlt = multiLineStringSchema.oneOf[GeoJSON.Geometry]("MultiLineString")
      val polygonAlt = polygonSchema.oneOf[GeoJSON.Geometry]("Polygon")
      val multiPolygonAlt = multiPolygonSchema.oneOf[GeoJSON.Geometry]("MultiPolygon")
      val geometryCollectionAlt = geometryCollectionSchema.oneOf[GeoJSON.Geometry]("GeometryCollection")
      union(pointAlt, multiPointAlt, lineStringAlt, multiLineStringAlt, polygonAlt, multiPolygonAlt, geometryCollectionAlt) {
        case x: GeoJSON.Point => pointAlt(x)
        case x: GeoJSON.MultiPoint => multiPointAlt(x)
        case x: GeoJSON.LineString => lineStringAlt(x)
        case x: GeoJSON.MultiLineString => multiLineStringAlt(x)
        case x: GeoJSON.Polygon => polygonAlt(x)
        case x: GeoJSON.MultiPolygon => multiPolygonAlt(x)
        case x: GeoJSON.GeometryCollection => geometryCollectionAlt(x)
      }.addHints(Discriminated("type"))
    }
    val featureSchema: Schema[GeoJSON.Feature] =
      struct(
        map(string, string).required[GeoJSON.Feature]("properties", _.properties),
        geometrySchema.required[GeoJSON.Feature]("geometry", _.geometry),
        bboxSchema.optional[GeoJSON.Feature]("bbox", _.bbox)
      )((properties, geometry, bbox) => GeoJSON.Feature(properties, geometry, bbox))
    val simpleGeoJSONSchema: Schema[GeoJSON.SimpleGeoJSON] = {
      val featureAlt = featureSchema.oneOf[GeoJSON.SimpleGeoJSON]("Feature")
      union(featureAlt) {
        case x: GeoJSON.Feature => featureAlt(x)
      }.addHints(Discriminated("type"))
    }
    val featureCollectionSchema: Schema[GeoJSON.FeatureCollection] =
      struct(
        list(simpleGeoJSONSchema).required[GeoJSON.FeatureCollection]("features", _.features.toList),
        bboxSchema.optional[GeoJSON.FeatureCollection]("bbox", _.bbox)
      )((features, bbox) => GeoJSON.FeatureCollection(features.toIndexedSeq, bbox))
    val featureAlt = featureSchema.oneOf[GeoJSON.GeoJSON]("Feature")
    val featureCollectionAlt = featureCollectionSchema.oneOf[GeoJSON.GeoJSON]("FeatureCollection")
    union(featureAlt, featureCollectionAlt) {
      case x: GeoJSON.Feature => featureAlt(x)
      case x: GeoJSON.FeatureCollection => featureCollectionAlt(x)
    }.addHints(Discriminated("type"))
  }
  implicit val gitHubActionsAPIJCodec: JCodec[GitHubActionsAPI.Response] = JCodec.deriveJCodecFromSchema({
    val rowsSchema: Schema[GitHubActionsAPI.Artifact] =
      struct(
        long.required[GitHubActionsAPI.Artifact]("id", _.id),
        string.required[GitHubActionsAPI.Artifact]("node_id", _.node_id),
        string.required[GitHubActionsAPI.Artifact]("name", _.name),
        long.required[GitHubActionsAPI.Artifact]("size_in_bytes", _.size_in_bytes),
        string.required[GitHubActionsAPI.Artifact]("url", _.url),
        string.required[GitHubActionsAPI.Artifact]("archive_download_url", _.archive_download_url),
        string.required[GitHubActionsAPI.Artifact]("expired", _.expired.toString),
        timestamp.required[GitHubActionsAPI.Artifact]("created_at", x => PlatformUtils.toTimestamp(x.created_at)),
        timestamp.required[GitHubActionsAPI.Artifact]("expires_at", x => PlatformUtils.toTimestamp(x.expires_at))
      )((id, node_id, name, size_in_bytes, url, archive_download_url, expired, created_at, expires_at) =>
        GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url, expired.toBoolean,
          PlatformUtils.toInstant(created_at), PlatformUtils.toInstant(expires_at)))
    struct(
      int.required[GitHubActionsAPI.Response]("total_count", _.total_count),
      list(rowsSchema).required[GitHubActionsAPI.Response]("artifacts", _.artifacts.toList)
    ) { (total_count, artifacts) =>
      GitHubActionsAPI.Response(total_count, artifacts)
    }
  })
  implicit val googleMapsAPIJCodec: JCodec[GoogleMapsAPI.DistanceMatrix] = JCodec.deriveJCodecFromSchema({
    val valueSchema: Schema[GoogleMapsAPI.Value] =
      struct(
        string.required[GoogleMapsAPI.Value]("text", _.text),
        int.required[GoogleMapsAPI.Value]("value", _.value)
      )(GoogleMapsAPI.Value.apply)
    val elementsSchema: Schema[GoogleMapsAPI.Elements] =
      struct(
        valueSchema.required[GoogleMapsAPI.Elements]("distance", _.distance),
        valueSchema.required[GoogleMapsAPI.Elements]("duration", _.duration),
        string.required[GoogleMapsAPI.Elements]("status", _.status)
      )(GoogleMapsAPI.Elements.apply)
    val rowsSchema: Schema[GoogleMapsAPI.Rows] =
      struct(
        list(elementsSchema).required[GoogleMapsAPI.Rows]("elements", _.elements.toList)
      )(elements => GoogleMapsAPI.Rows(elements.toVector))
    struct(
      list(string).required[GoogleMapsAPI.DistanceMatrix]("destination_addresses", _.destination_addresses.toList),
      list(string).required[GoogleMapsAPI.DistanceMatrix]("origin_addresses", _.origin_addresses.toList),
      list(rowsSchema).required[GoogleMapsAPI.DistanceMatrix]("rows", _.rows.toList),
      string.required[GoogleMapsAPI.DistanceMatrix]("status", _.status)
    ) { (destination_addresses, origin_addresses, rows, status) =>
      GoogleMapsAPI.DistanceMatrix(destination_addresses.toVector, origin_addresses.toVector, rows.toVector, status)
    }
  })
  val intJCodec: JCodec[Int] = JCodec.deriveJCodecFromSchema(int)
  implicit val listOfBooleansJCodec: JCodec[List[Boolean]] = JCodec.deriveJCodecFromSchema(list(boolean))
  implicit val mapOfIntsToBooleansJCodec: JCodec[Map[Int, Boolean]] = JCodec.deriveJCodecFromSchema(map(int, boolean))
  implicit val missingRequiredFieldsJCodec: JCodec[MissingRequiredFields] =
    JCodec.deriveJCodecFromSchema(struct(
      string.required[MissingRequiredFields]("s", _.s),
      int.required[MissingRequiredFields]("i", _.i)
    )(MissingRequiredFields.apply))
  val nestedStructsSchema: Schema[NestedStructs] =
    recursive(struct(nestedStructsSchema.optional[NestedStructs]("n", _.n))(NestedStructs.apply))
  implicit val nestedStructsJCodec: JCodec[NestedStructs] = JCodec.deriveJCodecFromSchema(nestedStructsSchema)
  implicit val openRTBJCodec: JCodec[OpenRTB.BidRequest] = JCodec.deriveJCodecFromSchema {
    val metricSchema: Schema[OpenRTB.Metric] =
      struct(
        string.required[OpenRTB.Metric]("type", _.`type`),
        double.required[OpenRTB.Metric]("value", _.value),
        string.optional[OpenRTB.Metric]("vendor", _.vendor),
      )(OpenRTB.Metric.apply)
    val formatSchema: Schema[OpenRTB.Format] =
      struct(
        int.optional[OpenRTB.Format]("w", _.w),
        int.optional[OpenRTB.Format]("h", _.h),
        int.optional[OpenRTB.Format]("wratio", _.wratio),
        int.optional[OpenRTB.Format]("hratio", _.hratio),
        int.optional[OpenRTB.Format]("wmin", _.wmin)
      )(OpenRTB.Format.apply)
    val bannerSchema: Schema[OpenRTB.Banner] =
      struct(
        formatSchema.optional[OpenRTB.Banner]("format", _.format),
        int.optional[OpenRTB.Banner]("w", _.w),
        int.optional[OpenRTB.Banner]("h", _.h),
        int.optional[OpenRTB.Banner]("wmax", _.wmax),
        int.optional[OpenRTB.Banner]("hmax", _.hmax),
        int.optional[OpenRTB.Banner]("wmin", _.wmin),
        int.optional[OpenRTB.Banner]("hmin", _.hmin),
        list(int).optional[OpenRTB.Banner]("btype", x => toOptList(x.btype)),
        list(int).optional[OpenRTB.Banner]("battr", x => toOptList(x.battr)),
        int.optional[OpenRTB.Banner]("pos", _.pos),
        list(string).optional[OpenRTB.Banner]("mimes", x => toOptList(x.mimes)),
        int.optional[OpenRTB.Banner]("topframe", _.topframe),
        list(int).optional[OpenRTB.Banner]("expdir", x => toOptList(x.expdir)),
        list(int).optional[OpenRTB.Banner]("api", x => toOptList(x.api)),
        string.optional[OpenRTB.Banner]("id", _.id),
        int.optional[OpenRTB.Banner]("vcm", _.vcm),
      ) { (format, w, h, wmax, hmax, wmin, hmin, btype, battr, pos, mimes, topframe, expdir, api, id, vcm) =>
        OpenRTB.Banner(format, w, h, wmax, hmax, wmin, hmin, btype.getOrElse(Nil), battr.getOrElse(Nil), pos,
          mimes.getOrElse(Nil), topframe, expdir.getOrElse(Nil), api.getOrElse(Nil), id, vcm)
      }
    val videoSchema: Schema[OpenRTB.Video] =
      struct.genericArity(
        list(string).optional[OpenRTB.Video]("mimes", x => toOptList(x.mimes)),
        int.optional[OpenRTB.Video]("minduration", _.minduration),
        int.optional[OpenRTB.Video]("maxduration", _.maxduration),
        list(int).optional[OpenRTB.Video]("protocols", x => toOptList(x.protocols)),
        int.optional[OpenRTB.Video]("protocol", _.protocol),
        int.optional[OpenRTB.Video]("w", _.w),
        int.optional[OpenRTB.Video]("h", _.h),
        int.optional[OpenRTB.Video]("startdelay", _.startdelay),
        int.optional[OpenRTB.Video]("placement", _.placement),
        int.optional[OpenRTB.Video]("linearity", _.linearity),
        int.optional[OpenRTB.Video]("skip", _.skip),
        int.optional[OpenRTB.Video]("skipmin", x => toOpt(x.skipmin, 0)),
        int.optional[OpenRTB.Video]("skipafter", x => toOpt(x.skipafter, 0)),
        int.optional[OpenRTB.Video]("sequence", _.sequence),
        list(int).optional[OpenRTB.Video]("battr", x => toOptList(x.battr)),
        int.optional[OpenRTB.Video]("maxextended", _.maxextended),
        int.optional[OpenRTB.Video]("minbitrate", _.minbitrate),
        int.optional[OpenRTB.Video]("maxbitrate", _.maxbitrate),
        int.optional[OpenRTB.Video]("boxingallowed", x => toOpt(x.boxingallowed, 1)),
        list(int).optional[OpenRTB.Video]("playbackmethod", x => toOptList(x.playbackmethod)),
        int.optional[OpenRTB.Video]("playbackend", _.playbackend),
        list(int).optional[OpenRTB.Video]("delivery", x => toOptList(x.delivery)),
        int.optional[OpenRTB.Video]("pos", _.pos),
        list(bannerSchema).optional[OpenRTB.Video]("companionad", x => toOptList(x.companionad)),
        list(int).optional[OpenRTB.Video]("api", x => toOptList(x.api)),
        list(int).optional[OpenRTB.Video]("companiontype", x => toOptList(x.companiontype))
      ) { ps: IndexedSeq[Any] =>
        OpenRTB.Video(
          ps(0).asInstanceOf[Option[List[String]]].getOrElse(Nil),
          ps(1).asInstanceOf[Option[Int]],
          ps(2).asInstanceOf[Option[Int]],
          ps(3).asInstanceOf[Option[List[Int]]].getOrElse(Nil),
          ps(4).asInstanceOf[Option[Int]],
          ps(5).asInstanceOf[Option[Int]],
          ps(6).asInstanceOf[Option[Int]],
          ps(7).asInstanceOf[Option[Int]],
          ps(8).asInstanceOf[Option[Int]],
          ps(9).asInstanceOf[Option[Int]],
          ps(10).asInstanceOf[Option[Int]],
          ps(11).asInstanceOf[Option[Int]].getOrElse(0),
          ps(12).asInstanceOf[Option[Int]].getOrElse(0),
          ps(13).asInstanceOf[Option[Int]],
          ps(14).asInstanceOf[Option[List[Int]]].getOrElse(Nil),
          ps(15).asInstanceOf[Option[Int]],
          ps(16).asInstanceOf[Option[Int]],
          ps(17).asInstanceOf[Option[Int]],
          ps(18).asInstanceOf[Option[Int]].getOrElse(1),
          ps(19).asInstanceOf[Option[List[Int]]].getOrElse(Nil),
          ps(20).asInstanceOf[Option[Int]],
          ps(21).asInstanceOf[Option[List[Int]]].getOrElse(Nil),
          ps(22).asInstanceOf[Option[Int]],
          ps(23).asInstanceOf[Option[List[OpenRTB.Banner]]].getOrElse(Nil),
          ps(24).asInstanceOf[Option[List[Int]]].getOrElse(Nil),
          ps(25).asInstanceOf[Option[List[Int]]].getOrElse(Nil)
        )
      }
    val audioSchema: Schema[OpenRTB.Audio] =
      struct(
        list(string).optional[OpenRTB.Audio]("mimes", x => toOptList(x.mimes)),
        int.optional[OpenRTB.Audio]("minduration", _.minduration),
        int.optional[OpenRTB.Audio]("maxduration", _.maxduration),
        list(int).optional[OpenRTB.Audio]("protocols", x => toOptList(x.protocols)),
        int.optional[OpenRTB.Audio]("startdelay", _.startdelay),
        int.optional[OpenRTB.Audio]("sequence", _.sequence),
        list(int).optional[OpenRTB.Audio]("battr", x => toOptList(x.battr)),
        int.optional[OpenRTB.Audio]("maxextended", _.maxextended),
        int.optional[OpenRTB.Audio]("minbitrate", _.minbitrate),
        int.optional[OpenRTB.Audio]("maxbitrate", _.maxbitrate),
        list(int).optional[OpenRTB.Audio]("delivery", x => toOptList(x.delivery)),
        list(bannerSchema).optional[OpenRTB.Audio]("companionad", x => toOptList(x.companionad)),
        list(int).optional[OpenRTB.Audio]("api", x => toOptList(x.api)),
        list(int).optional[OpenRTB.Audio]("companiontype", x => toOptList(x.companiontype)),
        int.optional[OpenRTB.Audio]("maxseq", _.maxseq),
        int.optional[OpenRTB.Audio]("feed", _.feed),
        int.optional[OpenRTB.Audio]("stitched", _.stitched),
        int.optional[OpenRTB.Audio]("nvol", _.nvol)
      ) { (mimes, minduration, maxduration, protocols, startdelay, sequence, battr, maxextended, minbitrate, maxbitrate,
        delivery, companionad, api, companiontype, maxseq, feed, stitched, nvol) =>
        OpenRTB.Audio(mimes.getOrElse(Nil), minduration, maxduration, protocols.getOrElse(Nil), startdelay, sequence,
          battr.getOrElse(Nil), maxextended, minbitrate, maxbitrate, delivery.getOrElse(Nil),
          companionad.getOrElse(Nil), api.getOrElse(Nil), companiontype.getOrElse(Nil), maxseq, feed, stitched, nvol)
      }
    val nativeSchema: Schema[OpenRTB.Native] =
      struct(
        string.required[OpenRTB.Native]("request", _.request),
        string.optional[OpenRTB.Native]("ver", _.ver),
        list(int).optional[OpenRTB.Native]("api", x => toOptList(x.api)),
        list(int).optional[OpenRTB.Native]("battr", x => toOptList(x.battr))
      ) { (request, ver, api, battr) =>
        OpenRTB.Native(request, ver, api.getOrElse(Nil), battr.getOrElse(Nil))
      }
    val dealSchema: Schema[OpenRTB.Deal] =
      struct(
        string.required[OpenRTB.Deal]("id", _.id),
        double.optional[OpenRTB.Deal]("bidfloor", x => toOpt(x.bidfloor, 0.0)),
        string.optional[OpenRTB.Deal]("bidfloorcur", x => toOpt(x.bidfloorcur, "USD")),
        int.optional[OpenRTB.Deal]("at", _.at),
        list(string).optional[OpenRTB.Deal]("wseat", x => toOptList(x.wseat)),
        list(string).optional[OpenRTB.Deal]("wadomain", x => toOptList(x.wadomain))
      ) { (id, bidfloor, bidfloorcur, at, wseat, wadomain) =>
        OpenRTB.Deal(id, bidfloor.getOrElse(0.0), bidfloorcur.getOrElse("USD"), at, wseat.getOrElse(Nil),
          wadomain.getOrElse(Nil))
      }
    val pmpSchema: Schema[OpenRTB.Pmp] =
      struct(
        int.optional[OpenRTB.Pmp]("private_auction", x => toOpt(x.private_auction, 0)),
        list(dealSchema).optional[OpenRTB.Pmp]("deals", x => toOptList(x.deals)),
      ) { (private_auction, deals) =>
        OpenRTB.Pmp(private_auction.getOrElse(0), deals.getOrElse(Nil))
      }
    val impSchema: Schema[OpenRTB.Imp] =
      struct(
        string.required[OpenRTB.Imp]("id", _.id),
        list(metricSchema).optional[OpenRTB.Imp]("metric", x => toOptList(x.metric)),
        bannerSchema.optional[OpenRTB.Imp]("banner", _.banner),
        videoSchema.optional[OpenRTB.Imp]("video", _.video),
        audioSchema.optional[OpenRTB.Imp]("audio", _.audio),
        nativeSchema.optional[OpenRTB.Imp]("native", _.native),
        pmpSchema.optional[OpenRTB.Imp]("pmp", _.pmp),
        string.optional[OpenRTB.Imp]("displaymanager", _.displaymanager),
        string.optional[OpenRTB.Imp]("displaymanagerver", _.displaymanagerver),
        int.optional[OpenRTB.Imp]("instl", x => toOpt(x.instl, 0)),
        string.optional[OpenRTB.Imp]("tagid", _.tagid),
        double.optional[OpenRTB.Imp]("bidfloor", x => toOpt(x.bidfloor, 0.0)),
        string.optional[OpenRTB.Imp]("bidfloorcur", x => toOpt(x.bidfloorcur, "USD")),
        int.optional[OpenRTB.Imp]("clickbrowser", _.clickbrowser),
        int.optional[OpenRTB.Imp]("secure", x => toOpt(x.secure, 0)),
        list(string).optional[OpenRTB.Imp]("iframebuster", x => toOptList(x.iframebuster)),
        int.optional[OpenRTB.Imp]("exp", _.exp),
      ) { (id, metric, banner, video, audio, native, pmp, displaymanager, displaymanagerver, instl, tagid, bidfloor,
           bidfloorcur, clickbrowser, secure, iframebuster, exp) =>
        OpenRTB.Imp(id, metric.getOrElse(Nil), banner, video, audio, native, pmp, displaymanager, displaymanagerver,
          instl.getOrElse(0), tagid, bidfloor.getOrElse(0.0), bidfloorcur.getOrElse("USD"), clickbrowser,
          secure.getOrElse(0), iframebuster.getOrElse(Nil), exp)
      }
    val publisherSchema: Schema[OpenRTB.Publisher] =
      struct(
        string.optional[OpenRTB.Publisher]("id", _.id),
        string.optional[OpenRTB.Publisher]("name", _.name),
        list(string).optional[OpenRTB.Publisher]("cat", x => toOptList(x.cat)),
        string.optional[OpenRTB.Publisher]("domain", _.domain),
      ) { (id, name, cat, domain) =>
        OpenRTB.Publisher(id, name, cat.getOrElse(Nil), domain)
      }
    val producerSchema: Schema[OpenRTB.Producer] =
      struct(
        string.optional[OpenRTB.Producer]("id", _.id),
        string.optional[OpenRTB.Producer]("name", _.name),
        list(string).optional[OpenRTB.Producer]("cat", x => toOptList(x.cat)),
        string.optional[OpenRTB.Producer]("domain", _.domain),
      ) { (id, name, cat, domain) =>
        OpenRTB.Producer(id, name, cat.getOrElse(Nil), domain)
      }
    val segmentSchema: Schema[OpenRTB.Segment] =
      struct(
        string.optional[OpenRTB.Segment]("id", _.id),
        string.optional[OpenRTB.Segment]("name", _.name),
        string.optional[OpenRTB.Segment]("value", _.value),
      )(OpenRTB.Segment.apply)
    val dataSchema: Schema[OpenRTB.Data] =
      struct(
        string.optional[OpenRTB.Data]("id", _.id),
        string.optional[OpenRTB.Data]("name", _.name),
        list(segmentSchema).optional[OpenRTB.Data]("segment", x => toOptList(x.segment)),
      ) { (id, name, segment) =>
        OpenRTB.Data(id, name, segment.getOrElse(Nil))
      }
    val contentSchema: Schema[OpenRTB.Content] =
      struct.genericArity(
        string.optional[OpenRTB.Content]("id", _.id),
        int.optional[OpenRTB.Content]("episode", _.episode),
        string.optional[OpenRTB.Content]("title", _.title),
        string.optional[OpenRTB.Content]("series", _.series),
        string.optional[OpenRTB.Content]("season", _.season),
        string.optional[OpenRTB.Content]("artist", _.artist),
        string.optional[OpenRTB.Content]("genre", _.genre),
        string.optional[OpenRTB.Content]("album", _.album),
        string.optional[OpenRTB.Content]("isrc", _.isrc),
        producerSchema.optional[OpenRTB.Content]("producer", _.producer),
        string.optional[OpenRTB.Content]("url", _.url),
        list(string).optional[OpenRTB.Content]("cat", x => toOptList(x.cat)),
        int.optional[OpenRTB.Content]("prodq", _.prodq),
        int.optional[OpenRTB.Content]("videoquality", _.videoquality),
        int.optional[OpenRTB.Content]("context", _.context),
        string.optional[OpenRTB.Content]("contentrating", _.contentrating),
        string.optional[OpenRTB.Content]("userrating", _.userrating),
        int.optional[OpenRTB.Content]("qagmediarating", _.qagmediarating),
        string.optional[OpenRTB.Content]("keywords", _.keywords),
        int.optional[OpenRTB.Content]("livestream", _.livestream),
        int.optional[OpenRTB.Content]("sourcerelationship", _.sourcerelationship),
        int.optional[OpenRTB.Content]("len", _.len),
        string.optional[OpenRTB.Content]("language", _.language),
        int.optional[OpenRTB.Content]("embeddable", _.embeddable),
        dataSchema.optional[OpenRTB.Content]("data", _.data)
      ) { ps: IndexedSeq[Any] =>
        OpenRTB.Content(
          ps(0).asInstanceOf[Option[String]],
          ps(1).asInstanceOf[Option[Int]],
          ps(2).asInstanceOf[Option[String]],
          ps(3).asInstanceOf[Option[String]],
          ps(4).asInstanceOf[Option[String]],
          ps(5).asInstanceOf[Option[String]],
          ps(6).asInstanceOf[Option[String]],
          ps(7).asInstanceOf[Option[String]],
          ps(8).asInstanceOf[Option[String]],
          ps(9).asInstanceOf[Option[OpenRTB.Producer]],
          ps(10).asInstanceOf[Option[String]],
          ps(11).asInstanceOf[Option[List[String]]].getOrElse(Nil),
          ps(12).asInstanceOf[Option[Int]],
          ps(13).asInstanceOf[Option[Int]],
          ps(14).asInstanceOf[Option[Int]],
          ps(15).asInstanceOf[Option[String]],
          ps(16).asInstanceOf[Option[String]],
          ps(17).asInstanceOf[Option[Int]],
          ps(18).asInstanceOf[Option[String]],
          ps(19).asInstanceOf[Option[Int]],
          ps(20).asInstanceOf[Option[Int]],
          ps(21).asInstanceOf[Option[Int]],
          ps(22).asInstanceOf[Option[String]],
          ps(23).asInstanceOf[Option[Int]],
          ps(22).asInstanceOf[Option[OpenRTB.Data]]
        )
      }
    val siteSchema: Schema[OpenRTB.Site] =
      struct(
        string.optional[OpenRTB.Site]("id", _.id),
        string.optional[OpenRTB.Site]("name", _.name),
        string.optional[OpenRTB.Site]("domain", _.domain),
        list(string).optional[OpenRTB.Site]("cat", x => toOptList(x.cat)),
        list(string).optional[OpenRTB.Site]("sectioncat", x => toOptList(x.sectioncat)),
        list(string).optional[OpenRTB.Site]("pagecat", x => toOptList(x.pagecat)),
        string.optional[OpenRTB.Site]("page", _.page),
        string.optional[OpenRTB.Site]("ref", _.ref),
        string.optional[OpenRTB.Site]("search", _.search),
        int.optional[OpenRTB.Site]("mobile", _.mobile),
        int.optional[OpenRTB.Site]("privacypolicy", _.privacypolicy),
        publisherSchema.optional[OpenRTB.Site]("publisher", _.publisher),
        contentSchema.optional[OpenRTB.Site]("content", _.content),
        string.optional[OpenRTB.Site]("keywords", _.keywords)
      ) { (id, name, domain, cat, sectioncat, pagecat, page, ref, search, mobile, privacypolicy, publisher, content,
        keywords) =>
        OpenRTB.Site(id, name, domain, cat.getOrElse(Nil), sectioncat.getOrElse(Nil), pagecat.getOrElse(Nil), page, ref,
          search, mobile, privacypolicy, publisher, content, keywords)
      }
    val appSchema: Schema[OpenRTB.App] =
      struct(
        string.optional[OpenRTB.App]("id", _.id),
        string.optional[OpenRTB.App]("name", _.name),
        string.optional[OpenRTB.App]("bundle", _.bundle),
        string.optional[OpenRTB.App]("domain", _.domain),
        string.optional[OpenRTB.App]("storeurl", _.storeurl),
        list(string).optional[OpenRTB.App]("cat", x => toOptList(x.cat)),
        list(string).optional[OpenRTB.App]("sectioncat", x => toOptList(x.sectioncat)),
        list(string).optional[OpenRTB.App]("pagecat", x => toOptList(x.pagecat)),
        string.optional[OpenRTB.App]("ver", _.ver),
        int.optional[OpenRTB.App]("privacypolicy", _.privacypolicy),
        int.optional[OpenRTB.App]("paid", _.paid),
        publisherSchema.optional[OpenRTB.App]("publisher", _.publisher),
        contentSchema.optional[OpenRTB.App]("content", _.content),
        string.optional[OpenRTB.App]("keywords", _.keywords)
      ) { (id, name, bundle, domain, storeurl, cat, sectioncat, pagecat, ver, privacypolicy, paid, publisher, content,
        keywords) =>
        OpenRTB.App(id, name, bundle, domain, storeurl, cat.getOrElse(Nil), sectioncat.getOrElse(Nil),
          pagecat.getOrElse(Nil), ver, privacypolicy, paid, publisher, content, keywords)
      }
    val geoSchema: Schema[OpenRTB.Geo] =
      struct(
        double.optional[OpenRTB.Geo]("lat", _.lat),
        double.optional[OpenRTB.Geo]("lon", _.lon),
        int.optional[OpenRTB.Geo]("type", _.`type`),
        int.optional[OpenRTB.Geo]("accuracy", _.accuracy),
        int.optional[OpenRTB.Geo]("lastfix", _.lastfix),
        int.optional[OpenRTB.Geo]("ipservice", _.ipservice),
        string.optional[OpenRTB.Geo]("country", _.country),
        string.optional[OpenRTB.Geo]("region", _.region),
        string.optional[OpenRTB.Geo]("regionfips104", _.regionfips104),
        string.optional[OpenRTB.Geo]("metro", _.metro),
        string.optional[OpenRTB.Geo]("city", _.city),
        string.optional[OpenRTB.Geo]("zip", _.zip),
        string.optional[OpenRTB.Geo]("utcoffset", _.utcoffset)
      )(OpenRTB.Geo.apply)
    val deviceSchema: Schema[OpenRTB.Device] =
      struct.genericArity(
        string.optional[OpenRTB.Device]("ua", _.ua),
        geoSchema.optional[OpenRTB.Device]("geo", _.geo),
        int.optional[OpenRTB.Device]("dnt", _.dnt),
        int.optional[OpenRTB.Device]("lmt", _.lmt),
        string.optional[OpenRTB.Device]("ip", _.ip),
        int.optional[OpenRTB.Device]("devicetype", _.devicetype),
        string.optional[OpenRTB.Device]("make", _.make),
        string.optional[OpenRTB.Device]("model", _.model),
        string.optional[OpenRTB.Device]("os", _.os),
        string.optional[OpenRTB.Device]("osv", _.osv),
        string.optional[OpenRTB.Device]("hwv", _.hwv),
        int.optional[OpenRTB.Device]("h", _.h),
        int.optional[OpenRTB.Device]("w", _.w),
        int.optional[OpenRTB.Device]("ppi", _.ppi),
        double.optional[OpenRTB.Device]("pxratio", _.pxratio),
        int.optional[OpenRTB.Device]("js", _.js),
        int.optional[OpenRTB.Device]("geofetch", _.geofetch),
        string.optional[OpenRTB.Device]("flashver", _.flashver),
        string.optional[OpenRTB.Device]("language", _.language),
        string.optional[OpenRTB.Device]("carrier", _.carrier),
        string.optional[OpenRTB.Device]("mccmnc", _.mccmnc),
        int.optional[OpenRTB.Device]("connectiontype", _.connectiontype),
        string.optional[OpenRTB.Device]("ifa", _.ifa),
        string.optional[OpenRTB.Device]("didsha1", _.didsha1),
        string.optional[OpenRTB.Device]("didmd5", _.didmd5),
        string.optional[OpenRTB.Device]("dpidsha1", _.dpidsha1),
        string.optional[OpenRTB.Device]("dpidmd5", _.dpidmd5),
        string.optional[OpenRTB.Device]("macsha1", _.macsha1),
        string.optional[OpenRTB.Device]("macmd5", _.macmd5)
      ) { ps: IndexedSeq[Any] =>
        OpenRTB.Device(
          ps(0).asInstanceOf[Option[String]],
          ps(1).asInstanceOf[Option[OpenRTB.Geo]],
          ps(2).asInstanceOf[Option[Int]],
          ps(3).asInstanceOf[Option[Int]],
          ps(4).asInstanceOf[Option[String]],
          ps(5).asInstanceOf[Option[Int]],
          ps(6).asInstanceOf[Option[String]],
          ps(7).asInstanceOf[Option[String]],
          ps(8).asInstanceOf[Option[String]],
          ps(9).asInstanceOf[Option[String]],
          ps(10).asInstanceOf[Option[String]],
          ps(11).asInstanceOf[Option[Int]],
          ps(12).asInstanceOf[Option[Int]],
          ps(13).asInstanceOf[Option[Int]],
          ps(14).asInstanceOf[Option[Double]],
          ps(15).asInstanceOf[Option[Int]],
          ps(16).asInstanceOf[Option[Int]],
          ps(17).asInstanceOf[Option[String]],
          ps(18).asInstanceOf[Option[String]],
          ps(19).asInstanceOf[Option[String]],
          ps(20).asInstanceOf[Option[String]],
          ps(21).asInstanceOf[Option[Int]],
          ps(22).asInstanceOf[Option[String]],
          ps(23).asInstanceOf[Option[String]],
          ps(24).asInstanceOf[Option[String]],
          ps(25).asInstanceOf[Option[String]],
          ps(26).asInstanceOf[Option[String]],
          ps(27).asInstanceOf[Option[String]],
          ps(28).asInstanceOf[Option[String]]
        )
      }
    val userSchema: Schema[OpenRTB.User] =
      struct(
        string.optional[OpenRTB.User]("id", _.id),
        string.optional[OpenRTB.User]("buyeruid", _.buyeruid),
        int.optional[OpenRTB.User]("yob", _.yob),
        string.optional[OpenRTB.User]("gender", _.gender),
        string.optional[OpenRTB.User]("keywords", _.keywords),
        string.optional[OpenRTB.User]("customdata", _.customdata),
        geoSchema.optional[OpenRTB.User]("geo", _.geo),
        dataSchema.optional[OpenRTB.User]("data", _.data),
      )(OpenRTB.User.apply)
    val sourceSchema: Schema[OpenRTB.Source] =
      struct(
        int.optional[OpenRTB.Source]("fd", _.fd),
        string.optional[OpenRTB.Source]("tid", _.tid),
        string.optional[OpenRTB.Source]("pchain", _.pchain),
      )(OpenRTB.Source.apply)
    val reqsSchema: Schema[OpenRTB.Reqs] =
      struct(
        int.required[OpenRTB.Reqs]("coppa", _.coppa),
      )(OpenRTB.Reqs.apply)
    struct(
      string.required[OpenRTB.BidRequest]("id", _.id),
      list(impSchema).optional[OpenRTB.BidRequest]("imp", x => toOptList(x.imp)),
      siteSchema.optional[OpenRTB.BidRequest]("site", _.site),
      appSchema.optional[OpenRTB.BidRequest]("app", _.app),
      deviceSchema.optional[OpenRTB.BidRequest]("device", _.device),
      userSchema.optional[OpenRTB.BidRequest]("user", _.user),
      int.optional[OpenRTB.BidRequest]("test", x => toOpt(x.test, 0)),
      int.optional[OpenRTB.BidRequest]("at", x => toOpt(x.at, 2)),
      int.optional[OpenRTB.BidRequest]("tmax", _.tmax),
      list(string).optional[OpenRTB.BidRequest]("wset", x => toOptList(x.wset)),
      list(string).optional[OpenRTB.BidRequest]("bset", x => toOptList(x.bset)),
      int.optional[OpenRTB.BidRequest]("allimps", x => toOpt(x.allimps, 0)),
      list(string).optional[OpenRTB.BidRequest]("cur", x => toOptList(x.cur)),
      list(string).optional[OpenRTB.BidRequest]("wlang", x => toOptList(x.wlang)),
      list(string).optional[OpenRTB.BidRequest]("bcat", x => toOptList(x.bcat)),
      list(string).optional[OpenRTB.BidRequest]("badv", x => toOptList(x.badv)),
      list(string).optional[OpenRTB.BidRequest]("bapp", x => toOptList(x.bapp)),
      sourceSchema.optional[OpenRTB.BidRequest]("source", _.source),
      reqsSchema.optional[OpenRTB.BidRequest]("reqs", _.reqs),
    ) { (id, imp, site, app, device, user, test, at, tmax, wset, bset, allimps, cur, wlang, bcat, badv, bapp, source, reqs) =>
      OpenRTB.BidRequest(id, imp.getOrElse(Nil), site, app, device, user, test.getOrElse(0), at.getOrElse(2), tmax,
        wset.getOrElse(Nil), bset.getOrElse(Nil), allimps.getOrElse(0), cur.getOrElse(Nil), wlang.getOrElse(Nil),
        bcat.getOrElse(Nil), badv.getOrElse(Nil), bapp.getOrElse(Nil), source, reqs)
    }
  }
  implicit val primitivesJCodec: JCodec[Primitives] = JCodec.deriveJCodecFromSchema(struct(
    byte.required[Primitives]("b", _.b),
    short.required[Primitives]("s", _.s),
    int.required[Primitives]("i", _.i),
    long.required[Primitives]("l", _.l),
    boolean.required[Primitives]("bl", _.bl),
    string.required[Primitives]("ch", _.ch.toString),
    double.required[Primitives]("dbl", _.dbl),
    float.required[Primitives]("f", _.f)
  )((b, s, i, l, bl, st, dbl, f) => Primitives(b, s, i, l, bl, {
    if (st.length == 1) st.charAt(0)
    else sys.error("illegal char")
  }, dbl, f)))
  implicit val setOfIntsJCodec: JCodec[Set[Int]] = JCodec.deriveJCodecFromSchema(set(int))
  val stringJCodec: JCodec[String] = JCodec.deriveJCodecFromSchema(string)
  implicit val twitterAPIJCodec: JCodec[Seq[TwitterAPI.Tweet]] = JCodec.deriveJCodecFromSchema({
    val urlsSchema: Schema[TwitterAPI.Urls] = struct(
      string.required[TwitterAPI.Urls]("url", _.url),
      string.required[TwitterAPI.Urls]("expanded_url", _.expanded_url),
      string.required[TwitterAPI.Urls]("display_url", _.display_url),
      list(int).optional[TwitterAPI.Urls]("indices", xs => toOptList(xs.indices.toList))
    ) { (url, expanded_url, display_url, indices) =>
      TwitterAPI.Urls(url, expanded_url, display_url, indices.getOrElse(Nil))
    }
    val urlSchema: Schema[TwitterAPI.Url] = struct(
      list(urlsSchema).optional[TwitterAPI.Url]("urls", xs => toOptList(xs.urls.toList))
    )(xs => TwitterAPI.Url(xs.getOrElse(Nil)))
    val userEntitiesSchema: Schema[TwitterAPI.UserEntities] = struct(
      urlSchema.required[TwitterAPI.UserEntities]("url", _.url),
      urlSchema.required[TwitterAPI.UserEntities]("description", _.description),
    )(TwitterAPI.UserEntities.apply)
    val userMentionsSchema: Schema[TwitterAPI.UserMentions] = struct(
      string.required[TwitterAPI.UserMentions]("screen_name", _.screen_name),
      string.required[TwitterAPI.UserMentions]("name", _.name),
      long.required[TwitterAPI.UserMentions]("id", _.id),
      string.required[TwitterAPI.UserMentions]("id_str", _.id_str),
      list(int).optional[TwitterAPI.UserMentions]("indices", xs => toOptList(xs.indices.toList))
    ) { (screen_name, name, id, id_str, indices) =>
      TwitterAPI.UserMentions(screen_name, name, id, id_str, indices.getOrElse(Nil))
    }
    val entitiesSchema: Schema[TwitterAPI.Entities] = struct(
      list(string).optional[TwitterAPI.Entities]("hashtags", xs => toOptList(xs.hashtags.toList)),
      list(string).optional[TwitterAPI.Entities]("symbols", xs => toOptList(xs.symbols.toList)),
      list(userMentionsSchema).optional[TwitterAPI.Entities]("user_mentions", xs => toOptList(xs.user_mentions.toList)),
      list(urlsSchema).optional[TwitterAPI.Entities]("urls", xs => toOptList(xs.urls.toList))
    ) { (hashtags, symbols, user_mentions, urls) =>
      TwitterAPI.Entities(hashtags.getOrElse(Nil), symbols.getOrElse(Nil), user_mentions.getOrElse(Nil), urls.getOrElse(Nil))
    }
    val userSchema: Schema[TwitterAPI.User] = struct.genericArity(
      long.required[TwitterAPI.User]("id", _.id),
      string.required[TwitterAPI.User]("id_str", _.id_str),
      string.required[TwitterAPI.User]("name", _.name),
      string.required[TwitterAPI.User]("screen_name", _.screen_name),
      string.required[TwitterAPI.User]("location", _.location),
      string.required[TwitterAPI.User]("description", _.description),
      string.required[TwitterAPI.User]("url", _.url),
      userEntitiesSchema.required[TwitterAPI.User]("entities", _.entities),
      boolean.required[TwitterAPI.User]("protected", _.`protected`),
      int.required[TwitterAPI.User]("followers_count", _.followers_count),
      int.required[TwitterAPI.User]("friends_count", _.friends_count),
      int.required[TwitterAPI.User]("listed_count", _.listed_count),
      string.required[TwitterAPI.User]("created_at", _.created_at),
      int.required[TwitterAPI.User]("favourites_count", _.favourites_count),
      int.required[TwitterAPI.User]("utc_offset", _.utc_offset),
      string.required[TwitterAPI.User]("time_zone", _.time_zone),
      boolean.required[TwitterAPI.User]("geo_enabled", _.geo_enabled),
      boolean.required[TwitterAPI.User]("verified", _.verified),
      int.required[TwitterAPI.User]("statuses_count", _.statuses_count),
      string.required[TwitterAPI.User]("lang", _.lang),
      boolean.required[TwitterAPI.User]("contributors_enabled", _.contributors_enabled),
      boolean.required[TwitterAPI.User]("is_translator", _.is_translator),
      boolean.required[TwitterAPI.User]("is_translation_enabled", _.is_translation_enabled),
      string.required[TwitterAPI.User]("profile_background_color", _.profile_background_color),
      string.required[TwitterAPI.User]("profile_background_image_url", _.profile_background_image_url),
      string.required[TwitterAPI.User]("profile_background_image_url_https", _.profile_background_image_url_https),
      boolean.required[TwitterAPI.User]("profile_background_tile", _.profile_background_tile),
      string.required[TwitterAPI.User]("profile_image_url", _.profile_image_url),
      string.required[TwitterAPI.User]("profile_image_url_https", _.profile_image_url_https),
      string.required[TwitterAPI.User]("profile_banner_url", _.profile_banner_url),
      string.required[TwitterAPI.User]("profile_link_color", _.profile_link_color),
      string.required[TwitterAPI.User]("profile_sidebar_border_color", _.profile_sidebar_border_color),
      string.required[TwitterAPI.User]("profile_sidebar_fill_color", _.profile_sidebar_fill_color),
      string.required[TwitterAPI.User]("profile_text_color", _.profile_text_color),
      boolean.required[TwitterAPI.User]("profile_use_background_image", _.profile_use_background_image),
      boolean.required[TwitterAPI.User]("has_extended_profile", _.has_extended_profile),
      boolean.required[TwitterAPI.User]("default_profile", _.default_profile),
      boolean.required[TwitterAPI.User]("default_profile_image", _.default_profile_image),
      boolean.required[TwitterAPI.User]("following", _.following),
      boolean.required[TwitterAPI.User]("follow_request_sent", _.follow_request_sent),
      boolean.required[TwitterAPI.User]("notifications", _.notifications),
      string.required[TwitterAPI.User]("translator_type", _.translator_type),
    ) { ps: IndexedSeq[Any] =>
      TwitterAPI.User(
        ps(0).asInstanceOf[Long],
        ps(1).asInstanceOf[String],
        ps(2).asInstanceOf[String],
        ps(3).asInstanceOf[String],
        ps(4).asInstanceOf[String],
        ps(5).asInstanceOf[String],
        ps(6).asInstanceOf[String],
        ps(7).asInstanceOf[TwitterAPI.UserEntities],
        ps(8).asInstanceOf[Boolean],
        ps(9).asInstanceOf[Int],
        ps(10).asInstanceOf[Int],
        ps(11).asInstanceOf[Int],
        ps(12).asInstanceOf[String],
        ps(13).asInstanceOf[Int],
        ps(14).asInstanceOf[Int],
        ps(15).asInstanceOf[String],
        ps(16).asInstanceOf[Boolean],
        ps(17).asInstanceOf[Boolean],
        ps(18).asInstanceOf[Int],
        ps(19).asInstanceOf[String],
        ps(20).asInstanceOf[Boolean],
        ps(21).asInstanceOf[Boolean],
        ps(22).asInstanceOf[Boolean],
        ps(23).asInstanceOf[String],
        ps(24).asInstanceOf[String],
        ps(25).asInstanceOf[String],
        ps(26).asInstanceOf[Boolean],
        ps(27).asInstanceOf[String],
        ps(28).asInstanceOf[String],
        ps(29).asInstanceOf[String],
        ps(30).asInstanceOf[String],
        ps(31).asInstanceOf[String],
        ps(32).asInstanceOf[String],
        ps(33).asInstanceOf[String],
        ps(34).asInstanceOf[Boolean],
        ps(35).asInstanceOf[Boolean],
        ps(36).asInstanceOf[Boolean],
        ps(37).asInstanceOf[Boolean],
        ps(38).asInstanceOf[Boolean],
        ps(39).asInstanceOf[Boolean],
        ps(40).asInstanceOf[Boolean],
        ps(41).asInstanceOf[String]
      )
    }
    val retweetedStatusSchema: Schema[TwitterAPI.RetweetedStatus] = struct.genericArity(
      string.required[TwitterAPI.RetweetedStatus]("created_at", _.created_at),
      long.required[TwitterAPI.RetweetedStatus]("id", _.id),
      string.required[TwitterAPI.RetweetedStatus]("id_str", _.id_str),
      string.required[TwitterAPI.RetweetedStatus]("text", _.text),
      boolean.required[TwitterAPI.RetweetedStatus]("truncated", _.truncated),
      entitiesSchema.required[TwitterAPI.RetweetedStatus]("entities", _.entities),
      string.required[TwitterAPI.RetweetedStatus]("source", _.source),
      string.optional[TwitterAPI.RetweetedStatus]("in_reply_to_status_id", _.in_reply_to_status_id),
      string.optional[TwitterAPI.RetweetedStatus]("in_reply_to_status_id_str", _.in_reply_to_status_id_str),
      string.optional[TwitterAPI.RetweetedStatus]("in_reply_to_user_id", _.in_reply_to_user_id),
      string.optional[TwitterAPI.RetweetedStatus]("in_reply_to_user_id_str", _.in_reply_to_user_id_str),
      string.optional[TwitterAPI.RetweetedStatus]("in_reply_to_screen_name", _.in_reply_to_screen_name),
      userSchema.required[TwitterAPI.RetweetedStatus]("user", _.user),
      string.optional[TwitterAPI.RetweetedStatus]("geo", _.geo),
      string.optional[TwitterAPI.RetweetedStatus]("coordinates", _.coordinates),
      string.optional[TwitterAPI.RetweetedStatus]("place", _.place),
      string.optional[TwitterAPI.RetweetedStatus]("contributors", _.contributors),
      boolean.required[TwitterAPI.RetweetedStatus]("is_quote_status", _.is_quote_status),
      int.required[TwitterAPI.RetweetedStatus]("retweet_count", _.retweet_count),
      int.required[TwitterAPI.RetweetedStatus]("favorite_count", _.favorite_count),
      boolean.required[TwitterAPI.RetweetedStatus]("favorited", _.favorited),
      boolean.required[TwitterAPI.RetweetedStatus]("retweeted", _.retweeted),
      boolean.required[TwitterAPI.RetweetedStatus]("possibly_sensitive", _.possibly_sensitive),
      string.required[TwitterAPI.RetweetedStatus]("lang", _.lang)
    ) { ps: IndexedSeq[Any] =>
      TwitterAPI.RetweetedStatus(
        ps(0).asInstanceOf[String],
        ps(1).asInstanceOf[Long],
        ps(2).asInstanceOf[String],
        ps(3).asInstanceOf[String],
        ps(4).asInstanceOf[Boolean],
        ps(5).asInstanceOf[TwitterAPI.Entities],
        ps(6).asInstanceOf[String],
        ps(7).asInstanceOf[Option[String]],
        ps(8).asInstanceOf[Option[String]],
        ps(9).asInstanceOf[Option[String]],
        ps(10).asInstanceOf[Option[String]],
        ps(11).asInstanceOf[Option[String]],
        ps(12).asInstanceOf[TwitterAPI.User],
        ps(13).asInstanceOf[Option[String]],
        ps(14).asInstanceOf[Option[String]],
        ps(15).asInstanceOf[Option[String]],
        ps(16).asInstanceOf[Option[String]],
        ps(17).asInstanceOf[Boolean],
        ps(18).asInstanceOf[Int],
        ps(19).asInstanceOf[Int],
        ps(20).asInstanceOf[Boolean],
        ps(21).asInstanceOf[Boolean],
        ps(22).asInstanceOf[Boolean],
        ps(23).asInstanceOf[String]
      )
    }
    val tweetSchema: Schema[TwitterAPI.Tweet] = struct.genericArity(
      string.required[TwitterAPI.Tweet]("created_at", _.created_at),
      long.required[TwitterAPI.Tweet]("id", _.id),
      string.required[TwitterAPI.Tweet]("id_str", _.id_str),
      string.required[TwitterAPI.Tweet]("text", _.text),
      boolean.required[TwitterAPI.Tweet]("truncated", _.truncated),
      entitiesSchema.required[TwitterAPI.Tweet]("entities", _.entities),
      string.required[TwitterAPI.Tweet]("source", _.source),
      string.optional[TwitterAPI.Tweet]("in_reply_to_status_id", _.in_reply_to_status_id),
      string.optional[TwitterAPI.Tweet]("in_reply_to_status_id_str", _.in_reply_to_status_id_str),
      string.optional[TwitterAPI.Tweet]("in_reply_to_user_id", _.in_reply_to_user_id),
      string.optional[TwitterAPI.Tweet]("in_reply_to_user_id_str", _.in_reply_to_user_id_str),
      string.optional[TwitterAPI.Tweet]("in_reply_to_screen_name", _.in_reply_to_screen_name),
      userSchema.required[TwitterAPI.Tweet]("user", _.user),
      string.optional[TwitterAPI.Tweet]("geo", _.geo),
      string.optional[TwitterAPI.Tweet]("coordinates", _.coordinates),
      string.optional[TwitterAPI.Tweet]("place", _.place),
      string.optional[TwitterAPI.Tweet]("contributors", _.contributors),
      retweetedStatusSchema.required[TwitterAPI.Tweet]("retweeted_status", _.retweeted_status),
      boolean.required[TwitterAPI.Tweet]("is_quote_status", _.is_quote_status),
      int.required[TwitterAPI.Tweet]("retweet_count", _.retweet_count),
      int.required[TwitterAPI.Tweet]("favorite_count", _.favorite_count),
      boolean.required[TwitterAPI.Tweet]("favorited", _.favorited),
      boolean.required[TwitterAPI.Tweet]("retweeted", _.retweeted),
      boolean.required[TwitterAPI.Tweet]("possibly_sensitive", _.possibly_sensitive),
      string.required[TwitterAPI.Tweet]("lang", _.lang)
    ) { ps: IndexedSeq[Any] =>
      TwitterAPI.Tweet(
        ps(0).asInstanceOf[String],
        ps(1).asInstanceOf[Long],
        ps(2).asInstanceOf[String],
        ps(3).asInstanceOf[String],
        ps(4).asInstanceOf[Boolean],
        ps(5).asInstanceOf[TwitterAPI.Entities],
        ps(6).asInstanceOf[String],
        ps(7).asInstanceOf[Option[String]],
        ps(8).asInstanceOf[Option[String]],
        ps(9).asInstanceOf[Option[String]],
        ps(10).asInstanceOf[Option[String]],
        ps(11).asInstanceOf[Option[String]],
        ps(12).asInstanceOf[TwitterAPI.User],
        ps(13).asInstanceOf[Option[String]],
        ps(14).asInstanceOf[Option[String]],
        ps(15).asInstanceOf[Option[String]],
        ps(16).asInstanceOf[Option[String]],
        ps(17).asInstanceOf[TwitterAPI.RetweetedStatus],
        ps(18).asInstanceOf[Boolean],
        ps(19).asInstanceOf[Int],
        ps(20).asInstanceOf[Int],
        ps(21).asInstanceOf[Boolean],
        ps(22).asInstanceOf[Boolean],
        ps(23).asInstanceOf[Boolean],
        ps(24).asInstanceOf[String]
      )
    }
    bijection[List[TwitterAPI.Tweet], Seq[TwitterAPI.Tweet]](list(tweetSchema), _.toSeq, _.toList)
  })
  implicit val vectorOfBooleansJCodec: JCodec[Vector[Boolean]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Boolean], Vector[Boolean]](list(boolean), _.toVector, _.toList))
}
