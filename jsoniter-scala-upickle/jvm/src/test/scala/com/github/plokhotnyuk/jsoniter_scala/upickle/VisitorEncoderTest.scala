package com.github.plokhotnyuk.jsoniter_scala.upickle

import com.github.plokhotnyuk.jsoniter_scala.core.writeToString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import ujson.{Arr, Null, Obj, Value}

import scala.language.implicitConversions

class VisitorEncoderTest {
  val arr: Value =
    Arr(0, 1, "bloop",
      3, Null, 5.5d,
      Arr("a", "b", "c"),
      Obj(
        "foo7" -> "bar",
        "arr" -> Arr(Null, Null)
      )
    )

  @Test
  def main(): Unit = {
    val jsonStr = """[0.0,1.0,"bloop",3.0,null,5.5,["a","b","c"],{"foo7":"bar","arr":[null, null]}]"""
    val res = writeToString(arr)(ValueEncoder)
    assertEquals(jsonStr, res)
  }
}
