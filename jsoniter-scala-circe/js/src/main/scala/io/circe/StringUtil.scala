package io.circe

private[circe] object StringUtil {
  def toString(buf: Array[Byte], len: Int): String = {
    val end = len - 1
    val s = new java.lang.StringBuilder(end)
    var i = 1
    while (i < end) {
      s.append(buf(i).toChar)
      i += 1
    }
    new String(s)
  }
}