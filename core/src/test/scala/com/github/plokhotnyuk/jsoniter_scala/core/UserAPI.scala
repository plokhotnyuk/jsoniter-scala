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
    private val r0: Array[String] = Array("name")
    private val r1: Array[String] = Array("id", "model")

    def nullValue: User = null

    def decode(in: JsonReader, default: User): User = d0(in, default)

    def encode(x: User, out: JsonWriter): Unit = e0(x, out)

    private def d2(in: JsonReader, default: Device): Device = 
      if (in.isNextToken('{')) {
        var _id: Int = 0
        var _model: String = null
        var req0 = 3
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          do {
            val l = in.readKeyAsCharBuf()
            (in.charBufToHashCode(l): @switch) match {
              case 3355 => 
                if (in.isCharBufEqualsTo(l, "id")) {
                  _id = in.readInt()
                  req0 &= -2
                } else in.skip()
              case 104069929 =>
                if (in.isCharBufEqualsTo(l, "model")) {
                  _model = in.readString(_model)
                  req0 &= -3
                } else in.skip()
              case _ => in.skip()
            }
          } while (in.isNextToken(','))
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        if (req0 == 0) new Device(id = _id, model = _model)
        else in.requiredKeyError(r1, Array(req0))
      } else in.readNullOrTokenError(default, '{')

    private def d1(in: JsonReader, default: Seq[Device]): Seq[Device] = 
      if (in.isNextToken('[')) {
        if (in.isNextToken(']')) default
        else {
          in.rollbackToken()
          val x = Seq.newBuilder[Device]
          do x += d2(in, null)
          while (in.isNextToken(','))
          if (in.isCurrentToken(']')) x.result()
          else in.arrayEndError()
        }
      } else in.readNullOrTokenError(default, '[')

    private def d0(in: JsonReader, default: User): User = 
      if (in.isNextToken('{')) {
        var _name: String = null
        var _devices: Seq[Device] = Seq.empty[Device]
        var req0 = 1
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          do {
            val l = in.readKeyAsCharBuf()
            (in.charBufToHashCode(l): @switch) match {
              case 3373707 =>
                if (in.isCharBufEqualsTo(l, "name")) {
                  _name = in.readString(_name)
                  req0 &= -2
                } else in.skip()
              case 1559801053 =>
                if (in.isCharBufEqualsTo(l, "devices")) _devices = d1(in, _devices)
                else in.skip()
              case _ => in.skip()
            }
          } while (in.isNextToken(','))
          if (in.isCurrentToken('}').`unary_!`) in.objectEndOrCommaError()
        }
        if (req0 == 0) new User(name = _name, devices = _devices)
        else in.requiredKeyError(r0, Array(req0))
      } else in.readNullOrTokenError(default, '{')

    private def e2(x: Device, out: JsonWriter): Unit = 
      if (x ne null) {
        out.writeObjectStart()
        out.writeNonEscapedAsciiKey("id")
        out.writeVal(x.id)
        out.writeNonEscapedAsciiKey("model")
        out.writeVal(x.model)
        out.writeObjectEnd()
      } else out.writeNull()

    private def e1(x: Seq[Device], out: JsonWriter): Unit = {
      out.writeArrayStart()
      x.foreach { x =>
        out.writeComma()
        e2(x, out)
      }
      out.writeArrayEnd()
    }

    private def e0(x: User, out: JsonWriter): Unit = 
      if (x ne null) {
        out.writeObjectStart()
        out.writeNonEscapedAsciiKey("name")
        out.writeVal(x.name)
        val v = x.devices
        if ((v ne null) && !v.isEmpty) {
          out.writeNonEscapedAsciiKey("devices")
          e1(v, out)
        }
        out.writeObjectEnd()
      } else out.writeNull()
  }
}
