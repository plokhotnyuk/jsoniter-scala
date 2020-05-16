package com.github.plokhotnyuk.jsoniter_scala.benchmark

object BitMask {
  def toBitMask(vs: Array[Int], mvl: Int): Array[Long] = {
    val vsl = vs.length
    if (vsl == 0) new Array[Long](0)
    else {
      var mv = -1
      var i = 0
      while (i < vsl) {
        val v = vs(i)
        if (v >= mvl) throw new IllegalArgumentException(s"the bit set value: $v exceeds the limit: $mvl")
        if (v > mv) mv = v
        i += 1
      }
      val bs = new Array[Long]((mv >>> 6) + 1)
      i = 0
      while (i < vsl) {
        val v = vs(i)
        bs(v >>> 6) |= 1L << v
        i += 1
      }
      bs
    }
  }
}