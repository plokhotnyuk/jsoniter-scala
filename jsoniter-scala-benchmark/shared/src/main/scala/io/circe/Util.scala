package io.circe

import io.circe.Json._
import java.util

object Util {
  // Based on the work of Rafi Baker: https://gitter.im/circe/circe?at=60c27ac04fc7ad136ac3831c
  private[this] val dropEmptyValueFolder: Json.Folder[Json] = new Json.Folder[Json] {
    val onNull: Json = Null

    def onBoolean(value: Boolean): Json =
      if (value) True
      else False

    def onNumber(value: JsonNumber): Json = new JNumber(value)

    def onString(value: String): Json = new JString(value)

    def onArray(value: Vector[Json]): Json = {
      val builder = Vector.newBuilder[Json]
      value.foreach(v => if (!v.isNull) builder.addOne(v.foldWith(this)))
      val vec = builder.result()
      if (vec.isEmpty) Null
      else new JArray(vec)
    }

    def onObject(value: JsonObject): Json = new JObject(JsonObject.fromLinkedHashMap {
      val map = new util.LinkedHashMap[String, Json](value.size << 1, 0.5f)
      value.toIterable.foreach { kv =>
        val folded = kv._2.foldWith(this)
        if (!folded.isNull) map.put(kv._1, folded)
      }
      map
    })
  }

  def deepDropEmptyValues(json: Json): Json = json.foldWith(dropEmptyValueFolder)

  def toJObject(fields: (String, Json)*): Json = new JObject(JsonObject.fromLinkedHashMap {
    val map = new util.LinkedHashMap[String, Json](fields.size << 1, 0.5f)
    val it = fields.iterator
    while (it.hasNext) {
      val kv = it.next()
      val v = kv._2
      if (!(v.isNull || v.isArray && v.asInstanceOf[JArray].value.isEmpty)) map.put(kv._1, v)
    }
    map
  })
}
