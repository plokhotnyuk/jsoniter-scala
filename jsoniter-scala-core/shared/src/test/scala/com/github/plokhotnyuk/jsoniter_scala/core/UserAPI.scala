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
        var _id: Int = 0
        var _model: String = null
        var p0 = 0x3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var l = -1
          while (l < 0 || in.isNextToken(',')) {
            l = in.readKeyAsCharBuf()
            if (in.isCharBufEqualsTo(l, "id")) {
              if ((p0 & 0x1) != 0) p0 ^= 0x1
              else in.duplicatedKeyError(l)
              _id = in.readInt()
            } else if (in.isCharBufEqualsTo(l, "model")) {
              if ((p0 & 0x2) != 0) p0 ^= 0x2
              else in.duplicatedKeyError(l)
              _model = in.readString(_model)
            } else in.skip()
          }
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if ((p0 & 0x3) != 0) in.requiredFieldError(f1(Integer.numberOfTrailingZeros(p0)))
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
        var _name: String = null
        var _devices: Seq[Device] = Seq.empty[Device]
        var p0 = 0x3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var l = -1
          while (l < 0 || in.isNextToken(',')) {
            l = in.readKeyAsCharBuf()
            if (in.isCharBufEqualsTo(l, "name")) {
              if ((p0 & 0x1) != 0) p0 ^= 0x1
              else in.duplicatedKeyError(l)
              _name = in.readString(_name)
            } else if (in.isCharBufEqualsTo(l, "devices")) {
              if ((p0 & 0x2) != 0) p0 ^= 0x2
              else in.duplicatedKeyError(l)
              _devices = d1(in, _devices)
            } else in.skip()
          }
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if ((p0 & 0x1) != 0) in.requiredFieldError(f0(Integer.numberOfTrailingZeros(p0)))
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

    private[this] def f0(i: Int): String = (i: @switch) match {
      case 0 => "name"
    }

    private[this] def f1(i: Int): String = (i: @switch) match {
      case 0 => "id"
      case 1 => "model"
    }
  }
}
