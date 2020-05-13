package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{PrintWriter, StringWriter}

import org.scalatest.Assertions

object AssertionUtils extends Assertions {
  def assertStackOverflow(f: => Unit): String = new StringWriter {
    intercept[StackOverflowError](f).printStackTrace(new PrintWriter(this))
  }.toString
}
