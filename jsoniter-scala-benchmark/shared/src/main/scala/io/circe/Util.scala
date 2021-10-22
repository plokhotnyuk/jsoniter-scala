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
}
