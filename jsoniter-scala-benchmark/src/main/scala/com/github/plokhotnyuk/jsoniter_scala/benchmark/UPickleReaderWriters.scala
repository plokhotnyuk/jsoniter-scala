package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import upickle.AttributeTagged
import upickle.core.Visitor

object UPickleReaderWriters extends AttributeTagged {
  override val tagName: String = "type"
  implicit val bigDecimalReader: Reader[BigDecimal] = new SimpleReader[BigDecimal] {
    override val expectedMsg = "expected signed number"

    override def visitString(s: CharSequence, index: Int): BigDecimal = new java.math.BigDecimal(s.toString)

    override def visitInt32(d: Int, index: Int): BigDecimal = BigDecimal(d)

    override def visitInt64(d: Long, index: Int): BigDecimal = BigDecimal(d)

    override def visitFloat64(d: Double, index: Int): BigDecimal = BigDecimal(d)

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): BigDecimal =
      visitString(s, index)
  }
  implicit val bigDecimalWriter: Writer[BigDecimal] = numWriter[BigDecimal]
  implicit val bigIntReader: Reader[BigInt] = new SimpleReader[BigInt] {
    override val expectedMsg = "expected signed number"

    override def visitString(s: CharSequence, index: Int): BigInt =
      new BigInt(new java.math.BigDecimal(s.toString).toBigIntegerExact)

    override def visitInt32(d: Int, index: Int): BigInt = BigInt(d)

    override def visitInt64(d: Long, index: Int): BigInt = BigInt(d)

    override def visitFloat64(d: Double, index: Int): BigInt = new BigInt(new java.math.BigDecimal(d).toBigIntegerExact)

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): BigInt =
      visitString(s, index)
  }
  implicit val bigIntWriter: Writer[BigInt] = new Writer[BigInt] {
    def write0[V](out: Visitor[_, V], v: BigInt): V =
      out.visitFloat64String(new java.math.BigDecimal(v.bigInteger).toPlainString, -1)
  }
  implicit val doubleWriter: Writer[Double] = numWriter[Double]
  implicit val floatWriter: Writer[Float] = numWriter[Float]
  implicit val longWriter: Writer[Long] = numWriter[Long]
  implicit val adtReaderWriter: ReadWriter[ADTBase] = ReadWriter.merge(macroRW[X], macroRW[Y], macroRW[Z])
  implicit val anyRefsReaderWriter: ReadWriter[AnyRefs] = macroRW
  implicit val anyValsReaderWriter: ReadWriter[AnyVals] = {
    implicit val byteValReaderWriter: ReadWriter[ByteVal] = readwriter[Byte].bimap(_.a, ByteVal.apply)
    implicit val shortValReaderWriter: ReadWriter[ShortVal] = readwriter[Short].bimap(_.a, ShortVal.apply)
    implicit val intValReaderWriter: ReadWriter[IntVal] = readwriter[Int].bimap(_.a, IntVal.apply)
    implicit val longValReaderWriter: ReadWriter[LongVal] = readwriter[Long].bimap(_.a, LongVal.apply)
    implicit val booleanValReaderWriter: ReadWriter[BooleanVal] = readwriter[Boolean].bimap(_.a, BooleanVal.apply)
    implicit val doubleValReaderWriter: ReadWriter[DoubleVal] = readwriter[Double].bimap(_.a, DoubleVal.apply)
    implicit val charValReaderWriter: ReadWriter[CharVal] = readwriter[Char].bimap(_.a, CharVal.apply)
    implicit val floatValReaderWriter: ReadWriter[FloatVal] = readwriter[Float].bimap(_.a, FloatVal.apply)
    macroRW
  }
  implicit val extractFieldsReaderWriter: ReadWriter[ExtractFields] = macroRW
  implicit val geoJsonReaderWriter: ReadWriter[GeoJSON] = {
    implicit lazy val v1: ReadWriter[Point] = macroRW
    implicit lazy val v2: ReadWriter[MultiPoint] = macroRW
    implicit lazy val v3: ReadWriter[LineString] = macroRW
    implicit lazy val v4: ReadWriter[MultiLineString] = macroRW
    implicit lazy val v5: ReadWriter[Polygon] = macroRW
    implicit lazy val v6: ReadWriter[MultiPolygon] = macroRW
    implicit lazy val v7: ReadWriter[GeometryCollection] = macroRW
    implicit lazy val v8: ReadWriter[Geometry] = macroRW
    implicit lazy val v9: ReadWriter[FeatureCollection] = macroRW
    implicit lazy val v10: ReadWriter[Feature] = macroRW
    macroRW
  }
  implicit val googleMApsAPIReaderWriter: ReadWriter[DistanceMatrix] = {
    implicit val v1: ReadWriter[Value] = macroRW
    implicit val v2: ReadWriter[Elements] = macroRW
    implicit val v3: ReadWriter[Rows] = macroRW
    macroRW[DistanceMatrix]
  }
  implicit val nestedStructsReaderWriter: ReadWriter[NestedStructs] = macroRW
  implicit val missingReqFieldsReaderWriter: ReadWriter[MissingReqFields] = macroRW
  implicit val primitivesReaderWriter: ReadWriter[Primitives] = macroRW
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
      "Clubs" -> Clubs
    )
    (strReader(s => suite.getOrElse(s.toString, throw new IllegalArgumentException("SuitADT"))), strWriter[SuitADT])
  }
  implicit val (suitEnumReader, suitEnumWriter) = (strReader(s => SuitEnum.withName(s.toString)), strWriter[SuitEnum])
  implicit val (suitReader, suitWriter) = (strReader(s => Suit.valueOf(s.toString)), strWriter[Suit])
  implicit val (yearReader, yearWriter) = (strReader(Year.parse), strWriter[Year])
  implicit val (yearMonthReader, yearMonthWriter) = (strReader(YearMonth.parse), strWriter[YearMonth])
  implicit val (zonedDateTimeReader, zonedDateTimeWriter) = (strReader(ZonedDateTime.parse), strWriter[ZonedDateTime])
  implicit val (zonedIdReader, zonedIdWriter) = (strReader(s => ZoneId.of(s.toString)), strWriter[ZoneId])
  implicit val (zonedOffsetReader, zonedOffsetWriter) = (strReader(s => ZoneOffset.of(s.toString)), strWriter[ZoneOffset])
  implicit val twitterAPIReaderWriter: ReadWriter[Tweet] = {
    implicit val v1: ReadWriter[Urls] = macroRW
    implicit val v2: ReadWriter[Url] = macroRW
    implicit val v3: ReadWriter[UserMentions] = macroRW
    implicit val v4: ReadWriter[Entities] = macroRW
    implicit val v5: ReadWriter[UserEntities] = macroRW
    implicit val v6: ReadWriter[User] = macroRW
    implicit val v7: ReadWriter[RetweetedStatus] = macroRW
    macroRW[Tweet]
  }

  override def objectTypeKeyReadMap(s: CharSequence): CharSequence =
    s"com.github.plokhotnyuk.jsoniter_scala.benchmark.$s"

  override def objectTypeKeyWriteMap(s: CharSequence): CharSequence = simpleName(s.toString)

  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]](_.getOrElse(null.asInstanceOf[T]))

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] = implicitly[Reader[T]].mapNulls(Option.apply)

  private def strReader[T](f: CharSequence => T): SimpleReader[T] = new SimpleReader[T] {
    override val expectedMsg = "expected string"

    override def visitString(s: CharSequence, index: Int) = f(s)
  }

  private def strWriter[V]: Writer[V] = new Writer[V] {
    def write0[R](out: Visitor[_, R], v: V): R = out.visitString(v.toString, -1)
  }

  private def numWriter[V]: Writer[V] = new Writer[V] {
    def write0[R](out: Visitor[_, R], v: V): R = out.visitFloat64String(v.toString, -1)
  }

  private def simpleName(s: String): String = s.substring(s.lastIndexOf('.') + 1)
}