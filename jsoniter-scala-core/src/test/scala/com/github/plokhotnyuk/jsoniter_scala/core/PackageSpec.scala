package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.{BufferOverflowException, ByteBuffer}
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.PropertyChecks

class PackageSpec extends WordSpec with Matchers with PropertyChecks {
  "readFromStream" should {
    "parse JSON from the provided input stream" in {
      readFromStream(getClass.getResourceAsStream("user_api_response.json"))(codec) shouldBe user
    }
    "throw an exception if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonParseException](readFromStream(new ByteArrayInputStream(httpMessage))(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw an exception in case of the provided params are null" in {
      intercept[NullPointerException](readFromStream(new ByteArrayInputStream(compactJson))(null))
      intercept[NullPointerException](readFromStream(null)(codec))
    }
  }
  "readFromArray" should {
    "parse JSON from the byte array" in {
      readFromArray(compactJson)(codec) shouldBe user
    }
    "throw an exception if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonParseException](readFromArray(httpMessage)(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw an exception in case of the provided params are null" in {
      intercept[NullPointerException](readFromArray(compactJson)(null))
      intercept[NullPointerException](readFromArray(null.asInstanceOf[Array[Byte]])(codec))
    }
  }
  "readFromSubArray" should {
    "parse JSON from the byte array within specified positions" in {
      readFromSubArray(httpMessage, 66, httpMessage.length)(codec) shouldBe user
    }
    "throw an exception if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonParseException](readFromSubArray(httpMessage, 0, httpMessage.length)(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw an exception in case of the provided params are invalid or null" in {
      intercept[NullPointerException](readFromSubArray(httpMessage, 66, httpMessage.length)(null))
      intercept[NullPointerException](readFromSubArray(null.asInstanceOf[Array[Byte]], 0, 50)(codec))
      intercept[NullPointerException](readFromSubArray(httpMessage, 66, httpMessage.length, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArray(httpMessage, 50, 200)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](readFromSubArray(httpMessage, 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
    }
  }
  "readFromByteBuffer" should {
    "parse JSON from the current position of the provided direct byte buffer" in {
      val bbuf = ByteBuffer.allocateDirect(httpMessage.length)
      bbuf.put(httpMessage)
      bbuf.position(66)
      bbuf.limit(httpMessage.length)
      readFromByteBuffer(bbuf)(codec) shouldBe user
      bbuf.position shouldBe 151
    }
    "parse JSON from the current position of the provided array based byte buffer" in {
      val bbuf = ByteBuffer.wrap(httpMessage)
      bbuf.position(66)
      bbuf.limit(httpMessage.length)
      readFromByteBuffer(bbuf)(codec) shouldBe user
      bbuf.position shouldBe 151
    }
    "throw an exception if cannot parse input with message containing input offset & hex dump of affected part of the direct byte buffer" in {
      val bbuf = ByteBuffer.allocateDirect(httpMessage.length)
      bbuf.put(httpMessage)
      bbuf.position(10)
      assert(intercept[JsonParseException](readFromByteBuffer(bbuf)(codec)).getMessage ==
        """expected '{', offset: 0x00000000, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 30 30 20 4f 4b 0a 43 6f 6e 74 65 6e 74 2d 54 79 | 00 OK.Content-Ty |
          || 00000010 | 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f | pe: application/ |
          || 00000020 | 6a 73 6f 6e 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e | json.Content-Len |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
      bbuf.position shouldBe 11
    }
    "throw an exception if cannot parse input with message containing input offset & hex dump of affected part the array based byte buffer" in {
      val bbuf = ByteBuffer.wrap(httpMessage)
      bbuf.position(10)
      assert(intercept[JsonParseException](readFromByteBuffer(bbuf)(codec)).getMessage ==
        """expected '{', offset: 0x0000000a, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
      bbuf.position shouldBe 11
    }
    "throw an exception in case of the provided params are null" in {
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
      scanJsonValuesFromStream(inputStream) { (u: User) =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq(user, user1, user2)
    }
    "scanning of JSON values can be interrupted by returning `false` from the consumer" in {
      var users: Seq[User] = Seq.empty
      scanJsonValuesFromStream(inputStream) { (u: User) =>
        users = users :+ u
        false
      }(codec)
      users shouldBe Seq(user)
    }
    "throw an exception in case of the provided params are null" in {
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
      scanJsonArrayFromStream(inputStream) { (u: User) =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq(user, user1, user2)
    }
    "scanning of JSON array values can be interrupted by returning `false` from the consumer" in {
      var users: Seq[User] = Seq.empty
      scanJsonArrayFromStream(inputStream) { (u: User) =>
        users = users :+ u
        false
      }(codec)
      users shouldBe Seq(user)
    }
    "scan null value from the provided input stream" in {
      var users: Seq[User] = Seq.empty
      scanJsonArrayFromStream(new ByteArrayInputStream("null".getBytes("UTF-8"))) { (u: User) =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq()
    }
    "throw a parse exception in case of JSON array is not closed properly" in {
      assert(intercept[JsonParseException] {
        scanJsonArrayFromStream(new ByteArrayInputStream("""[{"name":"x"}y""".getBytes("UTF-8"))) { (_: User) =>
          true
        }(codec)
      }.getMessage.contains("expected ']' or ',', offset: 0x0000000d"))
    }
    "throw a parse exception in case of input isn't JSON array or null" in {
      assert(intercept[JsonParseException] {
        scanJsonArrayFromStream(new ByteArrayInputStream("""{}""".getBytes("UTF-8"))) { (_: User) =>
          true
        }(codec)
      }.getMessage.contains("expected '[' or null, offset: 0x00000000"))
    }
    "throw an exception in case of the provided params are null" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanJsonArrayFromStream(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStream(inputStream, null)(skip)(codec))
      intercept[NullPointerException](scanJsonArrayFromStream(inputStream)(npe)(codec))
    }
  }
  "writeToStream" should {
    "serialize an object to the provided output stream" in {
      val out1 = new ByteArrayOutputStream()
      writeToStream(user, out1)(codec)
      out1.toString("UTF-8") shouldBe toString(compactJson)
      val out2 = new ByteArrayOutputStream()
      writeToStream(user, out2, WriterConfig(indentionStep = 2))(codec)
      out2.toString("UTF-8") shouldBe toString(prettyJson)
    }
    "throw i/o exception in case of the provided params are null" in {
      intercept[NullPointerException](writeToStream(user, new ByteArrayOutputStream())(null))
      intercept[NullPointerException](writeToStream(user, null.asInstanceOf[OutputStream])(codec))
      intercept[NullPointerException](writeToStream(user, new ByteArrayOutputStream(), null)(codec))
    }
  }
  "writeToArray" should {
    "serialize an object to a new instance of byte array" in {
      toString(writeToArray(user)(codec)) shouldBe toString(compactJson)
      toString(writeToArray(user, WriterConfig(indentionStep = 2))(codec)) shouldBe toString(prettyJson)
    }
    "throw i/o exception in case of the provided params are null" in {
      intercept[NullPointerException](writeToArray(user)(null))
      intercept[NullPointerException](writeToArray(user, null.asInstanceOf[WriterConfig])(codec))
    }
  }
  "writeToPreallocatedArray" should {
    val buf = new Array[Byte](150)

    "serialize an object to the provided byte array from specified position" in {
      val from1 = 10
      val to1 = writeToPreallocatedArray(user, buf, from1)(codec)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      val from2 = 0
      val to2 = writeToPreallocatedArray(user, buf, from2, WriterConfig(indentionStep = 2))(codec)
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
    }
    "throw array index out of bounds exception in case of the provided byte array is overflown during serialization" in {
      assert(intercept[ArrayIndexOutOfBoundsException](writeToPreallocatedArray(user, buf, 100)(codec))
        .getMessage.contains("`buf` length exceeded"))
    }
    "throw i/o exception in case of the provided params are invalid or null" in {
      intercept[NullPointerException](writeToPreallocatedArray(user, buf, 0)(null))
      intercept[NullPointerException](writeToPreallocatedArray(user, null, 50)(codec))
      intercept[NullPointerException](writeToPreallocatedArray(user, buf, 0, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](writeToPreallocatedArray(user, new Array[Byte](10), 50)(codec))
        .getMessage.contains("`from` should be positive and not greater than `buf` length"))
    }
  }
  "writeToByteBuffer" should {
    val buf = new Array[Byte](150)

    "serialize an object to the provided direct byte buffer from the current position" in {
      val bbuf = ByteBuffer.allocateDirect(150)
      val from1 = 10
      bbuf.position(from1)
      writeToByteBuffer(user, bbuf)(codec)
      val to1 = bbuf.limit
      bbuf.position(from1)
      bbuf.get(buf, from1, to1 - from1)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      val from2 = 0
      bbuf.position(from2)
      bbuf.limit(150)
      writeToByteBuffer(user, bbuf, WriterConfig(indentionStep = 2))(codec)
      val to2 = bbuf.limit
      bbuf.position(from2)
      bbuf.get(buf, from2, to2 - from2)
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
    }
    "serialize an object to the provided array-based byte buffer from the current position" in {
      val bbuf = ByteBuffer.wrap(buf)
      val from1 = 10
      bbuf.position(from1)
      writeToByteBuffer(user, bbuf)(codec)
      val to1 = bbuf.limit
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      val from2 = 0
      bbuf.position(from2)
      writeToByteBuffer(user, bbuf, WriterConfig(indentionStep = 2))(codec)
      val to2 = bbuf.limit
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
    }
    "throw an exception in case of the provided byte buffer is overflown during serialization" in {
      val bbuf1 = ByteBuffer.allocateDirect(150)
      bbuf1.position(100)
      intercept[BufferOverflowException](writeToByteBuffer(user, bbuf1)(codec))
      bbuf1.position shouldBe 100
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
      bbuf2.position(100)
      intercept[BufferOverflowException](writeToByteBuffer(user, bbuf2)(codec))
      bbuf2.position shouldBe 100
    }
    "throw i/o exception in case of the provided params are invalid or null" in {
      intercept[NullPointerException](writeToByteBuffer(user, null)(codec))
      val bbuf1 = ByteBuffer.allocateDirect(150)
      intercept[NullPointerException](writeToByteBuffer(user, bbuf1)(null))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf1, null)(codec))
      val bbuf2 = ByteBuffer.wrap(new Array[Byte](150))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf2)(null))
      intercept[NullPointerException](writeToByteBuffer(user, bbuf2, null)(codec))
    }
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}
