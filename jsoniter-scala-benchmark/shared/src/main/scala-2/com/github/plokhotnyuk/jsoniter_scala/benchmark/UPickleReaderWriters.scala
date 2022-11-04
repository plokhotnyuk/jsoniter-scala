package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import upickle.AttributeTagged
import upickle.core.Visitor
import java.time._
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.ClassTag

object UPickleReaderWriters extends AttributeTagged {
  override val tagName: String = "type"
  implicit val (bigDecimalReader: Reader[BigDecimal], bigDecimalWriter: Writer[BigDecimal]) =
    (numReader(s => BigDecimal(s.toString)), numWriter[BigDecimal])
  implicit val (bigIntReader: Reader[BigInt], bigIntWriter: Writer[BigInt]) =
    (numReader(s => BigInt(s.toString)), numWriter[BigInt])
  implicit val doubleWriter: Writer[Double] = numWriter[Double]
  implicit val floatWriter: Writer[Float] = numWriter[Float]
  implicit val longWriter: Writer[Long] = numWriter[Long]
  implicit val adtReadWriter: ReadWriter[ADTBase] = ReadWriter.merge(macroRW[X], macroRW[Y], macroRW[Z])
  implicit val anyValsReadWriter: ReadWriter[AnyVals] = {
    implicit val v1: ReadWriter[ByteVal] = readwriter[Byte].bimap(_.a, ByteVal.apply)
    implicit val v2: ReadWriter[ShortVal] = readwriter[Short].bimap(_.a, ShortVal.apply)
    implicit val v3: ReadWriter[IntVal] = readwriter[Int].bimap(_.a, IntVal.apply)
    implicit val v4: ReadWriter[LongVal] = readwriter[Long].bimap(_.a, LongVal.apply)
    implicit val v5: ReadWriter[BooleanVal] = readwriter[Boolean].bimap(_.a, BooleanVal.apply)
    implicit val v6: ReadWriter[DoubleVal] = readwriter[Double].bimap(_.a, DoubleVal.apply)
    implicit val v7: ReadWriter[CharVal] = readwriter[Char].bimap(_.a, CharVal.apply)
    implicit val v8: ReadWriter[FloatVal] = readwriter[Float].bimap(_.a, FloatVal.apply)
    macroRW
  }
  val base64ReadWriter: ReadWriter[Array[Byte]] =
    readwriter[String].bimap(Base64.getEncoder.encodeToString, Base64.getDecoder.decode)
  implicit val extractFieldsReadWriter: ReadWriter[ExtractFields] = macroRW
  implicit val simpleGeometryReadWriter: ReadWriter[GeoJSON.SimpleGeometry] =
    ReadWriter.merge(macroRW[GeoJSON.Point], macroRW[GeoJSON.MultiPoint], macroRW[GeoJSON.LineString],
      macroRW[GeoJSON.MultiLineString], macroRW[GeoJSON.Polygon], macroRW[GeoJSON.MultiPolygon])
  implicit val geometryReadWriter: ReadWriter[GeoJSON.Geometry] =
    ReadWriter.merge(macroRW[GeoJSON.Point], macroRW[GeoJSON.MultiPoint], macroRW[GeoJSON.LineString],
      macroRW[GeoJSON.MultiLineString], macroRW[GeoJSON.Polygon], macroRW[GeoJSON.MultiPolygon],
      macroRW[GeoJSON.GeometryCollection])
  implicit val simpleGeoJsonReadWriter: ReadWriter[GeoJSON.SimpleGeoJSON] =
    ReadWriter.merge(macroRW[GeoJSON.Feature])
  implicit val geoJsonReadWriter: ReadWriter[GeoJSON.GeoJSON] =
    ReadWriter.merge(macroRW[GeoJSON.Feature], macroRW[GeoJSON.FeatureCollection])
  implicit val gitHubActionsAPIFromTos: ReadWriter[GitHubActionsAPI.Response] = {
    implicit val v1: ReadWriter[Boolean] =
      ReadWriter.join(strReader(x => java.lang.Boolean.parseBoolean(x.toString)), strWriter[Boolean])
    implicit val v2: ReadWriter[GitHubActionsAPI.Artifact] = macroRW
    macroRW
  }
  implicit val googleMapsAPIReadWriter: ReadWriter[GoogleMapsAPI.DistanceMatrix] = {
    implicit val v1: ReadWriter[GoogleMapsAPI.Value] = macroRW
    implicit val v2: ReadWriter[GoogleMapsAPI.Elements] = macroRW
    implicit val v3: ReadWriter[GoogleMapsAPI.Rows] = macroRW
    macroRW
  }
  implicit val nestedStructsReadWriter: ReadWriter[NestedStructs] = macroRW
  implicit val missingRequiredFieldsReadWriter: ReadWriter[MissingRequiredFields] = macroRW
  implicit val primitivesReadWriter: ReadWriter[Primitives] = macroRW
  implicit val (durationReader: Reader[Duration], durationWriter: Writer[Duration]) =
    (strReader(Duration.parse), strWriter[Duration])
  implicit val (instantReader: Reader[Instant], instantWriter: Writer[Instant]) =
    (strReader(Instant.parse), strWriter[Instant])
  implicit val (localDateReader: Reader[LocalDate], localDateWriter: Writer[LocalDate]) =
    (strReader(LocalDate.parse), strWriter[LocalDate])
  implicit val (localDateTimeReader: Reader[LocalDateTime], localDateTimeWriter: Writer[LocalDateTime]) =
    (strReader(LocalDateTime.parse), strWriter[LocalDateTime])
  implicit val (localTimeReader: Reader[LocalTime], localTimeWriter: Writer[LocalTime]) =
    (strReader(LocalTime.parse), strWriter[LocalTime])
  implicit val (monthDayReader: Reader[MonthDay], monthDayWriter: Writer[MonthDay]) =
    (strReader(MonthDay.parse), strWriter[MonthDay])
  implicit val (offsetDateTimeReader: Reader[OffsetDateTime], offsetDateTimeWriter: Writer[OffsetDateTime]) =
    (strReader(OffsetDateTime.parse), strWriter[OffsetDateTime])
  implicit val (offsetTimeReader: Reader[OffsetTime], offsetTimeWriter: Writer[OffsetTime]) =
    (strReader(OffsetTime.parse), strWriter[OffsetTime])
  implicit val openRTBReadWriter: ReadWriter[OpenRTB.BidRequest] = {
    implicit val v1: ReadWriter[OpenRTB.Segment] = macroRW
    implicit val v2: ReadWriter[OpenRTB.Format] = macroRW
    implicit val v3: ReadWriter[OpenRTB.Deal] = macroRW
    implicit val v4: ReadWriter[OpenRTB.Metric] = macroRW
    implicit val v5: ReadWriter[OpenRTB.Banner] = macroRW
    implicit val v6: ReadWriter[OpenRTB.Audio] = macroRW
    implicit val v7: ReadWriter[OpenRTB.Video] = macroRW
    implicit val v8: ReadWriter[OpenRTB.Native] = macroRW
    implicit val v9: ReadWriter[OpenRTB.Pmp] = macroRW
    implicit val v10: ReadWriter[OpenRTB.Producer] = macroRW
    implicit val v11: ReadWriter[OpenRTB.Data] = macroRW
    implicit val v12: ReadWriter[OpenRTB.Content] = macroRW
    implicit val v13: ReadWriter[OpenRTB.Publisher] = macroRW
    implicit val v14: ReadWriter[OpenRTB.Geo] = macroRW
    implicit val v15: ReadWriter[OpenRTB.Imp] = macroRW
    implicit val v16: ReadWriter[OpenRTB.Site] = macroRW
    implicit val v17: ReadWriter[OpenRTB.App] = macroRW
    implicit val v18: ReadWriter[OpenRTB.Device] = macroRW
    implicit val v19: ReadWriter[OpenRTB.User] = macroRW
    implicit val v20: ReadWriter[OpenRTB.Source] = macroRW
    implicit val v21: ReadWriter[OpenRTB.Reqs] = macroRW
    macroRW
  }
  implicit val (periodReader: Reader[Period], periodWriter: Writer[Period]) =
    (strReader(Period.parse), strWriter[Period])
  implicit val (suiteADTReader: Reader[SuitADT], suiteADTWriter: Writer[SuitADT]) = (strReader {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s => suite.getOrElse(s.toString, throw new IllegalArgumentException("SuitADT"))
  }, strWriter[SuitADT])
  implicit val (suitEnumReader: Reader[SuitEnum], suitEnumWriter: Writer[SuitEnum]) = (strReader[SuitEnum]({
    val ec = new ConcurrentHashMap[String, SuitEnum]
    (cs: CharSequence) => {
      val s = cs.toString
      var x = ec.get(s)
      if (x eq null) {
        x = SuitEnum.withName(s)
        ec.put(s, x)
      }
      x
    }
  }), strWriter[SuitEnum])
  implicit val (suitReader: Reader[Suit], suitWriter: Writer[Suit]) =
    (strReader(s => Suit.valueOf(s.toString)), strWriter[Suit])
  implicit val (yearReader: Reader[Year], yearWriter: Writer[Year]) =
    (strReader(Year.parse), strWriter[Year])
  implicit val (yearMonthReader: Reader[YearMonth], yearMonthWriter: Writer[YearMonth]) =
    (strReader(YearMonth.parse), strWriter[YearMonth])
  implicit val (zonedDateTimeReader: Reader[ZonedDateTime], zonedDateTimeWriter: Writer[ZonedDateTime]) =
    (strReader(ZonedDateTime.parse), strWriter[ZonedDateTime])
  implicit val (zonedIdReader: Reader[ZoneId], zonedIdWriter: Writer[ZoneId]) =
    (strReader(s => ZoneId.of(s.toString)), strWriter[ZoneId])
  implicit val (zonedOffsetReader: Reader[ZoneOffset], zonedOffsetWriter: Writer[ZoneOffset]) =
    (strReader(s => ZoneOffset.of(s.toString)), strWriter[ZoneOffset])
  implicit val twitterAPIReadWriter: ReadWriter[TwitterAPI.Tweet] = {
    implicit val v1: ReadWriter[TwitterAPI.Urls] = macroRW
    implicit val v2: ReadWriter[TwitterAPI.Url] = macroRW
    implicit val v3: ReadWriter[TwitterAPI.UserMentions] = macroRW
    implicit val v4: ReadWriter[TwitterAPI.Entities] = macroRW
    implicit val v5: ReadWriter[TwitterAPI.UserEntities] = macroRW
    implicit val v6: ReadWriter[TwitterAPI.User] = macroRW
    implicit val v7: ReadWriter[TwitterAPI.RetweetedStatus] = macroRW
    macroRW
  }

  override def annotate[V](rw: CaseR[V], n: String): TaggedReader.Leaf[V] =
    new TaggedReader.Leaf[V](simpleName(n), rw)

  override def annotate[V](rw: CaseW[V], n: String)(implicit c: ClassTag[V]): TaggedWriter.Leaf[V] =
    new TaggedWriter.Leaf[V](c, simpleName(n), rw)

  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]](_.getOrElse(null.asInstanceOf[T]))

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] =
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(x => new Some(x))) {
      override def visitNull(index: Int): Option[T] = None
    }

  private def strReader[T](f: CharSequence => T): SimpleReader[T] = new SimpleReader[T] {
    override val expectedMsg = "expected string"

    override def visitString(s: CharSequence, index: Int): T = f(s)
  }

  private def strWriter[V]: Writer[V] = new Writer[V] {
    def write0[R](out: Visitor[_, R], v: V): R = out.visitString(v.toString, -1)
  }

  private def numReader[T](f: CharSequence => T): SimpleReader[T] = new SimpleReader[T] {
    override val expectedMsg = "expected number"

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): T = f(s)
  }

  private def numWriter[V]: Writer[V] = new Writer[V] {
    def write0[R](out: Visitor[_, R], v: V): R = out.visitFloat64String(v.toString, -1)
  }

  private def simpleName(s: String): String = s.substring(s.lastIndexOf('.') + 1)
}
