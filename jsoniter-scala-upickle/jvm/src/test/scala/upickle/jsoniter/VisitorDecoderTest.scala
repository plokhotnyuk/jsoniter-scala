package upickle.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReaderException, JsonValueCodec, readFromString}
import com.github.plokhotnyuk.jsoniter_scala.upickle.JsoniterScalaCodec
import org.junit.jupiter.api.Assertions.{assertEquals, assertThrows, assertTrue}
import org.junit.jupiter.api.Test
import ujson.StringRenderer
import upickle.core.NoOpVisitor

import java.io.StringWriter

class VisitorDecoderTest {
  val strRender: JsonValueCodec[StringWriter] = JsoniterScalaCodec.visitorDecoder(visitor = StringRenderer())
  val noOp: JsonValueCodec[Unit] = JsoniterScalaCodec.visitorDecoder(visitor = NoOpVisitor)

  @Test
  def parse(): Unit = {
    val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
    readFromString(jsonStr)(noOp)
  }

  @Test
  def valid_double(): Unit = {
    val jsonStr = "1.7976931348623157e+308" // Double.MAX_VALUE
    val res: String = readFromString(jsonStr)(strRender).toString

    assertEquals(java.lang.Double.MAX_VALUE, java.lang.Double.valueOf(res))
  }

  @Test
  def big_int_as_str(): Unit = {
    val jsonStr = "9223372036854775808" // Long.MAX_VALUE + 1
    val res: String = readFromString(jsonStr)(strRender).toString

    assertEquals("\"9223372036854775808\"", res)
  }

  @Test
  def big_dec_as_str(): Unit = {
    val jsonStr = "1.7976931348623157e+310" // Double.MAX_VALUE
    val res: String = readFromString(jsonStr)(strRender).toString

    assertEquals("\"1.7976931348623157e+310\"", res)
  }

  @Test
  def invalid_json(): Unit = {
    val jsonStr = """{"n":null[]"""
    val ex = assertThrows(classOf[JsonReaderException], () => readFromString(jsonStr)(noOp))

    assertTrue(ex.getMessage.contains("expected '}' or ','"))
  }

  @Test
  def deep_json(): Unit = {
    val jsonStr1 = """[{"n":""" * 16 + "[]" + "}]" * 16
    val jsonStr2 = """{"n":[""" * 16 + "{}" + "]}" * 16
    val ex1 = assertThrows(classOf[JsonReaderException], () => readFromString(jsonStr1)(noOp))
    val ex2 = assertThrows(classOf[JsonReaderException], () => readFromString(jsonStr2)(noOp))

    assertTrue(ex1.getMessage.contains("depth limit exceeded"))
    assertTrue(ex2.getMessage.contains("depth limit exceeded"))
  }

  @Test
  def round_trip(): Unit = {
    val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
    val expected = """{"n":null,"s":"VVV","n1":1,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}""" // ujson writes 1.0 as 1 for some reason
    val res = readFromString(jsonStr)(strRender).toString

    assertEquals(expected, res)
  }
}
