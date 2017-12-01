package com.github.plokhotnyuk.jsoniter_scala

import java.io.{ByteArrayOutputStream, IOException, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8

import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class JsonWriterSpec extends WordSpec with Matchers with PropertyChecks {
  case class Device(id: Int, model: String)

  case class User(name: String, devices: Seq[Device])

  val codec: JsonCodec[User] = JsonCodecMaker.make[User](CodecMakerConfig())
  val user = User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X")))
  val json = """{"name":"John","devices":[{"id":2,"model":"iPhone X"}]}"""
  val prettyJson: String =
    """{
      |  "name": "John",
      |  "devices": [
      |    {
      |      "id": 2,
      |      "model": "iPhone X"
      |    }
      |  ]
      |}""".stripMargin
  val buf = new Array[Byte](100)
  "JsonWriter.write" should {
    "serialize an object to the provided output stream" in {
      val out1 = new ByteArrayOutputStream()
      JsonWriter.write(codec, user, out1)
      out1.toString("UTF-8") shouldBe json
      val out2 = new ByteArrayOutputStream()
      JsonWriter.write(codec, user, out2, WriterConfig(indentionStep = 2))
      out2.toString("UTF-8") shouldBe prettyJson
    }
    "serialize an object to a new instance of byte array" in {
      new String(JsonWriter.write(codec, user), UTF_8) shouldBe json
      new String(JsonWriter.write(codec, user, WriterConfig(indentionStep = 2)), UTF_8) shouldBe prettyJson
    }
    "serialize an object to the provided byte array from specified position" in {
      val from1 = 10
      val to1 = JsonWriter.write(codec, user, buf, from1)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe json
      val from2 = 0
      val to2 = JsonWriter.write(codec, user, buf, from2, WriterConfig(indentionStep = 2))
      new String(buf, from2, to2 - from2, UTF_8) shouldBe prettyJson
    }
    "throw array index out of bounds exception in case of the provided byte array is overflown during serialization" in {
      assert(intercept[ArrayIndexOutOfBoundsException](JsonWriter.write(codec, user, buf, 50))
        .getMessage.contains("`buf` length exceeded"))
    }
    "throw i/o exception in case of the provided params are invalid" in {
      intercept[NullPointerException](JsonWriter.write(null, user))
      intercept[NullPointerException](JsonWriter.write(null, user, new ByteArrayOutputStream()))
      intercept[NullPointerException](JsonWriter.write(null, user, buf, 0))
      intercept[NullPointerException](JsonWriter.write(codec, user, null.asInstanceOf[OutputStream]))
      intercept[NullPointerException](JsonWriter.write(codec, user, null, 50))
      intercept[NullPointerException](JsonWriter.write(codec, user, null.asInstanceOf[WriterConfig]))
      intercept[NullPointerException](JsonWriter.write(codec, user, new ByteArrayOutputStream(), null))
      intercept[NullPointerException](JsonWriter.write(codec, user, buf, 0, null))
      assert(intercept[ArrayIndexOutOfBoundsException](JsonWriter.write(codec, user, new Array[Byte](10), 50))
        .getMessage.contains("`from` should be positive and not greater than `buf` length"))
    }
  }
  "JsonWriter.writeVal for boolean" should {
    "write valid true and false values" in {
      withWriter(_.writeVal(true)) shouldBe "true"
      withWriter(_.writeVal(false)) shouldBe "false"
    }
  }
  "JsonWriter.writeVal for string" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[String])) shouldBe "null"
    }
    "write string of Unicode chars which are non-surrogate and should not be escaped" in {
      forAll(minSuccessful(10000)) { (s: String) =>
        whenever(!s.exists(ch => Character.isSurrogate(ch) || ch < ' ' || ch == '\\' || ch == '"')) {
          withWriter(_.writeVal(s)) shouldBe '"' + s + '"'
        }
      }
    }
    "write strings with chars that should be escaped" in {
      withWriter(_.writeVal("\b\f\n\r\t\\\"\u0000\u001f")) shouldBe "\"\\b\\f\\n\\r\\t\\\\\\\"\\u0000\\u001f\""
    }
    "write strings with valid surrogate pair chars" in {
      withWriter(_.writeVal("𝄞")) shouldBe "\"\ud834\udd1e\""
    }
    "write strings with escaped unicode chars if it is specified by provided writer config" in {
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\u0000")) shouldBe "\"\\u0000\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\u001f")) shouldBe "\"\\u001f\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\u007F")) shouldBe "\"\\u007f\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\b\f\n\r\t")) shouldBe "\"\\b\\f\\n\\r\\t\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("/Aиბ𝄞")) shouldBe "\"/A\\u0438\\u10d1\\ud834\\udd1e\""
    }
    "throw i/o exception in case of illegal character surrogate pair" in {
      assert(intercept[IOException](withWriter(_.writeVal("\udd1e")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(_.writeVal("\ud834")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(_.writeVal("\udd1e\udd1e")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(_.writeVal("\ud834\ud834")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(_.writeVal("\udd1e\ud834")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\udd1e")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\ud834")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\udd1e\udd1e")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\ud834\ud834")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal("\udd1e\ud834")))
        .getMessage.contains("illegal char sequence of surrogate pair"))
    }
  }
  "JsonWriter.writeVal for char" should {
    "write string with Unicode chars which are non-surrogate or should not be escaped" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(!Character.isSurrogate(ch) && ch >= ' ' && ch != '\\' && ch != '"') {
          withWriter(_.writeVal(ch)) shouldBe "\"" + ch + "\""
        }
      }
    }
    "write string with chars that should be escaped" in {
      withWriter(_.writeVal('\b')) shouldBe """"\b""""
      withWriter(_.writeVal('\f')) shouldBe """"\f""""
      withWriter(_.writeVal('\n')) shouldBe """"\n""""
      withWriter(_.writeVal('\r')) shouldBe """"\r""""
      withWriter(_.writeVal('\t')) shouldBe """"\t""""
      withWriter(_.writeVal('\\')) shouldBe """"\\""""
      withWriter(_.writeVal('\"')) shouldBe """"\"""""
      withWriter(_.writeVal('\u0000')) shouldBe "\"\\u0000\""
      withWriter(_.writeVal('\u001f')) shouldBe "\"\\u001f\""
    }
    "write string with escaped Unicode chars if it is specified by provided writer config" in {
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\u0000')) shouldBe "\"\\u0000\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\u001f')) shouldBe "\"\\u001f\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\u007F')) shouldBe "\"\\u007f\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\b')) shouldBe "\"\\b\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\f')) shouldBe "\"\\f\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\n')) shouldBe "\"\\n\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\r')) shouldBe "\"\\r\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\t')) shouldBe "\"\\t\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('и')) shouldBe "\"\\u0438\""
      withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('ბ')) shouldBe "\"\\u10d1\""
    }
    "throw i/o exception in case of surrogate pair character" in {
      assert(intercept[IOException](withWriter(_.writeVal('\udd1e')))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(_.writeVal('\ud834')))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\udd1e')))
        .getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = true))(_.writeVal('\ud834')))
        .getMessage.contains("illegal char sequence of surrogate pair"))
    }
  }
  "JsonWriter.writeVal for int" should {
    "write any int values" in {
      forAll(minSuccessful(10000)) { (n: Int) =>
        withWriter(_.writeVal(n)) shouldBe n.toString
      }
    }
  }
  "JsonWriter.writeVal long" should {
    "write any long values" in {
      forAll(minSuccessful(10000)) { (n: Int) =>
        withWriter(_.writeVal(n.toLong)) shouldBe n.toString
      }
      forAll(minSuccessful(10000)) { (n: Long) =>
        withWriter(_.writeVal(n)) shouldBe n.toString
      }
    }
  }
  "JsonWriter.writeVal for float" should {
    "write finite float values" in {
      forAll(minSuccessful(10000)) { (n: Float) =>
        whenever(java.lang.Float.isFinite(n)) {
          withWriter(_.writeVal(n)) shouldBe n.toString
        }
      }
    }
    "throw i/o exception on non-finite numbers" in {
      assert(intercept[IOException](withWriter(_.writeVal(Float.NaN)))
        .getMessage.contains("illegal number"))
      assert(intercept[IOException](withWriter(_.writeVal(Float.PositiveInfinity)))
        .getMessage.contains("illegal number"))
      assert(intercept[IOException](withWriter(_.writeVal(Float.NegativeInfinity)))
        .getMessage.contains("illegal number"))
    }
  }
  "JsonWriter.writeVal for double" should {
    "write finite double values" in {
      forAll(minSuccessful(10000)) { (n: Double) =>
        whenever(java.lang.Double.isFinite(n)) {
          withWriter(_.writeVal(n)) shouldBe n.toString
        }
      }
    }
    "throw i/o exception on non-finite numbers" in {
      assert(intercept[IOException](withWriter(_.writeVal(Double.NaN)))
        .getMessage.contains("illegal number"))
      assert(intercept[IOException](withWriter(_.writeVal(Double.PositiveInfinity)))
        .getMessage.contains("illegal number"))
      assert(intercept[IOException](withWriter(_.writeVal(Double.NegativeInfinity)))
        .getMessage.contains("illegal number"))
    }
  }
  "JsonWriter.writeVal for BigInt" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[BigInt])) shouldBe "null"
    }
    "write number values" in {
      forAll(minSuccessful(10000)) { (n: BigInt) =>
        withWriter(_.writeVal(n)) shouldBe n.toString
      }
    }
  }
  "JsonWriter.writeVal for BigDecimal" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[BigDecimal])) shouldBe "null"
    }
    "write number values" in {
      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        withWriter(_.writeVal(n)) shouldBe n.toString
      }
    }
  }

  def withWriter(f: JsonWriter => Unit): String = withWriter(WriterConfig())(f)

  def withWriter(cfg: WriterConfig)(f: JsonWriter => Unit): String = {
    val out = new ByteArrayOutputStream(256)
    val writer = new JsonWriter(new Array[Byte](0), 0, 0, out, true, cfg)
    try f(writer)
    finally writer.flushBuffer()
    out.toString("UTF-8")
  }
}