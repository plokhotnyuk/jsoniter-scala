package com.github.plokhotnyuk.jsoniter_scala.macros

import scala.quoted._

case class FromExprException(name: String, expr:Expr[Any]) extends RuntimeException