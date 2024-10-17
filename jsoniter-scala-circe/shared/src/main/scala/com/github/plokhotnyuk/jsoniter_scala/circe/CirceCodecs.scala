package com.github.plokhotnyuk.jsoniter_scala.circe

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe._
import java.time._

/**
 * Implicit instances of circe's codec for `BigInt` and `java.time.*` types.
 *
 * Uses jsoniter-scala for efficient encoding and decoding.
 */
object CirceCodecs {
  private[this] val pool = new ThreadLocal[(Array[Byte], JsonReader, JsonWriter)] {
    override def initialValue(): (Array[Byte], JsonReader, JsonWriter) = {
      val buf = new Array[Byte](128) // should be enough for the longest zoned date time value
      buf(0) = '"'
      new Tuple3(buf, new JsonReader(buf, charBuf = new Array[Char](128)), new JsonWriter(buf))
    }
  }

  private[this] class ShortAsciiStringCodec[A](codec: JsonValueCodec[A], name: String) extends Codec[A] {
    override def apply(x: A): Json = {
      val tlb = pool.get
      val buf = tlb._1
      io.circe.JsoniterScalaCodec.asciiStringToJString(buf, tlb._3.write(codec, x, buf, 0, 128, WriterConfig))
    }

    override def apply(c: HCursor): Decoder.Result[A] = {
      val tlb = pool.get
      val buf = tlb._1
      val s = io.circe.JsoniterScalaCodec.stringValue(c)
      var len = 0
      if ((s ne null) && {
        len = s.length
        len <= 126
      } && {
        var bits, i = 0
        while (i < len) {
          val ch = s.charAt(i)
          buf(i + 1) = ch.toByte
          bits |= ch
          i += 1
        }
        buf(i + 1) = '"'
        bits < 0x80
      }) {
        try return new scala.util.Right(tlb._2.read(codec, buf, 0, len + 2, ReaderConfig))
        catch { case _: JsonReaderException => }
      }
      error(c)
    }

    private[this] def error(c: HCursor): Decoder.Result[A] = new scala.util.Left(DecodingFailure(name, c.history))
  }

  // codecs for numeric types
  implicit val byteC3C: Codec[Byte] = io.circe.JsoniterScalaCodec.byteCodec
  implicit val shortC3C: Codec[Short] = io.circe.JsoniterScalaCodec.shortCodec
  implicit val intC3C: Codec[Int] = io.circe.JsoniterScalaCodec.intCodec
  implicit val longC3C: Codec[Long] = io.circe.JsoniterScalaCodec.longCodec
  implicit val floatC3C: Codec[Float] = io.circe.JsoniterScalaCodec.floatCodec
  implicit val doubleC3C: Codec[Double] = io.circe.JsoniterScalaCodec.doubleCodec
  implicit val bigIntC3C: Codec[BigInt] = io.circe.JsoniterScalaCodec.bigIntCodec
  implicit val bigDecimalC3C: Codec[BigDecimal] =io.circe.JsoniterScalaCodec.bigDecimalCodec
  // codecs for java.time.* types
  implicit val durationC3C: Codec[Duration] = new ShortAsciiStringCodec(new JsonValueCodec[Duration] {
    override def decodeValue(in: JsonReader, default: Duration): Duration = in.readDuration(default)

    override def encodeValue(x: Duration, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: Duration = null
  }, "duration")
  implicit val instantC3C: Codec[Instant] = new ShortAsciiStringCodec(new JsonValueCodec[Instant] {
    override def decodeValue(in: JsonReader, default: Instant): Instant = in.readInstant(default)

    override def encodeValue(x: Instant, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: Instant = null
  }, "instant")
  implicit val localDateC3C: Codec[LocalDate] = new ShortAsciiStringCodec(new JsonValueCodec[LocalDate] {
    override def decodeValue(in: JsonReader, default: LocalDate): LocalDate = in.readLocalDate(default)

    override def encodeValue(x: LocalDate, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: LocalDate = null
  }, "local date")
  implicit val localDateTimeC3C: Codec[LocalDateTime] = new ShortAsciiStringCodec(new JsonValueCodec[LocalDateTime] {
    override def decodeValue(in: JsonReader, default: LocalDateTime): LocalDateTime = in.readLocalDateTime(default)

    override def encodeValue(x: LocalDateTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: LocalDateTime = null
  }, "local date time")
  implicit val localTimeC3C: Codec[LocalTime] = new ShortAsciiStringCodec(new JsonValueCodec[LocalTime] {
    override def decodeValue(in: JsonReader, default: LocalTime): LocalTime = in.readLocalTime(default)

    override def encodeValue(x: LocalTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: LocalTime = null
  }, "local time")
  implicit val monthDayC3C: Codec[MonthDay] = new ShortAsciiStringCodec(new JsonValueCodec[MonthDay] {
    override def decodeValue(in: JsonReader, default: MonthDay): MonthDay = in.readMonthDay(default)

    override def encodeValue(x: MonthDay, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: MonthDay = null
  }, "month day")
  implicit val offsetDateTimeC3C: Codec[OffsetDateTime] = new ShortAsciiStringCodec(new JsonValueCodec[OffsetDateTime] {
    override def decodeValue(in: JsonReader, default: OffsetDateTime): OffsetDateTime = in.readOffsetDateTime(default)

    override def encodeValue(x: OffsetDateTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: OffsetDateTime = null
  }, "offset date time")
  implicit val offsetTimeC3C: Codec[OffsetTime] = new ShortAsciiStringCodec(new JsonValueCodec[OffsetTime] {
    override def decodeValue(in: JsonReader, default: OffsetTime): OffsetTime = in.readOffsetTime(default)

    override def encodeValue(x: OffsetTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: OffsetTime = null
  }, "offset time")
  implicit val periodC3C: Codec[Period] = new ShortAsciiStringCodec(new JsonValueCodec[Period] {
    override def decodeValue(in: JsonReader, default: Period): Period = in.readPeriod(default)

    override def encodeValue(x: Period, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: Period = null
  }, "period")
  implicit val yearMonthC3C: Codec[YearMonth] = new ShortAsciiStringCodec(new JsonValueCodec[YearMonth] {
    override def decodeValue(in: JsonReader, default: YearMonth): YearMonth = in.readYearMonth(default)

    override def encodeValue(x: YearMonth, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: YearMonth = null
  }, "year month")
  implicit val yearC3C: Codec[Year] = new ShortAsciiStringCodec(new JsonValueCodec[Year] {
      override def decodeValue(in: JsonReader, default: Year): Year = in.readYear(default)

      override def encodeValue(x: Year, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Year = null
    }, "year")
  implicit val zonedDateTimeC3C: Codec[ZonedDateTime] = new ShortAsciiStringCodec(new JsonValueCodec[ZonedDateTime] {
    override def decodeValue(in: JsonReader, default: ZonedDateTime): ZonedDateTime = in.readZonedDateTime(default)

    override def encodeValue(x: ZonedDateTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: ZonedDateTime = null
  }, "zoned date time")
}