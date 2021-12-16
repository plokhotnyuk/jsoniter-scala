package com.github.plokhotnyuk.jsoniter_scala.macros

import scala.quoted._
import scala.util._

sealed trait FieldNameMapper:
    def apply(input: String)(using Quotes): Either[(String, Expr[Any]),Option[String]]

class FieldNameFunctionWrapper(fun: PartialFunction[String, String]) extends FieldNameMapper:
    def apply(input:String)(using Quotes): Either[(String, Expr[Any]),Option[String]] = Right(fun.lift(input))

class FieldNameExprFunctionWrapper(fun: Expr[PartialFunction[String,String]]) extends FieldNameMapper:
    def apply(input:String)(using Quotes): Either[(String, Expr[Any]),Option[String]] = 
      CompileTimeEval.eval(fun, input)


object FieldNameMapper {


  inline given Conversion[PartialFunction[String,String], FieldNameMapper] = FieldNameFunctionWrapper(_)

  inline given Conversion[Function[String,String], FieldNameMapper] = 
    f => FieldNameFunctionWrapper( {case x => f(x) })


  given FromExpr[FieldNameMapper] with {

    def unapply(x: Expr[FieldNameMapper])(using Quotes): Option[FieldNameMapper] =
      import quotes.reflect._
      if (x.asTerm.tpe <:< TypeRepr.of[FieldNameFunctionWrapper] ) {
        FieldNameFunctionWrapper.toExprWrapper(x.asExprOf[FieldNameFunctionWrapper])
      } else if (x.asTerm.tpe <:< TypeRepr.of[FieldNameExprFunctionWrapper]) {
        throw new FromExprException("Double application of FromExpr ",x)
      } else {
        throw new FromExprException("FieldNameMapper", x)
      }
           
  }

}


object FieldNameFunctionWrapper {

  def toExprWrapper(x: Expr[FieldNameFunctionWrapper])(using Quotes): Option[FieldNameExprFunctionWrapper] =
    x match
      case '{ FieldNameFunctionWrapper($fun) } =>
        Some(FieldNameExprFunctionWrapper(fun))
      case _ =>
        throw new FromExprException("FieldNameExpr",x)

}


object CompileTimeEval {

  def eval(fun: Expr[PartialFunction[String,String]], input:String)(using Quotes): Either[(String,Expr[Any]),Option[String]] =
    import quotes.reflect._
    if (fun.asTerm.tpe <:< TypeRepr.of[Map[String,String]]) {
      evalMap(fun.asExprOf[Map[String,String]], input)
    } else {
      evalTerm(fun.asTerm, input) 
    }
 
  def evalMap(m: Expr[Map[String,String]], input: String)(using Quotes): Either[(String,Expr[Any]),Option[String]] =
    import quotes.reflect._
    m match
      case '{  Map(${Varargs(args)} )} =>
        for(a <- args) {
          if (a.asTerm.tpe <:<  TypeRepr.of[(String,String)]) {
            val kv = a.asExprOf[(String,String)]
            summon[FromExpr[(String,String)]].unapply(kv) match
              case Some((k,v)) =>
                if (k == input) {
                  return Right(Some(v))
                } 
              case None =>
                return Left((s"Can't eval ${a.show} at compile time",a))
          } else {
            return Left((s"Can't case ${a.show} to (String,String)",a))
          }
        }
        Right(None)
      case _ =>
        Left((s"map ${m.show} should be a constrictoor literal, we have ${m.asTerm}", m))

  def evalTerm(using Quotes)(ft: quotes.reflect.Term, input:String): Either[(String,Expr[Any]),Option[String]] =
    import quotes.reflect._
    ft match
      case Inlined(oring, bindings, body) => evalTerm(body, input)
      case Lambda(params, body) => ???
      case Select(term, string) => ???  

  
  
}