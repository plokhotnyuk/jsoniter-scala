//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.37.6"

package com.github.plokhotnyuk.jsoniter_scala.core

object Main extends App {
  var r = 0x3333333333333333L
  var i = 1L
  while (i <= 10000000000L) {
    r += Math.multiplyHigh(r, i) // Replace by `NativeMath.multiplyHigh` for Scala Native
    i += 1L
  }
  println(r)
}
