package io.circe

private[circe] object StringUtil {
  /**
   * Converts a byte array to a String.
   *
   * @param buf The input byte array.
   * @param len The length of the input byte array to convert.
   * @return A String containing the characters represented by the byte array.
   */  
  def toString(buf: Array[Byte], len: Int): String = new String(buf, 0, 1, len - 2)
}