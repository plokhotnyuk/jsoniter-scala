package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.{BufferOverflowException, ByteBuffer, ReadOnlyBufferException}
import java.nio.charset.StandardCharsets.UTF_8
import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import java.math.MathContext

class PackageSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "readFromStream and readFromStreamReentrant" should {
    "parse JSON from the provided input stream" in {
      def check(f: InputStream => User): Unit =
        f(new ByteArrayInputStream(prettyJson)) shouldBe user

      check(readFromStream(_)(codec))
      check(readFromStreamReentrant(_)(codec))
      check(readFromStream(_)(reentrantCodec))
      check(readFromStreamReentrant(_)(reentrantCodec))
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part" in {
      def check(f: InputStream => User): Unit =
        assert(intercept[JsonReaderException](f(new ByteArrayInputStream(httpMessage))).getMessage ==
          """expected '{', offset: 0x00000000, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
            || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
            || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)

      check(readFromStream(_)(codec))
      check(readFromStreamReentrant(_)(codec))
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      def streamWithError = new ByteArrayInputStream(errorJson)

      def check(f: (InputStream, ReaderConfig) => User): Unit = {
        f(streamWithError, ReaderConfig.withCheckForEndOfInput(false)) shouldBe user
        assert(intercept[JsonReaderException](f(streamWithError, ReaderConfig)).getMessage ==
          """expected end of input, offset: 0x00000094, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000070 | 20 20 20 20 20 22 6d 6f 64 65 6c 22 3a 20 22 69 |      "model": "i |
            || 00000080 | 50 68 6f 6e 65 20 58 22 0a 20 20 20 20 7d 0a 20 | Phone X".    }.  |
            || 00000090 | 20 5d 0a 7d 7d 7d 7d 7d 7d 7d                   |  ].}}}}}}}       |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
      }

      check(readFromStream(_, _)(codec))
      check(readFromStreamReentrant(_, _)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromStream(new ByteArrayInputStream(compactJson))(null))
      intercept[NullPointerException](readFromStreamReentrant(new ByteArrayInputStream(compactJson))(null))
      intercept[NullPointerException](readFromStream(null)(codec))
      intercept[NullPointerException](readFromStreamReentrant(null)(codec))
    }
  }
  "readFromArray and readFromArrayReentrant" should {
    "parse JSON from the byte array" in {
      readFromArray(compactJson)(codec) shouldBe user
      readFromArrayReentrant(compactJson)(codec) shouldBe user
      readFromArray(compactJson)(reentrantCodec) shouldBe user
      readFromArrayReentrant(compactJson)(reentrantCodec) shouldBe user
    }
    "throw JsonParseException if cannot parse input with message containing input offset & hex dump of affected part" in {
      def check(f: Array[Byte] => User): Unit =
        assert(intercept[JsonReaderException](f(httpMessage)).getMessage ==
          """expected '{', offset: 0x00000000, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
            || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
            || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)

      check(readFromArray(_)(codec))
      check(readFromArrayReentrant(_)(codec))
    }
    "optionally throw JsonParseException if there are remaining non-whitespace characters" in {
      def check(f: (Array[Byte], ReaderConfig) => User): Unit = {
        val compactJsonWithError = compactJson ++ "}}}}}".getBytes(UTF_8)
        f(compactJsonWithError, ReaderConfig.withCheckForEndOfInput(false)) shouldBe user
        assert(intercept[JsonReaderException](f(compactJsonWithError, ReaderConfig)).getMessage ==
          """expected end of input, offset: 0x00000054, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
            || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
            || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
      }

      check(readFromArray(_, _)(codec))
      check(readFromArrayReentrant(_, _)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromArray(compactJson)(null))
      intercept[NullPointerException](readFromArrayReentrant(compactJson)(null))
      intercept[NullPointerException](readFromArray(null)(codec))
      intercept[NullPointerException](readFromArrayReentrant(null)(codec))
    }
  }
  "readFromSubArray and readFromSubArrayReentrant" should {
    "parse JSON from the byte array within specified positions" in {
      readFromSubArray(httpMessage, 66, httpMessage.length)(codec) shouldBe user
      readFromSubArrayReentrant(httpMessage, 66, httpMessage.length)(codec) shouldBe user
      readFromSubArray(httpMessage, 66, httpMessage.length)(reentrantCodec) shouldBe user
      readFromSubArrayReentrant(httpMessage, 66, httpMessage.length)(reentrantCodec) shouldBe user
    }
    "throw JsonReaderException if cannot parse input with message containing input offset & hex dump of affected part" in {
      def check(f: (Array[Byte], Int, Int) => User): Unit =
        assert(intercept[JsonReaderException](f(httpMessage, 0, httpMessage.length)).getMessage ==
          """expected '{', offset: 0x00000000, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
            || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
            || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)

      check(readFromSubArray(_, _, _)(codec))
      check(readFromSubArrayReentrant(_, _, _)(codec))
    }
    "optionally throw JsonReaderException if there are remaining non-whitespace characters" in {
      def check(f: (Array[Byte], Int, Int, ReaderConfig) => User): Unit = {
        val msgWithError = httpMessage ++ "}}}}}".getBytes(UTF_8)
        f(msgWithError, 66, msgWithError.length, ReaderConfig.withCheckForEndOfInput(false)) shouldBe user
        assert(intercept[JsonReaderException](f(msgWithError, 66, msgWithError.length, ReaderConfig)).getMessage ==
          """expected end of input, offset: 0x00000097, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000070 | 43 20 4f 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a | C One X"},{"id": |
            || 00000080 | 32 2c 22 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e | 2,"model":"iPhon |
            || 00000090 | 65 20 58 22 7d 5d 7d 7d 7d 7d 7d 7d             | e X"}]}}}}}}     |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
      }

      check(readFromSubArray(_, _, _, _)(codec))
      check(readFromSubArrayReentrant(_, _, _, _)(codec))
    }
    "throw ArrayIndexOutOfBoundsException or NullPointerException in case of the provided params are invalid or null" in {
      intercept[NullPointerException](readFromSubArray(httpMessage, 66, httpMessage.length)(null))
      intercept[NullPointerException](readFromSubArrayReentrant(httpMessage, 66, httpMessage.length)(null))
      intercept[NullPointerException](readFromSubArray(null, 0, 50)(codec))
      intercept[NullPointerException](readFromSubArrayReentrant(null, 0, 50)(codec))
      intercept[NullPointerException](readFromSubArray(httpMessage, 66, httpMessage.length, null)(codec))
      intercept[NullPointerException](readFromSubArrayReentrant(httpMessage, 66, httpMessage.length, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArray(httpMessage, 50, 200)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArrayReentrant(httpMessage, 50, 200)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArray(httpMessage, 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArrayReentrant(httpMessage, 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
    }
  }
  "readFromByteBuffer and readFromByteBufferReentrant" should {
    "parse JSON from the current position of the provided direct byte buffer" in {
      def check(f: ByteBuffer => User): Unit = {
        val bbuf = ByteBuffer.allocateDirect(100000)
        bbuf.position(50000)
        bbuf.put(httpMessage)
        bbuf.position(50066)
        bbuf.limit(50000 + httpMessage.length)
        f(bbuf) shouldBe user
        bbuf.position() shouldBe 50000 + httpMessage.length
        bbuf.limit() shouldBe 50000 + httpMessage.length
      }

      check(readFromByteBuffer(_)(codec))
      check(readFromByteBufferReentrant(_)(codec))
      check(readFromByteBuffer(_)(reentrantCodec))
      check(readFromByteBufferReentrant(_)(reentrantCodec))
    }
    "parse JSON from the current position of the provided array based byte buffer" in {
      def check(f: ByteBuffer => User): Unit = {
        var bbuf = ByteBuffer.wrap(new Array[Byte](100000))
        bbuf.position(10000)
        bbuf = bbuf.slice()
        bbuf.position(50000)
        bbuf.put(httpMessage)
        bbuf.position(50066)
        bbuf.limit(50000 + httpMessage.length)
        f(bbuf) shouldBe user
        bbuf.position() shouldBe 50000 + httpMessage.length
        bbuf.limit() shouldBe 50000 + httpMessage.length
        bbuf.arrayOffset() shouldBe 10000
      }

      check(readFromByteBuffer(_)(codec))
      check(readFromByteBufferReentrant(_)(codec))
      check(readFromByteBuffer(_)(reentrantCodec))
      check(readFromByteBufferReentrant(_)(reentrantCodec))
    }
    "throw JsonReaderException if cannot parse input with message containing input offset & hex dump of affected part of the direct byte buffer" in {
      def check(f: ByteBuffer => User): Unit = {
        val bbuf = ByteBuffer.allocateDirect(httpMessage.length)
        bbuf.put(httpMessage)
        bbuf.position(10)
        assert(intercept[JsonReaderException](f(bbuf)).getMessage ==
          (if (TestUtils.isNative) {
            """expected '{', offset: 0x0000000a, buf:
              |+----------+-------------------------------------------------+------------------+
              ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
              |+----------+-------------------------------------------------+------------------+
              || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
              || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
              || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
              |+----------+-------------------------------------------------+------------------+""".stripMargin
          } else {
            """expected '{', offset: 0x00000000, buf:
              |+----------+-------------------------------------------------+------------------+
              ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
              |+----------+-------------------------------------------------+------------------+
              || 00000000 | 30 30 20 4f 4b 0a 43 6f 6e 74 65 6e 74 2d 54 79 | 00 OK.Content-Ty |
              || 00000010 | 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f | pe: application/ |
              || 00000020 | 6a 73 6f 6e 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e | json.Content-Len |
              |+----------+-------------------------------------------------+------------------+""".stripMargin
          }))
        bbuf.position() shouldBe 11
      }

      check(readFromByteBuffer(_)(codec))
      check(readFromByteBufferReentrant(_)(codec))
    }
    "throw JsonReaderException if cannot parse input with message containing input offset & hex dump of affected part the array based byte buffer" in {
      def check(f: ByteBuffer => User): Unit = {
        val bbuf = ByteBuffer.wrap(httpMessage)
        bbuf.position(10)
        assert(intercept[JsonReaderException](f(bbuf)).getMessage ==
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

      check(readFromByteBuffer(_)(codec))
      check(readFromByteBufferReentrant(_)(codec))
    }
    "optionally throw JsonReaderException if there are remaining non-whitespace characters" in {
      def bbufWithError: ByteBuffer = ByteBuffer.wrap(compactJson ++ "}}}}}".getBytes(UTF_8))

      def directBbufWithError: ByteBuffer = {
        val compactJsonWithError = compactJson ++ "}}}}}".getBytes(UTF_8)
        val bbuf = ByteBuffer.allocateDirect(compactJsonWithError.length)
        bbuf.put(compactJsonWithError)
        bbuf.position(0)
        bbuf
      }

      def check(f: (ByteBuffer, ReaderConfig) => User): Unit = {
        f(bbufWithError, ReaderConfig.withCheckForEndOfInput(false)) shouldBe user
        assert(intercept[JsonReaderException](f(bbufWithError, ReaderConfig)).getMessage ==
          """expected end of input, offset: 0x00000054, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
            || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
            || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
        f(directBbufWithError, ReaderConfig.withCheckForEndOfInput(false)) shouldBe user
        assert(intercept[JsonReaderException](f(directBbufWithError, ReaderConfig)).getMessage ==
          """expected end of input, offset: 0x00000054, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
            || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
            || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
      }

      check(readFromByteBuffer(_, _)(codec))
      check(readFromByteBufferReentrant(_, _)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromByteBuffer(null.asInstanceOf[ByteBuffer])(codec))
      intercept[NullPointerException](readFromByteBufferReentrant(null.asInstanceOf[ByteBuffer])(codec))
      intercept[NullPointerException](readFromByteBuffer(ByteBuffer.allocateDirect(150))(null))
      intercept[NullPointerException](readFromByteBufferReentrant(ByteBuffer.allocateDirect(150))(null))
      intercept[NullPointerException](readFromByteBuffer(ByteBuffer.wrap(new Array[Byte](150)))(null))
      intercept[NullPointerException](readFromByteBufferReentrant(ByteBuffer.wrap(new Array[Byte](150)))(null))
    }
  }
  "scanValueStream and scanValueStreamReentrant" should {
    "scan JSON values from the provided input stream" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit = {
        var users: Seq[User] = Seq.empty
        f(new ByteArrayInputStream(valueStreamJson), { (u: User) =>
          users = users :+ u
          true
        })
        users shouldBe Seq(user, user1, user2)
      }

      check(scanJsonValuesFromStream(_)(_)(codec))
      check(scanJsonValuesFromStreamReentrant(_)(_)(codec))
      check(scanJsonValuesFromStream(_)(_)(reentrantCodec))
      check(scanJsonValuesFromStreamReentrant(_)(_)(reentrantCodec))
    }
    "scanning of JSON values can be interrupted by returning `false` from the consumer" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit = {
        var users: Seq[User] = Seq.empty
        f(new ByteArrayInputStream(valueStreamJson), { (u: User) =>
          users = users :+ u
          false
        })
        users shouldBe Seq(user)
      }

      check(scanJsonValuesFromStream(_)(_)(codec))
      check(scanJsonValuesFromStreamReentrant(_)(_)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanJsonValuesFromStream(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonValuesFromStreamReentrant(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonValuesFromStream(new ByteArrayInputStream(valueStreamJson), null)(skip)(codec))
      intercept[NullPointerException](scanJsonValuesFromStreamReentrant(new ByteArrayInputStream(valueStreamJson), null)(skip)(codec))
      intercept[NullPointerException](scanJsonValuesFromStream(new ByteArrayInputStream(valueStreamJson))(npe)(codec))
      intercept[NullPointerException](scanJsonValuesFromStreamReentrant(new ByteArrayInputStream(valueStreamJson))(npe)(codec))
    }
  }
  "scanArray and scanArrayReentrant" should {
    "scan values of JSON array from the provided input stream" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit = {
        var users: Seq[User] = Seq.empty
        f(new ByteArrayInputStream("[]".getBytes(UTF_8)), { (u: User) =>
          users = users :+ u
          true
        })
        users shouldBe Seq()
        f(new ByteArrayInputStream(arrayJson), { (u: User) =>
          users = users :+ u
          true
        })
        users shouldBe Seq(user, user1, user2)
      }

      check(scanJsonArrayFromStream(_)(_)(codec))
      check(scanJsonArrayFromStreamReentrant(_)(_)(codec))
      check(scanJsonArrayFromStream(_)(_)(reentrantCodec))
      check(scanJsonArrayFromStreamReentrant(_)(_)(reentrantCodec))
    }
    "scanning of JSON array values can be interrupted by returning `false` from the consumer" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit = {
        var users: Seq[User] = Seq.empty
        f(new ByteArrayInputStream(arrayJson), { (u: User) =>
          users = users :+ u
          false
        })
        users shouldBe Seq(user)
      }

      check(scanJsonArrayFromStream(_)(_)(codec))
      check(scanJsonArrayFromStreamReentrant(_)(_)(codec))
    }
    "scan null value from the provided input stream" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit = {
        var users: Seq[User] = Seq.empty
        f(new ByteArrayInputStream("null".getBytes("UTF-8")), { (u: User) =>
          users = users :+ u
          true
        })
        users shouldBe Seq()
      }

      check(scanJsonArrayFromStream(_)(_)(codec))
      check(scanJsonArrayFromStreamReentrant(_)(_)(codec))
    }
    "optionally throw JsonReaderException if there are remaining non-whitespace characters" in {
      def check(f: (InputStream, ReaderConfig, User => Boolean) => Unit): Unit = {
        f(new ByteArrayInputStream(arrayWithErrorJson), ReaderConfig, _ => false)
        f(new ByteArrayInputStream(arrayWithErrorJson), ReaderConfig.withCheckForEndOfInput(false), _ => true)
        assert(intercept[JsonReaderException](f(new ByteArrayInputStream(arrayWithErrorJson), ReaderConfig, _ => true)).getMessage ==
          """expected end of input, offset: 0x00000193, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000170 | 65 6c 22 3a 20 22 69 50 68 6f 6e 65 20 58 22 0a | el": "iPhone X". |
            || 00000180 | 20 20 20 20 20 20 7d 0a 20 20 20 20 5d 0a 20 20 |       }.    ].   |
            || 00000190 | 7d 0a 5d 5d 5d 5d 5d 5d 5d                      | }.]]]]]]]        |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
      }

      check(scanJsonArrayFromStream(_, _)(_)(codec))
      check(scanJsonArrayFromStreamReentrant(_, _)(_)(codec))
    }
    "throw a parse exception in case of JSON array is not closed properly" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit =
        assert(intercept[JsonReaderException] {
          f(new ByteArrayInputStream("""[{"name":"x"}y""".getBytes("UTF-8")), _ => true)
        }.getMessage.contains("expected ']' or ',', offset: 0x0000000d"))

      check(scanJsonArrayFromStream(_)(_)(codec))
      check(scanJsonArrayFromStreamReentrant(_)(_)(codec))
    }
    "throw a parse exception in case of input isn't JSON array or null" in {
      def check(f: (InputStream, User => Boolean) => Unit): Unit =
        assert(intercept[JsonReaderException] {
          f(new ByteArrayInputStream("""{}""".getBytes("UTF-8")), _ => true)
        }.getMessage.contains("expected '[' or null, offset: 0x00000000"))

      check(scanJsonArrayFromStream(_)(_)(codec))
      check(scanJsonArrayFromStreamReentrant(_)(_)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanJsonArrayFromStream(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStreamReentrant(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStream(new ByteArrayInputStream(arrayJson), null)(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStreamReentrant(new ByteArrayInputStream(arrayJson), null)(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStream(new ByteArrayInputStream(arrayJson))(npe)(codec))
      intercept[NullPointerException](scanJsonArrayFromStreamReentrant(new ByteArrayInputStream(arrayJson))(npe)(codec))
    }
  }
  "readFromString and readFromStringReentrant" should {
    "parse JSON from the byte array" in {
      readFromString(toString(compactJson))(codec) shouldBe user
      readFromStringReentrant(toString(compactJson))(codec) shouldBe user
      readFromString(toString(compactJson))(reentrantCodec) shouldBe user
      readFromStringReentrant(toString(compactJson))(reentrantCodec) shouldBe user
    }
    "throw JsonReaderException if cannot parse input with message containing input offset & hex dump of affected part" in {
      def check(f: String => Unit): Unit =
        assert(intercept[JsonReaderException](f(toString(httpMessage))).getMessage ==
          """expected '{', offset: 0x00000000, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
            || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
            || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)

      check(readFromString(_)(codec))
      check(readFromStringReentrant(_)(codec))
    }
    "size of the hex dump can be altered to have more lines" in {
      def check(f: (String, ReaderConfig) => Unit): Unit =
        assert(intercept[JsonReaderException](f(toString(httpMessage), ReaderConfig.withHexDumpSize(10))).getMessage ==
          """expected '{', offset: 0x00000000, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
            || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
            || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
            || 00000030 | 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 35 | ontent-Length: 5 |
            || 00000040 | 35 0a 0a 7b 22 6e 61 6d 65 22 3a 22 4a 6f 68 6e | 5..{"name":"John |
            || 00000050 | 22 2c 22 64 65 76 69 63 65 73 22 3a 5b 7b 22 69 | ","devices":[{"i |
            || 00000060 | 64 22 3a 31 2c 22 6d 6f 64 65 6c 22 3a 22 48 54 | d":1,"model":"HT |
            || 00000070 | 43 20 4f 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a | C One X"},{"id": |
            || 00000080 | 32 2c 22 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e | 2,"model":"iPhon |
            || 00000090 | 65 20 58 22 7d 5d 7d                            | e X"}]}          |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)

      check(readFromString(_, _)(codec))
      check(readFromStringReentrant(_, _)(codec))
    }
    "optionally throw JsonReaderException if there are remaining non-whitespace characters" in {
      def check(f: (String, ReaderConfig) => User): Unit = {
        val compactJsonWithError = compactJson ++ "}}}}}".getBytes(UTF_8)
        f(toString(compactJsonWithError), ReaderConfig.withCheckForEndOfInput(false)) shouldBe user
        assert(intercept[JsonReaderException](f(toString(compactJsonWithError), ReaderConfig)).getMessage ==
          """expected end of input, offset: 0x00000054, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000030 | 6e 65 20 58 22 7d 2c 7b 22 69 64 22 3a 32 2c 22 | ne X"},{"id":2," |
            || 00000040 | 6d 6f 64 65 6c 22 3a 22 69 50 68 6f 6e 65 20 58 | model":"iPhone X |
            || 00000050 | 22 7d 5d 7d 7d 7d 7d 7d 7d                      | "}]}}}}}}        |
            |+----------+-------------------------------------------------+------------------+""".stripMargin)
      }

      check(readFromString(_, _)(codec))
      check(readFromStringReentrant(_, _)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](readFromString(toString(compactJson))(null))
      intercept[NullPointerException](readFromStringReentrant(toString(compactJson))(null))
      intercept[NullPointerException](readFromString(null)(codec))
      intercept[NullPointerException](readFromStringReentrant(null)(codec))
    }
  }
  "writeToStream and writeToStreamReentrant" should {
    "serialize an object to the provided output stream" in {
      def check(f: (User, OutputStream, WriterConfig) => Unit): Unit =
        (1 to 99).foreach { preferredBufSize =>
          val out = new ByteArrayOutputStream()
          f(user, out, WriterConfig.withPreferredBufSize(preferredBufSize))
          out.toString("UTF-8") shouldBe toString(compactJson)
        }

      check(writeToStream(_, _, _)(codec))
      check(writeToStreamReentrant(_, _, _)(codec))
      check(writeToStream(_, _, _)(reentrantCodec))
      check(writeToStreamReentrant(_, _, _)(reentrantCodec))
    }
    "correctly write large data sets with BigDecimals to the provided output stream" in {
      val values: Seq[BigDecimal] = (1 to 3000).map(i => BigDecimal(i * 777))

      def check(f: (Seq[BigDecimal], OutputStream, JsonValueCodec[Seq[BigDecimal]]) => Unit): Unit = {
        val out = new ByteArrayOutputStream()
        f(values, out, new JsonValueCodec[Seq[BigDecimal]] {
          def encodeValue(x: Seq[BigDecimal], out: JsonWriter): Unit = {
            out.writeArrayStart()
            x.foreach(x => out.writeVal(x))
            out.writeArrayEnd()
          }

          def decodeValue(in: JsonReader, default: Seq[BigDecimal]): Seq[BigDecimal] = ???

          def nullValue: Seq[BigDecimal] = ???
        })
        out.toString("UTF-8") shouldBe values.mkString("[", ",", "]")
      }

      check(writeToStream(_, _)(_))
      check(writeToStreamReentrant(_, _)(_))
    }

    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](writeToStream[User](null, new ByteArrayOutputStream())(codec))
      intercept[NullPointerException](writeToStreamReentrant[User](null, new ByteArrayOutputStream())(codec))
      intercept[NullPointerException](writeToStream(user, new ByteArrayOutputStream())(null))
      intercept[NullPointerException](writeToStreamReentrant(user, new ByteArrayOutputStream())(null))
      intercept[NullPointerException](writeToStream(user, null.asInstanceOf[OutputStream])(codec))
      intercept[NullPointerException](writeToStreamReentrant(user, null.asInstanceOf[OutputStream])(codec))
      intercept[NullPointerException](writeToStream(user, new ByteArrayOutputStream(), null)(codec))
      intercept[NullPointerException](writeToStreamReentrant(user, new ByteArrayOutputStream(), null)(codec))
    }
  }
  "writeToArray and writeToArrayReentrant" should {
    "serialize an object to a new instance of byte array" in {
      toString(writeToArray(user)(codec)) shouldBe toString(compactJson)
      toString(writeToArrayReentrant(user)(codec)) shouldBe toString(compactJson)
      toString(writeToArray(user)(reentrantCodec)) shouldBe toString(compactJson)
      toString(writeToArrayReentrant(user)(reentrantCodec)) shouldBe toString(compactJson)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](writeToArray[User](null)(codec))
      intercept[NullPointerException](writeToArrayReentrant[User](null)(codec))
      intercept[NullPointerException](writeToArray(user)(null))
      intercept[NullPointerException](writeToArray(user, null.asInstanceOf[WriterConfig])(codec))
      intercept[NullPointerException](writeToArrayReentrant(user, null.asInstanceOf[WriterConfig])(codec))
    }
  }
  "writeToSubArray and writeToSubArrayReentrant" should {
    val buf = new Array[Byte](150)

    "serialize an object to the provided byte array from specified position" in {
      def check(f: (User, Array[Byte], Int, Int, WriterConfig) => Int): Unit = {
        val from1 = 10
        val to1 = f(user, buf, from1, buf.length - 10, WriterConfig)
        new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
        val from2 = 0
        val to2 = f(user, buf, from2, buf.length, WriterConfig.withIndentionStep(2))
        new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
      }

      check(writeToSubArray(_, _, _, _, _)(codec))
      check(writeToSubArrayReentrant(_, _, _, _, _)(codec))
    }
    "throw ArrayIndexOutOfBoundsException in case of the provided byte array is overflown during serialization" in {
      def check(f: (User, Array[Byte], Int, Int) => Int): Unit =
        assert(intercept[ArrayIndexOutOfBoundsException](f(user, buf, 100, buf.length))
          .getMessage.contains("`buf` length exceeded"))

      check(writeToSubArray(_, _, _, _)(codec))
      check(writeToSubArrayReentrant(_, _, _, _)(codec))
    }
    "throw ArrayIndexOutOfBoundsException or NullPointerException in case of the provided params are invalid or null" in {
      intercept[NullPointerException](writeToSubArray[User](null, buf, 0, buf.length)(codec))
      intercept[NullPointerException](writeToSubArrayReentrant[User](null, buf, 0, buf.length)(codec))
      intercept[NullPointerException](writeToSubArray(user, buf, 0, buf.length)(null))
      intercept[NullPointerException](writeToSubArrayReentrant(user, buf, 0, buf.length)(null))
      intercept[NullPointerException](writeToSubArray(user, null, 50, buf.length)(codec))
      intercept[NullPointerException](writeToSubArrayReentrant(user, null, 50, buf.length)(codec))
      intercept[NullPointerException](writeToSubArray(user, buf, 0, buf.length, null)(codec))
      intercept[NullPointerException](writeToSubArrayReentrant(user, buf, 0, buf.length, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArray(user, new Array[Byte](10), 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArrayReentrant(user, new Array[Byte](10), 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArray(user, new Array[Byte](10), 50, 100)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToSubArrayReentrant(user, new Array[Byte](10), 50, 100)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
    }
  }
  "writeToByteBuffer and writeToByteBufferReentrant" should {
    "serialize an object to the provided direct byte buffer from the current position" in {
      def check(f: (User, ByteBuffer, WriterConfig) => Unit): Unit =
        (1 to 99).foreach { preferredBufSize =>
          val buf = new Array[Byte](150)
          val bbuf = ByteBuffer.allocateDirect(150)
          val from = 10
          bbuf.position(from)
          f(user, bbuf, WriterConfig.withPreferredBufSize(preferredBufSize))
          val to = bbuf.position()
          bbuf.position(from)
          bbuf.get(buf, from, to - from)
          new String(buf, from, to - from, UTF_8) shouldBe toString(compactJson)
          bbuf.limit() shouldBe 150
        }

      check(writeToByteBuffer(_, _, _)(codec))
      check(writeToByteBufferReentrant(_, _, _)(codec))
      check(writeToByteBuffer(_, _, _)(reentrantCodec))
      check(writeToByteBufferReentrant(_, _, _)(reentrantCodec))
    }
    "serialize an object to the provided array-based byte buffer from the current position" in {
      def check(f: (User, ByteBuffer, WriterConfig) => Unit): Unit =
        (1 to 99).foreach { preferredBufSize =>
          val buf = new Array[Byte](160)
          var bbuf = ByteBuffer.wrap(buf)
          val offset = 10
          bbuf.position(offset)
          bbuf = bbuf.slice()
          val from = 5
          bbuf.position(from)
          f(user, bbuf, WriterConfig.withPreferredBufSize(preferredBufSize))
          val to = bbuf.position()
          new String(buf, from + offset, to - from, UTF_8) shouldBe toString(compactJson)
          bbuf.limit() shouldBe buf.length - offset
          bbuf.arrayOffset() shouldBe offset
        }

      check(writeToByteBuffer(_, _, _)(codec))
      check(writeToByteBufferReentrant(_, _, _)(codec))
      check(writeToByteBuffer(_, _, _)(reentrantCodec))
      check(writeToByteBufferReentrant(_, _, _)(reentrantCodec))
    }
    "throw BufferOverflowException in case of the provided byte buffer is overflown during serialization" in {
      def check(f: (User, ByteBuffer) => Unit): Unit = {
        val bbuf1 = ByteBuffer.allocateDirect(150)
        bbuf1.position(100)
        intercept[BufferOverflowException](f(user, bbuf1))
        bbuf1.position() shouldBe {
          if (TestUtils.isNative) 142
          else 100
        }
        val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
        bbuf2.position(100)
        intercept[BufferOverflowException](f(user, bbuf2))
        bbuf2.position() shouldBe 142
      }

      check(writeToByteBuffer(_, _)(codec))
      check(writeToByteBufferReentrant(_, _)(codec))
    }
    "throw ReadOnlyBufferException in case of the provided byte buffer is read-only" in {
      def check(f: (User, ByteBuffer) => Unit): Unit = {
        val bbuf1 = ByteBuffer.allocateDirect(150).asReadOnlyBuffer()
        bbuf1.position(100)
        intercept[ReadOnlyBufferException](f(user, bbuf1))
        bbuf1.position() shouldBe 100
        val bbuf2 = ByteBuffer.wrap(new Array[Byte](150)).asReadOnlyBuffer()
        bbuf2.position(100)
        intercept[ReadOnlyBufferException](f(user, bbuf2))
        bbuf2.position() shouldBe 100
      }

      check(writeToByteBuffer(_, _)(codec))
      check(writeToByteBufferReentrant(_, _)(codec))
    }
    "throw NullPointerException in case of the provided params are null" in {
      val bbuf1 = ByteBuffer.allocateDirect(150)
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
      intercept[NullPointerException](writeToByteBuffer(user, null)(codec))
      intercept[NullPointerException](writeToByteBufferReentrant(user, null)(codec))
      intercept[NullPointerException](writeToByteBuffer[User](null, bbuf1)(codec))
      intercept[NullPointerException](writeToByteBufferReentrant[User](null, bbuf1)(codec))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf1)(null))
      intercept[NullPointerException](writeToByteBufferReentrant(user, bbuf1)(null))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf1, null)(codec))
      intercept[NullPointerException](writeToByteBufferReentrant(user, bbuf1, null)(codec))
      intercept[NullPointerException](writeToByteBuffer[User](null, bbuf2)(codec))
      intercept[NullPointerException](writeToByteBufferReentrant[User](null, bbuf2)(codec))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf2)(null))
      intercept[NullPointerException](writeToByteBufferReentrant(user, bbuf2)(null))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf2, null)(codec))
      intercept[NullPointerException](writeToByteBufferReentrant(user, bbuf2, null)(codec))
    }
  }
  "writeToString and writeToStringReentrant" should {
    "serialize an object to a string" in {
      writeToString(user)(codec) shouldBe toString(compactJson)
      writeToStringReentrant(user)(codec) shouldBe toString(compactJson)
      writeToString(user)(reentrantCodec) shouldBe toString(compactJson)
      writeToStringReentrant(user)(reentrantCodec) shouldBe toString(compactJson)
    }
    "throw NullPointerException in case of the provided params are null" in {
      intercept[NullPointerException](writeToString[User](null)(codec))
      intercept[NullPointerException](writeToStringReentrant[User](null)(codec))
      intercept[NullPointerException](writeToString(user)(null))
      intercept[NullPointerException](writeToStringReentrant(user)(null))
      intercept[NullPointerException](writeToString(user, null.asInstanceOf[WriterConfig])(codec))
      intercept[NullPointerException](writeToStringReentrant(user, null.asInstanceOf[WriterConfig])(codec))
    }
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}
