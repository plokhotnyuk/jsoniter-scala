package com.github.plokhotnyuk.jsoniter_scala.macros

import org.scalatest.Assertions

object TestUtils extends Assertions {
  def assertStackOverflow(f: => Unit): String = "" // skip stackoverflow tests
}
