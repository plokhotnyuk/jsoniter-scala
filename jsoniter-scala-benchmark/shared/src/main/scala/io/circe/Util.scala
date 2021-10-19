package io.circe

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Json._
import java.util

object Util {
  // Based on the work of Rafi Baker: https://gitter.im/circe/circe?at=60c27ac04fc7ad136ac3831c
  private[this] val dropEmptyValueFolder: Json.Folder[Json] = new Json.Folder[Json] {
    def onNull: Json = Null

    def onBoolean(value: Boolean): Json =
      if (value) True
      else False

    def onNumber(value: JsonNumber): Json = new JNumber(value)

    def onString(value: String): Json = new JString(value)

    def onArray(value: Vector[Json]): Json = {
      val builder = Vector.newBuilder[Json]
      builder.sizeHint(value.size)
      value.foreach(v => if (!v.isNull) builder.addOne(v.foldWith(this)))
      val vec = builder.result()
      if (vec.isEmpty) Null
      else new JArray(vec)
    }

    def onObject(value: JsonObject): Json = new JObject(JsonObject.fromLinkedHashMap {
      val map = new util.LinkedHashMap[String, Json]
      value.toIterable.foreach { case (k, v) =>
        lazy val folded = v.foldWith(this)
        if (!{
          if (v.isArray) folded
          else v
        }.isNull) map.put(k, folded)
      }
      map
    })
  }

  def deepDropEmptyValues(json: Json): Json = json.foldWith(dropEmptyValueFolder)

  implicit val jsoniterScalaCodec: JsonValueCodec[Json] = new JsonValueCodec[Json] {
    override def decodeValue(in: JsonReader, default: Json): Json = {
      var b = in.nextToken()
      if (b == 'n') in.readNullOrError(default, "expected `null` value")
      else if (b == '"') new JString({
        in.rollbackToken()
        in.readString(null)
      }) else if (b == 'f' || b == 't') {
        in.rollbackToken()
        if (in.readBoolean()) Json.True
        else Json.False
      } else if (b >= '0' && b <= '9' || b == '-') new JNumber({
        in.rollbackToken()
        in.setMark()
        try {
          while ({
            b = in.nextByte()
            b >= '0' && b <= '9' || b == '-'
          }) ()
        } catch { case _: JsonReaderException => /* ignore the end of input error for now */} finally in.rollbackToMark()
        if (b == '.' || b == 'e' || b == 'E') new JsonBigDecimal(in.readBigDecimal(null).bigDecimal)
        else new JsonLong(in.readLong())
      }) else if (b == '[') new JArray({
        if (in.isNextToken(']')) Vector.empty
        else {
          in.rollbackToken()
          var x = new Array[Json](8)
          var i = 0
          while ({
            if (i == x.length) x = java.util.Arrays.copyOf(x, i << 1)
            x(i) = decodeValue(in, default)
            i += 1
            in.isNextToken(',')
          }) ()
          (if (in.isCurrentToken(']'))
            if (i == x.length) x
            else java.util.Arrays.copyOf(x, i)
          else in.arrayEndOrCommaError()).toVector
        }
      }) else if (b == '{') new JObject({
        if (in.isNextToken('}')) JsonObject.empty
        else {
          in.rollbackToken()
          val x = new util.LinkedHashMap[String, Json](8)
          while ({
            x.put(in.readKeyAsString(), decodeValue(in, default))
            in.isNextToken(',')
          }) ()
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          JsonObject.fromLinkedHashMap(x)
        }
      }) else in.decodeError("expected JSON value")
    }

    override def encodeValue(x: Json, out: JsonWriter): Unit = x match {
      case JNull => out.writeNull()
      case s: JString => out.writeVal(s.value)
      case b: JBoolean => out.writeVal(b.value)
      case n: JNumber => encodeJsonNumber(n.value, out)
      case a: JArray =>
        out.writeArrayStart()
        a.value.foreach(x => encodeValue(x, out))
        out.writeArrayEnd()
      case o: JObject =>
        out.writeObjectStart()
        o.value.toIterable.foreach { kv =>
          if (kv._2 ne Null) {
            out.writeKey(kv._1)
            encodeValue(kv._2, out)
          }
        }
        out.writeObjectEnd()
    }

    private[this] def encodeJsonNumber(x: JsonNumber, out: JsonWriter): Unit = x match {
      case l: JsonLong => out.writeVal(l.value)
      case f: JsonFloat => out.writeVal(f.value)
      case d: JsonDouble => out.writeVal(d.value)
      case bd: JsonBigDecimal => out.writeVal(bd.value)
      case _ => out.writeRawVal(x.toString.getBytes)
    }

    override val nullValue: Json = Json.Null
  }
}
