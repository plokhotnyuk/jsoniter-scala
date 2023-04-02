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
      (buf, new JsonReader(buf, charBuf = new Array[Char](128)), new JsonWriter(buf))
    }
  }

 /**
   * The circe's codec for `BigInt` values.
   *
   * @param x the `BigInt` value to encode
   * @return a JSON value representing the BigInt
   */
  implicit val bigIntC3c: Codec[BigInt] = new Codec[BigInt] {
    override def apply(x: BigInt): Json = io.circe.JsoniterScalaCodec.jsonValue(x)

    override def apply(c: HCursor): Decoder.Result[BigInt] = {
      val bi = io.circe.JsoniterScalaCodec.bigIntValue(c)
      if (bi ne null) new scala.util.Right(bi)
      else Decoder.decodeBigInt.apply(c)
    }
  }

  // codecs for java.time.* types
  
  implicit val durationC3C: Codec[Duration] = shortAsciiStringCodec("duration", _.readDuration(_), _.writeVal(_))
  implicit val instantC3C: Codec[Instant] = shortAsciiStringCodec("instant", _.readInstant(_), _.writeVal(_))
  implicit val localDateC3C: Codec[LocalDate] = shortAsciiStringCodec("local date", _.readLocalDate(_), _.writeVal(_))
  implicit val localDateTimeC3C: Codec[LocalDateTime] = shortAsciiStringCodec("local date time", _.readLocalDateTime(_), _.writeVal(_))
  implicit val localTimeC3C: Codec[LocalTime] = shortAsciiStringCodec("local time", _.readLocalTime(_), _.writeVal(_))
  implicit val monthDayC3C: Codec[MonthDay] = shortAsciiStringCodec("month day", _.readMonthDay(_), _.writeVal(_))
  implicit val offsetDateTimeC3C: Codec[OffsetDateTime] = shortAsciiStringCodec("offset date time", _.readOffsetDateTime(_), _.writeVal(_))
  implicit val offsetTimeC3C: Codec[OffsetTime] = shortAsciiStringCodec("offset time", _.readOffsetTime(_), _.writeVal(_))
  implicit val periodC3C: Codec[Period] = shortAsciiStringCodec("period", _.readPeriod(_), _.writeVal(_))
  implicit val yearMonthC3C: Codec[YearMonth] = shortAsciiStringCodec("year month", _.readYearMonth(_), _.writeVal(_))
  implicit val yearD5r: Decoder[Year] = shortAsciiStringCodec("year", _.readYear(_), _.writeVal(_))
  implicit val yearE5r: Encoder[Year] = (x: Year) => Json.fromString(x.toString)
  implicit val zonedDateTimeC3C: Codec[ZonedDateTime] = shortAsciiStringCodec("zoned date time", _.readZonedDateTime(_), _.writeVal(_))

  private[this] def shortAsciiStringCodec[A](name: String, read: (JsonReader, A) => A, write: (JsonWriter, A) => Unit): Codec[A] =
    new JsonValueCodec[A] with Codec[A] {
      override def apply(x: A): Json = {
        val (buf, _, writer) = pool.get
        io.circe.JsoniterScalaCodec.asciiStringToJString(buf, writer.write(this, x, buf, 0, 128, WriterConfig))
      }

      override def apply(c: HCursor): Decoder.Result[A] = {
        val (buf, reader, _) = pool.get
        val s = io.circe.JsoniterScalaCodec.stringValue(c)
        var len = 0
        if ((s ne null) && {
          len = s.length
          len <= 126
        } && {
          buf(0) = '"'
          var bits, i = 0
          while (i < len) {
            val ch = s.charAt(i)
            i += 1
            buf(i) = ch.toByte
            bits |= ch
          }
          buf(i + 1) = '"'
          bits < 0x80
        }) {
          try new scala.util.Right(reader.read(this, buf, 0, len + 2, ReaderConfig))
          catch {
            case _: JsonReaderException => error(c)
          }
        } else error(c)
      }

      override def decodeValue(in: JsonReader, default: A): A = read(in, default)

      override def encodeValue(x: A, out: JsonWriter): Unit = write(out, x)

      override val nullValue: A = null.asInstanceOf[A]

      private[this] def error(c: HCursor): Decoder.Result[A] = new scala.util.Left(DecodingFailure(name, c.history))
    }
}