package io.circe

import java.nio.charset.StandardCharsets

object StringUtil {
  def toString[A](buf: Array[Byte], len: Int): String =
    new String(buf, 1, len - 2, StandardCharsets.UTF_8)
}