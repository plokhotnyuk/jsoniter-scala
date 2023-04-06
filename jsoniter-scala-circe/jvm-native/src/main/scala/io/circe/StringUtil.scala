package io.circe

private[circe] object StringUtil {
  def toString(buf: Array[Byte], len: Int): String = new String(buf, 0, 1, len - 2)
}