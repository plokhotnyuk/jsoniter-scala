package com.github.plokhotnyuk.jsoniter_scala.circe

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe._
import java.nio.charset.StandardCharsets
import java.time._

object JavaTimeCodecs {
  implicit val durationC3C: Codec[Duration] =
    shortAsciiStringCodec("duration", _.readDuration(_), _.writeVal(_))
  implicit val instantC3C: Codec[Instant] =
    shortAsciiStringCodec("instant", _.readInstant(_), _.writeVal(_))
  implicit val localDateC3C: Codec[LocalDate] =
    shortAsciiStringCodec("local date", _.readLocalDate(_), _.writeVal(_))
  implicit val localDateTimeC3C: Codec[LocalDateTime] =
    shortAsciiStringCodec("local date time", _.readLocalDateTime(_), _.writeVal(_))
  implicit val localTimeC3C: Codec[LocalTime] =
    shortAsciiStringCodec("local time", _.readLocalTime(_), _.writeVal(_))
  implicit val monthDayC3C: Codec[MonthDay] =
    shortAsciiStringCodec("month day", _.readMonthDay(_), _.writeVal(_))
  implicit val offsetDateTimeC3C: Codec[OffsetDateTime] =
    shortAsciiStringCodec("offset date time", _.readOffsetDateTime(_), _.writeVal(_))
  implicit val offsetTimeC3C: Codec[OffsetTime] =
    shortAsciiStringCodec("offset time", _.readOffsetTime(_), _.writeVal(_))
  implicit val periodC3C: Codec[Period] =
    shortAsciiStringCodec("period", _.readPeriod(_), _.writeVal(_))
  implicit val yearC3C: Codec[Year] =
    shortAsciiStringCodec("year", _.readYear(_), _.writeVal(_))
  implicit val yearMonthC3C: Codec[YearMonth] =
    shortAsciiStringCodec("year month", _.readYearMonth(_), _.writeVal(_))
  implicit val zonedDateTimeC3C: Codec[ZonedDateTime] =
    shortAsciiStringCodec("zoned date time", _.readZonedDateTime(_), _.writeVal(_))
  implicit val zoneIdC3C: Codec[ZoneId] =
    shortAsciiStringCodec("zone id", _.readZoneId(_), _.writeVal(_))
  implicit val zoneOffsetC3C: Codec[ZoneOffset] =
    shortAsciiStringCodec("zone offset", _.readZoneOffset(_), _.writeVal(_))

  private[this] val pool = new ThreadLocal[(Array[Byte], JsonReader, JsonWriter)] {
    override def initialValue(): (Array[Byte], JsonReader, JsonWriter) = {
      val buf = new Array[Byte](128) // should be enough for the longest zoned date time value
      (buf, new JsonReader(buf, charBuf = new Array[Char](128)), new JsonWriter(buf))
    }
  }

  private[this] def shortAsciiStringCodec[A](name: String, read: (JsonReader, A) => A, write: (JsonWriter, A) => Unit): Codec[A] =
    new JsonValueCodec[A] with Codec[A] {
      def apply(x: A): Json = {
        val (buf, _, writer) = pool.get
        val len = writer.write(this, x, buf, 0, buf.length, WriterConfig)
        Json.fromString(new String(buf, 1, len - 2, StandardCharsets.UTF_8))
      }

      def apply(c: HCursor): Decoder.Result[A] = {
        val x = c.value.asString
        if (x eq None) error(c)
        else {
          val s = x.asInstanceOf[Some[String]].value
          val (buf, reader, _) = pool.get
          val len = s.length
          if (len + 2 > buf.length) error(c)
          else {
            buf(0) = '"'
            var bits = 0
            var i = 0
            while (i < len) {
              val ch = s.charAt(i)
              i += 1
              buf(i) = ch.toByte
              bits |= ch
            }
            buf(i + 1) = '"'
            if (bits >= 0x80) error(c)
            else try {
              new scala.util.Right(reader.read(this, buf, 0, len + 2, ReaderConfig))
            } catch {
              case _: JsonReaderException => error(c)
            }
          }
        }
      }

      override def decodeValue(in: JsonReader, default: A): A = read(in, default)

      override def encodeValue(x: A, out: JsonWriter): Unit = write(out, x)

      override val nullValue: A = null.asInstanceOf[A]

      private[this] def error(c: HCursor): Decoder.Result[A] = new scala.util.Left(DecodingFailure(name, c.history))
    }
}
