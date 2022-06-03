package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON.GeoJSON
import smithy4s.http.json._
import smithy4s.{ByteArray, Schema, Timestamp}
import smithy4s.api.Discriminated
import smithy4s.schema.Schema._
import java.time.Instant
import java.util.UUID

object Smithy4sCodecs {
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
  val base64JCodec: JCodec[Array[Byte]] =
    JCodec.deriveJCodecFromSchema(bijection[ByteArray, Array[Byte]](bytes, _.array, ByteArray.apply))
  val bigDecimalJCodec: JCodec[BigDecimal] = JCodec.deriveJCodecFromSchema(bigdecimal)
  val bigIntJCodec: JCodec[BigInt] = JCodec.deriveJCodecFromSchema(bigint)
  implicit val extractFieldsJCodec: JCodec[ExtractFields] = JCodec.deriveJCodecFromSchema(struct(
    string.required[ExtractFields]("s", _.s),
    int.required[ExtractFields]("i", _.i)
  )(ExtractFields.apply))
  implicit val geoJsonJCodec: JCodec[GeoJSON] = JCodec.deriveJCodecFromSchema {
    def toIndexedSeqOfIndexedSeq[A](xs: List[List[A]]): IndexedSeq[IndexedSeq[A]] =
      xs.foldLeft(IndexedSeq.newBuilder[IndexedSeq[A]])((a, x) => a.addOne(x.toIndexedSeq)).result()

    def toListOfList[A](xs: IndexedSeq[IndexedSeq[A]]): List[List[A]] =
      xs.foldLeft(List.newBuilder[List[A]])((a, x) => a.addOne(x.toList)).result()

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
    val featureAlt = featureSchema.oneOf[GeoJSON]("Feature")
    val featureCollectionAlt = featureCollectionSchema.oneOf[GeoJSON]("FeatureCollection")
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
  implicit val vectorOfBooleansJCodec: JCodec[Vector[Boolean]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Boolean], Vector[Boolean]](list(boolean), _.toVector, _.toList))
}
