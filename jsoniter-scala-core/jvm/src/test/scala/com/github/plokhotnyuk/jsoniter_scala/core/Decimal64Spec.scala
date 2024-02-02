package com.github.plokhotnyuk.jsoniter_scala.core

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import com.epam.deltix.dfp.Decimal64Utils
import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._

import java.io._
import java.nio.charset.StandardCharsets.UTF_8
import scala.util.Random

class Decimal64Spec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "JsonWriter.writeDecimal64Val and JsonWriter.writeDecimal64ValAsString and JsonWriter.writeDecimal64Key for an underlying representation of Decimal64" should {
    "write finite Decimal64 values" in {
      def check(n: Long): Unit = {
        val s = withWriter(_.writeDecimal64Val(n))
        print(s + " ")
        Decimal64Utils.compareTo(Decimal64Utils.parse(s), n) shouldBe 0 // no data loss when parsing by JVM, Native or JS Platform
        s.length should be <= 22 // length is 22 bytes or less
        withWriter(_.writeDecimal64ValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeDecimal64Key(n)) shouldBe s""""$s":"""
      }

      check(Decimal64Utils.ZERO)
      check(Decimal64Utils.ONE)
      check(Decimal64Utils.TEN)
      check(Decimal64Utils.THOUSAND)
      check(Decimal64Utils.MILLION)
      check(Decimal64Utils.ONE_TENTH)
      check(Decimal64Utils.ONE_HUNDREDTH)
      check(Decimal64Utils.parse("1000.0"))
      check(Decimal64Utils.parse("1000.001"))
      forAll(arbitrary[Long], minSuccessful(10000)) { n =>
        whenever(Decimal64Utils.isFinite(n)) {
          check(n)
        }
      }
      forAll(genFiniteDouble, minSuccessful(10000)) { d =>
        check(Decimal64Utils.fromDouble(d))
      }
      forAll(arbitrary[Long], minSuccessful(10000)) { l =>
        check(Decimal64Utils.fromFixedPoint(l >> 8, l.toByte))
      }
      forAll(arbitrary[Long], minSuccessful(10000)) { l =>
        check(Decimal64Utils.fromLong(l))
      }
      forAll(arbitrary[Int], minSuccessful(10000)) { i =>
        check(Decimal64Utils.fromInt(i))
      }
    }
    "throw i/o exception on non-finite numbers" in {
      forAll(arbitrary[Long], minSuccessful(100)) { n =>
        whenever(Decimal64Utils.isNonFinite(n)) {
          assert(intercept[JsonWriterException](withWriter(_.writeDecimal64Val(n))).getMessage.startsWith("illegal Decimal64 number"))
          assert(intercept[JsonWriterException](withWriter(_.writeDecimal64ValAsString(n))).getMessage.startsWith("illegal Decimal64 number"))
          assert(intercept[JsonWriterException](withWriter(_.writeDecimal64Key(n))).getMessage.startsWith("illegal Decimal64 number"))
        }
      }
    }
  }

  def reader(json: String, totalRead: Long = 0): JsonReader = reader2(json.getBytes(UTF_8), totalRead)

  def reader2(jsonBytes: Array[Byte], totalRead: Long = 0): JsonReader =
    new JsonReader(new Array[Byte](Random.nextInt(20) + 12), // 12 is a minimal allowed length to test resizing of the buffer
      0, 0, -1, new Array[Char](Random.nextInt(32)), null, new ByteArrayInputStream(jsonBytes), totalRead, readerConfig)

  def readerConfig: ReaderConfig = ReaderConfig
    .withPreferredBufSize(Random.nextInt(20) + 12) // 12 is a minimal allowed length to test resizing of the buffer
    .withPreferredCharBufSize(Random.nextInt(32))
    .withThrowReaderExceptionWithStackTrace(true)

  def withWriter(f: JsonWriter => Unit): String =
    withWriter(WriterConfig.withPreferredBufSize(1).withThrowWriterExceptionWithStackTrace(true))(f)

  def withWriter(cfg: WriterConfig)(f: JsonWriter => Unit): String = {
    val writer = new JsonWriter(new Array[Byte](Random.nextInt(16)), 0, 0, 0, false, false, null, null, cfg)
    new String(writer.write(new JsonValueCodec[String] {
      override def decodeValue(in: JsonReader, default: String): String = ""

      override def encodeValue(x: String, out: JsonWriter): Unit = f(writer)

      override val nullValue: String = ""
    }, "", cfg), "UTF-8")
  }
}
