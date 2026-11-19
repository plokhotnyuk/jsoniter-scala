/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference
import java.math.MathContext
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Base64, UUID}
import org.scalatest.wordspec.AnyWordSpec

class MemoryRetentionSpec extends AnyWordSpec {
  // A read case: JSON input, expected result, and reading by public methods of `JsonReader`. Cases with raw (unquoted)
  // bytes are read until the end of input, so they are not wrapped by JSON arrays
  case class Case(name: String, json: String, expected: Any, isRaw: Boolean, read: JsonReader => Any)

  // An entry point that uses the pooled `JsonReader`: reads the case from user provided objects and returns weak
  // references to them
  case class Entry(name: String, run: Case => Seq[WeakReference[AnyRef]])

  private[this] val bytes = Array.tabulate[Byte](1500)(_.toByte)

  private[this] val timeValues: Seq[(String, String, Any, JsonReader => Any, JsonReader => Any, JsonReader => Any)] = Seq(
    ("Duration", "PT1H2M3.456S", Duration.parse("PT1H2M3.456S"), _.readDuration(null), _.readKeyAsDuration(), _.readBytesAsDuration()),
    ("Instant", "2020-01-02T03:04:05.123456789Z", Instant.parse("2020-01-02T03:04:05.123456789Z"), _.readInstant(null), _.readKeyAsInstant(), _.readBytesAsInstant()),
    ("LocalDate", "2020-01-02", LocalDate.parse("2020-01-02"), _.readLocalDate(null), _.readKeyAsLocalDate(), _.readBytesAsLocalDate()),
    ("LocalDateTime", "2020-01-02T03:04:05.123", LocalDateTime.parse("2020-01-02T03:04:05.123"), _.readLocalDateTime(null), _.readKeyAsLocalDateTime(), _.readBytesAsLocalDateTime()),
    ("LocalTime", "03:04:05.123", LocalTime.parse("03:04:05.123"), _.readLocalTime(null), _.readKeyAsLocalTime(), _.readBytesAsLocalTime()),
    ("MonthDay", "--12-31", MonthDay.parse("--12-31"), _.readMonthDay(null), _.readKeyAsMonthDay(), _.readBytesAsMonthDay()),
    ("OffsetDateTime", "2020-01-02T03:04:05+01:00", OffsetDateTime.parse("2020-01-02T03:04:05+01:00"), _.readOffsetDateTime(null), _.readKeyAsOffsetDateTime(), _.readBytesAsOffsetDateTime()),
    ("OffsetTime", "03:04:05+01:00", OffsetTime.parse("03:04:05+01:00"), _.readOffsetTime(null), _.readKeyAsOffsetTime(), _.readBytesAsOffsetTime()),
    ("Period", "P1Y2M3D", Period.parse("P1Y2M3D"), _.readPeriod(null), _.readKeyAsPeriod(), _.readBytesAsPeriod()),
    ("Year", "2020", Year.of(2020), _.readYear(null), _.readKeyAsYear(), _.readBytesAsYear()),
    ("YearMonth", "2020-12", YearMonth.parse("2020-12"), _.readYearMonth(null), _.readKeyAsYearMonth(), _.readBytesAsYearMonth()),
    ("ZonedDateTime", "2020-01-02T03:04:05+01:00[Europe/Paris]", ZonedDateTime.parse("2020-01-02T03:04:05+01:00[Europe/Paris]"), _.readZonedDateTime(null), _.readKeyAsZonedDateTime(), _.readBytesAsZonedDateTime()),
    ("ZoneId", "Europe/Paris", ZoneId.of("Europe/Paris"), _.readZoneId(null), _.readKeyAsZoneId(), _.readBytesAsZoneId()),
    ("ZoneOffset", "+01:00", ZoneOffset.of("+01:00"), _.readZoneOffset(null), _.readKeyAsZoneOffset(), _.readBytesAsZoneOffset()))

  private[this] def value(name: String, json: String, expected: Any)(read: JsonReader => Any): Case =
    Case(name, json, expected, isRaw = false, read)

  private[this] def key(name: String, keyJson: String, expected: Any)(read: JsonReader => Any): Case =
    Case(name, "{" + keyJson + ":0}", expected, isRaw = false, in => {
      in.isNextToken('{')
      val k = read(in)
      in.readInt()
      if (!in.isNextToken('}')) in.objectEndOrCommaError()
      k
    })

  private[this] val cases: Seq[Case] = Seq(
    value("readBoolean", "true", true)(_.readBoolean()),
    value("readByte", "-128", (-128).toByte)(_.readByte()),
    value("readShort", "32767", 32767.toShort)(_.readShort()),
    value("readInt", "-2147483648", Int.MinValue)(_.readInt()),
    value("readLong", "9223372036854775807", Long.MaxValue)(_.readLong()),
    value("readFloat", "1.5", 1.5f)(_.readFloat()),
    value("readDouble", "1.2345678901234567e-300", 1.2345678901234567e-300)(_.readDouble()),
    value("readBigInt", "1234567890123456789012345678901234567890", BigInt("1234567890123456789012345678901234567890"))(_.readBigInt(null)),
    value("readBigInt with digits limit", "9" * 400, BigInt("9" * 400))(_.readBigInt(null, 1000)),
    value("readBigDecimal", "123456789012345678901234567890.123456789",
      BigDecimal("123456789012345678901234567890.123456789", MathContext.DECIMAL128))(_.readBigDecimal(null)),
    value("readBigDecimal with limits", "1." + "1" * 400, BigDecimal("1." + "1" * 400, MathContext.UNLIMITED))(
      _.readBigDecimal(null, MathContext.UNLIMITED, 6178, 1000)),
    value("readNumber", "12345678901234567890123", new java.math.BigInteger("12345678901234567890123"))(_.readNumber(null)),
    value("readNumber with limits", "1.5", new java.math.BigDecimal("1.5"))(_.readNumber(null, MathContext.UNLIMITED, 6178, 308)),
    value("readChar", "\"ж\"", 'ж')(_.readChar()),
    value("readString", "\"abc\"", "abc")(_.readString(null)),
    value("readString with non-ASCII and escaped chars", "\"привіт\\n\"", "привіт\n")(_.readString(null)),
    value("readString of null", "null", "default")(_.readString("default")),
    value("readUUID", "\"00000000-0000-0001-0000-000000000002\"", new UUID(1, 2))(_.readUUID(null)),
    value("readBase16AsBytes", "\"0102ff\"", Array[Byte](1, 2, -1))(_.readBase16AsBytes(null)),
    value("readBase64AsBytes", "\"" + Base64.getEncoder.encodeToString(bytes) + "\"", bytes)(_.readBase64AsBytes(null)),
    value("readBase64UrlAsBytes", "\"" + Base64.getUrlEncoder.encodeToString(bytes) + "\"", bytes)(_.readBase64UrlAsBytes(null)),
    value("readStringAsCharBuf", "\"abc\"", 3)(_.readStringAsCharBuf()),
    value("readStringAsBoolean", "\"true\"", true)(_.readStringAsBoolean()),
    value("readStringAsByte", "\"1\"", 1.toByte)(_.readStringAsByte()),
    value("readStringAsShort", "\"1\"", 1.toShort)(_.readStringAsShort()),
    value("readStringAsInt", "\"1\"", 1)(_.readStringAsInt()),
    value("readStringAsLong", "\"1\"", 1L)(_.readStringAsLong()),
    value("readStringAsFloat", "\"1.5\"", 1.5f)(_.readStringAsFloat()),
    value("readStringAsDouble", "\"1.5\"", 1.5)(_.readStringAsDouble()),
    value("readStringAsBigInt", "\"12345678901234567890\"", BigInt("12345678901234567890"))(_.readStringAsBigInt(null)),
    value("readStringAsBigDecimal", "\"1.5\"", BigDecimal("1.5"))(_.readStringAsBigDecimal(null)),
    value("readStringAsNumber", "\"1.5\"", new java.math.BigDecimal("1.5"))(_.readStringAsNumber(null)),
    value("readRawValAsBytes", "{\"a\":[1,2]}", "{\"a\":[1,2]}".getBytes(UTF_8))(_.readRawValAsBytes()),
    value("skip", "{\"a\":[1,\"b\"]}", ())(_.skip()),
    value("setMark, skipToKey, and rollbackToMark", "{\"a\":1,\"b\":2}", true) { in =>
      in.isNextToken('{')
      in.setMark()
      val isFound = in.skipToKey("b")
      val b = in.readInt()
      in.rollbackToMark()
      var sum = 0
      while ({
        in.readKeyAsString()
        sum += in.readInt()
        in.isNextToken(',')
      }) ()
      isFound && b == 2 && sum == 3
    },
    key("readKeyAsString", "\"k\"", "k")(_.readKeyAsString()),
    key("readKeyAsCharBuf", "\"k\"", 1)(_.readKeyAsCharBuf()),
    key("readKeyAsCharBufOrNonEscapedAsciiMatch", "\"k\"", -1)(_.readKeyAsCharBufOrNonEscapedAsciiMatch("k")),
    key("readKeyAsBoolean", "\"true\"", true)(_.readKeyAsBoolean()),
    key("readKeyAsByte", "\"1\"", 1.toByte)(_.readKeyAsByte()),
    key("readKeyAsChar", "\"c\"", 'c')(_.readKeyAsChar()),
    key("readKeyAsShort", "\"1\"", 1.toShort)(_.readKeyAsShort()),
    key("readKeyAsInt", "\"1\"", 1)(_.readKeyAsInt()),
    key("readKeyAsLong", "\"1\"", 1L)(_.readKeyAsLong()),
    key("readKeyAsFloat", "\"1.5\"", 1.5f)(_.readKeyAsFloat()),
    key("readKeyAsDouble", "\"1.5\"", 1.5)(_.readKeyAsDouble()),
    key("readKeyAsBigInt", "\"12345678901234567890\"", BigInt("12345678901234567890"))(_.readKeyAsBigInt()),
    key("readKeyAsBigInt with digits limit", "\"12345678901234567890\"", BigInt("12345678901234567890"))(_.readKeyAsBigInt(100)),
    key("readKeyAsBigDecimal", "\"1.5\"", BigDecimal("1.5"))(_.readKeyAsBigDecimal()),
    key("readKeyAsBigDecimal with limits", "\"1.5\"", BigDecimal("1.5"))(_.readKeyAsBigDecimal(MathContext.UNLIMITED, 6178, 308)),
    key("readKeyAsNumber", "\"1.5\"", new java.math.BigDecimal("1.5"))(_.readKeyAsNumber()),
    key("readKeyAsNumber with limits", "\"1.5\"", new java.math.BigDecimal("1.5"))(_.readKeyAsNumber(MathContext.UNLIMITED, 6178, 308)),
    key("readKeyAsUUID", "\"00000000-0000-0001-0000-000000000002\"", new UUID(1, 2))(_.readKeyAsUUID())) ++
    timeValues.flatMap { case (tpe, s, expected, readValue, readKey, readBytes) =>
      Seq(
        value(s"read$tpe", "\"" + s + "\"", expected)(readValue),
        key(s"readKeyAs$tpe", "\"" + s + "\"", expected)(readKey),
        Case(s"readBytesAs$tpe", s, expected, isRaw = true, readBytes))
    }

  private[this] def codec(c: Case): JsonValueCodec[Any] = new JsonValueCodec[Any] {
    def decodeValue(in: JsonReader, default: Any): Any = {
      val x = c.read(in)
      val isExpected = (x, c.expected) match {
        case (bs1: Array[Byte], bs2: Array[Byte]) => java.util.Arrays.equals(bs1, bs2)
        case (x1, x2) => x1 == x2
      }
      if (!isExpected) throw new IllegalStateException(s"expected ${c.expected} but got $x")
      x
    }

    def encodeValue(x: Any, out: JsonWriter): Unit = ()

    def nullValue: Any = null
  }

  // Bytes of the case surrounded by bytes that are out of the range for reading
  private[this] def surroundedBytes(c: Case): Array[Byte] = ("x" + c.json + "x").getBytes(UTF_8)

  private[this] val entries: Seq[Entry] = Seq(
    Entry("readFromArray", { c =>
      val buf = c.json.getBytes(UTF_8)
      readFromArray(buf)(codec(c))
      Seq(new WeakReference(buf))
    }),
    Entry("readFromSubArray", { c =>
      val buf = surroundedBytes(c)
      readFromSubArray(buf, 1, buf.length - 1)(codec(c))
      Seq(new WeakReference(buf))
    }),
    Entry("readFromByteBuffer with a heap buffer", { c =>
      val buf = surroundedBytes(c)
      val bbuf = ByteBuffer.wrap(buf)
      bbuf.position(1).limit(buf.length - 1)
      readFromByteBuffer(bbuf)(codec(c))
      Seq(new WeakReference(buf), new WeakReference(bbuf))
    }),
    Entry("readFromByteBuffer with a direct buffer", { c =>
      val buf = c.json.getBytes(UTF_8)
      val bbuf = ByteBuffer.allocateDirect(buf.length)
      bbuf.put(buf).flip()
      readFromByteBuffer(bbuf)(codec(c))
      Seq(new WeakReference(bbuf))
    }),
    Entry("readFromStream", { c =>
      val buf = c.json.getBytes(UTF_8)
      val in = new ByteArrayInputStream(buf)
      readFromStream(in)(codec(c))
      Seq(new WeakReference(buf), new WeakReference(in))
    }),
    Entry("scanJsonValuesFromStream", { c =>
      val buf = c.json.getBytes(UTF_8)
      val in = new ByteArrayInputStream(buf)
      scanJsonValuesFromStream(in)((_: Any) => true)(codec(c))
      Seq(new WeakReference(buf), new WeakReference(in))
    }),
    Entry("scanJsonArrayFromStream", { c =>
      val buf = ("[" + c.json + "]").getBytes(UTF_8)
      val in = new ByteArrayInputStream(buf)
      scanJsonArrayFromStream(in)((_: Any) => true)(codec(c))
      Seq(new WeakReference(buf), new WeakReference(in))
    }))

  // Requests garbage collections until all referenced objects are collected or the deadline is reached. Short-lived
  // garbage is allocated between requests to trigger young collections when explicit ones are disabled or delayed.
  // The sentinel object is collected by any garbage collection, so it detects when collections have not happened
  // at all, and then the test is canceled instead of failing. Returns names of retained objects
  private[this] def awaitCollected(refs: Seq[(String, WeakReference[AnyRef])]): Seq[String] = {
    val sentinel = new WeakReference(new Object)
    val deadline = System.nanoTime + 10000000000L // 10 seconds
    var garbage: Array[Byte] = null
    while (refs.exists(_._2.get ne null) && System.nanoTime < deadline) {
      System.gc()
      var i = 0
      while (i < 64) {
        garbage = new Array[Byte](65536)
        i += 1
      }
      Thread.sleep(1)
    }
    assert(garbage ne null)
    val retained = refs.collect { case (name, ref) if ref.get ne null => name }.distinct
    if (retained.nonEmpty && (sentinel.get ne null)) cancel("garbage collections are not happening")
    retained
  }

  "The pooled JsonReader" should {
    "not retain the provided input after reading" in {
      // Each case is read from each entry point in a separated thread that keeps its own pooled reader alive until all
      // inputs are checked. It allows checking all of them by one wait for garbage collections, because the input
      // retained by a pooled reader cannot be released by reading of other inputs that would happen in the same thread
      val runs = for (c <- cases; e <- entries if !(c.isRaw && e.name == "scanJsonArrayFromStream"))
        yield (s"${c.name} from ${e.name}", c, e)
      val results = new Array[Seq[WeakReference[AnyRef]]](runs.size)
      val errors = new java.util.concurrent.ConcurrentLinkedQueue[String]
      val read = new java.util.concurrent.CountDownLatch(runs.size)
      val checked = new java.util.concurrent.CountDownLatch(1)
      val threads = runs.zipWithIndex.map { case ((name, c, e), i) =>
        val t = new Thread(new Runnable {
          def run(): Unit = {
            try results(i) = e.run(c) // the input is created and read in a separated method to not keep local references
            catch {
              case ex: Throwable => errors.add(s"$name: $ex")
            } finally read.countDown()
            checked.await()
          }
        })
        t.setDaemon(true)
        t.start()
        t
      }
      try {
        read.await()
        if (!errors.isEmpty) fail(errors.toArray.mkString("reading failed for:\n", "\n", ""))
        val retained = awaitCollected(runs.zipWithIndex.flatMap { case ((name, _, _), i) => results(i).map(name -> _) })
        if (retained.nonEmpty) fail(retained.mkString("the provided input is retained after reading by:\n", "\n", ""))
      } finally {
        checked.countDown()
        threads.foreach(_.join())
      }
    }
  }
}
