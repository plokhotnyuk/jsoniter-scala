package io.circe

object StringUtil {
  def toString[A](buf: Array[Byte], len: Int): String =
    new String(buf, 0, 1, len - 2)
}