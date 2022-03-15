package io.circe

private[circe] object StringUtil {
  def toString[A](buf: Array[Byte], len: Int): String = {
    val end = len - 1
    var s = ""
    var i = 1
    while (i < end) {
      s += buf(i).toChar.toString
      i += 1
    }
    s
  }
}