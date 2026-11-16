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

import java.nio.charset.StandardCharsets.UTF_8
import scala.annotation.switch

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

object UserAPI {
  val user = User(name = "John", devices = Seq(Device(id = 1, model = "HTC One X"), Device(id = 2, model = "iPhone X")))
  val user1 = User(name = "Jon", devices = Seq(Device(id = 1, model = "HTC One X")))
  val user2 = User(name = "Joe", devices = Seq(Device(id = 2, model = "iPhone X")))
  val prettyJson: Array[Byte] =
    """{
      |  "name": "John",
      |  "devices": [
      |    {
      |      "id": 1,
      |      "model": "HTC One X"
      |    },
      |    {
      |      "id": 2,
      |      "model": "iPhone X"
      |    }
      |  ]
      |}""".stripMargin.getBytes(UTF_8)
  val compactJson: Array[Byte] =
    """{"name":"John","devices":[{"id":1,"model":"HTC One X"},{"id":2,"model":"iPhone X"}]}""".getBytes(UTF_8)
  val errorJson: Array[Byte] =
    """{
      |  "name": "John",
      |  "devices": [
      |    {
      |      "id": 1,
      |      "model": "HTC One X"
      |    },
      |    {
      |      "id": 2,
      |      "model": "iPhone X"
      |    }
      |  ]
      |}}}}}}}""".stripMargin.getBytes(UTF_8)
  val valueStreamJson: Array[Byte] =
    """{"name":"John","devices":[{"id":1,"model":"HTC One X"},{"id":2,"model":"iPhone X"}]}
      |{"name":"Jon","devices":[{"id":1,"model":"HTC One X"}]}
      |{"name":"Joe","devices":[{"id":2,"model":"iPhone X"}]}""".stripMargin.getBytes(UTF_8)
  val arrayJson: Array[Byte] =
    """[
      |  {
      |    "name": "John",
      |    "devices": [
      |      {
      |        "id": 1,
      |        "model": "HTC One X"
      |      },
      |      {
      |        "id": 2,
      |        "model": "iPhone X"
      |      }
      |    ]
      |  },
      |  {
      |    "name": "Jon",
      |    "devices": [
      |      {
      |        "id": 1,
      |        "model": "HTC One X"
      |      }
      |    ]
      |  },
      |  {
      |    "name": "Joe",
      |    "devices": [
      |      {
      |        "id": 2,
      |        "model": "iPhone X"
      |      }
      |    ]
      |  }
      |]""".stripMargin.getBytes(UTF_8)
  val arrayWithErrorJson: Array[Byte] =
    """[
      |  {
      |    "name": "John",
      |    "devices": [
      |      {
      |        "id": 1,
      |        "model": "HTC One X"
      |      },
      |      {
      |        "id": 2,
      |        "model": "iPhone X"
      |      }
      |    ]
      |  },
      |  {
      |    "name": "Jon",
      |    "devices": [
      |      {
      |        "id": 1,
      |        "model": "HTC One X"
      |      }
      |    ]
      |  },
      |  {
      |    "name": "Joe",
      |    "devices": [
      |      {
      |        "id": 2,
      |        "model": "iPhone X"
      |      }
      |    ]
      |  }
      |]]]]]]]""".stripMargin.getBytes(UTF_8)
  val httpMessage: Array[Byte] =
    """HTTP/1.0 200 OK
      |Content-Type: application/json
      |Content-Length: 55
      |
      |{"name":"John","devices":[{"id":1,"model":"HTC One X"},{"id":2,"model":"iPhone X"}]}""".stripMargin.getBytes(UTF_8)
  val codec: JsonValueCodec[User] = new JsonValueCodec[User] {
    val nullValue: User = null

    def decodeValue(in: JsonReader, default: User): User = d0(in, default)

    def encodeValue(x: User, out: JsonWriter): Unit = e0(x, out)

    private[this] def d2(in: JsonReader, default: Device): Device =
      if (in.isNextToken('{')) {
        val ns_ = ns1
        var _id: Int = 0
        var _model: String = null
        var p0 = 0x3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var i = -1
          while (i < 0 || in.isNextToken(',')) {
            i += 1
            if (i >= 2) i -= 2
            val l = in.readKeyAsCharBufOrNonEscapedAsciiMatch(ns_(i))
            val s = i
            if (l >= 0) {
              while ({
                i += 1
                if (i >= 2) i = 0
                !in.isCharBufEqualsTo(l, ns_(i)) && (i != s || { i = 2; false })
              }) ()
            }
            if (i < 2) {
              val m = 1 << i
              if ((p0 & m) == 0) in.duplicatedKeyError(ns_(i))
              p0 ^= m
            }
            (i: @switch) match {
              case 0 => _id = in.readInt()
              case 1 => _model = in.readString(_model)
              case _ =>
                in.skip()
                i = s + 1
            }
          }
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if (p0 != 0) in.requiredFieldError(ns_(Integer.numberOfTrailingZeros(p0)))
        new Device(id = _id, model = _model)
      } else in.readNullOrTokenError(default, '{')

    private[this] def d1(in: JsonReader, default: Seq[Device]): Seq[Device] =
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) default
        else {
          in.rollbackToken()
          val x = Seq.newBuilder[Device]
          while ({
            x += d2(in, null)
            in.isNextToken(',')
          }) ()
          if (in.isCurrentToken(']')) x.result()
          else in.arrayEndOrCommaError()
        }
      } else in.readNullOrTokenError(default, '[')

    private[this] def d0(in: JsonReader, default: User): User =
      if (in.isNextToken('{')) {
        val ns_ = ns0
        var _name: String = null
        var _devices: Seq[Device] = Seq.empty[Device]
        var p0 = 0x3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var i = -1
          while (i < 0 || in.isNextToken(',')) {
            i += 1
            if (i >= 2) i -= 2
            val l = in.readKeyAsCharBufOrNonEscapedAsciiMatch(ns_(i))
            val s = i
            if (l >= 0) {
              while ({
                i += 1
                if (i >= 2) i = 0
                !in.isCharBufEqualsTo(l, ns_(i)) && (i != s || { i = 2; false })
              }) ()
            }
            if (i < 2) {
              val m = 1 << i
              if ((p0 & m) == 0) in.duplicatedKeyError(ns_(i))
              p0 ^= m
            }
            (i: @switch) match {
              case 0 => _name = in.readString(_name)
              case 1 => _devices = d1(in, _devices)
              case _ =>
                in.skip()
                i = s + 1
            }
          }
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if ((p0 & 0x1) != 0) in.requiredFieldError(ns_(Integer.numberOfTrailingZeros(p0 & 0x1)))
        new User(name = _name, devices = _devices)
      } else in.readNullOrTokenError(default, '{')

    private[this] def e2(x: Device, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeNonEscapedAsciiKey("id")
      out.writeVal(x.id)
      out.writeNonEscapedAsciiKey("model")
      out.writeVal(x.model)
      out.writeObjectEnd()
    }

    private[this] def e1(x: Seq[Device], out: JsonWriter): Unit = {
      out.writeArrayStart()
      x.foreach { x =>
        e2(x, out)
      }
      out.writeArrayEnd()
    }

    private[this] def e0(x: User, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeNonEscapedAsciiKey("name")
      out.writeVal(x.name)
      val v = x.devices
      if (!v.isEmpty) {
        out.writeNonEscapedAsciiKey("devices")
        e1(v, out)
      }
      out.writeObjectEnd()
    }

    private[this] def nsi0(): Array[String] = Array("name", "devices")

    private[this] val ns0: Array[String] = nsi0()

    private[this] def nsi1(): Array[String] = Array("id", "model")

    private[this] val ns1: Array[String] = nsi1()
  }
  val reentrantCodec: JsonValueCodec[User] = new JsonValueCodec[User] {
    override def decodeValue(in: JsonReader, default: User): User =
      readFromArrayReentrant(in.readRawValAsBytes())(codec)

    override def encodeValue(x: User, out: JsonWriter): Unit =
      out.writeRawVal(writeToArrayReentrant(x)(codec))

    override def nullValue: User = null
  }
}
