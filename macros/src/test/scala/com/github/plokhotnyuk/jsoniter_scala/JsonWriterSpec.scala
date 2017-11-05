package com.github.plokhotnyuk.jsoniter_scala

import java.io.{ByteArrayOutputStream, IOException, OutputStream}

import org.scalatest.{Matchers, WordSpec}

class JsonWriterSpec extends WordSpec with Matchers {
  case class Device(id: Int, model: String)
  implicit val deviceCodec: JsonCodec[Device] = JsonCodec.materialize[Device]

  case class User(name: String, devices: Seq[Device])
  val userCodec: JsonCodec[User] = JsonCodec.materialize[User]

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
      JsonWriter.write(userCodec, user, out1)
      out1.toString("UTF-8") shouldBe json
      val out2 = new ByteArrayOutputStream()
      JsonWriter.write(userCodec, user, out2, WriterConfig(indentionStep = 2))
      out2.toString("UTF-8") shouldBe prettyJson
    }
    "serialize an object to a new instance of byte array" in {
      new String(JsonWriter.write(userCodec, user), "UTF-8") shouldBe json
      new String(JsonWriter.write(userCodec, user, WriterConfig(indentionStep = 2)), "UTF-8") shouldBe prettyJson
    }
    "serialize an object to the provided byte array from specified position" in {
      val from1 = 10
      val to1 = JsonWriter.write(userCodec, user, buf, from1)
      new String(buf, from1, to1 - from1, "UTF-8") shouldBe json
      val from2 = 0
      val to2 = JsonWriter.write(userCodec, user, buf, from2, WriterConfig(indentionStep = 2))
      new String(buf, from2, to2 - from2, "UTF-8") shouldBe prettyJson
    }
    "throw i/o exception in case of the provided byte array is overflown during serialization" in {
      assert(intercept[IOException](JsonWriter.write(userCodec, user, buf, 50))
        .getMessage.contains("buf is overflown"))
    }
    "throw i/o exception in case of the provided params are invalid" in {
      assert(intercept[IOException](JsonWriter.write(null, user))
        .getMessage.contains("codec should be not null"))
      assert(intercept[IOException](JsonWriter.write(null, user, new ByteArrayOutputStream()))
        .getMessage.contains("codec should be not null"))
      assert(intercept[IOException](JsonWriter.write(null, user, buf, 0))
        .getMessage.contains("codec should be not null"))
      assert(intercept[IOException](JsonWriter.write(userCodec, user, null.asInstanceOf[OutputStream]))
        .getMessage.contains("out should be not null"))
      assert(intercept[IOException](JsonWriter.write(userCodec, user, null, 50))
        .getMessage.contains("buf should be non empty"))
      assert(intercept[IOException](JsonWriter.write(userCodec, user, new Array[Byte](10), 50))
        .getMessage.contains("from should be positive and not greater than buf length"))
    }
  }
  "JsonWriter.writeVal for boolean" should {
    "write valid true and false values" in {
      serialized(_.writeVal(true)) shouldBe "true"
      serialized(_.writeVal(false)) shouldBe "false"
    }
  }
  "JsonWriter.writeVal for string" should {
    "write null value" in {
      serialized(_.writeVal(null.asInstanceOf[String])) shouldBe "null"
    }
    "write ascii chars" in {
      val text = new String(Array.fill[Char]('a')(10000))
      serialized(_.writeVal(text)) shouldBe '"' + text + '"'
    }
    "write strings with escaped whitespace chars" in {
      serialized(_.writeVal("\b\f\n\r\t\\\"")) shouldBe """"\b\f\n\r\t\\\"""""
    }
    "write strings with unicode chars" in {
      serialized(_.writeVal("иბ𝄞")) shouldBe "\"иბ\ud834\udd1e\""
    }
    "write strings with escaped unicode chars" in {
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\u0000")) shouldBe "\"\\u0000\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\u001f")) shouldBe "\"\\u001f\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\u007F")) shouldBe "\"\\u007f\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\b\f\n\r\t")) shouldBe "\"\\b\\f\\n\\r\\t\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal("/Aиბ𝄞")) shouldBe "\"/A\\u0438\\u10d1\\ud834\\udd1e\""
    }
    "throw i/o exception in case of illegal character surrogate pair" in {
      assert(intercept[IOException](serialized(_.writeVal("\udd1e"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(_.writeVal("\ud834"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(_.writeVal("\udd1e\udd1e"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(_.writeVal("\ud834\ud834"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(_.writeVal("\udd1e\ud834"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\udd1e"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\ud834"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\udd1e\udd1e"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\ud834\ud834"))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal("\udd1e\ud834"))).getMessage.contains("illegal char sequence of surrogate pair"))
    }
  }
  "JsonWriter.writeVal for char" should {
    "write ascii chars" in {
      serialized(_.writeVal('a')) shouldBe "\"a\""
    }
    "write escaped whitespace chars" in {
      serialized(_.writeVal('\b')) shouldBe """"\b""""
      serialized(_.writeVal('\f')) shouldBe """"\f""""
      serialized(_.writeVal('\n')) shouldBe """"\n""""
      serialized(_.writeVal('\r')) shouldBe """"\r""""
      serialized(_.writeVal('\t')) shouldBe """"\t""""
      serialized(_.writeVal('\\')) shouldBe """"\\""""
      serialized(_.writeVal('\"')) shouldBe """"\"""""
    }
    "write strings with unicode chars" in {
      serialized(_.writeVal('и')) shouldBe "\"и\""
      serialized(_.writeVal('ბ')) shouldBe "\"ბ\""
    }
    "write strings with escaped unicode chars" in {
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\u0000')) shouldBe "\"\\u0000\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\u001f')) shouldBe "\"\\u001f\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\u007F')) shouldBe "\"\\u007f\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\b')) shouldBe "\"\\b\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\f')) shouldBe "\"\\f\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\n')) shouldBe "\"\\n\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\r')) shouldBe "\"\\r\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\t')) shouldBe "\"\\t\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('и')) shouldBe "\"\\u0438\""
      serialized(WriterConfig(escapeUnicode = true))(_.writeVal('ბ')) shouldBe "\"\\u10d1\""
    }
    "throw i/o exception in case of surrogate pair character" in {
      assert(intercept[IOException](serialized(_.writeVal('\udd1e'))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(_.writeVal('\ud834'))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\udd1e'))).getMessage.contains("illegal char sequence of surrogate pair"))
      assert(intercept[IOException](serialized(WriterConfig(escapeUnicode = true))(_.writeVal('\ud834'))).getMessage.contains("illegal char sequence of surrogate pair"))
    }
  }
  "JsonWriter.writeVal for int" should {
    "write int values" in {
      serialized(_.writeVal(0)) shouldBe "0"
      serialized(_.writeVal(-0)) shouldBe "0"
      serialized(_.writeVal(123)) shouldBe "123"
      serialized(_.writeVal(-123)) shouldBe "-123"
      serialized(_.writeVal(123456)) shouldBe "123456"
      serialized(_.writeVal(-123456)) shouldBe "-123456"
      serialized(_.writeVal(123456789)) shouldBe "123456789"
      serialized(_.writeVal(-123456789)) shouldBe "-123456789"
      serialized(_.writeVal(2147483647)) shouldBe "2147483647"
      serialized(_.writeVal(-2147483648)) shouldBe "-2147483648"
    }
  }
  "JsonWriter.writeVal long" should {
    "write long values" in {
      serialized(_.writeVal(0L)) shouldBe "0"
      serialized(_.writeVal(-0L)) shouldBe "0"
      serialized(_.writeVal(123L)) shouldBe "123"
      serialized(_.writeVal(-123L)) shouldBe "-123"
      serialized(_.writeVal(123456L)) shouldBe "123456"
      serialized(_.writeVal(-123456L)) shouldBe "-123456"
      serialized(_.writeVal(123456789L)) shouldBe "123456789"
      serialized(_.writeVal(-123456789L)) shouldBe "-123456789"
      serialized(_.writeVal(123456789012L)) shouldBe "123456789012"
      serialized(_.writeVal(-123456789012L)) shouldBe "-123456789012"
      serialized(_.writeVal(123456789012345L)) shouldBe "123456789012345"
      serialized(_.writeVal(-123456789012345L)) shouldBe "-123456789012345"
      serialized(_.writeVal(123456789012345678L)) shouldBe "123456789012345678"
      serialized(_.writeVal(-123456789012345678L)) shouldBe "-123456789012345678"
      serialized(_.writeVal(9223372036854775807L)) shouldBe "9223372036854775807"
      serialized(_.writeVal(-9223372036854775808L)) shouldBe "-9223372036854775808"
    }
  }
  "JsonWriter.writeVal for float" should {
    "write float values" in {
      serialized(_.writeVal(0.0f)) shouldBe "0.0"
      serialized(_.writeVal(-0.0f)) shouldBe "-0.0"
      serialized(_.writeVal(12345.678f)) shouldBe "12345.678"
      serialized(_.writeVal(-12345.678f)) shouldBe "-12345.678"
      serialized(_.writeVal(1.23456788e14f)) shouldBe "1.23456788E14"
      serialized(_.writeVal(-1.2345679e-6f)) shouldBe "-1.2345679E-6"
    }
    "throw i/o exception on illegal JSON numbers" in {
      assert(intercept[IOException](serialized(_.writeVal(0.0f/0.0f))).getMessage.contains("illegal number"))
      assert(intercept[IOException](serialized(_.writeVal(1.0f/0.0f))).getMessage.contains("illegal number"))
      assert(intercept[IOException](serialized(_.writeVal(-1.0f/0.0f))).getMessage.contains("illegal number"))
    }
  }
  "JsonWriter.writeVal for double" should {
    "write double values" in {
      serialized(_.writeVal(0.0)) shouldBe "0.0"
      serialized(_.writeVal(-0.0)) shouldBe "-0.0"
      serialized(_.writeVal(123456789.12345678)) shouldBe "1.2345678912345678E8"
      serialized(_.writeVal(-123456789.12345678)) shouldBe "-1.2345678912345678E8"
      serialized(_.writeVal(123456789.123456e10)) shouldBe "1.23456789123456E18"
      serialized(_.writeVal(-123456789.123456e-10)) shouldBe "-0.0123456789123456"
    }
    "throw i/o exception on illegal JSON numbers" in {
      assert(intercept[IOException](serialized(_.writeVal(0.0/0.0))).getMessage.contains("illegal number"))
      assert(intercept[IOException](serialized(_.writeVal(1.0/0.0))).getMessage.contains("illegal number"))
      assert(intercept[IOException](serialized(_.writeVal(-1.0/0.0))).getMessage.contains("illegal number"))
    }
  }
  "JsonWriter.writeVal for BigInt" should {
    "write null value" in {
      serialized(_.writeVal(null.asInstanceOf[BigInt])) shouldBe "null"
    }
    "write number values" in {
      serialized(_.writeVal(BigInt("0"))) shouldBe "0"
      serialized(_.writeVal(BigInt("-0"))) shouldBe "0"
      serialized(_.writeVal(BigInt("12345678901234567890123456789"))) shouldBe "12345678901234567890123456789"
      serialized(_.writeVal(BigInt("-12345678901234567890123456789"))) shouldBe "-12345678901234567890123456789"
    }
  }
  "JsonWriter.writeVal for BigDecimal" should {
    "write null value" in {
      serialized(_.writeVal(null.asInstanceOf[BigDecimal])) shouldBe "null"
    }
    "write number values" in {
      serialized(_.writeVal(BigDecimal("0"))) shouldBe "0"
      serialized(_.writeVal(BigDecimal("-0"))) shouldBe "0"
      serialized(_.writeVal(BigDecimal("1234567890123456789.0123456789"))) shouldBe "1234567890123456789.0123456789"
      serialized(_.writeVal(BigDecimal("-1234567890123456789.0123456789"))) shouldBe "-1234567890123456789.0123456789"
    }
  }

  def serialized(f: JsonWriter => Unit): String = serialized(WriterConfig())(f)

  def serialized(cfg: WriterConfig)(f: JsonWriter => Unit): String = {
    val out = new ByteArrayOutputStream(1024)
    val writer = new JsonWriter(new Array[Byte](1), 0, 0, out, true, cfg)
    try f(writer)
    finally writer.flushBuffer()
    out.toString("UTF-8")
  }
}
