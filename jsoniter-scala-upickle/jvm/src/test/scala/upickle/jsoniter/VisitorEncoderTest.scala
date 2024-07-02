package upickle.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.writeToString
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import ujson.{Arr, Null, Obj}

import scala.language.implicitConversions

class VisitorEncoderTest {
  val arr: Arr =
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
    val jsonStr = """[0, 1, "bloop", 3, null, 5.5, ["a", "b", "c"], {"foo7": "bar", "arr": [null, null]}]"""

    val res = writeToString(arr)(ValueEncoder)
    JSONAssert.assertEquals(jsonStr, res, true)
  }
}
