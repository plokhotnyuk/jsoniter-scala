package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, InputStream}
import java.math.MathContext
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Base64, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import java.time.format.DateTimeParseException
import scala.collection.AbstractIterator
import scala.util.Random

class JsonReaderSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "ReaderConfig.<init>" should {
    "have safe and handy defaults" in {
      ReaderConfig.throwReaderExceptionWithStackTrace shouldBe false
      ReaderConfig.appendHexDumpToParseException shouldBe true
      ReaderConfig.maxBufSize shouldBe 33554432
      ReaderConfig.maxCharBufSize shouldBe 4194304
      ReaderConfig.preferredBufSize shouldBe 32768
      ReaderConfig.preferredCharBufSize shouldBe 4096
      ReaderConfig.hexDumpSize shouldBe 2
    }
    "throw exception in case for unsupported values of params" in {
      ReaderConfig.withMaxBufSize(32768).maxBufSize shouldBe 32768
      assert(intercept[IllegalArgumentException](ReaderConfig.withMaxBufSize(32767))
        .getMessage.startsWith("'maxBufSize' should be not less than 'preferredBufSize'"))
      assert(intercept[IllegalArgumentException](ReaderConfig.withMaxBufSize(2147483646))
        .getMessage.startsWith("'maxBufSize' should be not greater than 2147483645"))
      ReaderConfig.withMaxCharBufSize(4096).maxCharBufSize shouldBe 4096
      assert(intercept[IllegalArgumentException](ReaderConfig.withMaxCharBufSize(4095))
        .getMessage.startsWith("'maxCharBufSize' should be not less than 'preferredCharBufSize'"))
      assert(intercept[IllegalArgumentException](ReaderConfig.withMaxCharBufSize(2147483646))
        .getMessage.startsWith("'maxCharBufSize' should be not greater than 2147483645"))
      ReaderConfig.withPreferredBufSize(12).preferredBufSize shouldBe 12
      assert(intercept[IllegalArgumentException](ReaderConfig.withPreferredBufSize(11))
        .getMessage.startsWith("'preferredBufSize' should be not less than 12"))
      assert(intercept[IllegalArgumentException](ReaderConfig.withPreferredBufSize(33554433))
        .getMessage.startsWith("'preferredBufSize' should be not greater than 'maxBufSize'"))
      ReaderConfig.withPreferredCharBufSize(0).preferredCharBufSize shouldBe 0
      assert(intercept[IllegalArgumentException](ReaderConfig.withPreferredCharBufSize(-1))
        .getMessage.startsWith("'preferredCharBufSize' should be not less than 0"))
      assert(intercept[IllegalArgumentException](ReaderConfig.withPreferredCharBufSize(4194305))
        .getMessage.startsWith("'preferredCharBufSize' should be not greater than 'maxCharBufSize'"))
      ReaderConfig.withHexDumpSize(5).hexDumpSize shouldBe 5
      assert(intercept[IllegalArgumentException](ReaderConfig.withHexDumpSize(0))
        .getMessage.startsWith("'hexDumpSize' should be not less than 1"))
    }
  }
  "JsonReader.toHashCode" should {
    "produce the same hash value for strings as JDK by default" in {
      forAll(arbitrary[String], minSuccessful(10000)) { x =>
        assert(JsonReader.toHashCode(x.toCharArray, x.length) == x.hashCode)
      }
    }
    "produce 0 hash value when the provided 'len' isn't greater than 0" in {
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        whenever(x <= 0) {
          assert(JsonReader.toHashCode("VVV".toCharArray, x) == 0)
        }
      }
    }
    "throw exception in case null value for the char array is provided" in {
      intercept[NullPointerException](JsonReader.toHashCode(null, 1))
    }
    "throw exception in case the char array length is less than the provided 'len'" in {
      intercept[ArrayIndexOutOfBoundsException](JsonReader.toHashCode("XXX".toCharArray, 4))
    }
  }
  "JsonReader.skip" should {
    def validateSkip(s: String, ws: String): Unit = {
      def checkWithSuffix(s: String, suffix: Char): Unit = {
        val r = reader(ws + s + suffix)
        r.skip()
        r.nextToken().toChar shouldBe suffix
      }

      def check(s: String): Unit = {
        val r = reader(ws + s)
        r.skip()
        assert(intercept[JsonReaderException](r.nextToken()).getMessage.startsWith("unexpected end of input"))
      }

      checkWithSuffix(s, ',')
      checkWithSuffix(s, '}')
      checkWithSuffix(s, ']')
      check(s)
    }

    "skip string values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip("""""""", ws)
        validateSkip("""" """", ws)
        validateSkip(""""["""", ws)
        validateSkip(""""{"""", ws)
        validateSkip(""""0"""", ws)
        validateSkip(""""9"""", ws)
        validateSkip(""""-"""", ws)
      }
    }
    "throw parsing exception when skipping string that is not closed by parentheses" in {
      assert(intercept[JsonReaderException](validateSkip(""""""", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](validateSkip(""""abc""", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000005"))
    }
    "skip string values with escaped characters" in {
      forAll(genWhitespaces) { ws =>
        validateSkip(""""\\"""", ws)
        validateSkip(""""\\\"\\"""", ws)
      }
    }
    "skip number values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip("0", ws)
        validateSkip("-0.0", ws)
        validateSkip("7.1e+123456789", ws)
        validateSkip("8.1E-123456789", ws)
        validateSkip("987654321.0E+10", ws)
      }
    }
    "skip boolean values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip("true", ws)
        validateSkip("false", ws)
      }
    }
    "throw parsing exception when skipping truncated boolean value" in {
      assert(intercept[JsonReaderException](validateSkip("t", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](validateSkip("f", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
    }
    "skip null values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip("null", ws)
      }
    }
    "skip object values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip("{}", ws)
        validateSkip("{{{{{}}}}{{{}}}}", ws)
        validateSkip("""{"{"}""", ws)
      }
    }
    "throw parsing exception when skipping not closed object" in {
      assert(intercept[JsonReaderException](validateSkip("{{}", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000004"))
    }
    "skip array values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip("[]", ws)
        validateSkip("[[[[[]]]][[[]]]]", ws)
        validateSkip("""["["]""", ws)
      }
    }
    "throw parsing exception when skipping not closed array" in {
      assert(intercept[JsonReaderException](validateSkip("[[]", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000004"))
    }
    "skip mixed values" in {
      forAll(genWhitespaces) { ws =>
        validateSkip(
          """
            |{
            |  "x": {
            |    "xx": [
            |      -1.0,
            |      1,
            |      4.0E20
            |    ],
            |    "yy": {
            |      "xxx": true,
            |      "yyy": false,
            |      "zzz": null
            |    }
            |  },
            |  "y": [
            |    [1, 2, 3],
            |    [4, 5, 6],
            |    [7, 8, 9]
            |  ]
            |}""".stripMargin, ws)
      }
    }
    "throw parsing exception when skipping not from start of JSON value" in {
      def checkError(invalidInput: String): Unit =
        assert(intercept[JsonReaderException](validateSkip(invalidInput, ""))
          .getMessage.startsWith("expected value, offset: 0x00000000"))

      checkError("]")
      checkError("}")
      checkError(",")
      checkError(":")
    }
  }
  "JsonReader.skipWhitespaces" should {
    "be visible in the jsoniter_scala package" in {
      def toJsonValuesIteratorFromStream[A](in: InputStream, config: ReaderConfig = ReaderConfig)
                                           (implicit codec: JsonValueCodec[A]): Iterator[A] =
        new AbstractIterator[A] {
          private[this] val reader = new JsonReader(
            buf = new Array[Byte](config.preferredBufSize),
            charBuf = new Array[Char](config.preferredCharBufSize),
            in = in,
            config = config)
          private[this] var continue: Boolean = reader.skipWhitespaces()

          override def hasNext: Boolean = continue

          override def next(): A = {
            val x = codec.decodeValue(reader, codec.nullValue)
            continue = reader.skipWhitespaces()
            x
          }
        }

      def toInputStream(s: String): InputStream = new ByteArrayInputStream(s.getBytes(UTF_8))

      implicit val intCodec: JsonValueCodec[Int] = new JsonValueCodec[Int] {
        override def decodeValue(in: JsonReader, default: Int): Int = in.readInt()

        override def encodeValue(x: Int, out: JsonWriter): Unit = out.writeVal(x)

        override def nullValue: Int = 0
      }
      assert(toJsonValuesIteratorFromStream(toInputStream("")).toList == List())
      assert(toJsonValuesIteratorFromStream(toInputStream("\n\n")).toList == List())
      assert(toJsonValuesIteratorFromStream(toInputStream("1")).toList == List(1))
      assert(toJsonValuesIteratorFromStream(toInputStream("1\n2\n3")).toList == List(1, 2, 3))
      assert(toJsonValuesIteratorFromStream(toInputStream("\n1\n2\n\n3\n")).toList == List(1, 2, 3))
      assert(intercept[JsonReaderException](toJsonValuesIteratorFromStream(toInputStream("01\n")).toList)
        .getMessage.startsWith("illegal number with leading zero, offset: 0x00000000"))
    }
  }
  "JsonReader.endOfInputOrError" should {
    "be visible in the jsoniter_scala package" in {
      def toJsonArrayIteratorFromStream[A](in: InputStream, config: ReaderConfig = ReaderConfig)
                                           (implicit codec: JsonValueCodec[A]): Iterator[A] =
        new AbstractIterator[A] {
          private[this] val reader = new JsonReader(
            buf = new Array[Byte](config.preferredBufSize),
            charBuf = new Array[Char](config.preferredCharBufSize),
            in = in,
            config = config)
          private[this] var continue: Boolean =
            if (reader.isNextToken('[')) !reader.isNextToken(']') && {
              reader.rollbackToken()
              true
            } else reader.readNullOrTokenError(false, '[')

          override def hasNext: Boolean = continue

          override def next(): A = {
            val x = codec.decodeValue(reader, codec.nullValue)
            continue = reader.isNextToken(',') || checkEndConditions()
            x
          }

          private[this] def checkEndConditions(): Boolean =
            (reader.isCurrentToken(']') || reader.decodeError("expected ']' or ','")) &&
              config.checkForEndOfInput && {
                reader.endOfInputOrError()
                false
              }
        }

      def toInputStream(s: String): InputStream = new ByteArrayInputStream(s.getBytes(UTF_8))

      implicit val intCodec: JsonValueCodec[Int] = new JsonValueCodec[Int] {
        override def decodeValue(in: JsonReader, default: Int): Int = in.readInt()

        override def encodeValue(x: Int, out: JsonWriter): Unit = out.writeVal(x)

        override def nullValue: Int = 0
      }
      assert(toJsonArrayIteratorFromStream(toInputStream("null")).toList == List())
      assert(toJsonArrayIteratorFromStream(toInputStream("[]")).toList == List())
      assert(toJsonArrayIteratorFromStream(toInputStream("[1]")).toList == List(1))
      assert(toJsonArrayIteratorFromStream(toInputStream("[1,2,3]")).toList == List(1, 2, 3))
      assert(toJsonArrayIteratorFromStream(toInputStream("\n[1, \n2,\n 3\n]")).toList == List(1, 2, 3))
      assert(intercept[JsonReaderException](toJsonArrayIteratorFromStream(toInputStream("")).toList)
        .getMessage.startsWith("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonReaderException](toJsonArrayIteratorFromStream(toInputStream("1")).toList)
        .getMessage.startsWith("expected '[' or null, offset: 0x00000000"))
      assert(intercept[JsonReaderException](toJsonArrayIteratorFromStream(toInputStream("[01]")).toList)
        .getMessage.startsWith("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonReaderException](toJsonArrayIteratorFromStream(toInputStream("[1")).toList)
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](toJsonArrayIteratorFromStream(toInputStream("[1]1")).toList)
        .getMessage.startsWith("expected end of input, offset: 0x00000003"))
    }
  }
  "JsonReader.readRawValueAsBytes" should {
    def check(s: String, ws: String): Unit =
      new String(reader(ws + s).readRawValAsBytes(), UTF_8) shouldBe ws + s

    "read raw values" in {
      check("""""""", " ")
      forAll(genWhitespaces) { ws =>
        check("""""""", ws)
        check("""" """", ws)
        check(""""["""", ws)
        check(""""{"""", ws)
        check(""""0"""", ws)
        check(""""9"""", ws)
        check(""""-"""", ws)
      }
    }
    "throw parsing exception when reading string that is not closed by parentheses" in {
      assert(intercept[JsonReaderException](check(""""""", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonReaderException](check(""""abc""", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000004"))
    }
    "read raw string values with escaped characters" in {
      forAll(genWhitespaces) { ws =>
        check(""""\\"""", ws)
        check(""""\\\"\\"""", ws)
      }
    }
    "read raw number values" in {
      forAll(genWhitespaces) { ws =>
        check("0", ws)
        check("-0.0", ws)
        check("7.1e+123456789", ws)
        check("8.1E-123456789", ws)
        check("987654321.0E+10", ws)
      }
    }
    "read raw boolean values" in {
      forAll(genWhitespaces) { ws =>
        check("true", ws)
        check("false", ws)
      }
    }
    "throw parsing exception when reading truncated boolean value" in {
      assert(intercept[JsonReaderException](check("t", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonReaderException](check("f", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000001"))
    }
    "read raw null values" in {
      forAll(genWhitespaces) { ws =>
        check("null", ws)
      }
    }
    "read raw object values" in {
      forAll(genWhitespaces) { ws =>
        check("{}", ws)
        check("{{{{{}}}}{{{}}}}", ws)
        check("""{"{"}""", ws)
      }
    }
    "throw parsing exception when reading not closed object" in {
      assert(intercept[JsonReaderException](check("{{}", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000003"))
    }
    "read raw array values" in {
      forAll(genWhitespaces) { ws =>
        check("[]", ws)
        check("[[[[[]]]][[[]]]]", ws)
        check("""["["]""", ws)
      }
    }
    "throw parsing exception when reading not closed array" in {
      assert(intercept[JsonReaderException](check("[[]", ""))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000003"))
    }
    "read raw mixed values" in {
      forAll(genWhitespaces) { ws =>
        check(
          """
            |{
            |  "x": {
            |    "xx": [
            |      -1.0,
            |      1,
            |      4.0E20
            |    ],
            |    "yy": {
            |      "xxx": true,
            |      "yyy": false,
            |      "zzz": null
            |    }
            |  },
            |  "y": [
            |    [1, 2, 3],
            |    [4, 5, 6],
            |    [7, 8, 9]
            |  ]
            |}""".stripMargin, ws)
      }
    }
    "throw parsing exception when reading not from start of JSON value" in {
      def checkError(invalidInput: String): Unit =
        assert(intercept[JsonReaderException](check(invalidInput, ""))
          .getMessage.startsWith("expected value, offset: 0x00000000"))

      checkError("]")
      checkError("}")
      checkError(",")
      checkError(":")
    }
  }
  "JsonReader.nextByte" should {
    "return next byte of input" in {
      val r = reader("{\n}")
      assert(r.nextByte() == '{')
      assert(r.nextByte() == '\n')
      assert(r.nextByte() == '}')
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}")
      r.skip()
      assert(intercept[JsonReaderException](r.nextByte() == '{')
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.nextToken" should {
    "find next non-whitespace byte of input" in {
      val r = reader("{\n}")
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '}')
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}")
      r.skip()
      assert(intercept[JsonReaderException](r.nextToken() == '{')
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.isNextToken" should {
    "return true in case of the next parsed token matches with provided one" in {
      val jsonReader = reader("{\n}")
      jsonReader.isNextToken('{') shouldBe true
      jsonReader.isNextToken('}') shouldBe true
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}")
      r.skip()
      assert(intercept[JsonReaderException](r.isNextToken('{'))
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.isCurrentToken" should {
    "return true in case of the recently parsed token matches with provided one" in {
      val jsonReader = reader("{\n}")
      jsonReader.nextToken()
      jsonReader.isCurrentToken('{') shouldBe true
      jsonReader.isNextToken('}')
      jsonReader.isCurrentToken('}') shouldBe true
    }
    "throw exception in case of isCurrentToken was called before nextToken or isNextToken" in {
      val jsonReader = reader("{\n}")
      assert(intercept[IllegalStateException](jsonReader.isCurrentToken('{'))
        .getMessage.startsWith("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readANullOrError" should {
    "parse null value" in {
      val r = reader("null")
      r.isNextToken('n') shouldBe true
      r.readNullOrError("default", "error") shouldBe "default"
    }
    "throw parse exception in case of invalid null value" in {
      def checkError(s: String, error: String): Unit = {
        assert(intercept[JsonReaderException] {
          val r = reader(s)
          r.isNextToken('n') shouldBe true
          r.readNullOrError("default", "expected null")
        }.getMessage.startsWith(error))
      }

      checkError("", "unexpected end of input, offset: 0x00000000")
      checkError("n", "unexpected end of input, offset: 0x00000001")
      checkError("nu", "unexpected end of input, offset: 0x00000002")
      checkError("nul", "unexpected end of input, offset: 0x00000003")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonU = if (ch == 'u') 'X' else ch
        val nonL = if (ch == 'l') 'X' else ch
        checkError(s"n${nonU}ll", """expected null, offset: 0x00000001""")
        checkError(s"nu${nonL}l", """expected null, offset: 0x00000002""")
        checkError(s"nul${nonL}", """expected null, offset: 0x00000003""")
      }
    }
    "throw array index out of bounds exception in case of call without preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[IllegalStateException](reader("null").readNullOrError("default", "error"))
        .getMessage.startsWith("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readNullOrTokenError" should {
    "parse null value" in {
      val r = reader("null")
      r.isNextToken('n') shouldBe true
      r.readNullOrTokenError("default", 'x') shouldBe "default"
    }
    "throw parse exception in case of invalid null value" in {
      def checkError(s: String, error: String): Unit = {
        assert(intercept[JsonReaderException] {
          val r = reader(s)
          val _ = r.isNextToken('n')
          r.readNullOrTokenError("default", '\"')
        }.getMessage.startsWith(error))
      }

      checkError("", "unexpected end of input, offset: 0x00000000")
      checkError("n", "unexpected end of input, offset: 0x00000001")
      checkError("nu", "unexpected end of input, offset: 0x00000002")
      checkError("nul", "unexpected end of input, offset: 0x00000003")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonU = if (ch == 'u') 'X' else ch
        val nonL = if (ch == 'l') 'X' else ch
        checkError(s"n${nonU}ll", """expected '"' or null, offset: 0x00000001""")
        checkError(s"nu${nonL}l", """expected '"' or null, offset: 0x00000002""")
        checkError(s"nul${nonL}", """expected '"' or null, offset: 0x00000003""")
      }
    }
    "throw array index out of bounds exception in case of call without preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[IllegalStateException](reader("null").readNullOrError("default", "error"))
        .getMessage.startsWith("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.rollbackToken" should {
    "rollback of reading last byte of input" in {
      val r = reader("""{"x":1}""")
      assert(r.nextToken() == '{')
      r.rollbackToken()
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '"')
      r.rollbackToken()
      assert(r.nextToken() == '"')
    }
    "throw array index out of bounds exception in case of missing preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[IllegalStateException](reader("{}").rollbackToken())
        .getMessage.startsWith("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readBoolean, JsonReader.readStringAsBoolean and JsonReader.readKeyAsBoolean" should {
    def check(s: String, value: Boolean, ws: String): Unit = {
      reader(ws + s).readBoolean() shouldBe value
      reader(s"""$ws"$s"""").readStringAsBoolean() shouldBe value
      reader(s"""$ws"$s":""").readKeyAsBoolean() shouldBe value
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readBoolean()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsBoolean()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsBoolean()).getMessage.startsWith(error2))
    }

    "parse valid true and false values" in {
      forAll(genWhitespaces) { ws =>
        check("true", value = true, ws)
        check("false", value = false, ws)
      }
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      checkError("tru", "unexpected end of input, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("fals", "unexpected end of input, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonTorForWhitespace = if (ch == 't' || ch == 'f' || ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') 'X' else ch
        val nonR = if (ch == 'r') 'X' else ch
        val nonU = if (ch == 'u') 'X' else ch
        val nonE = if (ch == 'e') 'X' else ch
        val nonA = if (ch == 'a') 'X' else ch
        val nonL = if (ch == 'l') 'X' else ch
        val nonS = if (ch == 's') 'X' else ch
        checkError(s"${nonTorForWhitespace}rue", "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
        checkError(s"t${nonR}ue", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
        checkError(s"tr${nonU}e", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
        checkError(s"tru${nonE}", "illegal boolean, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
        checkError(s"${nonTorForWhitespace}alse", "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
        checkError(s"f${nonA}lse", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
        checkError(s"fa${nonL}se", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
        checkError(s"fal${nonS}e", "illegal boolean, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
        checkError(s"fals${nonE}", "illegal boolean, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
      }
    }
  }
  "JsonReader.readKeyAsUUID" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonReaderException](reader(""""00000000-0000-0000-0000-000000000000"""").readKeyAsUUID())
        .getMessage.startsWith("unexpected end of input, offset: 0x00000026"))
      assert(intercept[JsonReaderException](reader(""""00000000-0000-0000-0000-000000000000"x""").readKeyAsUUID())
        .getMessage.startsWith("expected ':', offset: 0x00000026"))
    }
  }
  "JsonReader.readUUID and JsonReader.readKeyAsUUID" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readUUID(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsUUID())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = new UUID(0, 0)
      reader("null").readUUID(default) shouldBe default
    }
    "parse UUID from a string representation according to format that defined in IETF RFC4122 (section 3)" in {
      def check(x: UUID, ws: String): Unit = {
        val s = x.toString
        reader(s"""$ws"${s.toLowerCase}"""").readUUID(null) shouldBe x
        reader(s"""$ws"${s.toUpperCase}"""").readUUID(null) shouldBe x
        reader(s"""$ws"${s.toLowerCase}":""").readKeyAsUUID() shouldBe x
        reader(s"""$ws"${s.toUpperCase}":""").readKeyAsUUID() shouldBe x
      }

      forAll(genUUID, genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception for empty input and illegal or broken UUID string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readUUID(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsUUID()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError("""""""", "unexpected end of input, offset: 0x00000002")
      checkError(""""00000000-0000-0000-0000-000000000000""", "unexpected end of input, offset: 0x00000025")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonHexDigit = if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        checkError(s""""${nonHexDigit}0000000-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000001")
        checkError(s""""0${nonHexDigit}000000-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000002")
        checkError(s""""00${nonHexDigit}00000-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000003")
        checkError(s""""000${nonHexDigit}0000-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000004")
        checkError(s""""0000${nonHexDigit}000-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000005")
        checkError(s""""00000${nonHexDigit}00-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000006")
        checkError(s""""000000${nonHexDigit}0-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000007")
        checkError(s""""0000000${nonHexDigit}-0000-0000-0000-000000000000"""", "expected hex digit, offset: 0x00000008")
        checkError(s""""00000000${nonDash}0000-0000-0000-000000000000"""", "expected '-', offset: 0x00000009")
        checkError(s""""00000000-${nonHexDigit}000-0000-0000-000000000000"""", "expected hex digit, offset: 0x0000000a")
        checkError(s""""00000000-0${nonHexDigit}00-0000-0000-000000000000"""", "expected hex digit, offset: 0x0000000b")
        checkError(s""""00000000-00${nonHexDigit}0-0000-0000-000000000000"""", "expected hex digit, offset: 0x0000000c")
        checkError(s""""00000000-000${nonHexDigit}-0000-0000-000000000000"""", "expected hex digit, offset: 0x0000000d")
        checkError(s""""00000000-0000${nonDash}0000-0000-000000000000"""", "expected '-', offset: 0x0000000e")
        checkError(s""""00000000-0000-${nonHexDigit}000-0000-000000000000"""", "expected hex digit, offset: 0x0000000f")
        checkError(s""""00000000-0000-0${nonHexDigit}00-0000-000000000000"""", "expected hex digit, offset: 0x00000010")
        checkError(s""""00000000-0000-00${nonHexDigit}0-0000-000000000000"""", "expected hex digit, offset: 0x00000011")
        checkError(s""""00000000-0000-000${nonHexDigit}-0000-000000000000"""", "expected hex digit, offset: 0x00000012")
        checkError(s""""00000000-0000-0000${nonDash}0000-000000000000"""", "expected '-', offset: 0x00000013")
        checkError(s""""00000000-0000-0000-${nonHexDigit}000-000000000000"""", "expected hex digit, offset: 0x00000014")
        checkError(s""""00000000-0000-0000-0${nonHexDigit}00-000000000000"""", "expected hex digit, offset: 0x00000015")
        checkError(s""""00000000-0000-0000-00${nonHexDigit}0-000000000000"""", "expected hex digit, offset: 0x00000016")
        checkError(s""""00000000-0000-0000-000${nonHexDigit}-000000000000"""", "expected hex digit, offset: 0x00000017")
        checkError(s""""00000000-0000-0000-0000${nonDash}000000000000"""", "expected '-', offset: 0x00000018")
        checkError(s""""00000000-0000-0000-0000-${nonHexDigit}00000000000"""", "expected hex digit, offset: 0x00000019")
        checkError(s""""00000000-0000-0000-0000-0${nonHexDigit}0000000000"""", "expected hex digit, offset: 0x0000001a")
        checkError(s""""00000000-0000-0000-0000-00${nonHexDigit}000000000"""", "expected hex digit, offset: 0x0000001b")
        checkError(s""""00000000-0000-0000-0000-000${nonHexDigit}00000000"""", "expected hex digit, offset: 0x0000001c")
        checkError(s""""00000000-0000-0000-0000-0000${nonHexDigit}0000000"""", "expected hex digit, offset: 0x0000001d")
        checkError(s""""00000000-0000-0000-0000-00000${nonHexDigit}000000"""", "expected hex digit, offset: 0x0000001e")
        checkError(s""""00000000-0000-0000-0000-000000${nonHexDigit}00000"""", "expected hex digit, offset: 0x0000001f")
        checkError(s""""00000000-0000-0000-0000-0000000${nonHexDigit}0000"""", "expected hex digit, offset: 0x00000020")
        checkError(s""""00000000-0000-0000-0000-00000000${nonHexDigit}000"""", "expected hex digit, offset: 0x00000021")
        checkError(s""""00000000-0000-0000-0000-000000000${nonHexDigit}00"""", "expected hex digit, offset: 0x00000022")
        checkError(s""""00000000-0000-0000-0000-0000000000${nonHexDigit}0"""", "expected hex digit, offset: 0x00000023")
        checkError(s""""00000000-0000-0000-0000-00000000000${nonHexDigit}"""", "expected hex digit, offset: 0x00000024")
        checkError(s""""00000000-0000-0000-0000-000000000000${nonDoubleQuotes}""", """expected '"', offset: 0x00000025""")
      }
    }
  }
  "JsonReader.readBase16AsBytes" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readBase16AsBytes(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = new Array[Byte](0)
      reader("null").readBase16AsBytes(default) shouldBe default
    }
    "parse Base16 representation according to format that defined in RFC4648" in {
      val toHex: Array[Char] = "0123456789abcdef".toCharArray

      def toBase16(bs: Array[Byte]): String = {
        val sb = (new StringBuilder).append('"')
        var i = 0
        while (i < bs.length) {
          val b = bs(i)
          sb.append(toHex(b >> 4 & 0xF)).append(toHex(b & 0xF))
          i += 1
        }
        sb.append('"').toString
      }

      def check(s: String, ws: String): Unit = {
        val bs = s.getBytes(UTF_8)
        val base16LowerCase = toBase16(bs)
        val base16UpperCase = base16LowerCase.toUpperCase
        toBase16(reader(ws + base16LowerCase).readBase16AsBytes(null)) shouldBe base16LowerCase
        toBase16(reader(ws + base16UpperCase).readBase16AsBytes(null)) shouldBe base16LowerCase
      }

      forAll(arbitrary[String], genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception for empty input and illegal or too long Base16 string" in {
      def checkError(json: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(json).readBase16AsBytes(null)).getMessage.startsWith(error))

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""0""", "unexpected end of input, offset: 0x00000002")
      checkError(""""00""", "unexpected end of input, offset: 0x00000003")
      checkError(""""000""", "unexpected end of input, offset: 0x00000004")
      checkError(""""0000""", "unexpected end of input, offset: 0x00000005")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonHexDigit = if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f') 'X' else ch
        val nonHexDigitOrDoubleQuotes = if (nonHexDigit == '"') 'X' else nonHexDigit
        checkError(s""""${nonHexDigitOrDoubleQuotes}000"""", """expected '"' or hex digit, offset: 0x00000001""")
        checkError(s""""0${nonHexDigit}00"""", "expected hex digit, offset: 0x00000002")
        checkError(s""""00${nonHexDigitOrDoubleQuotes}0"""", """expected '"' or hex digit, offset: 0x00000003""")
        checkError(s""""000${nonHexDigit}"""", "expected hex digit, offset: 0x00000004")
      }
      val sb = new StringBuilder
      sb.append('"')
      var i = 0
      while (i < (ReaderConfig.maxCharBufSize + 1) * 4) {
        sb.append('1')
        i += 1
      }
      sb.append('"')
      checkError(sb.toString, "too long string exceeded 'maxCharBufSize'")
    }
  }
  "JsonReader.readBase64AsBytes and JsonReader.readBase64UrlAsBytes" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readBase64AsBytes(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readBase64UrlAsBytes(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = new Array[Byte](0)
      reader("null").readBase64AsBytes(default) shouldBe default
      reader("null").readBase64UrlAsBytes(default) shouldBe default
    }
    "parse Base64 representation according to format that defined in RFC4648" in {
      def check(s: String, ws: String): Unit = {
        val bs = s.getBytes(UTF_8)
        val base64 = s""""${Base64.getEncoder.encodeToString(bs)}""""
        val base64Url = s""""${Base64.getUrlEncoder.encodeToString(bs)}""""
        val base64WithoutPadding = s""""${Base64.getEncoder.withoutPadding.encodeToString(bs)}""""
        val base64UrlWithoutPadding = s""""${Base64.getUrlEncoder.withoutPadding.encodeToString(bs)}""""
        s""""${Base64.getEncoder.encodeToString(reader(ws + base64).readBase64AsBytes(null))}"""" shouldBe base64
        s""""${Base64.getUrlEncoder.encodeToString(reader(ws + base64Url).readBase64UrlAsBytes(null))}"""" shouldBe base64Url
        s""""${Base64.getEncoder.withoutPadding.encodeToString(reader(ws + base64WithoutPadding).readBase64AsBytes(null))}"""" shouldBe base64WithoutPadding
        s""""${Base64.getUrlEncoder.withoutPadding.encodeToString(reader(ws + base64UrlWithoutPadding).readBase64UrlAsBytes(null))}"""" shouldBe base64UrlWithoutPadding
      }

      forAll(arbitrary[String], genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception for empty input and illegal or too long base64 string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readBase64AsBytes(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readBase64UrlAsBytes(null)).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""0""", "unexpected end of input, offset: 0x00000002")
      checkError(""""00""", "unexpected end of input, offset: 0x00000003")
      checkError(""""000""", "unexpected end of input, offset: 0x00000004")
      checkError(""""0000""", "unexpected end of input, offset: 0x00000005")
      checkError(""""!000"""", """expected '"' or base64 digit, offset: 0x00000001""")
      checkError(""""0!00"""", "expected base64 digit, offset: 0x00000002")
      checkError(""""00!0"""", """expected '"' or '=' or base64 digit, offset: 0x00000003""")
      checkError(""""000!"""", """expected '"' or '=', offset: 0x00000004""")
      checkError(""""00=!"""", "expected '=', offset: 0x00000004")
      checkError(""""00==!""", """expected '"', offset: 0x00000005""")
      checkError(""""000=!""", """expected '"', offset: 0x00000005""")
      val sb = new StringBuilder
      sb.append('"')
      var i = 0
      while (i < (ReaderConfig.maxCharBufSize + 1) * 3) {
        sb.append('1')
        i += 1
      }
      sb.append('"')
      checkError(sb.toString, "too long string exceeded 'maxCharBufSize'")
    }
  }
  "JsonReader.readDuration and JsonReader.readKeyAsDuration" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readDuration(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsDuration())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = Duration.parse("P2DT3H4M")
      reader("null").readDuration(default) shouldBe default
    }
    "parse Duration from a string representation according to JDK format that is based on ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = Duration.parse(s)
        reader(s"""$ws"$s"""").readDuration(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsDuration() shouldBe x
        reader(s"""$ws"-$s"""").readDuration(null) shouldBe x.negated()
        reader(s"""$ws"-$s":""").readKeyAsDuration() shouldBe x.negated()
      }

      forAll(genWhitespaces) { ws =>
        check("P0D", ws)
        check("PT0S", ws)
        check("P1D", ws)
        check("PT1S", ws)
        check("P-1D", ws)
        check("PT-1S", ws)
      }
      forAll(genDuration, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken Duration string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readDuration(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsDuration()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError("""""""", "expected 'P' or '-', offset: 0x00000001")
      checkError(""""-"""", "expected 'P', offset: 0x00000002")
      checkError(""""PXD"""", "expected '-' or digit, offset: 0x00000002")
      checkError(""""PT0SX""", """expected '"', offset: 0x00000005""")
      checkError(""""P-XD"""", "expected digit, offset: 0x00000003")
      checkError(""""P1XD"""", "expected 'D' or digit, offset: 0x00000003")
      checkError(""""P106751991167301D"""", "illegal duration, offset: 0x00000011")
      checkError(""""P1067519911673000D"""", "illegal duration, offset: 0x00000012")
      checkError(""""P-106751991167301D"""", "illegal duration, offset: 0x00000012")
      checkError(""""P1DX1H"""", """expected 'T' or '"', offset: 0x00000004""")
      checkError(""""P1DTXH"""", "expected '-' or digit, offset: 0x00000005")
      checkError(""""P1DT-XH"""", "expected digit, offset: 0x00000006")
      checkError(""""P1DT1XH"""", "expected 'H' or 'M' or 'S or '.' or digit, offset: 0x00000006")
      checkError(""""P0DT2562047788015216H"""", "illegal duration, offset: 0x00000015")
      checkError(""""P0DT-2562047788015216H"""", "illegal duration, offset: 0x00000016")
      checkError(""""P0DT153722867280912931M"""", "illegal duration, offset: 0x00000017")
      checkError(""""P0DT-153722867280912931M"""", "illegal duration, offset: 0x00000018")
      checkError(""""P0DT9223372036854775808S"""", "illegal duration, offset: 0x00000018")
      checkError(""""P0DT92233720368547758000S"""", "illegal duration, offset: 0x00000018")
      checkError(""""P0DT-9223372036854775809S"""", "illegal duration, offset: 0x00000018")
      checkError(""""P1DT1HXM"""", """expected '"' or '-' or digit, offset: 0x00000007""")
      checkError(""""P1DT1H-XM"""", "expected digit, offset: 0x00000008")
      checkError(""""P1DT1H1XM"""", "expected 'M' or 'S or '.' or digit, offset: 0x00000008")
      checkError(""""P0DT0H153722867280912931M"""", "illegal duration, offset: 0x00000019")
      checkError(""""P0DT0H-153722867280912931M"""", "illegal duration, offset: 0x0000001a")
      checkError(""""P0DT0H9223372036854775808S"""", "illegal duration, offset: 0x0000001a")
      checkError(""""P0DT0H92233720368547758000S"""", "illegal duration, offset: 0x0000001a")
      checkError(""""P0DT0H-9223372036854775809S"""", "illegal duration, offset: 0x0000001a")
      checkError(""""P1DT1H1MXS"""", """expected '"' or '-' or digit, offset: 0x00000009""")
      checkError(""""P1DT1H1M-XS"""", "expected digit, offset: 0x0000000a")
      checkError(""""P1DT1H1M0XS"""", "expected 'S or '.' or digit, offset: 0x0000000a")
      checkError(""""P1DT1H1M0.XS"""", "expected 'S' or digit, offset: 0x0000000b")
      checkError(""""P1DT1H1M0.012345678XS"""", "expected 'S', offset: 0x00000014")
      checkError(""""P1DT1H1M0.0123456789S"""", "expected 'S', offset: 0x00000014")
      checkError(""""P0DT0H0M9223372036854775808S"""", "illegal duration, offset: 0x0000001c")
      checkError(""""P0DT0H0M92233720368547758080S"""", "illegal duration, offset: 0x0000001c")
      checkError(""""P0DT0H0M-9223372036854775809S"""", "illegal duration, offset: 0x0000001c")
      checkError(""""P106751991167300DT24H"""", "illegal duration, offset: 0x00000015")
      checkError(""""P0DT2562047788015215H60M"""", "illegal duration, offset: 0x00000018")
      checkError(""""P0DT0H153722867280912930M60S"""", "illegal duration, offset: 0x0000001c")
    }
  }
  "JsonReader.readInstant and JsonReader.readKeyAsInstant" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readInstant(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsInstant())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = Instant.parse("2008-01-20T07:24:33Z")
      reader("null").readInstant(default) shouldBe default
    }
    "parse Instant from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = try {
          Instant.parse(s)
        } catch {
          case _: DateTimeParseException => OffsetDateTime.parse(s).toInstant
        }
        reader(s"""$ws"$s"""").readInstant(null) shouldBe x
        reader(s"""$ws"$s": """).readKeyAsInstant() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("+999999999-12-31T23:59:59.999999999Z", ws)
        check("-999999999-01-01T00:00:00Z", ws)
        check("2008-01-01T07:24:33Z", ws)
        check("2008-01-01T07:24:33.Z", ws)
        check("2008-01-01T07:24:33.123Z", ws)
        check("2008-01-01T07:24:33.123456Z", ws)
        check("2008-01-01T07:24:33.123456789Z", ws)
        check("2008-01-01T07:24:33.123456789+11:22:33", ws)
        check("2008-01-01T07:24:33.123456789-11:22:33", ws)
      }
      forAll(genInstant, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
      forAll(genOffsetDateTime, genWhitespaces, minSuccessful(10000)) { (x, ws) =>
        check((if (x.getSecond == 0) x.plusSeconds(1) else x).toString, ws)
      }
    }
    "throw parsing exception for empty input and illegal or broken Instant string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readInstant(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsInstant()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008-01-20T07:24:33Z""", "unexpected end of input, offset: 0x00000015")
      checkError(""""+1000000000=01-20T07:24:33Z"""", "expected '-', offset: 0x0000000c")
      checkError(""""-0000-01-20T07:24:33Z"""", "illegal year, offset: 0x00000005")
      checkError(""""+1000000001-01-20T07:24:33Z"""", "illegal year, offset: 0x0000000b")
      checkError(""""+4000000000-01-20T07:24:33Z"""", "illegal year, offset: 0x0000000b")
      checkError(""""+9999999999-01-20T07:24:33Z"""", "illegal year, offset: 0x0000000b")
      checkError(""""-1000000001-01-20T07:24:33Z"""", "illegal year, offset: 0x0000000b")
      checkError(""""-4000000000-01-20T07:24:33Z"""", "illegal year, offset: 0x0000000b")
      checkError(""""-9999999999-01-20T07:24:33Z"""", "illegal year, offset: 0x0000000b")
      checkError(""""2008-00-20T07:24:33Z"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-13-20T07:24:33Z"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-01-00T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2007-02-29T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-02-30T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-03-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-04-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-05-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-06-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-07-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-08-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-09-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-10-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-11-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-12-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-20T24:24:33Z"""", "illegal hour, offset: 0x0000000d")
      checkError(""""2008-01-20T07:60:33Z"""", "illegal minute, offset: 0x00000010")
      checkError(""""2008-01-20T07:24:60Z"""", "illegal second, offset: 0x00000013")
      checkError(""""2008-01-20T07:24:33.+20:10"""", "illegal timezone offset hour, offset: 0x00000017")
      checkError(""""2008-01-20T07:24:33.+10:60"""", "illegal timezone offset minute, offset: 0x0000001a")
      checkError(""""2008-01-20T07:24:33.+10:10:60"""", "illegal timezone offset second, offset: 0x0000001d")
      checkError(""""2008-01-20T07:24:33.+18:00:01"""", "illegal timezone offset, offset: 0x0000001e")
      checkError(""""2008-01-20T07:24:33.-18:00:01"""", "illegal timezone offset, offset: 0x0000001e")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrSignOrZ= if (ch >= '0' && ch <= '9' || ch == '+' || ch == '-' || ch == 'Z') 'X' else ch
        val nonDigitOrDash= if (ch >= '0' && ch <= '9' || ch == '-') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        val nonT = if (ch == 'T') 'X' else ch
        val nonColon = if (ch == ':') 'X' else ch
        val nonDotOrSignOrZ = if (ch == '.' || ch == '+' || ch == '-' || ch == 'Z') 'X' else ch
        val nonSignOrZ = if (ch == '+' || ch == '-' || ch == 'Z') 'X' else ch
        val nonColonOrDoubleQuotes = if (ch == ':' || ch == '"') 'X' else ch
        checkError(s""""${nonNumber}008-01-20T07:24:33Z"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08-01-20T07:24:33Z"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8-01-20T07:24:33Z"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}-01-20T07:24:33Z"""", "expected digit, offset: 0x00000004")
        checkError(s""""2008${nonDash}01-20T07:24:33Z"""", "expected '-', offset: 0x00000005")
        checkError(s""""2008-${nonDigit}0-20T07:24:33Z"""", "expected digit, offset: 0x00000006")
        checkError(s""""2008-0${nonDigit}-20T07:24:33Z"""", "expected digit, offset: 0x00000007")
        checkError(s""""2008-01${nonDash}20T07:24:33Z"""", "expected '-', offset: 0x00000008")
        checkError(s""""2008-01-${nonDigit}0T07:24:33Z"""", "expected digit, offset: 0x00000009")
        checkError(s""""2008-01-2${nonDigit}T07:24:33Z"""", "expected digit, offset: 0x0000000a")
        checkError(s""""2008-01-20${nonT}07:24:33Z"""", "expected 'T', offset: 0x0000000b")
        checkError(s""""2008-01-20T${nonDigit}7:24:33Z"""", "expected digit, offset: 0x0000000c")
        checkError(s""""2008-01-20T0${nonDigit}:24:33Z"""", "expected digit, offset: 0x0000000d")
        checkError(s""""2008-01-20T07${nonColon}24:33Z"""", "expected ':', offset: 0x0000000e")
        checkError(s""""2008-01-20T07:${nonDigit}4:33Z"""", "expected digit, offset: 0x0000000f")
        checkError(s""""2008-01-20T07:2${nonDigit}:33Z"""", "expected digit, offset: 0x00000010")
        checkError(s""""2008-01-20T07:24${nonColon}33Z"""", "expected ':', offset: 0x00000011")
        checkError(s""""2008-01-20T07:24:${nonDigit}3Z"""", "expected digit, offset: 0x00000012")
        checkError(s""""2008-01-20T07:24:3${nonDigit}Z"""", "expected digit, offset: 0x00000013")
        checkError(s""""2008-01-20T07:24:33${nonDotOrSignOrZ}"""", "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
        checkError(s""""2008-01-20T07:24:33Z${nonDoubleQuotes}"""", """expected '"', offset: 0x00000015""")
        checkError(s""""2008-01-20T07:24:33.${nonDigitOrSignOrZ}"""", "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
        checkError(s""""2008-01-20T07:24:33.000${nonDigitOrSignOrZ}"""", "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
        checkError(s""""2008-01-20T07:24:33.123456789${nonSignOrZ}"""", "expected '+' or '-' or 'Z', offset: 0x0000001e")
        checkError(s""""2008-01-20T07:24:33+${nonDigit}0"""", "expected digit, offset: 0x00000015")
        checkError(s""""2008-01-20T07:24:33-${nonDigit}0"""", "expected digit, offset: 0x00000015")
        checkError(s""""2008-01-20T07:24:33.+${nonDigit}0"""", "expected digit, offset: 0x00000016")
        checkError(s""""2008-01-20T07:24:33.+1${nonDigit}"""", "expected digit, offset: 0x00000017")
        checkError(s""""2008-01-20T07:24:33.+10${nonColonOrDoubleQuotes}"""", """expected ':' or '"', offset: 0x00000018""")
        checkError(s""""2008-01-20T07:24:33.+10:${nonDigit}0"""", "expected digit, offset: 0x00000019")
        checkError(s""""2008-01-20T07:24:33.+10:1${nonDigit}"""", "expected digit, offset: 0x0000001a")
        checkError(s""""2008-01-20T07:24:33.+10:10${nonColonOrDoubleQuotes}10"""", """expected ':' or '"', offset: 0x0000001b""")
        checkError(s""""2008-01-20T07:24:33.+10:10:${nonDigit}0"""", "expected digit, offset: 0x0000001c")
        checkError(s""""2008-01-20T07:24:33.+10:10:1${nonDigit}"""", "expected digit, offset: 0x0000001d")
        checkError(s""""+${nonDigit}0000-01-20T07:24:33Z"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000-01-20T07:24:33Z"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00-01-20T07:24:33Z"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0-01-20T07:24:33Z"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigit}-01-20T07:24:33Z"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000006")
        checkError(s""""+10000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000007")
        checkError(s""""+100000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000008")
        checkError(s""""+1000000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000009")
        checkError(s""""+10000000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x0000000a")
        checkError(s""""+100000000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x0000000b")
      }
    }
  }
  "JsonReader.readLocalDate and JsonReader.readKeyAsLocalDate" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readLocalDate(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsLocalDate())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDate.parse("2008-01-20")
      reader("null").readLocalDate(default) shouldBe default
    }
    "parse LocalDate from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = LocalDate.parse(s)
        reader(s"""$ws"$s"""").readLocalDate(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsLocalDate() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("+999999999-12-31", ws)
        check("-999999999-01-01", ws)
        check("2008-01-20", ws)
      }
      forAll(genLocalDate, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken LocalDate string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readLocalDate(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsLocalDate()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008-01-20""", "unexpected end of input, offset: 0x0000000b")
      checkError(""""+1000000000-01-20"""", "expected '-', offset: 0x0000000b")
      checkError(""""-1000000000-01-20"""", "expected '-', offset: 0x0000000b")
      checkError(""""-0000-01-20"""", "illegal year, offset: 0x00000005")
      checkError(""""2008-00-20"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-13-20"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-01-00"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-32"""", "illegal day, offset: 0x0000000a")
      checkError(""""2007-02-29"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-02-30"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-03-32"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-04-31"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-05-32"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-06-31"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-07-32"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-08-32"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-09-31"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-10-32"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-11-31"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-12-32"""", "illegal day, offset: 0x0000000a")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDash = if (ch >= '0' && ch <= '9' || ch == '-') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        checkError(s""""${nonNumber}008-01-20"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08-01-20"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8-01-20"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}-01-20"""", "expected digit, offset: 0x00000004")
        checkError(s""""2008${nonDash}01-20"""", "expected '-', offset: 0x00000005")
        checkError(s""""+${nonDigit}0000-01-20"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000-01-20"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00-01-20"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0-01-20"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigit}-01-20"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDash}-01-20"""", "expected '-' or digit, offset: 0x00000006")
        checkError(s""""+10000${nonDigitOrDash}-01-20"""", "expected '-' or digit, offset: 0x00000007")
        checkError(s""""+100000${nonDigitOrDash}-01-20"""", "expected '-' or digit, offset: 0x00000008")
        checkError(s""""+1000000${nonDigitOrDash}-01-20"""", "expected '-' or digit, offset: 0x00000009")
        checkError(s""""+10000000${nonDigitOrDash}-01-20"""", "expected '-' or digit, offset: 0x0000000a")
        checkError(s""""+999999999${nonDash}01-20"""", "expected '-', offset: 0x0000000b")
        checkError(s""""2008-${nonDigit}1-20"""", "expected digit, offset: 0x00000006")
        checkError(s""""2008-0${nonDigit}-20"""", "expected digit, offset: 0x00000007")
        checkError(s""""2008-01${nonDash}20"""", "expected '-', offset: 0x00000008")
        checkError(s""""2008-01-${nonDigit}0"""", "expected digit, offset: 0x00000009")
        checkError(s""""2008-01-2${nonDigit}"""", "expected digit, offset: 0x0000000a")
        checkError(s""""2008-01-20${nonDoubleQuotes}"""", """expected '"', offset: 0x0000000b""")
      }
    }
  }
  "JsonReader.readLocalDateTime and JsonReader.readKeyAsLocalDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readLocalDateTime(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsLocalDateTime())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDateTime.parse("2008-01-20T07:24:33")
      reader("null").readLocalDateTime(default) shouldBe default
    }
    "parse LocalDateTime from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = LocalDateTime.parse(s)
        reader(s"""$ws"$s"""").readLocalDateTime(null) shouldBe x
        reader(s"""$ws"$s":$ws""").readKeyAsLocalDateTime() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("+999999999-12-31T23:59:59.999999999", ws)
        check("-999999999-01-01T00:00:00", ws)
        check("2008-01-01T07:24", ws)
        check("2008-01-01T07:24:33.", ws)
        check("2008-01-01T07:24:33.000", ws)
        check("2008-01-01T07:24:33.000000", ws)
        check("2008-01-01T07:24:33.000000000", ws)
      }
      forAll(genLocalDateTime, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readLocalDateTime(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsLocalDateTime()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008-01-20T07:24:33""", "unexpected end of input, offset: 0x00000014")
      checkError(""""+1000000000-01-20T07:24:33"""", "expected '-', offset: 0x0000000b")
      checkError(""""-1000000000-01-20T07:24:33"""", "expected '-', offset: 0x0000000b")
      checkError(""""-0000-01-20T07:24:33"""", "illegal year, offset: 0x00000005")
      checkError(""""2008-00-20T07:24:33"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-13-20T07:24:33"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-01-00T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2007-02-29T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-02-30T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-03-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-04-31T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-05-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-06-31T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-07-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-08-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-09-31T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-10-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-11-31T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-12-32T07:24:33"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-20T24:24:33"""", "illegal hour, offset: 0x0000000d")
      checkError(""""2008-01-20T07:60:33"""", "illegal minute, offset: 0x00000010")
      checkError(""""2008-01-20T07:24:60"""", "illegal second, offset: 0x00000013")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDash = if (ch >= '0' && ch <= '9' || ch == '-') 'X' else ch
        val nonDigitOrDoubleQuotes = if (ch >= '0' && ch <= '9' || ch == '"') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        val nonT = if (ch == 'T') 'X' else ch
        val nonColon = if (ch == ':') 'X' else ch
        val nonColonOrDoubleQuotes = if (ch == ':' || ch == '"') 'X' else ch
        val nonDotOrDoubleQuotes = if (ch == '.' || ch == '"') 'X' else ch
        checkError(s""""${nonNumber}008-01-20T07:24:33"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08-01-20T07:24:33"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8-01-20T07:24:33"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}-01-20T07:24:33"""", "expected digit, offset: 0x00000004")
        checkError(s""""2008${nonDash}01-20T07:24:33"""", "expected '-', offset: 0x00000005")
        checkError(s""""+${nonDigit}0000-01-20T07:24:33"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000-01-20T07:24:33"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00-01-20T07:24:33"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0-01-20T07:24:33"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigit}-01-20T07:24:33"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDash}-01-20T07:24:33"""", "expected '-' or digit, offset: 0x00000006")
        checkError(s""""+10000${nonDigitOrDash}-01-20T07:24:33"""", "expected '-' or digit, offset: 0x00000007")
        checkError(s""""+100000${nonDigitOrDash}-01-20T07:24:33"""", "expected '-' or digit, offset: 0x00000008")
        checkError(s""""+1000000${nonDigitOrDash}-01-20T07:24:33"""", "expected '-' or digit, offset: 0x00000009")
        checkError(s""""+10000000${nonDigitOrDash}-01-20T07:24:33"""", "expected '-' or digit, offset: 0x0000000a")
        checkError(s""""+999999999${nonDash}01-20T07:24:33"""", "expected '-', offset: 0x0000000b")
        checkError(s""""2008-${nonDigit}1-20T07:24:33"""", "expected digit, offset: 0x00000006")
        checkError(s""""2008-0${nonDigit}-20T07:24:33"""", "expected digit, offset: 0x00000007")
        checkError(s""""2008-01${nonDash}20T07:24:33"""", "expected '-', offset: 0x00000008")
        checkError(s""""2008-01-${nonDigit}0T07:24:33"""", "expected digit, offset: 0x00000009")
        checkError(s""""2008-01-2${nonDigit}T07:24:33"""", "expected digit, offset: 0x0000000a")
        checkError(s""""2008-01-20${nonT}07:24:33"""", "expected 'T', offset: 0x0000000b")
        checkError(s""""2008-01-20T${nonDigit}7:24:33"""", "expected digit, offset: 0x0000000c")
        checkError(s""""2008-01-20T0${nonDigit}:24:33"""", "expected digit, offset: 0x0000000d")
        checkError(s""""2008-01-20T07${nonColon}24:33"""", "expected ':', offset: 0x0000000e")
        checkError(s""""2008-01-20T07:${nonDigit}4:33"""", "expected digit, offset: 0x0000000f")
        checkError(s""""2008-01-20T07:2${nonDigit}:33"""", "expected digit, offset: 0x00000010")
        checkError(s""""2008-01-20T07:24${nonColonOrDoubleQuotes}33"""", """expected ':' or '"', offset: 0x00000011""")
        checkError(s""""2008-01-20T07:24:${nonDigit}3"""", "expected digit, offset: 0x00000012")
        checkError(s""""2008-01-20T07:24:3${nonDigit}"""", "expected digit, offset: 0x00000013")
        checkError(s""""2008-01-20T07:24:33${nonDotOrDoubleQuotes}"""", """expected '.' or '"', offset: 0x00000014""")
        checkError(s""""2008-01-20T07:24:33.${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x00000015""")
        checkError(s""""2008-01-20T07:24:33.123456789${nonDoubleQuotes}"""", """expected '"', offset: 0x0000001e""")
      }
    }
  }
  "JsonReader.readLocalTime and JsonReader.readKeyAsLocalTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readLocalTime(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsLocalTime())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = LocalTime.parse("07:24:33")
      reader("null").readLocalTime(default) shouldBe default
    }
    "parse LocalTime from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = LocalTime.parse(s)
        reader(s"""$ws"$s"""").readLocalTime(null) shouldBe x
        reader(s"""$ws"$s":$ws""").readKeyAsLocalTime() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("23:59:59.999999999", ws)
        check("00:00", ws)
        check("07:24:33", ws)
        check("07:24:33.", ws)
        check("07:24:33.000", ws)
        check("07:24:33.000000", ws)
        check("07:24:33.000000000", ws)
      }
      forAll(genLocalTime, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readLocalTime(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsLocalTime()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""07:24:33""", "unexpected end of input, offset: 0x00000009")
      checkError(""""24:24:33"""", "illegal hour, offset: 0x00000002")
      checkError(""""07:60:33"""", "illegal minute, offset: 0x00000005")
      checkError(""""07:24:60"""", "illegal second, offset: 0x00000008")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDoubleQuotes = if (ch >= '0' && ch <= '9' || ch == '"') 'X' else ch
        val nonColon = if (ch == ':') 'X' else ch
        val nonColonOrDoubleQuotes = if (ch == ':' || ch == '"') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        val nonDotOrDoubleQuotes = if (ch == '.' || ch == '"') 'X' else ch
        checkError(s""""${nonDigit}7:24:33"""", "expected digit, offset: 0x00000001")
        checkError(s""""0${nonDigit}:24:33"""", "expected digit, offset: 0x00000002")
        checkError(s""""07${nonColon}24:33"""", "expected ':', offset: 0x00000003")
        checkError(s""""07:${nonDigit}4:33"""", "expected digit, offset: 0x00000004")
        checkError(s""""07:2${nonDigit}:33"""", "expected digit, offset: 0x00000005")
        checkError(s""""07:24${nonColonOrDoubleQuotes}33"""", """expected ':' or '"', offset: 0x00000006""")
        checkError(s""""07:24:${nonDigit}3"""", "expected digit, offset: 0x00000007")
        checkError(s""""07:24:3${nonDigit}"""", "expected digit, offset: 0x00000008")
        checkError(s""""07:24:33${nonDotOrDoubleQuotes}"""", """expected '.' or '"', offset: 0x00000009""")
        checkError(s""""07:24:33.${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x0000000a""")
        checkError(s""""07:24:33.123456789${nonDoubleQuotes}"""", """expected '"', offset: 0x00000013""")
      }
    }
  }
  "JsonReader.readMonthDay and JsonReader.readKeyAsMonthDay" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readMonthDay(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsMonthDay())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = MonthDay.parse("--01-20")
      reader("null").readMonthDay(default) shouldBe default
    }
    "parse MonthDay from a string representation according to ISO-8601 format" in {
      def check(x: MonthDay, ws: String): Unit = {
        val s = x.toString
        reader(s"""$ws"$s"""").readMonthDay(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsMonthDay() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check(MonthDay.of(12, 31), ws)
        check(MonthDay.of(1, 1), ws)
      }
      forAll(genMonthDay, genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readMonthDay(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsMonthDay()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""=-01-20"""", "expected '-', offset: 0x00000001")
      checkError(""""-=01-20"""", "expected '-', offset: 0x00000002")
      checkError(""""--00-20"""", "illegal month, offset: 0x00000004")
      checkError(""""--13-20"""", "illegal month, offset: 0x00000004")
      checkError(""""--01-00"""", "illegal day, offset: 0x00000007")
      checkError(""""--01-32"""", "illegal day, offset: 0x00000007")
      checkError(""""--02-30"""", "illegal day, offset: 0x00000007")
      checkError(""""--03-32"""", "illegal day, offset: 0x00000007")
      checkError(""""--04-31"""", "illegal day, offset: 0x00000007")
      checkError(""""--05-32"""", "illegal day, offset: 0x00000007")
      checkError(""""--06-31"""", "illegal day, offset: 0x00000007")
      checkError(""""--07-32"""", "illegal day, offset: 0x00000007")
      checkError(""""--08-32"""", "illegal day, offset: 0x00000007")
      checkError(""""--09-31"""", "illegal day, offset: 0x00000007")
      checkError(""""--10-32"""", "illegal day, offset: 0x00000007")
      checkError(""""--11-31"""", "illegal day, offset: 0x00000007")
      checkError(""""--12-32"""", "illegal day, offset: 0x00000007")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        checkError(s""""--${nonDigit}1-20"""", "expected digit, offset: 0x00000003")
        checkError(s""""--0${nonDigit}-20"""", "expected digit, offset: 0x00000004")
        checkError(s""""--01${nonDash}20"""", "expected '-', offset: 0x00000005")
        checkError(s""""--01-${nonDigit}0"""", "expected digit, offset: 0x00000006")
        checkError(s""""--01-2${nonDigit}"""", "expected digit, offset: 0x00000007")
        checkError(s""""--01-20${nonDoubleQuotes}"""", """expected '"', offset: 0x00000008""")
      }
    }
  }
  "JsonReader.readOffsetDateTime and JsonReader.readKeyAsOffsetDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readOffsetDateTime(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsOffsetDateTime())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetDateTime.parse("2008-01-20T07:24Z")
      reader("null").readOffsetDateTime(default) shouldBe default
    }
    "parse OffsetDateTime from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = OffsetDateTime.parse(s)
        reader(s"""$ws"$s"""").readOffsetDateTime(null) shouldBe x
        reader(s"""$ws"$s":$ws""").readKeyAsOffsetDateTime() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("+999999999-12-31T23:59:59.999999999-18:00", ws)
        check("-999999999-01-01T00:00:00+18:00", ws)
        check("2008-01-20T07:24Z", ws)
        check("2008-01-20T07:24:33.Z", ws)
        check("2008-01-20T07:24:33.000Z", ws)
        check("2008-01-20T07:24:33.000000000Z", ws)
        check("2008-01-20T07:24:33.000000000+00:00", ws)
      }
      forAll(genOffsetDateTime, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken OffsetDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readOffsetDateTime(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsOffsetDateTime()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008-01-20T07:24:33Z""", "unexpected end of input, offset: 0x00000015")
      checkError(""""+1000000000-01-20T07:24:33Z"""", "expected '-', offset: 0x0000000b")
      checkError(""""-1000000000-01-20T07:24:33Z"""", "expected '-', offset: 0x0000000b")
      checkError(""""-0000-01-20T07:24:33Z"""", "illegal year, offset: 0x00000005")
      checkError(""""2008-00-20T07:24:33Z"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-13-20T07:24:33Z"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-01-00T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2007-02-29T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-02-30T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-03-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-04-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-05-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-06-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-07-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-08-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-09-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-10-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-11-31T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-12-32T07:24:33Z"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-20T24:24:33Z"""", "illegal hour, offset: 0x0000000d")
      checkError(""""2008-01-20T07:60:33Z"""", "illegal minute, offset: 0x00000010")
      checkError(""""2008-01-20T07:24:60Z"""", "illegal second, offset: 0x00000013")
      checkError(""""2008-01-20T07:24:33.+20:10"""", "illegal timezone offset hour, offset: 0x00000017")
      checkError(""""2008-01-20T07:24:33.+10:60"""", "illegal timezone offset minute, offset: 0x0000001a")
      checkError(""""2008-01-20T07:24:33.+10:10:60"""", "illegal timezone offset second, offset: 0x0000001d")
      checkError(""""2008-01-20T07:24:33.+18:00:01"""", "illegal timezone offset, offset: 0x0000001e")
      checkError(""""2008-01-20T07:24:33.-18:00:01"""", "illegal timezone offset, offset: 0x0000001e")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonNumberOrZ = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDash = if (ch >= '0' && ch <= '9' || ch == '-') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        val nonT = if (ch == 'T') 'X' else ch
        val nonColon = if (ch == ':') 'X' else ch
        val nonColonOrDoubleQuotes = if (ch == ':' || ch == '"') 'X' else ch
        val nonColonOrSignOrZ = if (ch == ':' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        val nonDotOrSignOrZ = if (ch == '.' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        val nonSignOrZ = if (ch == '.' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        checkError(s""""${nonNumber}008-01-20T07:24:33Z"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08-01-20T07:24:33Z"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8-01-20T07:24:33Z"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}-01-20T07:24:33Z"""", "expected digit, offset: 0x00000004")
        checkError(s""""2008${nonDash}01-20T07:24:33Z"""", "expected '-', offset: 0x00000005")
        checkError(s""""+${nonDigit}0000-01-20T07:24:33Z"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000-01-20T07:24:33Z"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00-01-20T07:24:33Z"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0-01-20T07:24:33Z"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigit}-01-20T07:24:33Z"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000006")
        checkError(s""""+10000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000007")
        checkError(s""""+100000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000008")
        checkError(s""""+1000000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x00000009")
        checkError(s""""+10000000${nonDigitOrDash}-01-20T07:24:33Z"""", "expected '-' or digit, offset: 0x0000000a")
        checkError(s""""+999999999${nonDash}01-20T07:24:33Z"""", "expected '-', offset: 0x0000000b")
        checkError(s""""2008-${nonDigit}1-20T07:24:33Z"""", "expected digit, offset: 0x00000006")
        checkError(s""""2008-0${nonDigit}-20T07:24:33Z"""", "expected digit, offset: 0x00000007")
        checkError(s""""2008-01${nonDash}20T07:24:33Z"""", "expected '-', offset: 0x00000008")
        checkError(s""""2008-01-${nonDigit}0T07:24:33Z"""", "expected digit, offset: 0x00000009")
        checkError(s""""2008-01-2${nonDigit}T07:24:33Z"""", "expected digit, offset: 0x0000000a")
        checkError(s""""2008-01-20${nonT}07:24:33Z"""", "expected 'T', offset: 0x0000000b")
        checkError(s""""2008-01-20T${nonDigit}7:24:33Z"""", "expected digit, offset: 0x0000000c")
        checkError(s""""2008-01-20T0${nonDigit}:24:33Z"""", "expected digit, offset: 0x0000000d")
        checkError(s""""2008-01-20T07${nonColon}24:33Z"""", "expected ':', offset: 0x0000000e")
        checkError(s""""2008-01-20T07:${nonDigit}4:33Z"""", "expected digit, offset: 0x0000000f")
        checkError(s""""2008-01-20T07:2${nonDigit}:33Z"""", "expected digit, offset: 0x00000010")
        checkError(s""""2008-01-20T07:24${nonColonOrSignOrZ}33Z"""", "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
        checkError(s""""2008-01-20T07:24:${nonDigit}3Z"""", "expected digit, offset: 0x00000012")
        checkError(s""""2008-01-20T07:24:3${nonDigit}Z"""", "expected digit, offset: 0x00000013")
        checkError(s""""2008-01-20T07:24:33${nonDotOrSignOrZ}"""", "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
        checkError(s""""2008-01-20T07:24:33Z${nonDoubleQuotes}"""", """expected '"', offset: 0x00000015""")
        checkError(s""""2008-01-20T07:24:33.${nonNumberOrZ}"""", "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
        checkError(s""""2008-01-20T07:24:33.000${nonNumberOrZ}"""", "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
        checkError(s""""2008-01-20T07:24:33.123456789${nonSignOrZ}"""", "expected '+' or '-' or 'Z', offset: 0x0000001e")
        checkError(s""""2008-01-20T07:24+${nonDigit}0"""", "expected digit, offset: 0x00000012")
        checkError(s""""2008-01-20T07:24+1${nonDigit}"""", "expected digit, offset: 0x00000013")
        checkError(s""""2008-01-20T07:24:33+${nonDigit}0"""", "expected digit, offset: 0x00000015")
        checkError(s""""2008-01-20T07:24:33-${nonDigit}0"""", "expected digit, offset: 0x00000015")
        checkError(s""""2008-01-20T07:24:33.+${nonDigit}0"""", "expected digit, offset: 0x00000016")
        checkError(s""""2008-01-20T07:24:33.+1${nonDigit}"""", "expected digit, offset: 0x00000017")
        checkError(s""""2008-01-20T07:24:33.+10${nonColonOrDoubleQuotes}"""", """expected ':' or '"', offset: 0x00000018""")
        checkError(s""""2008-01-20T07:24:33.+10:${nonDigit}0"""", "expected digit, offset: 0x00000019")
        checkError(s""""2008-01-20T07:24:33.+10:1${nonDigit}"""", "expected digit, offset: 0x0000001a")
        checkError(s""""2008-01-20T07:24:33.+10:10${nonColonOrDoubleQuotes}10"""", """expected ':' or '"', offset: 0x0000001b""")
        checkError(s""""2008-01-20T07:24:33.+10:10:${nonDigit}0"""", "expected digit, offset: 0x0000001c")
        checkError(s""""2008-01-20T07:24:33.+10:10:1${nonDigit}"""", "expected digit, offset: 0x0000001d")
      }
    }
  }
  "JsonReader.readOffsetTime and JsonReader.readKeyAsOffsetTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readOffsetTime(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsOffsetTime())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetTime.parse("07:24:33+01:00")
      reader("null").readOffsetTime(default) shouldBe default
    }
    "parse OffsetTime from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = OffsetTime.parse(s)
        reader(s"""$ws"$s"""").readOffsetTime(null) shouldBe x
        reader(s"""$ws"$s":$ws""").readKeyAsOffsetTime() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("23:59:59.999999999-18:00", ws)
        check("00:00:00+18:00", ws)
        check("07:24Z", ws)
        check("07:24:33.Z", ws)
        check("07:24:33.000Z", ws)
        check("07:24:33.000000000Z", ws)
        check("07:24:33.000000000+00:00", ws)
      }
      forAll(genOffsetTime, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken OffsetTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readOffsetTime(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsOffsetTime()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""07:24:33Z""", "unexpected end of input, offset: 0x0000000a")
      checkError(""""24:24:33Z"""", "illegal hour, offset: 0x00000002")
      checkError(""""07:60:33Z"""", "illegal minute, offset: 0x00000005")
      checkError(""""07:24:60Z"""", "illegal second, offset: 0x00000008")
      checkError(""""07:24:33.+19:10"""", "illegal timezone offset hour, offset: 0x0000000c")
      checkError(""""07:24:33.+10:60"""", "illegal timezone offset minute, offset: 0x0000000f")
      checkError(""""07:24:33.+10:10:60"""", "illegal timezone offset second, offset: 0x00000012")
      checkError(""""07:24:33.+18:00:01"""", "illegal timezone offset, offset: 0x00000013")
      checkError(""""07:24:33.-18:00:01"""", "illegal timezone offset, offset: 0x00000013")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumberOrZ = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        val nonSignOrZ = if (ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonColon = if (ch == ':') 'X' else ch
        val nonColonOrDoubleQuotes = if (ch == ':' || ch == '"') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        val nonDotOrSignOrZ = if (ch == '.' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        val nonColonOrSignOrZ = if (ch == ':' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        checkError(s""""${nonDigit}7:24:33Z"""", "expected digit, offset: 0x00000001")
        checkError(s""""0${nonDigit}:24:33Z"""", "expected digit, offset: 0x00000002")
        checkError(s""""07${nonColon}24:33Z"""", "expected ':', offset: 0x00000003")
        checkError(s""""07:${nonDigit}4:33Z"""", "expected digit, offset: 0x00000004")
        checkError(s""""07:2${nonDigit}:33Z"""", "expected digit, offset: 0x00000005")
        checkError(s""""07:24${nonColonOrSignOrZ}33Z"""", "expected ':' or '+' or '-' or 'Z', offset: 0x00000006")
        checkError(s""""07:24:${nonDigit}3Z"""", "expected digit, offset: 0x00000007")
        checkError(s""""07:24:3${nonDigit}Z"""", "expected digit, offset: 0x00000008")
        checkError(s""""07:24:33${nonDotOrSignOrZ}"""", "expected '.' or '+' or '-' or 'Z', offset: 0x00000009")
        checkError(s""""07:24:33.${nonNumberOrZ}"""", "expected '+' or '-' or 'Z' or digit, offset: 0x0000000a")
        checkError(s""""07:24:33.123456789${nonSignOrZ}"""", "expected '+' or '-' or 'Z', offset: 0x00000013")
        checkError(s""""07:24:33.+10${nonColonOrDoubleQuotes}"""", """expected ':' or '"', offset: 0x0000000d""")
        checkError(s""""07:24:33.+10:${nonDigit}"""", "expected digit, offset: 0x0000000e")
        checkError(s""""07:24:33.+10:1${nonDigit}"""", "expected digit, offset: 0x0000000f")
        checkError(s""""07:24:33.+10:10${nonColonOrDoubleQuotes}10"""", """expected ':' or '"', offset: 0x00000010""")
        checkError(s""""07:24:33.+10:10:${nonDigit}0"""", "expected digit, offset: 0x00000011")
        checkError(s""""07:24:33.+10:10:1${nonDigit}"""", "expected digit, offset: 0x00000012")
        checkError(s""""07:24:33.+10:10:10${nonDoubleQuotes}"""", """expected '"', offset: 0x00000013""")
      }
    }
  }
  "JsonReader.readPeriod and JsonReader.readKeyAsPeriod" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readPeriod(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsPeriod())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = Period.parse("P1Y2M3D")
      reader("null").readPeriod(default) shouldBe default
    }
    "parse Period from a string representation according to JDK format that is based on ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = Period.parse(s)
        reader(s"""$ws"$s"""").readPeriod(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsPeriod() shouldBe x
        if (x.getYears != Int.MinValue && x.getMonths != Int.MinValue && x.getDays != Int.MinValue) {
          reader(s"""$ws"-$s"""").readPeriod(null) shouldBe x.negated()
          reader(s"""$ws"-$s":""").readKeyAsPeriod() shouldBe x.negated()
        }
      }

      forAll(genWhitespaces) { ws =>
        check("P0D", ws)
      }
      forAll(genPeriod, genWhitespaces, minSuccessful(1000))((x, ws) => check(x.toString, ws))
      forAll(arbitrary[Int], arbitrary[Int], genWhitespaces, minSuccessful(1000)) { (x, y, ws) =>
        check(s"P${x}Y", ws)
        check(s"P${x}M", ws)
        check(s"P${x}D", ws)
        check(s"P${x}Y${y}M", ws)
        check(s"P${x}M${y}D", ws)
        check(s"P${x}Y${y}D", ws)
      }
      forAll(Gen.choose(-1000000, 1000000), genWhitespaces, minSuccessful(1000)) { (w, ws) =>
        check(s"P${w}W", ws)
        check(s"P1Y${w}W", ws)
        check(s"P1Y1M${w}W", ws)
      }
      forAll(Gen.choose(-1000000, 1000000), Gen.choose(-1000000, 1000000), genWhitespaces, minSuccessful(1000)) { (w, d, ws) =>
        check(s"P${w}W${d}D", ws)
        check(s"P1Y${w}W${d}D", ws)
        check(s"P1Y1M${w}W${d}D", ws)
      }
    }
    "throw parsing exception for empty input and illegal or broken Period string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readPeriod(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsPeriod()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError("""""""", "expected 'P' or '-', offset: 0x00000001")
      checkError(""""-"""", "expected 'P', offset: 0x00000002")
      checkError(""""PXY"""", "expected '-' or digit, offset: 0x00000002")
      checkError(""""P-XY"""", "expected digit, offset: 0x00000003")
      checkError(""""P1XY"""", "expected 'Y' or 'M' or 'W' or 'D' or digit, offset: 0x00000003")
      checkError(""""P2147483648Y"""", "illegal period, offset: 0x0000000c")
      checkError(""""P21474836470Y"""", "illegal period, offset: 0x0000000c")
      checkError(""""P-2147483649Y"""", "illegal period, offset: 0x0000000c")
      checkError(""""P2147483648M"""", "illegal period, offset: 0x0000000c")
      checkError(""""P21474836470M"""", "illegal period, offset: 0x0000000c")
      checkError(""""P-2147483649M"""", "illegal period, offset: 0x0000000c")
      checkError(""""P2147483648W"""", "illegal period, offset: 0x0000000c")
      checkError(""""P21474836470W"""", "illegal period, offset: 0x0000000c")
      checkError(""""P-2147483649W"""", "illegal period, offset: 0x0000000c")
      checkError(""""P2147483648D"""", "illegal period, offset: 0x0000000c")
      checkError(""""P21474836470D"""", "illegal period, offset: 0x0000000c")
      checkError(""""P-2147483649D"""", "illegal period, offset: 0x0000000c")
      checkError(""""P1YXM"""", """expected '"' or '-' or digit, offset: 0x00000004""")
      checkError(""""P1Y-XM"""", "expected digit, offset: 0x00000005")
      checkError(""""P1Y1XM"""", "expected 'M' or 'W' or 'D' or digit, offset: 0x00000005")
      checkError(""""P1Y2147483648M"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y21474836470M"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y-2147483649M"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y2147483648W"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y21474836470W"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y-2147483649W"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y2147483648D"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y21474836470D"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y-2147483649D"""", "illegal period, offset: 0x0000000e")
      checkError(""""P1Y1MXW"""", """expected '"' or '-' or digit, offset: 0x00000006""")
      checkError(""""P1Y1M-XW"""", "expected digit, offset: 0x00000007")
      checkError(""""P1Y1M1XW"""", "expected 'W' or 'D' or digit, offset: 0x00000007")
      checkError(""""P1Y1M306783379W"""", "illegal period, offset: 0x0000000f")
      checkError(""""P1Y1M3067833790W"""", "illegal period, offset: 0x0000000f")
      checkError(""""P1Y1M-306783379W"""", "illegal period, offset: 0x00000010")
      checkError(""""P1Y1M2147483648D"""", "illegal period, offset: 0x00000010")
      checkError(""""P1Y1M21474836470D"""", "illegal period, offset: 0x00000010")
      checkError(""""P1Y1M-2147483649D"""", "illegal period, offset: 0x00000010")
      checkError(""""P1Y1M1WXD"""", """expected '"' or '-' or digit, offset: 0x00000008""")
      checkError(""""P1Y1M1W-XD"""", "expected digit, offset: 0x00000009")
      checkError(""""P1Y1M1W1XD"""", "expected 'D' or digit, offset: 0x00000009")
      checkError(""""P1Y1M306783378W8D"""", "illegal period, offset: 0x00000011")
      checkError(""""P1Y1M-306783378W-8D"""", "illegal period, offset: 0x00000013")
      checkError(""""P1Y1M1W2147483647D"""", "illegal period, offset: 0x00000012")
      checkError(""""P1Y1M-1W-2147483648D"""", "illegal period, offset: 0x00000014")
      checkError(""""P1Y1M0W2147483648D"""", "illegal period, offset: 0x00000012")
      checkError(""""P1Y1M0W21474836470D"""", "illegal period, offset: 0x00000012")
      checkError(""""P1Y1M0W-2147483649D"""", "illegal period, offset: 0x00000012")
      checkError(""""P1Y1M1W1DX""", """expected '"', offset: 0x0000000a""")
    }
  }
  "JsonReader.readYear and JsonReader.readKeyAsYear" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readYear(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsYear())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = Year.parse("2008")
      reader("null").readYear(default) shouldBe default
    }
    "parse Year from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = Year.parse(s)
        reader(s"""$ws"$s"""").readYear(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsYear() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("-999999999", ws)
        check("+999999999", ws)
        check("2008", ws)
      }
      forAll(genYear, genWhitespaces, minSuccessful(10000)) { (x, ws) =>
        check(toISO8601(x), ws)
      }
    }
    "throw parsing exception for empty input and illegal or broken Year string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readYear(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsYear()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008""", "unexpected end of input, offset: 0x00000005")
      checkError(""""+2008"""", "expected digit, offset: 0x00000006")
      checkError(""""+1000000000"""", """expected '"', offset: 0x0000000b""")
      checkError(""""-1000000000"""", """expected '"', offset: 0x0000000b""")
      checkError(""""-0000"""", "illegal year, offset: 0x00000005")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDoubleQuotes = if (ch >= '0' && ch <= '9' || ch == '"') 'X' else ch
        checkError(s""""${nonNumber}008"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}"""", "expected digit, offset: 0x00000004")
        checkError(s""""+${nonDigit}0000"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigit}"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x00000006""")
        checkError(s""""+10000${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x00000007""")
        checkError(s""""+100000${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x00000008""")
        checkError(s""""+1000000${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x00000009""")
        checkError(s""""+10000000${nonDigitOrDoubleQuotes}"""", """expected '"' or digit, offset: 0x0000000a""")
      }
    }
  }
  "JsonReader.readYearMonth and JsonReader.readKeyAsYearMonth" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readYearMonth(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsYearMonth())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = YearMonth.parse("2008-01")
      reader("null").readYearMonth(default) shouldBe default
    }
    "parse YearMonth from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = YearMonth.parse(s)
        reader(s"""$ws"$s"""").readYearMonth(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsYearMonth() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("+999999999-12", ws)
        check("-999999999-01", ws)
        check("2008-01", ws)
      }
      forAll(genYearMonth, genWhitespaces, minSuccessful(10000)) { (x, ws) =>
        val s = x.toString
        val fixed =
          if (x.getYear < 0 && !s.startsWith("-")) s"-$s"
          else if (x.getYear > 9999 && !s.startsWith("+")) s"+$s"
          else s
        check(fixed, ws)
      }
    }
    "throw parsing exception for empty input and illegal or broken YearMonth string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readYearMonth(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsYearMonth()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008-01""", "unexpected end of input, offset: 0x00000008")
      checkError(""""+1000000000-01"""", "expected '-', offset: 0x0000000b")
      checkError(""""-1000000000-01"""", "expected '-', offset: 0x0000000b")
      checkError(""""-0000-01"""", "illegal year, offset: 0x00000005")
      checkError(""""2008-00"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-13"""", "illegal month, offset: 0x00000007")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDash = if (ch >= '0' && ch <= '9' || ch == '-') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        checkError(s""""${nonNumber}008-01"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08-01"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8-01"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}-01"""", "expected digit, offset: 0x00000004")
        checkError(s""""2008${nonDash}01"""", "expected '-', offset: 0x00000005")
        checkError(s""""+${nonDigit}0000-01"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000-01"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00-01"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0-01"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigitOrDash}-01"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDash}-01"""", "expected '-' or digit, offset: 0x00000006")
        checkError(s""""+10000${nonDigitOrDash}-01"""", "expected '-' or digit, offset: 0x00000007")
        checkError(s""""+100000${nonDigitOrDash}-01"""", "expected '-' or digit, offset: 0x00000008")
        checkError(s""""+1000000${nonDigitOrDash}-01"""", "expected '-' or digit, offset: 0x00000009")
        checkError(s""""+10000000${nonDigitOrDash}-01"""", "expected '-' or digit, offset: 0x0000000a")
        checkError(s""""+999999999${nonDash}01"""", "expected '-', offset: 0x0000000b")
        checkError(s""""2008-${nonDigit}1"""", "expected digit, offset: 0x00000006")
        checkError(s""""2008-0${nonDigit}"""", "expected digit, offset: 0x00000007")
        checkError(s""""2008-01${nonDoubleQuotes}"""", """expected '"', offset: 0x00000008""")
      }
    }
  }
  "JsonReader.readZonedDateTime and JsonReader.readKeyAsZonedDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readZonedDateTime(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsZonedDateTime())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = ZonedDateTime.parse("2008-01-20T07:24Z[UTC]")
      reader("null").readZonedDateTime(default) shouldBe default
    }
    "parse ZonedDateTime from a string representation according to ISO-8601 format with optional IANA timezone identifier in JDK format" in {
      def check(s: String, ws: String): Unit = {
        val x = ZonedDateTime.parse(s)
        reader(s"""$ws"$s"""").readZonedDateTime(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsZonedDateTime() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("2008-01-20T07:24Z", ws)
        check("2008-01-20T07:24:33Z", ws)
        check("2008-01-20T07:24:33.Z", ws)
        check("2008-01-20T07:24:33.000Z", ws)
        check("2008-01-20T07:24+18:00", ws)
        check("2008-01-20T07:24:33+18:00", ws)
        check("2008-01-20T07:24:33+18:00[UTC+18:00]", ws)
        check("2008-01-20T07:24-18:00", ws)
        check("2008-01-20T07:24:33-18:00", ws)
        check("2008-01-20T07:24:33-18:00[UTC-18:00]", ws)
        check("+999999999-12-31T23:59:59.999999999+18:00", ws)
        check("+999999999-12-31T23:59:59.999999999-18:00", ws)
        check("-999999999-01-01T00:00:00+18:00", ws)
        check("-999999999-01-01T00:00:00-18:00", ws)
        check("2018-03-25T02:30+01:00[Europe/Warsaw]", ws)
        check("2018-03-25T02:30+00:00[Europe/Warsaw]", ws)
        check("2018-03-25T02:30+02:00[Europe/Warsaw]", ws)
        check("2018-03-25T02:30+03:00[Europe/Warsaw]", ws)
        check("2018-10-28T02:30+00:00[Europe/Warsaw]", ws)
        check("2018-10-28T02:30+01:00[Europe/Warsaw]", ws)
        check("2018-10-28T02:30+02:00[Europe/Warsaw]", ws)
        check("2018-10-28T02:30+03:00[Europe/Warsaw]", ws)
      }
      forAll(genZonedDateTime, genWhitespaces, minSuccessful(10000))((x, ws) => {
        val s = x.toString
        reader(s"""$ws"$s"""").readZonedDateTime(null) shouldBe x
        reader(s"""$ws"$s":$ws""").readKeyAsZonedDateTime() shouldBe x
      })
    }
    "throw parsing exception for empty input and illegal or broken ZonedDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readZonedDateTime(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsZonedDateTime()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""2008-01-20T07:24:33Z[UTC]""", "unexpected end of input, offset: 0x0000001a")
      checkError(""""+1000000000-01-20T07:24:33Z[UTC]"""", "expected '-', offset: 0x0000000b")
      checkError(""""-1000000000-01-20T07:24:33Z[UTC]"""", "expected '-', offset: 0x0000000b")
      checkError(""""2008-01-20T07:24:33X[UTC]"""", "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError(""""2008-01-20T07:24:33ZZ""", """expected '[' or '"', offset: 0x00000015""")
      checkError(""""2008-01-20T07:24:33.[UTC]"""", "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError(""""2008-01-20T07:24:33.000[UTC]"""", "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError(""""2008-01-20T07:24:33.123456789X[UTC]"""", "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError(""""2008-01-20T07:24:33.1234567890[UTC]"""", "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError(""""-0000-01-20T07:24:33Z[UTC]"""", "illegal year, offset: 0x00000005")
      checkError(""""2008-00-20T07:24:33Z[UTC]"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-13-20T07:24:33Z[UTC]"""", "illegal month, offset: 0x00000007")
      checkError(""""2008-01-00T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2007-02-29T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-02-30T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-03-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-04-31T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-05-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-06-31T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-07-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-08-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-09-31T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-10-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-11-31T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-12-32T07:24:33Z[UTC]"""", "illegal day, offset: 0x0000000a")
      checkError(""""2008-01-20T24:24:33Z[UTC]"""", "illegal hour, offset: 0x0000000d")
      checkError(""""2008-01-20T07:60:33Z[UTC]"""", "illegal minute, offset: 0x00000010")
      checkError(""""2008-01-20T07:24:60Z[UTC]"""", "illegal second, offset: 0x00000013")
      checkError(""""2008-01-20T07:24:33+[UTC]"""", "expected digit, offset: 0x00000015")
      checkError(""""2008-01-20T07:24:33-[UTC]"""", "expected digit, offset: 0x00000015")
      checkError(""""2008-01-20T07:24:33.+[UTC]"""", "expected digit, offset: 0x00000016")
      checkError(""""2008-01-20T07:24:33.+1[UTC]"""", "expected digit, offset: 0x00000017")
      checkError(""""2008-01-20T07:24:33.+10=[UTC]"""", """expected ':' or '[' or '"', offset: 0x00000018""")
      checkError(""""2008-01-20T07:24:33.+10:[UTC]"""", "expected digit, offset: 0x00000019")
      checkError(""""2008-01-20T07:24:33.+10:1[UTC]"""", "expected digit, offset: 0x0000001a")
      checkError(""""2008-01-20T07:24:33.+10:10[]"""", "illegal timezone, offset: 0x0000001c")
      checkError(""""2008-01-20T07:24:33.+10:10=10[UTC]"""", """expected ':' or '[' or '"', offset: 0x0000001b""")
      checkError(""""2008-01-20T07:24:33.+10:10:X0[UTC]"""", "expected digit, offset: 0x0000001c")
      checkError(""""2008-01-20T07:24:33.+10:10:1,[UTC]"""", "expected digit, offset: 0x0000001d")
      checkError(""""2008-01-20T07:24:33.+10:10:10[UTC]X"""", """expected '"', offset: 0x00000023""")
      checkError(""""2008-01-20T07:24:33.+18:01[UTC]"""", "illegal timezone offset, offset: 0x0000001b")
      checkError(""""2008-01-20T07:24:33.-18:01[UTC]"""", "illegal timezone offset, offset: 0x0000001b")
      checkError(""""2008-01-20T07:24:33.+20:10[UTC]"""", "illegal timezone offset hour, offset: 0x00000017")
      checkError(""""2008-01-20T07:24:33.+10:60[UTC]"""", "illegal timezone offset minute, offset: 0x0000001a")
      checkError(""""2008-01-20T07:24:33.+10:10:60[UTC]"""", "illegal timezone offset second, offset: 0x0000001d")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonNumber = if (ch >= '0' && ch <= '9' || ch == '-' || ch == '+') 'X' else ch
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonDigitOrDash = if (ch >= '0' && ch <= '9' || ch == '-') 'X' else ch
        val nonDash = if (ch == '-') 'X' else ch
        val nonT = if (ch == 'T') 'X' else ch
        val nonColon = if (ch == ':') 'X' else ch
        val nonColonOrSignOrZ = if (ch == ':' || ch == '-' || ch == '+' || ch == 'Z') 'X' else ch
        checkError(s""""${nonNumber}008-01-20T07:24:33Z[UTC]"""", "expected '-' or '+' or digit, offset: 0x00000001")
        checkError(s""""2${nonDigit}08-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000002")
        checkError(s""""20${nonDigit}8-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000003")
        checkError(s""""200${nonDigit}-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000004")
        checkError(s""""2008${nonDash}01-20T07:24:33Z[UTC]"""", "expected '-', offset: 0x00000005")
        checkError(s""""+${nonDigit}0000-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}000-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonDigit}00-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000004")
        checkError(s""""+100${nonDigit}0-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000005")
        checkError(s""""+1000${nonDigit}-01-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000006")
        checkError(s""""-1000${nonDigitOrDash}-01-20T07:24:33Z[UTC]"""", "expected '-' or digit, offset: 0x00000006")
        checkError(s""""+10000${nonDigitOrDash}-01-20T07:24:33Z[UTC]"""", "expected '-' or digit, offset: 0x00000007")
        checkError(s""""+100000${nonDigitOrDash}-01-20T07:24:33Z[UTC]"""", "expected '-' or digit, offset: 0x00000008")
        checkError(s""""+1000000${nonDigitOrDash}-01-20T07:24:33Z[UTC]"""", "expected '-' or digit, offset: 0x00000009")
        checkError(s""""+10000000${nonDigitOrDash}-01-20T07:24:33Z[UTC]"""", "expected '-' or digit, offset: 0x0000000a")
        checkError(s""""+999999999${nonDash}01-20T07:24:33Z[UTC]"""", "expected '-', offset: 0x0000000b")
        checkError(s""""2008-${nonDigit}1-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000006")
        checkError(s""""2008-0${nonDigit}-20T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000007")
        checkError(s""""2008-01${nonDash}20T07:24:33Z[UTC]"""", "expected '-', offset: 0x00000008")
        checkError(s""""2008-01-${nonDigit}0T07:24:33Z[UTC]"""", "expected digit, offset: 0x00000009")
        checkError(s""""2008-01-2${nonDigit}T07:24:33Z[UTC]"""", "expected digit, offset: 0x0000000a")
        checkError(s""""2008-01-20${nonT}07:24:33Z[UTC]"""", "expected 'T', offset: 0x0000000b")
        checkError(s""""2008-01-20T${nonDigit}7:24:33Z[UTC]"""", "expected digit, offset: 0x0000000c")
        checkError(s""""2008-01-20T0${nonDigit}:24:33Z[UTC]"""", "expected digit, offset: 0x0000000d")
        checkError(s""""2008-01-20T07${nonColon}24:33Z[UTC]"""", "expected ':', offset: 0x0000000e")
        checkError(s""""2008-01-20T07:${nonDigit}4:33Z[UTC]"""", "expected digit, offset: 0x0000000f")
        checkError(s""""2008-01-20T07:2${nonDigit}:33Z[UTC]"""", "expected digit, offset: 0x00000010")
        checkError(s""""2008-01-20T07:24${nonColonOrSignOrZ}33Z[UTC]"""", "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
        checkError(s""""2008-01-20T07:24:${nonDigit}3Z[UTC]"""", "expected digit, offset: 0x00000012")
        checkError(s""""2008-01-20T07:24:3${nonDigit}Z[UTC]"""", "expected digit, offset: 0x00000013")
        checkError(s""""2008-01-20T07:24:33.+${nonDigit}0:10[UTC]"""", "expected digit, offset: 0x00000016")
        checkError(s""""2008-01-20T07:24:33.+1${nonDigit}:10[UTC]"""", "expected digit, offset: 0x00000017")
        checkError(s""""2008-01-20T07:24:33.+10:${nonDigit}0[UTC]"""", "expected digit, offset: 0x00000019")
        checkError(s""""2008-01-20T07:24:33.+10:1${nonDigit}[UTC]"""", "expected digit, offset: 0x0000001a")
      }
    }
  }
  "JsonReader.readZoneId and JsonReader.readKeyAsZoneId" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readZoneId(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsZoneId())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneId.of("Europe/Warsaw")
      reader("null").readZoneId(default) shouldBe default
    }
    "parse ZoneId from a string representation according to ISO-8601 format for timezone offset or JDK format for IANA timezone identifier" in {
      def check(s: String, ws: String): Unit = {
        val x = ZoneId.of(s)
        reader(s"""$ws"$s"""").readZoneId(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsZoneId() shouldBe x
      }

      forAll(genZoneId, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken ZoneId string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readZoneId(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsZoneId()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError("""""""", "illegal timezone, offset: 0x00000001")
      checkError(""""+"""", "illegal timezone, offset: 0x00000002")
      //checkError(""""+1"""", "expected digit, offset: 0x00000003") FIXME: looks like a bug in ZoneId.of() parser
      checkError(""""XXX"""", "illegal timezone, offset: 0x00000004")
      checkError(""""+10="""", "illegal timezone, offset: 0x00000005")
      checkError(""""+10:"""", "illegal timezone, offset: 0x00000005")
      checkError(""""+10:1"""", "illegal timezone, offset: 0x00000006")
      checkError(""""+19:10"""", "illegal timezone, offset: 0x00000007")
      checkError(""""+10:60"""", "illegal timezone, offset: 0x00000007")
      checkError(""""+10:10:60"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""+18:00:01"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""-18:00:01"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""UT+"""", "illegal timezone, offset: 0x00000004")
      checkError(""""UT+10="""", "illegal timezone, offset: 0x00000007")
      checkError(""""UT+10:"""", "illegal timezone, offset: 0x00000007")
      checkError(""""UT+10:1"""", "illegal timezone, offset: 0x00000008")
      checkError(""""UT+19:10"""", "illegal timezone, offset: 0x00000009")
      checkError(""""UT+10:60"""", "illegal timezone, offset: 0x00000009")
      checkError(""""UT+10:10:60"""", "illegal timezone, offset: 0x0000000c")
      checkError(""""UT+18:00:01"""", "illegal timezone, offset: 0x0000000c")
      checkError(""""UT-18:00:01"""", "illegal timezone, offset: 0x0000000c")
      checkError(""""UTC+"""", "illegal timezone, offset: 0x00000005")
      checkError(""""UTC+10="""", "illegal timezone, offset: 0x00000008")
      checkError(""""UTC+10:"""", "illegal timezone, offset: 0x00000008")
      checkError(""""UTC+10:1"""", "illegal timezone, offset: 0x00000009")
      checkError(""""UTC+19:10"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""UTC+10:60"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""UTC+10:10:60"""", "illegal timezone, offset: 0x0000000d")
      checkError(""""UTC+18:00:01"""", "illegal timezone, offset: 0x0000000d")
      checkError(""""UTC-18:00:01"""", "illegal timezone, offset: 0x0000000d")
      checkError(""""GMT+"""", "illegal timezone, offset: 0x00000005")
      checkError(""""GMT+10="""", "illegal timezone, offset: 0x00000008")
      checkError(""""GMT+10:"""", "illegal timezone, offset: 0x00000008")
      checkError(""""GMT+10:1"""", "illegal timezone, offset: 0x00000009")
      checkError(""""GMT+19:10"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""GMT+10:60"""", "illegal timezone, offset: 0x0000000a")
      checkError(""""GMT+10:10:60"""", "illegal timezone, offset: 0x0000000d")
      checkError(""""GMT+18:00:01"""", "illegal timezone, offset: 0x0000000d")
      checkError(""""GMT-18:00:01"""", "illegal timezone, offset: 0x0000000d")
    }
  }
  "JsonReader.readZoneOffset and JsonReader.readKeyAsZoneOffset" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readZoneOffset(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsZoneOffset())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneOffset.of("+01:00")
      reader("null").readZoneOffset(default) shouldBe default
    }
    "parse ZoneOffset from a string representation according to ISO-8601 format" in {
      def check(s: String, ws: String): Unit = {
        val x = ZoneOffset.of(s)
        reader(s"""$ws"$s"""").readZoneOffset(null) shouldBe x
        reader(s"""$ws"$s":""").readKeyAsZoneOffset() shouldBe x
      }

      forAll(genWhitespaces) { ws =>
        check("Z", ws)
        check("+00", ws)
        check("+00:00", ws)
        check("-00", ws)
        check("-00:00", ws)
        check("+18", ws)
        check("+18:00", ws)
        check("-18", ws)
        check("-18:00", ws)
      }
      forAll(genZoneOffset, genWhitespaces, minSuccessful(10000))((x, ws) => check(x.toString, ws))
    }
    "throw parsing exception for empty input and illegal or broken ZoneOffset string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readZoneOffset(null)).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsZoneOffset()).getMessage.startsWith(error))
      }

      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError("""""""", "expected '+' or '-' or 'Z', offset: 0x00000001")
      checkError(""""+19:10"""", "illegal timezone offset hour, offset: 0x00000003")
      checkError(""""+10:60"""", "illegal timezone offset minute, offset: 0x00000006")
      checkError(""""+10:10:60"""", "illegal timezone offset second, offset: 0x00000009")
      checkError(""""+18:00:01"""", "illegal timezone offset, offset: 0x0000000a")
      checkError(""""-18:00:01"""", "illegal timezone offset, offset: 0x0000000a")
      forAll(genISO8859Char, minSuccessful(100)) { ch =>
        val nonDigit = if (ch >= '0' && ch <= '9') 'X' else ch
        val nonColonOrDoubleQuotes = if (ch == ':' || ch == '"') 'X' else ch
        val nonDoubleQuotes = if (ch == '"') 'X' else ch
        checkError(s""""+${nonDigit}0:10:10"""", "expected digit, offset: 0x00000002")
        checkError(s""""+1${nonDigit}:10:10"""", "expected digit, offset: 0x00000003")
        checkError(s""""+10${nonColonOrDoubleQuotes}10:10"""", """expected ':' or '"', offset: 0x00000004""")
        checkError(s""""+10:${nonDigit}0:10"""", "expected digit, offset: 0x00000005")
        checkError(s""""+10:1${nonDigit}:10"""", "expected digit, offset: 0x00000006")
        checkError(s""""+10:10${nonColonOrDoubleQuotes}10"""", """expected ':' or '"', offset: 0x00000007""")
        checkError(s""""+10:10:${nonDigit}0"""", "expected digit, offset: 0x00000008")
        checkError(s""""+10:10:1${nonDigit}"""", "expected digit, offset: 0x00000009")
        checkError(s""""+10:10:10${nonDoubleQuotes}"""", """expected '"', offset: 0x0000000a""")
      }

    }
  }
  "JsonReader.isCharBufEqualsTo" should {
    "return true when content of internal char buffer for the specified length is equal to the provided string" in {
      def check(s1: String, s2: String): Unit = {
        val r = reader(s""""$s1"""")
        r.isCharBufEqualsTo(r.readStringAsCharBuf(), s2) shouldBe s1 == s2
      }

      check("", "")
      check("x", "")
      check("", "x")
      check("x", "x")
      forAll(arbitrary[String], arbitrary[String], minSuccessful(10000)) { (s1, s2) =>
        whenever(s1.forall(ch => ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch))) {
          check(s1, s2)
        }
      }
    }
    "throw exception for null value of string to compare" in {
      val r = reader("""""""")
      intercept[NullPointerException](r.isCharBufEqualsTo(r.readStringAsCharBuf(), null))
    }
  }
  "JsonReader.readKeyAsString" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonReaderException](reader("""""""").readKeyAsString())
        .getMessage.startsWith("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](reader("""""x""").readKeyAsString())
        .getMessage.startsWith("expected ':', offset: 0x00000002"))
    }
  }
  "JsonReader.readString, JsonReader.readStringAsCharBuf and JsonReader.readKeyAsString" should {
    def check(s: String, ws: String): Unit = {
      reader(s"""$ws"$s"""").readString(null) shouldBe s
      val r = reader(s"""$ws"$s"""")
      r.isCharBufEqualsTo(r.readStringAsCharBuf(), s) shouldBe true
      reader(s"""$ws"$s":""").readKeyAsString() shouldBe s
    }

    def checkEscaped(s1: String, s2: String, ws: String): Unit = {
      reader(s"""$ws"$s1"""").readString(null) shouldBe s2
      val r = reader(s"""$ws"$s1"""")
      r.isCharBufEqualsTo(r.readStringAsCharBuf(), s2) shouldBe true
      reader(s"""$ws"$s1":""").readKeyAsString() shouldBe s2
    }

    def checkError(json: String, error: String): Unit = {
      assert(intercept[JsonReaderException](reader(json).readString(null)).getMessage.startsWith(error))
      assert(intercept[JsonReaderException](reader(json).readStringAsCharBuf()).getMessage.startsWith(error))
      assert(intercept[JsonReaderException](reader(json).readKeyAsString()).getMessage.startsWith(error))
    }

    def checkError2(jsonBytes: Array[Byte], error: String): Unit = {
      assert(intercept[JsonReaderException](reader2(jsonBytes).readString(null)).getMessage.startsWith(error))
      assert(intercept[JsonReaderException](reader2(jsonBytes).readStringAsCharBuf()).getMessage.startsWith(error))
      assert(intercept[JsonReaderException](reader2(jsonBytes).readKeyAsString()).getMessage.startsWith(error))
    }

    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readString(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readStringAsCharBuf())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
      assert(intercept[JsonReaderException](reader("null").readKeyAsString())
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      reader("null").readString("VVV") shouldBe "VVV"
    }
    "parse string with Unicode chars which are not escaped and are non-surrogate" in {
      forAll(arbitrary[String], genWhitespaces, minSuccessful(10000)) { (s, ws) =>
        whenever(s.forall(ch => ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch))) {
          check(s, ws)
        }
      }
    }
    "parse string with valid surrogate pairs" in {
      forAll(genHighSurrogateChar, genLowSurrogateChar, genWhitespaces, minSuccessful(10000)) { (hi, lo, ws) =>
        whenever(Character.isSurrogatePair(hi, lo)) {
          check(new String(Array(hi, lo)), ws)
        }
      }
    }
    "parse escaped chars of string value" in {
      forAll(genWhitespaces) { ws =>
        checkEscaped("""\b\f\n\r\t\/\\""", "\b\f\n\r\t/\\", ws)
        checkEscaped(""" \b\f\n\r\t\/\\""", " \b\f\n\r\t/\\", ws)
        checkEscaped("""  \b\f\n\r\t\/\\""", "  \b\f\n\r\t/\\", ws)
        checkEscaped("""   \b\f\n\r\t\/\\""", "   \b\f\n\r\t/\\", ws)
      }
    }
    "parse string with hexadecimal escaped chars which are non-surrogate" in {
      forAll(arbitrary[String], genWhitespaces, minSuccessful(10000)) { (s, ws) =>
        whenever(s.forall(ch => !Character.isSurrogate(ch))) {
          checkEscaped(s.map((ch: Char) => toHexEscaped(ch)).mkString, s, ws)
        }
      }
    }
    "parse string with hexadecimal escaped chars which are valid surrogate pairs" in {
      forAll(genHighSurrogateChar, genLowSurrogateChar, genWhitespaces, minSuccessful(10000)) { (hi, lo, ws) =>
        whenever(Character.isSurrogatePair(hi, lo)) {
          val s = new String(Array(hi, lo))
          checkEscaped(s.map((ch: Char) => toHexEscaped(ch)).mkString, s, ws)
        }
      }
    }
    "throw parsing exception for control chars that must be escaped" in {
      forAll(genControlChar, minSuccessful(100)) { ch =>
        checkError(s""""${ch.toString}"""", "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""\""", "unexpected end of input, offset: 0x00000002")
      checkError2(Array[Byte](0x22.toByte, 0xF0.toByte, 0x80.toByte, 0x80.toByte), "unexpected end of input, offset: 0x00000004")
    }
    "throw parsing exception for boolean values & numbers" in {
      checkError("true", """expected '"', offset: 0x00000000""")
      checkError("false", """expected '"', offset: 0x00000000""")
      checkError("12345", """expected '"', offset: 0x00000000""")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonReaderException](reader(s""""$s"""").readString(null)).getMessage.startsWith(error1))
        assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsCharBuf()).getMessage.startsWith(error1))
        assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsString()).getMessage.startsWith(error2))
      }

      checkError("\\x0008", "illegal escape sequence, offset: 0x00000002",
        "illegal escape sequence, offset: 0x00000002")
      checkError("\\uZ000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0Z00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00Z0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u×000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0×00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00×0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000×", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u000", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u00", "unexpected end of input, offset: 0x00000006", "expected hex digit, offset: 0x00000005")
      checkError("\\u0", "unexpected end of input, offset: 0x00000005", "unexpected end of input, offset: 0x00000006")
      checkError("\\", "unexpected end of input, offset: 0x00000003", "unexpected end of input, offset: 0x00000004")
      checkError("\\udd1e", "unexpected end of input, offset: 0x00000008",
        "unexpected end of input, offset: 0x00000009")
      checkError("\\ud834", "unexpected end of input, offset: 0x00000008",
        "unexpected end of input, offset: 0x00000009")
      checkError("\\ud834\\", "unexpected end of input, offset: 0x00000009",
        "unexpected end of input, offset: 0x0000000a")
      checkError("\\ud834\\x", "unexpected end of input, offset: 0x0000000a",
        "unexpected end of input, offset: 0x0000000b")
      checkError("\\ud834\\d834", "illegal escape sequence, offset: 0x00000008",
        "illegal escape sequence, offset: 0x00000008")
      checkError("\\ud834\\ud834", "illegal surrogate character pair, offset: 0x0000000c",
        "illegal surrogate character pair, offset: 0x0000000c")
    }
    "throw parsing exception in case of illegal byte sequence" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte).readString(null))
          .getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte).readStringAsCharBuf())
          .getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte :+ ':'.toByte).readString(null))
          .getMessage.startsWith(error))
      }

      checkError(Array[Byte](0x80.toByte), "malformed byte(s): 0x80, offset: 0x00000001")
      checkError(Array[Byte](0xC0.toByte, 0x80.toByte), "malformed byte(s): 0xc0, 0x80, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0x08.toByte), "malformed byte(s): 0xc8, 0x08, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0xFF.toByte), "malformed byte(s): 0xc8, 0xff, offset: 0x00000002")
      checkError(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte),
        "malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003")
      checkError(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xf0, 0x80, 0x80, 0x80, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x04.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x04, 0x9e, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0xFF.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x84, 0xff, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0xFF.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0xff, 0x9e, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0xFF.toByte, 0x84.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0xff, 0x84, 0x9e, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0x0E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x84, 0x0e, offset: 0x00000004")
    }
    "throw parsing exception for too long strings" in {
      val sb = new StringBuilder
      sb.append('"')
      var i = 0
      while (i < ReaderConfig.maxCharBufSize) {
        sb.append(' ')
        i += 1
      }
      sb.append('"')
      checkError(sb.toString, """too long string exceeded 'maxCharBufSize'""")
    }
  }
  "JsonReader.readKeyAsChar" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonReaderException](reader(""""x"""").readKeyAsChar())
        .getMessage.startsWith("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonReaderException](reader(""""x"x""").readKeyAsChar())
        .getMessage.startsWith("expected ':', offset: 0x00000003"))
    }
  }
  "JsonReader.readChar and JsonReader.readKeyAsChar" should {
    def check(ch: Char, ws: String): Unit = {
      reader(s"""$ws"${ch.toString}"""").readChar() shouldBe ch
      reader(s"""$ws"${ch.toString}":""").readKeyAsChar() shouldBe ch
    }

    def checkEscaped(escaped: String, nonEscaped: Char, ws: String): Unit = {
      reader(s"""$ws"$escaped"""").readChar() shouldBe nonEscaped
      reader(s"""$ws"$escaped":""").readKeyAsChar() shouldBe nonEscaped
    }

    def checkError(json: String, error: String): Unit = {
      assert(intercept[JsonReaderException](reader(json).readChar()).getMessage.startsWith(error))
      assert(intercept[JsonReaderException](reader(json).readKeyAsChar()).getMessage.startsWith(error))
    }

    def checkError2(jsonBytes: Array[Byte], error: String): Unit = {
      assert(intercept[JsonReaderException](reader2(jsonBytes).readChar()).getMessage.startsWith(error))
      assert(intercept[JsonReaderException](reader2(jsonBytes).readKeyAsChar()).getMessage.startsWith(error))
    }

    "parse Unicode char that is not escaped and is non-surrogate from string with length == 1" in {
      forAll(genChar, genWhitespaces, minSuccessful(10000)) { (ch, ws) =>
        whenever(ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          check(ch, ws)
        }
      }
    }
    "parse escaped chars of string value" in {
      forAll(genWhitespaces) { ws =>
        checkEscaped("""\b""", '\b', ws)
        checkEscaped("""\f""", '\f', ws)
        checkEscaped("""\n""", '\n', ws)
        checkEscaped("""\r""", '\r', ws)
        checkEscaped("""\t""", '\t', ws)
        checkEscaped("""\/""", '/', ws)
        checkEscaped("""\\""", '\\', ws)
      }
    }
    "parse hexadecimal escaped chars which are non-surrogate" in {
      forAll(genChar, genWhitespaces, minSuccessful(10000)) { (ch, ws) =>
        whenever(!Character.isSurrogate(ch)) {
          checkEscaped(toHexEscaped(ch), ch, ws)
        }
      }
    }
    "throw parsing exception for string with length > 1" in {
      forAll(genChar, minSuccessful(100)) { ch =>
        whenever(ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          checkError(s""""$ch$ch"""", """expected '"'""") // offset can differs for non-ASCII characters
        }
      }
    }
    "throw parsing exception for control chars that must be escaped" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonReaderException](reader2(bytes).readChar()).getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader2(bytes).readKeyAsChar()).getMessage.startsWith(error))
      }

      forAll(genControlChar, minSuccessful(100)) { (ch: Char) =>
        checkError(Array('"', ch.toByte, '"'), "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      checkError("", "unexpected end of input, offset: 0x00000000")
      checkError(""""""", "unexpected end of input, offset: 0x00000001")
      checkError(""""\""", "unexpected end of input, offset: 0x00000002")
      checkError("""""""", "illegal value for char, offset: 0x00000001")
      checkError2(Array[Byte](0x22.toByte, 0xC0.toByte), "unexpected end of input, offset: 0x00000002")
      checkError2(Array[Byte](0x22.toByte, 0xE0.toByte, 0x80.toByte), "unexpected end of input, offset: 0x00000003")
    }
    "throw parsing exception for null, boolean values & numbers" in {
      checkError("null", """expected '"', offset: 0x00000000""")
      checkError("true", """expected '"', offset: 0x00000000""")
      checkError("false", """expected '"', offset: 0x00000000""")
      checkError("12345", """expected '"', offset: 0x00000000""")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonReaderException](reader(s""""$s"""").readChar()).getMessage.startsWith(error1))
        assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsChar()).getMessage.startsWith(error2))
      }

      checkError("\\x0008", "illegal escape sequence, offset: 0x00000002",
        "illegal escape sequence, offset: 0x00000002")
      checkError("\\uZ000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0Z00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00Z0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u×000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0×00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00×0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000×", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u000", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u00", "unexpected end of input, offset: 0x00000006", "expected hex digit, offset: 0x00000005")
      checkError("\\u0", "unexpected end of input, offset: 0x00000005", "unexpected end of input, offset: 0x00000006")
      checkError("\\", "unexpected end of input, offset: 0x00000003", """expected '"', offset: 0x00000003""")
      checkError("\\udd1e", "illegal surrogate character, offset: 0x00000006",
        "illegal surrogate character, offset: 0x00000006")
      checkError("\\ud834", "illegal surrogate character, offset: 0x00000006",
        "illegal surrogate character, offset: 0x00000006")
    }
    "throw parsing exception in case of illegal byte sequence" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte).readChar())
          .getMessage.startsWith(error))
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte :+ ':'.toByte).readKeyAsChar())
          .getMessage.startsWith(error))
      }

      checkError(Array[Byte](0x80.toByte), "malformed byte(s): 0x80, offset: 0x00000001")
      checkError(Array[Byte](0xC0.toByte, 0x80.toByte), "malformed byte(s): 0xc0, 0x80, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0x08.toByte), "malformed byte(s): 0xc8, 0x08, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0xFF.toByte), "malformed byte(s): 0xc8, 0xff, offset: 0x00000002")
      checkError(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte),
        "malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003")
      checkError(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte),
        "illegal surrogate character, offset: 0x00000004")
    }
  }
  "JsonReader.readByte, JsonReader.readKeyAsByte and JsonReader.readStringAsByte" should {
    def check(n: Byte, ws: String): Unit = {
      val s = n.toString
      reader(ws + s).readByte() shouldBe n
      reader(s"""$ws"$s":""").readKeyAsByte() shouldBe n
      reader(s"""$ws"$s"""").readStringAsByte() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readByte()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsByte()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsByte()).getMessage.startsWith(error2))
    }

    "parse valid byte values" in {
      forAll(arbitrary[Byte], genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("123.0", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("123e10", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("123E10", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on byte overflow" in {
      checkError("128", "value is too large for byte, offset: 0x00000002",
        "value is too large for byte, offset: 0x00000003")
      checkError("-129", "value is too large for byte, offset: 0x00000003",
        "value is too large for byte, offset: 0x00000004")
      checkError("12345", "value is too large for byte, offset: 0x00000003",
        "value is too large for byte, offset: 0x00000004")
      checkError("-12345", "value is too large for byte, offset: 0x00000004",
        "value is too large for byte, offset: 0x00000005")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readByte()).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("0123", "illegal number with leading zero, offset: 0x00000000")
      checkError("-0123", "illegal number with leading zero, offset: 0x00000001")
      checkError("0128", "illegal number with leading zero, offset: 0x00000000")
      checkError("-0128", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readShort, JsonReader.readKeyAsShort and JsonReader.readStringAsShort" should {
    def check(n: Short, ws: String): Unit = {
      val s = n.toString
      reader(ws + s).readShort() shouldBe n
      reader(s"""$ws"$s":""").readKeyAsShort() shouldBe n
      reader(s"""$ws"$s"""").readStringAsShort() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readShort()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsShort()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsShort()).getMessage.startsWith(error2))
    }

    "parse valid short values" in {
      forAll(arbitrary[Short], genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("12345.0", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
      checkError("12345e10", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
      checkError("12345E10", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on short overflow" in {
      checkError("32768", "value is too large for short, offset: 0x00000004",
        "value is too large for short, offset: 0x00000005")
      checkError("-32769", "value is too large for short, offset: 0x00000005",
        "value is too large for short, offset: 0x00000006")
      checkError("12345678901", "value is too large for short, offset: 0x00000005",
        "value is too large for short, offset: 0x00000006")
      checkError("-12345678901", "value is too large for short, offset: 0x00000006",
        "value is too large for short, offset: 0x00000007")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readShort()).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345", "illegal number with leading zero, offset: 0x00000001")
      checkError("032767", "illegal number with leading zero, offset: 0x00000000")
      checkError("-032768", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readInt, JsonReader.readKeyAsInt and JsonReader.readStringAsInt" should {
    def check(n: Int, ws: String): Unit = {
      val s = n.toString
      reader(ws + s).readInt() shouldBe n
      reader(s"""$ws"$s":""").readKeyAsInt() shouldBe n
      reader(s"""$ws"$s"""").readStringAsInt() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readInt()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsInt()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsInt()).getMessage.startsWith(error2))
    }

    "parse valid int values" in {
      forAll(arbitrary[Int], genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("123456789.0", "illegal number, offset: 0x00000009", "illegal number, offset: 0x0000000a")
      checkError("123456789e10", "illegal number, offset: 0x00000009", "illegal number, offset: 0x0000000a")
      checkError("123456789E10", "illegal number, offset: 0x00000009", "illegal number, offset: 0x0000000a")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on int overflow" in {
      checkError("2147483648", "value is too large for int, offset: 0x00000009",
        "value is too large for int, offset: 0x0000000a")
      checkError("-2147483649", "value is too large for int, offset: 0x0000000a",
        "value is too large for int, offset: 0x0000000b")
      checkError("12345678901", "value is too large for int, offset: 0x0000000a",
        "value is too large for int, offset: 0x0000000b")
      checkError("-12345678901", "value is too large for int, offset: 0x0000000b",
        "value is too large for int, offset: 0x0000000c")
      checkError("12345678901234567890", "value is too large for int, offset: 0x0000000a",
        "value is too large for int, offset: 0x0000000b")
      checkError("-12345678901234567890", "value is too large for int, offset: 0x0000000b",
        "value is too large for int, offset: 0x0000000c")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readInt()).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("0123456789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-0123456789", "illegal number with leading zero, offset: 0x00000001")
      checkError("02147483647", "illegal number with leading zero, offset: 0x00000000")
      checkError("-02147483648", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readLong, JsonReader.readKeyAsLong and JsonReader.readStringAsLong" should {
    def check(n: Long, ws: String): Unit = {
      val s = n.toString
      reader(ws + s).readLong() shouldBe n
      reader(s"""$ws"$s":""").readKeyAsLong() shouldBe n
      reader(s"""$ws"$s"""").readStringAsLong() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readLong()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsLong()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsLong()).getMessage.startsWith(error2))
    }

    "parse valid long values" in {
      forAll(arbitrary[Long], genWhitespaces, minSuccessful(10000))(check)
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("1234567890123456789.0", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789e10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789E10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on long overflow" in {
      checkError("9223372036854775808", "value is too large for long, offset: 0x00000012",
        "value is too large for long, offset: 0x00000013")
      checkError("-9223372036854775809", "value is too large for long, offset: 0x00000013",
        "value is too large for long, offset: 0x00000014")
      checkError("12345678901234567890", "value is too large for long, offset: 0x00000013",
        "value is too large for long, offset: 0x00000014")
      checkError("-12345678901234567890", "value is too large for long, offset: 0x00000014",
        "value is too large for long, offset: 0x00000015")
      checkError("123456789012345678901234567890", "value is too large for long, offset: 0x00000013",
        "value is too large for long, offset: 0x00000014")
      checkError("-123456789012345678901234567890", "value is too large for long, offset: 0x00000014",
        "value is too large for long, offset: 0x00000015")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readLong()).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("01234567890123456789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-01234567890123456789", "illegal number with leading zero, offset: 0x00000001")
      checkError("09223372036854775807", "illegal number with leading zero, offset: 0x00000000")
      checkError("-09223372036854775808", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readFloat, JsonReader.readKeyAsFloat and JsonReader.readStringAsFloat" should {
    def check(s: String, n: Float, ws: String): Unit = {
      reader(ws + s).readFloat() shouldBe n
      reader(s"""$ws"$s":""").readKeyAsFloat() shouldBe n
      reader(s"""$ws"$s"""").readStringAsFloat() shouldBe n
    }

    def check2(s: String, n: Float, ws: String): Unit = {
      assert(reader(ws + s).readFloat().equals(n)) // compare boxed values here to avoid false positives when 0.0f == -0.0f returns true
      assert(reader(s"""$ws"$s":""").readKeyAsFloat().equals(n))
      assert(reader(s"""$ws"$s"""").readStringAsFloat().equals(n))
    }

    def checkFloat(s: String, ws: String): Unit = check(s, java.lang.Float.parseFloat(s), ws)

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readFloat()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsFloat()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsFloat()).getMessage.startsWith(error2))
    }
    "parse valid float values" in {
      forAll(genWhitespaces) { ws =>
        checkFloat("16777216.0", ws) // Round-down, halfway
        checkFloat("16777217.0", ws)
        checkFloat("16777218.0", ws)
        checkFloat("33554432.0", ws)
        checkFloat("33554434.0", ws)
        checkFloat("33554436.0", ws)
        checkFloat("17179869184.0", ws)
        checkFloat("17179870208.0", ws)
        checkFloat("17179871232.0", ws)
        checkFloat("16777218.0", ws) // Round-up, halfway
        checkFloat("16777219.0", ws)
        checkFloat("16777220.0", ws)
        checkFloat("33554436.0", ws)
        checkFloat("33554438.0", ws)
        checkFloat("33554440.0", ws)
        checkFloat("17179871232.0", ws)
        checkFloat("17179872256.0", ws)
        checkFloat("17179873280.0", ws)
        checkFloat("33554435.0", ws) // Round-up, above halfway
        checkFloat("17179870209.0", ws)
        checkFloat("37930954282500097", ws) // Fast path with `toFloat`
        checkFloat("48696272630054913", ws)
        checkFloat("1.00000017881393432617187499", ws) // Check exactly halfway, round-up at halfway
        checkFloat("1.000000178813934326171875", ws)
        checkFloat("1.00000017881393432617187501", ws)
        checkFloat("36028797018963967.0", ws) // 2^n - 1 integer regression
        checkFloat("1.17549435E-38", ws)
        checkFloat("1.17549434E-38", ws)
        checkFloat("1.17549433E-38", ws)
        checkFloat("1.17549432E-38", ws)
        checkFloat("1.17549431E-38", ws)
        checkFloat("1.17549430E-38", ws)
        checkFloat("1.17549429E-38", ws)
        checkFloat("1.17549428E-38", ws)
      }
      forAll(arbitrary[Float], genWhitespaces, minSuccessful(10000))((n, ws) => checkFloat(n.toString, ws))
      forAll(arbitrary[Double], genWhitespaces, minSuccessful(10000))((n, ws) => checkFloat(n.toString, ws))
      forAll(arbitrary[Int], genWhitespaces, minSuccessful(10000)) { (n, ws) =>
        val x = java.lang.Float.intBitsToFloat(n)
        whenever(java.lang.Float.isFinite(x)) {
          checkFloat(x.toString, ws)
        }
      }
      forAll(Gen.choose(0L, (1L << 32) - 1), Gen.choose(-22, 18), genWhitespaces, minSuccessful(10000)) { (m, e, ws) =>
        checkFloat(s"${m}e$e", ws)
      }
      forAll(genBigInt, genWhitespaces, minSuccessful(10000))((n, ws) => checkFloat(n.toString, ws))
      forAll(genBigDecimal, genWhitespaces, minSuccessful(10000))((n, ws) => checkFloat(n.toString, ws))
    }
    "parse infinities on float overflow" in {
      forAll(genWhitespaces) { ws =>
        check("12345e6789", Float.PositiveInfinity, ws)
        check("-12345e6789", Float.NegativeInfinity, ws)
        check("123456789012345678901234567890e9223372036854775799", Float.PositiveInfinity, ws)
        check("-123456789012345678901234567890e9223372036854775799", Float.NegativeInfinity, ws)
        check("12345678901234567890e12345678901234567890", Float.PositiveInfinity, ws)
        check("-12345678901234567890e12345678901234567890", Float.NegativeInfinity, ws)
      }
      reader("12345678901234567890e12345678901234567890$").readFloat() shouldBe Float.PositiveInfinity
      reader("-12345678901234567890e12345678901234567890$").readFloat() shouldBe Float.NegativeInfinity
    }
    "parse zeroes on float underflow" in {
      forAll(genWhitespaces) { ws =>
        check("12345e-6789", 0.0f, ws)
        check("-12345e-6789", -0.0f, ws)
        check("0.12345678901234567890e-9223372036854775799", 0.0f, ws)
        check("-0.12345678901234567890e-9223372036854775799", -0.0f, ws)
        check("12345678901234567890e-12345678901234567890", 0.0f, ws)
        check("-12345678901234567890e-12345678901234567890", -0.0f, ws)
      }
    }
    "parse positive and negative zeroes" in {
      forAll(genWhitespaces) { ws =>
        check2("0.0", 0.0f, ws)
        check2("-0.0", -0.0f, ws)
      }
    }
    "parse denormalized numbers with long mantissa and compensating exponent" in {
      forAll(genWhitespaces) { ws =>
        check("1" + "0" * 1000000 + "e-1000000", 1.0f, ws)
        check("0." + "0" * 1000000 + "1e1000000", 0.1f, ws)
      }
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(".1", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("0e$", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0e-$", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("0.E", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.+", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.-", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readFloat()).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
    "throw parsing exception on too long input" in {
      checkError(tooLongNumber, """too long part of input exceeded 'maxBufSize', offset: 0x02000000""",
        """too long part of input exceeded 'maxBufSize', offset: 0x02000001""")
    }
  }
  "JsonReader.readDouble, JsonReader.readKeyAsDouble and JsonReader.readStringAsDouble" should {
    def check(s: String, n: Double, ws: String): Unit = {
      reader(ws + s).readDouble() shouldBe n
      reader(s"""$ws"$s":""").readKeyAsDouble() shouldBe n
      reader(s"""$ws"$s"""").readStringAsDouble() shouldBe n
    }

    def check2(s: String, n: Double, ws: String): Unit = {
      assert(reader(ws + s).readDouble().equals(n)) // compare boxed values here to avoid false positives when 0.0 == -0.0 returns true
      assert(reader(s"""$ws"$s":""").readKeyAsDouble().equals(n))
      assert(reader(s"""$ws"$s"""").readStringAsDouble().equals(n))
    }

    def checkDouble(s: String, ws: String): Unit = check(s, java.lang.Double.parseDouble(s), ws)

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readDouble()).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsDouble()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsDouble()).getMessage.startsWith(error2))
    }

    "parse valid double values" in {
      forAll(genWhitespaces) { ws =>
        checkDouble("9007199254740992.0", ws) // Round-down, halfway
        checkDouble("9007199254740993.0", ws)
        checkDouble("9007199254740994.0", ws)
        checkDouble("18014398509481984.0", ws)
        checkDouble("18014398509481986.0", ws)
        checkDouble("18014398509481988.0", ws)
        checkDouble("9223372036854775808.0", ws)
        checkDouble("9223372036854776832.0", ws)
        checkDouble("9223372036854777856.0", ws)
        checkDouble("11417981541647679048466287755595961091061972992.0", ws)
        checkDouble("11417981541647680316116887983825362587765178368.0", ws)
        checkDouble("11417981541647681583767488212054764084468383744.0", ws)
        checkDouble("11417981541647681583767488212054764084468383744.0", ws)
        checkDouble("9007199254740994.0", ws) // Round-up, halfway
        checkDouble("9007199254740995.0", ws)
        checkDouble("9007199254740996.0", ws)
        checkDouble("18014398509481988.0", ws)
        checkDouble("18014398509481990.0", ws)
        checkDouble("18014398509481992.0", ws)
        checkDouble("9223372036854777856.0", ws)
        checkDouble("9223372036854778880.0", ws)
        checkDouble("9223372036854779904.0", ws)
        checkDouble("11417981541647681583767488212054764084468383744.0", ws)
        checkDouble("11417981541647682851418088440284165581171589120.0", ws)
        checkDouble("11417981541647684119068688668513567077874794496.0", ws)
        checkDouble("9223372036854776833.0", ws) // Round-up, above halfway
        checkDouble("11417981541647680316116887983825362587765178369.0", ws)
        checkDouble("36028797018963967.0", ws) // 2^n - 1 integer regression
        checkDouble("11224326888185522059941158352151320185835795563643008", ws) // Regression after reducing an error range
        checkDouble("2.2250738585072014E-308", ws)
        checkDouble("2.2250738585072013E-308", ws)
        checkDouble("2.2250738585072012E-308", ws)
        checkDouble("2.2250738585072011E-308", ws)
      }
      forAll(arbitrary[Double], genWhitespaces, minSuccessful(10000))((n, ws) => checkDouble(n.toString, ws))
      forAll(arbitrary[Float], genWhitespaces, minSuccessful(10000))((n, ws) => checkDouble(n.toString, ws))
      forAll(arbitrary[Long], genWhitespaces, minSuccessful(10000)) { (n, ws) =>
        val x = java.lang.Double.longBitsToDouble(n)
        whenever(java.lang.Double.isFinite(x)) {
          checkDouble(x.toString, ws)
        }
      }
      forAll(arbitrary[Long], genWhitespaces, minSuccessful(10000))((n, ws) => checkDouble(n.toString, ws))
      forAll(Gen.choose(0L, (1L << 53) - 1), Gen.choose(-22, 37), genWhitespaces, minSuccessful(10000)) { (m, e, ws) =>
        checkDouble(s"${m}e$e", ws)
      }
      forAll(genBigInt, genWhitespaces, minSuccessful(10000))((n, ws) => checkDouble(n.toString, ws))
      forAll(genBigDecimal, genWhitespaces, minSuccessful(10000))((n, ws) => checkDouble(n.toString, ws))
    }
    "parse infinities on double overflow" in {
      forAll(genWhitespaces) { ws =>
        check("12345e6789", Double.PositiveInfinity, ws)
        check("-12345e6789", Double.NegativeInfinity, ws)
        check("123456789012345678901234567890e9223372036854775799", Double.PositiveInfinity, ws)
        check("-123456789012345678901234567890e9223372036854775799", Double.NegativeInfinity, ws)
        check("12345678901234567890e12345678901234567890", Double.PositiveInfinity, ws)
        check("-12345678901234567890e12345678901234567890", Double.NegativeInfinity, ws)
      }
      reader("12345678901234567890e12345678901234567890$").readDouble() shouldBe Double.PositiveInfinity
      reader("-12345678901234567890e12345678901234567890$").readDouble() shouldBe Double.NegativeInfinity
    }
    "parse zeroes on double underflow" in {
      forAll(genWhitespaces) { ws =>
        check("12345e-6789", 0.0, ws)
        check("-12345e-6789", -0.0, ws)
        check("0.12345678901234567890e-9223372036854775799", 0.0, ws)
        check("-0.12345678901234567890e-9223372036854775799", -0.0, ws)
        check("12345678901234567890e-12345678901234567890", 0.0, ws)
        check("-1234567890123456789e-12345678901234567890", -0.0, ws)
      }
    }
    "parse positive and negative zeroes" in {
      forAll(genWhitespaces) { ws =>
        check2("0.0", 0.0, ws)
        check2("-0.0", -0.0, ws)
      }
    }
    "parse denormalized numbers with long mantissa and compensating exponent" in {
      forAll(genWhitespaces) { ws =>
        check("1" + "0" * 1000000 + "e-1000000", 1.0, ws)
        check("0." + "0" * 1000000 + "1e1000000", 0.1, ws)
      }
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(".1", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("0e$", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0e-$", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("0.E", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.-", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.+", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readDouble()).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
    "throw parsing exception on too long input" in {
      checkError(tooLongNumber, """too long part of input exceeded 'maxBufSize', offset: 0x02000000""",
       """too long part of input exceeded 'maxBufSize', offset: 0x02000001""")
    }
  }
  "JsonReader.readBigInt and JsonReader.readStringAsBigInt" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readBigInt(null))
        .getMessage.startsWith("illegal number, offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readStringAsBigInt(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      reader("null").readBigInt(BigInt("12345")) shouldBe BigInt("12345")
      reader("null").readStringAsBigInt(BigInt("12345")) shouldBe BigInt("12345")
    }
  }
  "JsonReader.readBigInt, JsonReader.readStringAsBigInt and JsonReader.readKeyAsBigInt" should {
    def check(n: BigInt, ws: String): Unit = {
      val s = n.toString
      reader(ws + s).readBigInt(null, Int.MaxValue) shouldBe n
      reader(s"""$ws"$s":""").readKeyAsBigInt(Int.MaxValue) shouldBe n
      reader(s"""$ws"$s"""").readStringAsBigInt(null, Int.MaxValue) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readBigInt(null)).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsBigInt()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsBigInt(null)).getMessage.startsWith(error2))
    }

    "parse valid number values" in {
      forAll(genBigInt, genWhitespaces, minSuccessful(10000))(check)
    }
    "parse big number values without overflow up to limits" in {
      forAll(genWhitespaces) { ws =>
        val bigNumber = "9" * 1000
        check(BigInt(bigNumber), ws)
        check(BigInt(s"-$bigNumber"), ws)
      }
    }
    "throw parsing exception for values with more than max allowed digits" in {
      checkError("9" * 308,
        "value exceeds limit for number of digits, offset: 0x00000133",
        "value exceeds limit for number of digits, offset: 0x00000134")
      checkError(s"-${"9" * 308}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("1234567890123456789.0", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789e10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789E10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readBigInt(null)).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345", "illegal number with leading zero, offset: 0x00000001")
    }
    "throw parsing exception on too long input" in {
      checkError(tooLongNumber, """too long part of input exceeded 'maxBufSize', offset: 0x02000000""",
        """too long part of input exceeded 'maxBufSize', offset: 0x02000001""")
    }
  }
  "JsonReader.readBigDecimal and JsonReader.readStringAsBigDecimal" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readBigDecimal(null))
        .getMessage.startsWith("illegal number, offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readStringAsBigDecimal(null))
        .getMessage.startsWith("""expected '"', offset: 0x00000000"""))
    }
    "return supplied default value instead of null value" in {
      reader("null").readBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
      reader("null").readStringAsBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
    }
  }
  "JsonReader.readBigDecimal, JsonReader.readKeyAsBigDecimal and JsonReader.readStringAsBigDecimal" should {
    def check(s: String, mc: MathContext, scaleLimit: Int, digitsLimit: Int, ws: String): Unit = {
      def compare(a: BigDecimal, b: BigDecimal): Unit = {
        a.bigDecimal shouldBe b.bigDecimal
        a.mc shouldBe b.mc
      }

      val n = BigDecimal(s, mc)
      compare(reader(ws + s).readBigDecimal(null, mc, scaleLimit, digitsLimit), n)
      compare(reader(s"""$ws"$s":""").readKeyAsBigDecimal(mc, scaleLimit, digitsLimit), n)
      compare(reader(s"""$ws"$s"""").readStringAsBigDecimal(null, mc, scaleLimit, digitsLimit), n)
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readBigDecimal(null)).getMessage.startsWith(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsBigDecimal()).getMessage.startsWith(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsBigDecimal(null))
        .getMessage.startsWith(error2))
    }

    "parse valid number values with scale less than specified maximum" in {
      forAll(genBigDecimal, genWhitespaces, minSuccessful(10000)) { (n, ws) =>
        check(n.toString, MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws)
      }
    }
    "parse big number values without overflow up to limits" in {
      forAll(genWhitespaces) { ws =>
        check("1234567890" * 30, bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check("12345e67", bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check("-12345e67", bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check("12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789",
          MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws)
        check("-12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789",
          MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws)
        check("1E+2147483646", MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws) // max positive scale that can be parsed
      }
    }
    "parse small number values without underflow up to limits" in {
      forAll(genWhitespaces) { ws =>
        check(s"0.${"0" * 100}1234567890123456789012345678901234",
          bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check("12345e-67", bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check("-12345e-67", bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check("12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789",
          MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws)
        check("-12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789",
          MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws)
        check("1E-2147483646", MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue, ws) // max negative scale that can be parsed
      }
    }
    "throw number format exception for too big mantissa" in {
      checkError("9" * 308,
        "value exceeds limit for number of digits, offset: 0x00000133",
        "value exceeds limit for number of digits, offset: 0x00000134")
      checkError(s"-${"9" * 308}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
      checkError(s"${"9" * 154}.${"9" * 154}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
      checkError(s"0.${"0" * 307}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
      checkError(s"${"9" * 308}.${"0" * 307}",
        "value exceeds limit for number of digits, offset: 0x00000133",
        "value exceeds limit for number of digits, offset: 0x00000134")
    }
    "throw number format exception for too big scale" in {
      forAll(genWhitespaces) { ws =>
        check(s"1e${bigDecimalScaleLimit - 1}", bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check(s"1e-${bigDecimalScaleLimit - 1}", bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check(s"${"1" * 50}e${bigDecimalScaleLimit - 17}",
          MathContext.DECIMAL128, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
        check(s"${"1" * 50}e-${bigDecimalScaleLimit + 15}",
          MathContext.DECIMAL128, bigDecimalScaleLimit, bigDecimalDigitsLimit, ws)
      }
      checkError(s"1e$bigDecimalScaleLimit",
        "value exceeds limit for scale, offset: 0x00000005",
        "value exceeds limit for scale, offset: 0x00000006")
      checkError(s"1e-$bigDecimalScaleLimit",
        "value exceeds limit for scale, offset: 0x00000006",
        "value exceeds limit for scale, offset: 0x00000007")
      checkError(s"1.1e${bigDecimalScaleLimit + 1}",
        "value exceeds limit for scale, offset: 0x00000007",
        "value exceeds limit for scale, offset: 0x00000008")
      checkError(s"1.1e-${bigDecimalScaleLimit + 1}",
        "value exceeds limit for scale, offset: 0x00000008",
        "value exceeds limit for scale, offset: 0x00000009")
      checkError("1e2147483648",
        "illegal number, offset: 0x0000000b",
        "illegal number, offset: 0x0000000c")
      checkError("1e-2147483649",
        "illegal number, offset: 0x0000000c",
        "illegal number, offset: 0x0000000d")
      checkError("1e9999999999",
        "illegal number, offset: 0x0000000b",
        "illegal number, offset: 0x0000000c")
      checkError("1e-9999999999",
        "illegal number, offset: 0x0000000c",
        "illegal number, offset: 0x0000000d")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(".1", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("0e$", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0e-$", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("0.E", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.-", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.+", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonReaderException](reader(s).readBigDecimal(null)).getMessage.startsWith(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
    "throw parsing exception on too long input" in {
      checkError(tooLongNumber, """too long part of input exceeded 'maxBufSize', offset: 0x02000000""",
        """too long part of input exceeded 'maxBufSize', offset: 0x02000001""")
    }
  }
  "JsonReader.setMark and JsonReader.rollbackToMark" should {
    "store current position of parsing and return back to it" in {
      def check[A](n: Int, s2: String)(f: JsonReader => A): Unit = {
        val jsonReader = reader("{}" + " " * n + s2)
        jsonReader.setMark()
        jsonReader.skip()
        jsonReader.setMark()
        f(jsonReader)
        jsonReader.rollbackToMark()
        jsonReader.nextToken().toChar shouldBe s2.charAt(0)
      }

      forAll(Gen.size, minSuccessful(10000)) { n =>
        check(n, "123456")(_.readBigInt(null))
        check(n, """"UTC"""")(_.readZoneId(null))
        check(n, "[true]")(_.readRawValAsBytes())
        check(n, "123.456")(_.readBigDecimal(null))
        check(n, "9223372036854776832")(_.readDouble())
        check(n, "1.00000017881393432617187499")(_.readFloat())
      }
    }
    "throw exception in case of rollbackToMark was called before setMark" in {
      val jsonReader = reader("{}")
      jsonReader.skip()
      assert(intercept[IllegalStateException](jsonReader.rollbackToMark())
        .getMessage.startsWith("expected preceding call of 'setMark()'"))
    }
  }
  "JsonReader.skipToKey" should {
    "return true in case of key is found and set current position of parsing to its value" in {
      val jsonReader = reader("""{"key1":1,"key2":2}""")
      jsonReader.isNextToken('{')
      jsonReader.skipToKey("key2") shouldBe true
      jsonReader.readInt() shouldBe 2
    }
    "return false in case of key cannot be found and set current positing to the closing of object" in {
      val jsonReader = reader("""{"key1":1}""")
      jsonReader.isNextToken('{')
      jsonReader.skipToKey("key2")
      jsonReader.isCurrentToken('}') shouldBe true
    }
  }
  "JsonReader.requiredFieldError" should {
    "throw parsing exception with missing required field" in {
      val jsonReader = reader("{}")
      jsonReader.nextToken()
      assert(intercept[JsonReaderException](jsonReader.requiredFieldError("name"))
        .getMessage.startsWith("""missing required field "name", offset: 0x00000000"""))
    }
  }
  "JsonReader.duplicatedKeyError" should {
    "throw parsing exception with name of duplicated key" in {
      val jsonReader = reader(""""xxx"""")
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonReaderException](jsonReader.duplicatedKeyError(len))
        .getMessage.startsWith("""duplicated field "xxx", offset: 0x00000004"""))
    }
  }
  "JsonReader.unexpectedKeyError" should {
    "throw parsing exception with name of unexpected key" in {
      val jsonReader = reader(""""xxx"""")
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonReaderException](jsonReader.unexpectedKeyError(len))
        .getMessage.startsWith("""unexpected field "xxx", offset: 0x00000004"""))
    }
  }
  "JsonReader.discriminatorError" should {
    "throw parsing exception with unexpected discriminator" in {
      val jsonReader = reader(""""xxx"""")
      jsonReader.readString(null)
      assert(intercept[JsonReaderException](jsonReader.discriminatorError())
        .getMessage.startsWith("illegal discriminator, offset: 0x00000004"))
    }
  }
  "JsonReader.discriminatorValueError" should {
    "throw parsing exception with unexpected discriminator value" in {
      val jsonReader = reader(""""xxx"""")
      val value = jsonReader.readString(null)
      assert(intercept[JsonReaderException](jsonReader.discriminatorValueError(value))
        .getMessage.startsWith("""illegal value of discriminator field "xxx", offset: 0x00000004"""))
    }
  }
  "JsonReader.enumValueError" should {
    "throw parsing exception with unexpected enum value as string" in {
      val jsonReader = reader(""""xxx"""")
      val value = jsonReader.readString(null)
      assert(intercept[JsonReaderException](jsonReader.enumValueError(value))
        .getMessage.startsWith("""illegal enum value "xxx", offset: 0x00000004"""))
    }
    "throw parsing exception with unexpected enum value as length of character buffer" in {
      val jsonReader = reader(""""xxx"""")
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonReaderException](jsonReader.enumValueError(len))
        .getMessage.startsWith("""illegal enum value "xxx", offset: 0x00000004"""))
    }
  }
  "JsonReader.commaError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("{}")
      jsonReader.isNextToken(',')
      assert(intercept[JsonReaderException](jsonReader.commaError())
        .getMessage.startsWith("expected ',', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayStartOrNullError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("{}")
      jsonReader.isNextToken('[')
      assert(intercept[JsonReaderException](jsonReader.arrayStartOrNullError())
        .getMessage.startsWith("expected '[' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("}")
      jsonReader.isNextToken(']')
      assert(intercept[JsonReaderException](jsonReader.arrayEndError())
        .getMessage.startsWith("expected ']', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndOrCommaError" should {
    val jsonReader = reader("}")
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonReaderException](jsonReader.arrayEndOrCommaError())
        .getMessage.startsWith("expected ']' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader.objectStartOrNullError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("[]")
      jsonReader.isNextToken('{')
      assert(intercept[JsonReaderException](jsonReader.objectStartOrNullError())
        .getMessage.startsWith("expected '{' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.objectEndOrCommaError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("]")
      jsonReader.isNextToken('}')
      assert(intercept[JsonReaderException](jsonReader.objectEndOrCommaError())
        .getMessage.startsWith("expected '}' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader" should {
    "support hex dumps with offsets that greater than 4Gb" in {
      assert(intercept[JsonReaderException](reader("null", 1L << 41).readInt())
        .getMessage.startsWith(
        """illegal number, offset: 0x20000000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 6e 75 6c 6c                                     | null             |
          |+----------+-------------------------------------------------+------------------+""".stripMargin))
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

  def tooLongNumber: String = {
    val sb = new StringBuilder
    var i = 0
    while (i < ReaderConfig.maxBufSize) {
      sb.append('1')
      i += 1
    }
    sb.toString
  }
}