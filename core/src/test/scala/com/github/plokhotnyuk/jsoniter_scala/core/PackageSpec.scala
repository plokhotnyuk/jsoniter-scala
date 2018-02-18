package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.PropertyChecks

class PackageSpec extends WordSpec with Matchers with PropertyChecks {
  "read" should {
    "parse JSON from the provided input stream" in {
      read(getClass.getResourceAsStream("user_api_response.json"))(codec) shouldBe user
    }
    "parse JSON from the byte array" in {
      read(compactJson)(codec) shouldBe user
    }
    "parse JSON from the byte array within specified positions" in {
      read(httpMessage, 66, httpMessage.length)(codec) shouldBe user
    }
    "throw an exception if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonParseException](read(httpMessage)(codec)).getMessage ==
        """expected '{' or null, offset: 0x00000000, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw an exception in case of the provided params are invalid" in {
      intercept[NullPointerException](read(compactJson)(null))
      intercept[NullPointerException](read(new ByteArrayInputStream(compactJson))(null))
      intercept[NullPointerException](read(httpMessage, 66, httpMessage.length)(null))
      intercept[NullPointerException](read(null.asInstanceOf[Array[Byte]])(codec))
      intercept[NullPointerException](read(null.asInstanceOf[Array[Byte]], 0, 50)(codec))
      intercept[NullPointerException](read(null.asInstanceOf[InputStream])(codec))
      intercept[NullPointerException](read(new ByteArrayInputStream(compactJson), null)(codec))
      intercept[NullPointerException](read(httpMessage, 66, httpMessage.length, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](read(httpMessage, 50, 200)(codec))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](read(httpMessage, 50, 10)(codec))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
    }
  }
  "scanValueStream" should {
    def inputStream: InputStream = getClass.getResourceAsStream("user_api_value_stream.json")

    "scan JSON values from the provided input stream" in {
      var users: Seq[User] = Seq.empty
      scanValueStream(inputStream) { (u: User) =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq(user, user1, user2)
    }
    "scanning of JSON values can be interrupted by returning `false` from the consumer" in {
      var users: Seq[User] = Seq.empty
      scanValueStream(inputStream) { (u: User) =>
        users = users :+ u
        false
      }(codec)
      users shouldBe Seq(user)
    }
    "throw an exception in case of the provided params are invalid" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanValueStream(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanValueStream(inputStream, null)(skip)(codec))
      intercept[NullPointerException](scanValueStream(inputStream)(npe)(codec))
    }
  }
  "scanArray" should {
    def inputStream: InputStream = getClass.getResourceAsStream("user_api_array.json")

    "scan values of JSON array from the provided input stream" in {
      var users: Seq[User] = Seq.empty
      scanArray(inputStream) { (u: User) =>
        users = users :+ u
        true
      }(codec)
      users shouldBe Seq(user, user1, user2)
    }
    "scanning of JSON array values can be interrupted by returning `false` from the consumer" in {
      var users: Seq[User] = Seq.empty
      scanArray(inputStream) { (u: User) =>
        users = users :+ u
        false
      }(codec)
      users shouldBe Seq(user)
    }
    "throw an exception in case of the provided params are invalid" in {
      val skip = (_: User) => true
      val npe = null.asInstanceOf[User => Boolean]
      intercept[NullPointerException](scanArray(null.asInstanceOf[InputStream])(skip)(codec))
      intercept[NullPointerException](scanArray(inputStream, null)(skip)(codec))
      intercept[NullPointerException](scanArray(inputStream)(npe)(codec))
    }
  }
  "write" should {
    val buf = new Array[Byte](150)

    "serialize an object to the provided output stream" in {
      val out1 = new ByteArrayOutputStream()
      write(user, out1)(codec)
      out1.toString("UTF-8") shouldBe toString(compactJson)
      val out2 = new ByteArrayOutputStream()
      write(user, out2, WriterConfig(indentionStep = 2))(codec)
      out2.toString("UTF-8") shouldBe toString(prettyJson)
    }
    "serialize an object to a new instance of byte array" in {
      toString(write(user)(codec)) shouldBe toString(compactJson)
      toString(write(user, WriterConfig(indentionStep = 2))(codec)) shouldBe toString(prettyJson)
    }
    "serialize an object to the provided byte array from specified position" in {
      val from1 = 10
      val to1 = write(user, buf, from1)(codec)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      val from2 = 0
      val to2 = write(user, buf, from2, WriterConfig(indentionStep = 2))(codec)
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
    }
    "throw array index out of bounds exception in case of the provided byte array is overflown during serialization" in {
      assert(intercept[ArrayIndexOutOfBoundsException](write(user, buf, 100)(codec))
        .getMessage.contains("`buf` length exceeded"))
    }
    "throw i/o exception in case of the provided params are invalid" in {
      intercept[NullPointerException](write(user)(null))
      intercept[NullPointerException](write(user, new ByteArrayOutputStream())(null))
      intercept[NullPointerException](write(user, buf, 0)(null))
      intercept[NullPointerException](write(user, null.asInstanceOf[OutputStream])(codec))
      intercept[NullPointerException](write(user, null, 50)(codec))
      intercept[NullPointerException](write(user, null.asInstanceOf[WriterConfig])(codec))
      intercept[NullPointerException](write(user, new ByteArrayOutputStream(), null)(codec))
      intercept[NullPointerException](write(user, buf, 0, null)(codec))
      assert(intercept[ArrayIndexOutOfBoundsException](write(user, new Array[Byte](10), 50)(codec))
        .getMessage.contains("`from` should be positive and not greater than `buf` length"))
    }
  }

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}
