package com.github.plokhotnyuk.jsoniter_scala.core

import scala.annotation.switch
import scala.reflect.io.Streamable

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

object UserAPI {
  val user = User(name = "John", devices = Seq(Device(id = 1, model = "HTC One X"), Device(id = 2, model = "iPhone X")))
  val user1 = User(name = "Jon", devices = Seq(Device(id = 1, model = "HTC One X")))
  val user2 = User(name = "Joe", devices = Seq(Device(id = 2, model = "iPhone X")))
  val prettyJson: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("user_api_response.json"))
  val compactJson: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("user_api_compact_response.json"))
  val httpMessage: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("user_api_http_response.txt"))
  val codec: JsonValueCodec[User] = new JsonValueCodec[User] {
    private[this] val r0: Array[String] = Array("name")
    private[this] val r1: Array[String] = Array("id", "model")
    val nullValue: User = null

    def decodeValue(in: JsonReader, default: User): User = d0(in, default)

    def encodeValue(x: User, out: JsonWriter): Unit = e0(x, out)

    private[this] def d2(in: JsonReader, default: Device): Device =
      if (in.isNextToken('{')) {
        var _id: Int = 0
        var _model: String = null
        var p0 = 3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          do {
            val l = in.readKeyAsCharBuf()
            (in.charBufToHashCode(l): @switch) match {
              case 3355 =>
                if (in.isCharBufEqualsTo(l, "id")) {
                  if ((p0 & 1) != 0) p0 ^= 1
                  else in.duplicatedKeyError(l)
                  _id = in.readInt()
                } else in.skip()
              case 104069929 =>
                if (in.isCharBufEqualsTo(l, "model")) {
                  if ((p0 & 2) != 0) p0 ^= 2
                  else in.duplicatedKeyError(l)
                  _model = in.readString(_model)
                } else in.skip()
              case _ =>
                in.skip()
            }
          } while (in.isNextToken(','))
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if ((p0 & 3) != 0) in.requiredFieldError(r1(Integer.numberOfTrailingZeros(p0)))
        new Device(id = _id, model = _model)
      } else in.readNullOrTokenError(default, '{')

    private[this] def d1(in: JsonReader, default: Seq[Device]): Seq[Device] =
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) default
        else {
          in.rollbackToken()
          val x = Seq.newBuilder[Device]
          do x += d2(in, null)
          while (in.isNextToken(','))
          if (in.isCurrentToken(']')) x.result()
          else in.arrayEndOrCommaError()
        }
      } else in.readNullOrTokenError(default, '[')

    private[this] def d0(in: JsonReader, default: User): User =
      if (in.isNextToken('{')) {
        var _name: String = null
        var _devices: Seq[Device] = Seq.empty[Device]
        var p0 = 3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          do {
            val l = in.readKeyAsCharBuf()
            (in.charBufToHashCode(l): @switch) match {
              case 3373707 =>
                if (in.isCharBufEqualsTo(l, "name")) {
                  if ((p0 & 1) != 0) p0 ^= 1
                  else in.duplicatedKeyError(l)
                  _name = in.readString(_name)
                } else in.skip()
              case 1559801053 =>
                if (in.isCharBufEqualsTo(l, "devices")) {
                  if ((p0 & 2) != 0) p0 ^= 2
                  else in.duplicatedKeyError(l)
                  _devices = d1(in, _devices)
                } else in.skip()
              case _ => in.skip()
            }
          } while (in.isNextToken(','))
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if ((p0 & 1) != 0) in.requiredFieldError(r0(Integer.numberOfTrailingZeros(p0)))
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
        out.writeComma()
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
  }
}
