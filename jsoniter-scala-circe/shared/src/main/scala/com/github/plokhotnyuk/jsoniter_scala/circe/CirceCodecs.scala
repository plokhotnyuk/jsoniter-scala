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
  private[this] val numberPool = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader(buf = new Array[Byte](512), charBuf = new Array[Char](512))
  }
  private[this] val javaTimePool = new ThreadLocal[(Array[Byte], JsonReader, JsonWriter)] {
    override def initialValue(): (Array[Byte], JsonReader, JsonWriter) = {
      val buf = new Array[Byte](512) // should be enough for the longest zoned date time value
      buf(0) = '"'
      new Tuple3(buf, new JsonReader(buf, charBuf = new Array[Char](512)), new JsonWriter(buf))
    }
  }
  private[this] val readerConfig =
    ReaderConfig.withAppendHexDumpToParseException(false).withPreferredBufSize(512).withMaxBufSize(512)
      .withPreferredCharBufSize(512).withMaxCharBufSize(512)
  private[this] val writeConfig = WriterConfig.withPreferredBufSize(512)

  private[this] class ShortAsciiStringCodec[A](codec: JsonValueCodec[A], name: String) extends Codec[A] {
    override def apply(x: A): Json = {
      val tlb = javaTimePool.get
      val buf = tlb._1
      io.circe.JsoniterScalaCodec.asciiStringToJString(buf, tlb._3.write(codec, x, buf, 0, 512, writeConfig))
    }

    override def apply(c: HCursor): Decoder.Result[A] = {
      val tlb = javaTimePool.get
      val buf = tlb._1
      val s = io.circe.JsoniterScalaCodec.stringValue(c)
      var len = 0
      if ((s ne null) && {
        len = s.length
        len <= 510
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
        try return new scala.util.Right(tlb._2.read(codec, buf, 0, len + 2, readerConfig))
        catch { case _: JsonReaderException => }
      }
      error(c)
    }

    private[this] def error(c: HCursor): Decoder.Result[A] = new scala.util.Left(DecodingFailure(name, c.history))
  }

  // codecs for numeric types
  implicit val byteC3C: Codec[Byte] = io.circe.JsoniterScalaCodec.byteCodec {
    val codec: JsonValueCodec[Byte] = new JsonValueCodec[Byte] {
      override def decodeValue(in: JsonReader, default: Byte): Byte = {
        val x = in.readByte(isToken = false)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: Byte, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Byte = 0
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val shortC3C: Codec[Short] = io.circe.JsoniterScalaCodec.shortCodec {
    val codec: JsonValueCodec[Short] = new JsonValueCodec[Short] {
      override def decodeValue(in: JsonReader, default: Short): Short = {
        val x = in.readShort(isToken = false)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: Short, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Short = 0
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val intC3C: Codec[Int] = io.circe.JsoniterScalaCodec.intCodec {
    val codec: JsonValueCodec[Int] = new JsonValueCodec[Int] {
      override def decodeValue(in: JsonReader, default: Int): Int = {
        val x = in.readInt(isToken = false)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: Int, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Int = 0
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val longC3C: Codec[Long] = io.circe.JsoniterScalaCodec.longCodec {
    val codec: JsonValueCodec[Long] = new JsonValueCodec[Long] {
      override def decodeValue(in: JsonReader, default: Long): Long = {
        val x = in.readLong(isToken = false)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: Long, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Long = 0
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val floatC3C: Codec[Float] = io.circe.JsoniterScalaCodec.floatCodec {
    val codec: JsonValueCodec[Float] = new JsonValueCodec[Float] {
      override def decodeValue(in: JsonReader, default: Float): Float = {
        val x = in.readFloat(isToken = false)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: Float, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Float = 0
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val doubleC3C: Codec[Double] = io.circe.JsoniterScalaCodec.doubleCodec {
    val codec: JsonValueCodec[Double] = new JsonValueCodec[Double] {
      override def decodeValue(in: JsonReader, default: Double): Double = {
        val x = in.readDouble(isToken = false)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: Double, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Double = 0
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val bigIntC3C: Codec[BigInt] = io.circe.JsoniterScalaCodec.bigIntCodec {
    val codec: JsonValueCodec[BigInt] = new JsonValueCodec[BigInt] {
      override def decodeValue(in: JsonReader, default: BigInt): BigInt = {
        val x = in.readBigInt(isToken = false, default, JsonReader.bigIntDigitsLimit)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: BigInt, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: BigInt = null
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  implicit val bigDecimalC3C: Codec[BigDecimal] = io.circe.JsoniterScalaCodec.bigDecimalCodec {
    val codec: JsonValueCodec[BigDecimal] = new JsonValueCodec[BigDecimal] {
      override def decodeValue(in: JsonReader, default: BigDecimal): BigDecimal = {
        val x = in.readBigDecimal(isToken = false, default, JsonReader.bigDecimalMathContext,
          JsonReader.bigDecimalScaleLimit, JsonReader.bigDecimalDigitsLimit)
        if (in.hasRemaining()) in.decodeError("expected '\"'")
        x
      }

      override def encodeValue(x: BigDecimal, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: BigDecimal = null
    }
    s =>
      numberPool.get.read(codec, s, readerConfig)
  }
  // codecs for java.time.* types
  implicit val durationC3C: Codec[Duration] = new ShortAsciiStringCodec(new JsonValueCodec[Duration] {
    override def decodeValue(in: JsonReader, default: Duration): Duration = in.readDuration(default)

    override def encodeValue(x: Duration, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: Duration = null
  }, "Duration")
  implicit val instantC3C: Codec[Instant] = new ShortAsciiStringCodec(new JsonValueCodec[Instant] {
    override def decodeValue(in: JsonReader, default: Instant): Instant = in.readInstant(default)

    override def encodeValue(x: Instant, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: Instant = null
  }, "Instant")
  implicit val localDateC3C: Codec[LocalDate] = new ShortAsciiStringCodec(new JsonValueCodec[LocalDate] {
    override def decodeValue(in: JsonReader, default: LocalDate): LocalDate = in.readLocalDate(default)

    override def encodeValue(x: LocalDate, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: LocalDate = null
  }, "LocalDate")
  implicit val localDateTimeC3C: Codec[LocalDateTime] = new ShortAsciiStringCodec(new JsonValueCodec[LocalDateTime] {
    override def decodeValue(in: JsonReader, default: LocalDateTime): LocalDateTime = in.readLocalDateTime(default)

    override def encodeValue(x: LocalDateTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: LocalDateTime = null
  }, "LocalDateTime")
  implicit val localTimeC3C: Codec[LocalTime] = new ShortAsciiStringCodec(new JsonValueCodec[LocalTime] {
    override def decodeValue(in: JsonReader, default: LocalTime): LocalTime = in.readLocalTime(default)

    override def encodeValue(x: LocalTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: LocalTime = null
  }, "LocalTime")
  implicit val monthDayC3C: Codec[MonthDay] = new ShortAsciiStringCodec(new JsonValueCodec[MonthDay] {
    override def decodeValue(in: JsonReader, default: MonthDay): MonthDay = in.readMonthDay(default)

    override def encodeValue(x: MonthDay, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: MonthDay = null
  }, "MonthDay")
  implicit val offsetDateTimeC3C: Codec[OffsetDateTime] = new ShortAsciiStringCodec(new JsonValueCodec[OffsetDateTime] {
    override def decodeValue(in: JsonReader, default: OffsetDateTime): OffsetDateTime = in.readOffsetDateTime(default)

    override def encodeValue(x: OffsetDateTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: OffsetDateTime = null
  }, "OffsetDateTime")
  implicit val offsetTimeC3C: Codec[OffsetTime] = new ShortAsciiStringCodec(new JsonValueCodec[OffsetTime] {
    override def decodeValue(in: JsonReader, default: OffsetTime): OffsetTime = in.readOffsetTime(default)

    override def encodeValue(x: OffsetTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: OffsetTime = null
  }, "OffsetTime")
  implicit val periodC3C: Codec[Period] = new ShortAsciiStringCodec(new JsonValueCodec[Period] {
    override def decodeValue(in: JsonReader, default: Period): Period = in.readPeriod(default)

    override def encodeValue(x: Period, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: Period = null
  }, "Period")
  implicit val yearMonthC3C: Codec[YearMonth] = new ShortAsciiStringCodec(new JsonValueCodec[YearMonth] {
    override def decodeValue(in: JsonReader, default: YearMonth): YearMonth = in.readYearMonth(default)

    override def encodeValue(x: YearMonth, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: YearMonth = null
  }, "YearMonth")
  implicit val yearC3C: Codec[Year] = new ShortAsciiStringCodec(new JsonValueCodec[Year] {
      override def decodeValue(in: JsonReader, default: Year): Year = in.readYear(default)

      override def encodeValue(x: Year, out: JsonWriter): Unit = out.writeVal(x)

      override def nullValue: Year = null
  }, "Year")
  implicit val zonedDateTimeC3C: Codec[ZonedDateTime] = new ShortAsciiStringCodec(new JsonValueCodec[ZonedDateTime] {
    override def decodeValue(in: JsonReader, default: ZonedDateTime): ZonedDateTime = in.readZonedDateTime(default)

    override def encodeValue(x: ZonedDateTime, out: JsonWriter): Unit = out.writeVal(x)

    override def nullValue: ZonedDateTime = null
  }, "ZonedDateTime")
}