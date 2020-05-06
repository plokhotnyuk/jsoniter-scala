package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.{BufferOverflowException, ByteBuffer, ReadOnlyBufferException}
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class PackageSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "readFromStream" should {
    "parse JSON from the provided input stream" in {
      readFromStream(getClass.getResourceAsStream("user_api_response.json"))(codec) shouldBe user
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonReaderException](readFromStream(new ByteArrayInputStream(httpMessage))(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      def streamWithError = getClass.getResourceAsStream("user_api_response_with_error.json")

      readFromStream(streamWithError, ReaderConfig.withCheckForEndOfInput(false))(codec) shouldBe user
      assert(intercept[JsonReaderException](readFromStream(streamWithError)(codec)).getMessage ==
        """expected end of input, offset: 0x00000094, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000070 | 20 20 20 20 20 22 6d 6f 64 65 6c 22 3a 20 22 69 |      "model": "i |
          || 00000080 | 50 68 6f 6e 65 20 58 22 0a 20 20 20 20 7d 0a 20 | Phone X".    }.  |
          || 00000090 | 20 5d 0a 7d 7d 7d 7d 7d 7d 7d                   |  ].}}}}}}}       |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromStream(new ByteArrayInputStream(compactJson))(null))
      intercept[NullPointerException](readFromStream(null)(codec))
    }
  }
  "readFromArray" should {
    "parse JSON from the byte array" in {
      readFromArray(compactJson)(codec) shouldBe user
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonReaderException](readFromArray(httpMessage)(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      val compactJsonWithError = compactJson ++ "}}}}}".getBytes(UTF_8)

      readFromArray(compactJsonWithError, ReaderConfig.withCheckForEndOfInput(false))(codec) shouldBe user
      assert(intercept[JsonReaderException](readFromArray(compactJsonWithError)(codec)).getMessage ==
        """expected end of input, offset: 0x00000054, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
          || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
          || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromArray(compactJson)(null))
      intercept[NullPointerException](readFromArray(null)(codec))
    }
  }
  "readFromSubArray" should {
    "parse JSON from the byte array within specified positions" in {
      readFromSubArray(httpMessage, 66, httpMessage.length)(codec) shouldBe user
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonReaderException](readFromSubArray(httpMessage, 0, httpMessage.length)(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      val msgWithError = httpMessage ++ "}}}}}".getBytes(UTF_8)

      readFromSubArray(msgWithError, 66, msgWithError.length, ReaderConfig.withCheckForEndOfInput(false))(codec) shouldBe user
      assert(intercept[JsonReaderException](readFromSubArray(msgWithError, 66, msgWithError.length)(codec)).getMessage ==
        """expected end of input, offset: 0x00000097, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000070 | 43 20 4f 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a | C One X"},{"id": |
          || 00000080 | 32 2c 22 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e | 2,"model":"iPhon |
          || 00000090 | 65 20 58 22 7d 5d 7d 7d 7d 7d 7d 7d             | e X"}]}}}}}}     |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw ArrayIndexOutOfBoundsException or NullPointerException in case of the provided params are invalid or null" in {
      intercept[NullPointerException](readFromSubArray(httpMessage, 66, httpMessage.length)(null))
      intercept[NullPointerException](readFromSubArray(null, 0, 50)(codec))
      intercept[NullPointerException](readFromSubArray(httpMessage, 66, httpMessage.length, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArray(httpMessage, 50, 200)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArray(httpMessage, 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
    }
  }
  "readFromByteBuffer" should {
    "parse JSON from the current position of the provided direct byte buffer" in {
      val bbuf = ByteBuffer.allocateDirect(100000)
      bbuf.position(50000)
      bbuf.put(httpMessage)
      bbuf.position(50066)
      bbuf.limit(50000 + httpMessage.length)
      readFromByteBuffer(bbuf)(codec) shouldBe user
      bbuf.position() shouldBe 50000 + httpMessage.length
      bbuf.limit() shouldBe 50000 + httpMessage.length
    }
    "parse JSON from the current position of the provided array based byte buffer" in {
      var bbuf = ByteBuffer.wrap(new Array[Byte](100000))
      bbuf.position(10000)
      bbuf = bbuf.slice()
      bbuf.position(50000)
      bbuf.put(httpMessage)
      bbuf.position(50066)
      bbuf.limit(50000 + httpMessage.length)
      readFromByteBuffer(bbuf)(codec) shouldBe user
      bbuf.position() shouldBe 50000 + httpMessage.length
      bbuf.limit() shouldBe 50000 + httpMessage.length
      bbuf.arrayOffset() shouldBe 10000
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part of the direct byte buffer" in {
      val bbuf = ByteBuffer.allocateDirect(httpMessage.length)
      bbuf.put(httpMessage)
      bbuf.position(10)
      assert(intercept[JsonReaderException](readFromByteBuffer(bbuf)(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 30 30 20 4f 4b 0a 43 6f 6e 74 65 6e 74 2d 54 79 | 00 OK.Content-Ty |
          || 00000010 | 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f | pe: application/ |
          || 00000020 | 6a 73 6f 6e 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e | json.Content-Len |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
      bbuf.position() shouldBe 11
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part the array based byte buffer" in {
      val bbuf = ByteBuffer.wrap(httpMessage)
      bbuf.position(10)
      assert(intercept[JsonReaderException](readFromByteBuffer(bbuf)(codec)).getMessage ==
        """expected '{', offset: 0x0000000a, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
      bbuf.position() shouldBe 11
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      def bbufWithError: ByteBuffer = ByteBuffer.wrap(compactJson ++ "}}}}}".getBytes(UTF_8))

      def directBbufWithError: ByteBuffer = {
        val compactJsonWithError = compactJson ++ "}}}}}".getBytes(UTF_8)
        val bbuf = ByteBuffer.allocateDirect(compactJsonWithError.length)
        bbuf.put(compactJsonWithError)
        bbuf.position(0)
        bbuf
      }

      readFromByteBuffer(bbufWithError, ReaderConfig.withCheckForEndOfInput(false))(codec) shouldBe user
      assert(intercept[JsonReaderException](readFromByteBuffer(bbufWithError)(codec)).getMessage ==
        """expected end of input, offset: 0x00000054, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
          || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
          || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
      readFromByteBuffer(directBbufWithError, ReaderConfig.withCheckForEndOfInput(false))(codec) shouldBe user
      assert(intercept[JsonReaderException](readFromByteBuffer(directBbufWithError)(codec)).getMessage ==
        """expected end of input, offset: 0x00000054, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
          || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
          || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromByteBuffer(null.asInstanceOf[ByteBuffer])(codec))
      val bbuf1 = ByteBuffer.allocateDirect(150)
      intercept[NullPointerException](readFromByteBuffer(bbuf1)(null))
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
      intercept[NullPointerException](readFromByteBuffer(bbuf2)(null))
    }
  }
  "scanValueStream" should {
    def inputStream: InputStream = getClass.getResourceAsStream("user_api_value_stream.json")

    "scan JSON values from the provided input stream" in {
      var users: Seq[User] = Seq.empty
      scanJsonValuesFromStream(inputStream) { u: User =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq(user, user1, user2)
    }
    "scanning of JSON values can be interrupted by returning `false` from the consumer" in {
      var users: Seq[User] = Seq.empty
      scanJsonValuesFromStream(inputStream) { u: User =>
        users = users :+ u
        false
      }(codec)
      users shouldBe Seq(user)
    }
    "throw NullPointerException in case of the provided params are null" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanJsonValuesFromStream(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonValuesFromStream(inputStream, null)(skip)(codec))
      intercept[NullPointerException](scanJsonValuesFromStream(inputStream)(npe)(codec))
    }
  }
  "scanArray" should {
    def inputStream: InputStream = getClass.getResourceAsStream("user_api_array.json")

    "scan values of JSON array from the provided input stream" in {
      var users: Seq[User] = Seq.empty
      scanJsonArrayFromStream(inputStream) { u: User =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq(user, user1, user2)
    }
    "scanning of JSON array values can be interrupted by returning `false` from the consumer" in {
      var users: Seq[User] = Seq.empty
      scanJsonArrayFromStream(inputStream) { u: User =>
        users = users :+ u
        false
      }(codec)
      users shouldBe Seq(user)
    }
    "scan null value from the provided input stream" in {
      var users: Seq[User] = Seq.empty
      scanJsonArrayFromStream(new ByteArrayInputStream("null".getBytes("UTF-8"))) { u: User =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq()
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      def inputStreamWithError: InputStream = getClass.getResourceAsStream("user_api_array_with_error.json")

      scanJsonArrayFromStream[User](inputStreamWithError)(_ => false)(codec)
      scanJsonArrayFromStream[User](inputStreamWithError, ReaderConfig.withCheckForEndOfInput(false))(_ => true)(codec)
      assert(intercept[JsonReaderException](scanJsonArrayFromStream[User](inputStreamWithError)(_ => true)(codec)).getMessage ==
        """expected end of input, offset: 0x00000193, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000170 | 65 6c 22 3a 20 22 69 50 68 6f 6e 65 20 58 22 0a | el": "iPhone X". |
          || 00000180 | 20 20 20 20 20 20 7d 0a 20 20 20 20 5d 0a 20 20 |       }.    ].   |
          || 00000190 | 7d 0a 5d 5d 5d 5d 5d 5d 5d                      | }.]]]]]]]        |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw a parse exception in case of JSON array is not closed properly" in {
      assert(intercept[JsonReaderException] {
        scanJsonArrayFromStream(new ByteArrayInputStream("""[{"name":"x"}y""".getBytes("UTF-8"))) { _: User =>
          true
        }(codec)
      }.getMessage.contains("expected ']' or ',', offset: 0x0000000d"))
    }
    "throw a parse exception in case of input isn't JSON array or null" in {
      assert(intercept[JsonReaderException] {
        scanJsonArrayFromStream(new ByteArrayInputStream("""{}""".getBytes("UTF-8"))) { _: User =>
          true
        }(codec)
      }.getMessage.contains("expected '[' or null, offset: 0x00000000"))
    }
    "throw NullPointerException in case of the provided params are null" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanJsonArrayFromStream(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStream(inputStream, null)(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStream(inputStream)(npe)(codec))
    }
  }
  "readFromString" should {
    "parse JSON from the byte array" in {
      readFromString(new String(compactJson, UTF_8))(codec) shouldBe user
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonReaderException](readFromString(new String(httpMessage, UTF_8))(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      val compactJsonWithError = compactJson ++ "}}}}}".getBytes(UTF_8)

      readFromString(new String(compactJsonWithError, UTF_8), ReaderConfig.withCheckForEndOfInput(false))(codec) shouldBe user
      assert(intercept[JsonReaderException](readFromString(new String(compactJsonWithError, UTF_8))(codec)).getMessage ==
        """expected end of input, offset: 0x00000054, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
          || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
          || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromString(new String(compactJson, UTF_8))(null))
      intercept[NullPointerException](readFromString(null)(codec))
    }
  }
  "writeToStream" should {
    "serialize an object to the provided output stream" in {
      val out1 = new ByteArrayOutputStream()
      writeToStream(user, out1)(codec)
      out1.toString("UTF-8") shouldBe toString(compactJson)
      val out2 = new ByteArrayOutputStream()
      writeToStream(user, out2, WriterConfig.withIndentionStep(2))(codec)
      out2.toString("UTF-8") shouldBe toString(prettyJson)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](writeToStream(user, new ByteArrayOutputStream())(null))
      intercept[NullPointerException](writeToStream(user, null.asInstanceOf[OutputStream])(codec))
      intercept[NullPointerException](writeToStream(user, new ByteArrayOutputStream(), null)(codec))
    }
  }
  "writeToArray" should {
    "serialize an object to a new instance of byte array" in {
      toString(writeToArray(user)(codec)) shouldBe toString(compactJson)
      toString(writeToArray(user, WriterConfig.withIndentionStep(2))(codec)) shouldBe toString(prettyJson)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](writeToArray(user)(null))
      intercept[NullPointerException](writeToArray(user, null.asInstanceOf[WriterConfig])(codec))
    }
  }
  "writeToPreallocatedArray" should {
    val buf = new Array[Byte](150)

    "serialize an object to the provided byte array from specified position" in {
      val from1 = 10
      val to1 = writeToSubArray(user, buf, from1, buf.length - 10)(codec)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      val from2 = 0
      val to2 = writeToSubArray(user, buf, from2, buf.length, WriterConfig.withIndentionStep(2))(codec)
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
    }
    "throw ArrayIndexOutOfBoundsException in case of the provided byte array is overflown during serialization" in {
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArray(user, buf, 100, buf.length)(codec))
        .getMessage.contains("`buf` length exceeded"))
    }
    "throw ArrayIndexOutOfBoundsException or NullPointerException in case of the provided params are invalid or null" in {
      intercept[NullPointerException](writeToSubArray(user, buf, 0, buf.length)(null))
      intercept[NullPointerException](writeToSubArray(user, null, 50, buf.length)(codec))
      intercept[NullPointerException](writeToSubArray(user, buf, 0, buf.length, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArray(user, new Array[Byte](10), 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArray(user, new Array[Byte](10), 50, 100)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
    }
  }
  "writeToByteBuffer" should {
    "serialize an object to the provided direct byte buffer from the current position" in {
      val buf = new Array[Byte](150)
      val bbuf = ByteBuffer.allocateDirect(150)
      val from1 = 10
      bbuf.position(from1)
      writeToByteBuffer(user, bbuf)(codec)
      val to1 = bbuf.position()
      bbuf.position(from1)
      bbuf.get(buf, from1, to1 - from1)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      bbuf.limit() shouldBe 150
      val from2 = 0
      bbuf.position(from2)
      writeToByteBuffer(user, bbuf, WriterConfig.withIndentionStep(2))(codec)
      val to2 = bbuf.position()
      bbuf.position(from2)
      bbuf.get(buf, from2, to2 - from2)
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
      bbuf.limit() shouldBe 150
    }
    "serialize an object to the provided array-based byte buffer from the current position" in {
      val buf = new Array[Byte](160)
      var bbuf = ByteBuffer.wrap(buf)
      val offset = 10
      bbuf.position(offset)
      bbuf = bbuf.slice()
      val from1 = 5
      bbuf.position(from1)
      writeToByteBuffer(user, bbuf)(codec)
      val to1 = bbuf.position()
      new String(buf, from1 + offset, to1 - from1, UTF_8) shouldBe toString(compactJson)
      bbuf.limit() shouldBe buf.length - offset
      bbuf.arrayOffset() shouldBe offset
      val from2 = 0
      bbuf.position(from2)
      writeToByteBuffer(user, bbuf, WriterConfig.withIndentionStep(2))(codec)
      val to2 = bbuf.position()
      new String(buf, from2 + offset, to2 - from2, UTF_8) shouldBe toString(prettyJson)
      bbuf.limit() shouldBe buf.length - offset
      bbuf.arrayOffset() shouldBe offset
    }
    "throw BufferOverflowException in case of the provided byte buffer is overflown during serialization" in {
      val bbuf1 = ByteBuffer.allocateDirect(150)
      bbuf1.position(100)
      intercept[BufferOverflowException](writeToByteBuffer(user, bbuf1)(codec))
      bbuf1.position() shouldBe 100
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
      bbuf2.position(100)
      intercept[BufferOverflowException](writeToByteBuffer(user, bbuf2)(codec))
      bbuf2.position() shouldBe 142
    }
    "throw ReadOnlyBufferException in case of the provided byte buffer is read-only" in {
      val bbuf1 = ByteBuffer.allocateDirect(150).asReadOnlyBuffer()
      bbuf1.position(100)
      intercept[ReadOnlyBufferException](writeToByteBuffer(user, bbuf1)(codec))
      bbuf1.position() shouldBe 100
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150)).asReadOnlyBuffer()
      bbuf2.position(100)
      intercept[ReadOnlyBufferException](writeToByteBuffer(user, bbuf2)(codec))
      bbuf2.position() shouldBe 100
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](writeToByteBuffer(user, null)(codec))
      val bbuf1 = ByteBuffer.allocateDirect(150)
      intercept[NullPointerException](writeToByteBuffer(user, bbuf1)(null))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf1, null)(codec))
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf2)(null))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf2, null)(codec))
    }
    "writeToString" should {
      "serialize an object to a string" in {
        writeToString(user)(codec) shouldBe toString(compactJson)
        writeToString(user, WriterConfig.withIndentionStep(2))(codec) shouldBe toString(prettyJson)
      }
      "throw NullPointerException in case of the provided params are null" in {
        intercept[NullPointerException](writeToArray(user)(null))
        intercept[NullPointerException](writeToArray(user, null.asInstanceOf[WriterConfig])(codec))
      }
    }
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}
