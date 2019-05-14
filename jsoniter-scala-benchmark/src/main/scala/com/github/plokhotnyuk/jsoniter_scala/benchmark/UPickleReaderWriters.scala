package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import upickle.AttributeTagged
import upickle.core.Visitor

object UPickleReaderWriters extends AttributeTagged {
  override val tagName: String = "type"
  implicit val (bigDecimalReader, bigDecimalWriter) = (numReader(s => BigDecimal(s.toString)), numWriter[BigDecimal])
  implicit val (bigIntReader, bigIntWriter) = (numReader(s => BigInt(s.toString)), numWriter[BigInt])
  implicit val doubleWriter: Writer[Double] = numWriter[Double]
  implicit val floatWriter: Writer[Float] = numWriter[Float]
  implicit val longWriter: Writer[Long] = numWriter[Long]
  implicit val adtReadWriter: ReadWriter[ADTBase] = ReadWriter.merge(macroRW[X], macroRW[Y], macroRW[Z])
  implicit val anyRefsReadWriter: ReadWriter[AnyRefs] = macroRW
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
  implicit val extractFieldsReadWriter: ReadWriter[ExtractFields] = macroRW
  implicit val geometryReadWriter: ReadWriter[GeoJSON.Geometry] =
    ReadWriter.merge(macroRW[GeoJSON.Point], macroRW[GeoJSON.MultiPoint], macroRW[GeoJSON.LineString],
      macroRW[GeoJSON.MultiLineString], macroRW[GeoJSON.Polygon], macroRW[GeoJSON.GeometryCollection])
  implicit val geoJsonReadWriter: ReadWriter[GeoJSON.GeoJSON] =
    ReadWriter.merge(macroRW[GeoJSON.Feature], macroRW[GeoJSON.FeatureCollection])
  implicit val googleMApsAPIReadWriter: ReadWriter[GoogleMapsAPI.DistanceMatrix] = {
    implicit val v1: ReadWriter[GoogleMapsAPI.Value] = macroRW
    implicit val v2: ReadWriter[GoogleMapsAPI.Elements] = macroRW
    implicit val v3: ReadWriter[GoogleMapsAPI.Rows] = macroRW
    macroRW[GoogleMapsAPI.DistanceMatrix]
  }
  implicit val nestedStructsReadWriter: ReadWriter[NestedStructs] = macroRW
  implicit val missingRequiredFieldsReadWriter: ReadWriter[MissingRequiredFields] = macroRW
  implicit val primitivesReadWriter: ReadWriter[Primitives] = macroRW
  implicit val (durationReader, durationWriter) = (strReader(Duration.parse), strWriter[Duration])
  implicit val (instantReader, instantWriter) = (strReader(Instant.parse), strWriter[Instant])
  implicit val (localDateReader, localDateWriter) = (strReader(LocalDate.parse), strWriter[LocalDate])
  implicit val (localDateTimeReader, localDateTimeWriter) = (strReader(LocalDateTime.parse), strWriter[LocalDateTime])
  implicit val (localTimeReader, localTimeWriter) = (strReader(LocalTime.parse), strWriter[LocalTime])
  implicit val (monthDayReader, monthDayWriter) = (strReader(MonthDay.parse), strWriter[MonthDay])
  implicit val (offsetDateTimeReader, offsetDateTimeWriter) = (strReader(OffsetDateTime.parse), strWriter[OffsetDateTime])
  implicit val (offsetTimeReader, offsetTimeWriter) = (strReader(OffsetTime.parse), strWriter[OffsetTime])
  implicit val (periodReader, periodWriter) = (strReader(Period.parse), strWriter[Period])
  implicit val (suiteADTReader: Reader[SuitADT], suiteADTWriter: Writer[SuitADT]) = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    (strReader(s => suite.getOrElse(s.toString, throw new IllegalArgumentException("SuitADT"))), strWriter[SuitADT])
  }
  implicit val (suitEnumReader, suitEnumWriter) = (strReader(s => SuitEnum.withName(s.toString)), strWriter[SuitEnum])
  implicit val (suitReader, suitWriter) = (strReader(s => Suit.valueOf(s.toString)), strWriter[Suit])
  implicit val (yearReader, yearWriter) = (strReader(Year.parse), strWriter[Year])
  implicit val (yearMonthReader, yearMonthWriter) = (strReader(YearMonth.parse), strWriter[YearMonth])
  implicit val (zonedDateTimeReader, zonedDateTimeWriter) = (strReader(ZonedDateTime.parse), strWriter[ZonedDateTime])
  implicit val (zonedIdReader, zonedIdWriter) = (strReader(s => ZoneId.of(s.toString)), strWriter[ZoneId])
  implicit val (zonedOffsetReader, zonedOffsetWriter) = (strReader(s => ZoneOffset.of(s.toString)), strWriter[ZoneOffset])
  implicit val twitterAPIReadWriter: ReadWriter[TwitterAPI.Tweet] = {
    implicit val v1: ReadWriter[TwitterAPI.Urls] = macroRW
    implicit val v2: ReadWriter[TwitterAPI.Url] = macroRW
    implicit val v3: ReadWriter[TwitterAPI.UserMentions] = macroRW
    implicit val v4: ReadWriter[TwitterAPI.Entities] = macroRW
    implicit val v5: ReadWriter[TwitterAPI.UserEntities] = macroRW
    implicit val v6: ReadWriter[TwitterAPI.User] = macroRW
    implicit val v7: ReadWriter[TwitterAPI.RetweetedStatus] = macroRW
    macroRW[TwitterAPI.Tweet]
  }

  override def objectTypeKeyReadMap(s: CharSequence): CharSequence =
    s"com.github.plokhotnyuk.jsoniter_scala.benchmark.$s"

  override def objectTypeKeyWriteMap(s: CharSequence): CharSequence = simpleName(s.toString)

  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]](_.getOrElse(null.asInstanceOf[T]))

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] = implicitly[Reader[T]].mapNulls(Option.apply)

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