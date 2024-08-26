package io.circe

private[circe] object StringUtil {
  @inline
  def toString(buf: Array[Byte], len: Int): String = {
    val end = len - 1
    val s = new java.lang.StringBuilder(end)
    var i = 1
    while (i < end) {
      s.append(buf(i).toChar)
      i += 1
    }
    s.toString // See https://github.com/scala-js/scala-js/blob/ef994c9c5ceb3f34a39781958e1b278b6dc21d17/javalib/src/main/scala/java/lang/_String.scala#L1025
  }
}