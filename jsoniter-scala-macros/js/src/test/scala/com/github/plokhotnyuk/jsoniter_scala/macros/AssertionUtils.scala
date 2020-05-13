package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{PrintWriter, StringWriter}

import org.scalatest.Assertions

object AssertionUtils extends Assertions {
  def assertStackOverflow(f: => Unit): String = {
    val stackTrace = new StringWriter {
      intercept[scala.scalajs.js.JavaScriptException](f).printStackTrace(new PrintWriter(this))
    }.toString
    assert(stackTrace.contains("stack size exceeded"))
    stackTrace
  }
}
