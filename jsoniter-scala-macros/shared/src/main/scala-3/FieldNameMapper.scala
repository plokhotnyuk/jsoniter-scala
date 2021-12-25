package com.github.plokhotnyuk.jsoniter_scala.macros


import scala.quoted._
import scala.util._

sealed trait FieldNameMapper:
    def apply(input: String)(using Quotes): Option[String]

class FieldNameFunctionWrapper(fun: PartialFunction[String, String]) extends FieldNameMapper:
    def apply(input:String)(using Quotes): Option[String] = fun.lift(input)

class FieldNameExprFunctionWrapper(fun: Expr[PartialFunction[String,String]]) extends FieldNameMapper:
    def apply(input:String)(using Quotes): Option[String] = 
      CompileTimeEval.evalApplyString(fun, input)


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
        //x match {
        //  case '{ FieldNameExprFunctionWrapper($fun) } =>
        //}
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

class CompileTimeEvalException(message: String, 
                               expr: Expr[Any],
                               reason: Exception = null
                               ) extends RuntimeException(message)

object CompileTimeEval {

  def evalApplyString(fun: Expr[PartialFunction[String,String]], input:String)(using Quotes): Option[String] =
    import quotes.reflect._
    if (fun.asTerm.tpe <:< TypeRepr.of[Map[String,String]]) {
      evalApplyStringMap(fun.asExprOf[Map[String,String]], input)
    } else {
      evalApplyStringTerm(fun.asTerm, input) 
    }
 

  def evalApplyStringMap(m: Expr[Map[String,String]], input: String)(using Quotes): Option[String] =
    import quotes.reflect._
    m match
      case '{  Map(${Varargs(args)} )} =>
        for(a <- args) {
          if (a.asTerm.tpe <:<  TypeRepr.of[(String,String)]) {
            val kv = a.asExprOf[(String,String)]
            summon[FromExpr[(String,String)]].unapply(kv) match
              case Some((k,v)) =>
                if (k == input) {
                  return Some(v)
                } 
              case None =>
                throw  CompileTimeEvalException(s"Can't eval ${a.show} at compile time",a)
          } else {
            throw CompileTimeEvalException(s"Can't case ${a.show} to (String,String)",a)
          }
        }
        None
      case _ =>
        throw CompileTimeEvalException(s"map ${m.show} should be a constrictoor literal, we have ${m.asTerm}", m)

  
  def evalApplyStringTerm(using Quotes)(ft: quotes.reflect.Term, input:String,
                                bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term] = Map.empty[quotes.reflect.Symbol, quotes.reflect.Term]): Option[String] =
      import quotes.reflect._
      ft match
        case Inlined(oring, inlineBindings, body) => evalApplyStringTerm(body, input, addBindings(ft, bindings, inlineBindings))
        case _ =>
          val termResult = ft match
            case Lambda(params, body) => 
              params match
                case List(param) => 
                  val nullTerm = Literal(NullConstant())
                  evalTerm(body,bindings.updated(param.symbol, Literal(StringConstant(input))),Some(nullTerm))
                case _ => 
                  throw CompileTimeEvalException(s"Expected that partial function have one parameter ${ft.show}", ft.asExpr)
            case other =>
              val applyTerm = Apply(other,List(Literal(StringConstant(input))))
              evalApply(applyTerm, bindings)
          termToOptString(termResult)
      

  def evalTerm(using Quotes)(ft: quotes.reflect.Term, 
                             bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term], 
                             optDefault: Option[quotes.reflect.Term]): quotes.reflect.Term = {
    import quotes.reflect._
    ft match {
        case Inlined(origin, inlineBindings, body) =>
          evalTerm(body, addBindings(ft,bindings,inlineBindings), optDefault)
        case id@Ident(_) =>
          bindings.get(id.symbol) match
            case Some(term) => evalTerm(term, bindings, optDefault)
            case None => throw CompileTimeEvalException(s"Unknown symbol: $id", id.asExpr)
        case m@Match(scrutinee, caseDefs ) =>
            evalMatch(m, bindings, optDefault)
        case If(cond, ifTrue, ifFalse) =>
            if (evalCondition(cond, bindings)) {
              evalTerm(ifTrue, bindings, optDefault)
            } else {
              evalTerm(ifFalse, bindings, optDefault)
            }
        case app@Apply(fun, args) =>
          evalApply(app,bindings)
        case block@Block(statements, exprs) =>
          evalBlock(block, bindings, optDefault)
        case lt@Literal(_) => lt
        case other =>
          throw CompileTimeEvalException(s"Unsupported constant expression: $other", ft.asExpr)
    } 
  }

  def evalMatch(using Quotes)(t: quotes.reflect.Match, 
                              bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term], 
                              optDefault:Option[quotes.reflect.Term]): quotes.reflect.Term =
    import quotes.reflect._
    val scrutinee = evalTerm(t.scrutinee, bindings, None)
    var result: Option[Term] = None
    var cases = t.cases
    while(!cases.isEmpty && result.isEmpty) {
      val c = cases.head
      cases = cases.tail
      result = evalCaseDef(t, c, scrutinee, bindings, optDefault) 
    }
    result match
      case Some(value) => value
      case None => optDefault.getOrElse( throw CompileTimeEvalException("MatchFailed and no default", t.asExpr) )

  def evalCaseDef(using Quotes)(m: quotes.reflect.Term,
                                caseDef: quotes.reflect.CaseDef, 
                                scrutinee: quotes.reflect.Term, 
                                bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term],
                                optDefault: Option[quotes.reflect.Term]): Option[quotes.reflect.Term] =
    import quotes.reflect._
    evalCaseDefPattern(m, caseDef.pattern, scrutinee, bindings).flatMap{ newBinding =>
      caseDef.guard match
        case Some(guard) => 
          if (evalCondition(guard, newBinding)) {
            Some(evalTerm(caseDef.rhs, newBinding, optDefault))
          } else None
        case None =>
            Some(evalTerm(caseDef.rhs, newBinding, optDefault))
    }

  def evalCaseDefPattern(using Quotes)(m: quotes.reflect.Term, 
                                      pattern: quotes.reflect.Tree, 
                                      scrutinee: quotes.reflect.Term, 
                                      bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term]): 
                                                                     Option[Map[quotes.reflect.Symbol, quotes.reflect.Term]] =
    import quotes.reflect._
    pattern match
      case TypedOrTest(v, tpt) =>
         evalCaseDefPattern(m, v, scrutinee, bindings).flatMap{ newBinding =>
           if (scrutinee.tpe <:< tpt.tpe) {
             Some(newBinding)
           } else None
         }
      case b@Bind(name, tree) =>
        evalCaseDefPattern(m, tree, scrutinee, bindings).map{newBindings => 
           tree match
            case ct:Term =>
              newBindings.updated(b.symbol, ct)
            case _ =>
              newBindings.updated(b.symbol, scrutinee)
        }
      case Unapply(fun,implicits,patterns) =>
        // TODO: implement regexpr ?
        throw CompileTimeEvalException(s"Unapply ${fun} is not supported in compile-time pattern", m.asExpr)
      case Alternatives(cases) =>
        var c = cases
        var retval: Option[Map[quotes.reflect.Symbol,quotes.reflect.Term]] = None
        while(!c.isEmpty) {
          val h = c.head
          c = c.tail
          retval = evalCaseDefPattern(m, h, scrutinee, bindings)      
        }
        retval
      case Wildcard() =>
        Some(bindings)
      case lit@Literal(constant) => 
        if (evalLitEquals(lit,scrutinee,bindings)) {
          Some(bindings)
        } else None
      case other =>
        throw CompileTimeEvalException(s"pattern ${other} is not supported in compile-time evaluation", m.asExpr)   

  // TODO: add FromExpr implicit searxh     
  def evalApply(using Quotes)(applyTerm: quotes.reflect.Apply, 
                              bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term]): quotes.reflect.Term = {
    import quotes.reflect._
    val evaluatedArgs = applyTerm.args.map(x => evalTerm(x,bindings,None))
    applyTerm.fun match
      case Select(qual, memberName) =>
        val qualSym = qual.symbol
        if (qualSym.flags.is(Flags.Module)) {
          if (qualSym.fullName.equals("com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodeMaker")) {
            // TODO: test
            applyJsonCodeMakerField(applyTerm, memberName, evaluatedArgs)
          } else {
            // TODO: test
            applyJavaReflectedModuleField(applyTerm, qualSym, memberName, evaluatedArgs)
          }
        } else if (isPrimitiveOrString(qual)) {
          // TODO: test
          val evaluatedQual = evalTerm(qual,bindings,None)
          applyPrimitiveOrStringField(applyTerm, evaluatedQual, memberName, evaluatedArgs)
        } else {
          throw new CompileTimeEvalException(s"expeted that $qual is module or primitive", applyTerm.asExpr)
        }
      case other =>
        val funSym = other.symbol
        if (funSym.flags.is(Flags.Module)) {
            // TODO: test
            applyJavaReflectModule(applyTerm, funSym, evaluatedArgs)
        } else {
           throw new CompileTimeEvalException(s"expeted that $funSym is module", applyTerm.asExpr)
        }
  }

  def evalCondition(using Quotes)(term: quotes.reflect.Term, bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term]): Boolean =
    import quotes.reflect._
    evalTerm(term, bindings, None) match
      case Literal(BooleanConstant(v)) => v
      case other => throw CompileTimeEvalException(s"Condition should return boolean value, we have ${other.show}", term.asExpr)

  def evalBlock(using Quotes)(block: quotes.reflect.Block,
                              bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term], 
                              optDefault: Option[quotes.reflect.Term]): quotes.reflect.Term = {
    import quotes.reflect.*
    var statements = block.statements
    var b = bindings
    while(!statements.isEmpty) {
       val h = statements.head
       statements = statements.tail
       h match
        case imp: Import => // ignore
        case exp: Export => // ignore
        case dfn: Definition => 
          b = addDefinition(block,b,dfn)
        case bt: Term =>
          throw CompileTimeEvalException(s"Term as non-last block statement have no sence in compile-time evaluation",bt.asExpr)
    }
    evalTerm(block.expr, b, optDefault)
  }

  def evalLitEquals(using Quotes)(lhs: quotes.reflect.Literal, rhs: quotes.reflect.Term, 
                               bindings: Map[quotes.reflect.Symbol, quotes.reflect.Term] ): Boolean = {
    import quotes.reflect.*
    rhs match
      case Literal(rconst) => lhs.constant.value == rconst.value
      case _ => false
  }

  def addBindings(using Quotes)(m: quotes.reflect.Term, 
                                x: Map[quotes.reflect.Symbol, quotes.reflect.Term], 
                                bindings:List[quotes.reflect.Definition]): Map[quotes.reflect.Symbol, quotes.reflect.Term] = {
    import quotes.reflect._
    var r = x
    var c = bindings
    while(! c.isEmpty) {
      val h = c.head
      c = c.tail
      r = addDefinition(m, r, h)
    }
    r
  }

  def addDefinition(using Quotes)(m: quotes.reflect.Term, 
                                x: Map[quotes.reflect.Symbol, quotes.reflect.Term], 
                                binding: quotes.reflect.Definition): Map[quotes.reflect.Symbol, quotes.reflect.Term] = {
    import quotes.reflect._
    binding match
        case vd@ValDef(name, tpt, optRhs) =>
          optRhs match
            case Some(rhs) => x.updated(vd.symbol, rhs)
            case None => x
        case other => 
          throw CompileTimeEvalException(s"definitions other then ValDefs is not supported, we have $other ",m.asExpr)
  }

  def applyJsonCodeMakerField(using Quotes)(t: quotes.reflect.Term,
                                         fieldName: String, 
                                         args: List[quotes.reflect.Term] ): quotes.reflect.Term = {
    val arg = termToString(args.head)
    val jvmResult = fieldName match {
      case "partialIdentity" => JsonCodecMaker.partialIdentity(arg)
      case "enforceCamelCase" => JsonCodecMaker.enforceCamelCase(arg)
      case "EnforcePascalCase" => JsonCodecMaker.EnforcePascalCase(arg)
      case "enforce_snake_case" => JsonCodecMaker.enforce_snake_case(arg)
      case "enforce-kebab-case" => JsonCodecMaker.`enforce-kebab-case`(arg)
      case _ => throw CompileTimeEvalException(s"Unknonwn JsonCodeMaker parial function field: ${fieldName}",t.asExpr)
    }
    jvmToTerm(t, jvmResult)
  }

 
  def retrieveRuntimeModule(using Quotes)(applyTerm: quotes.reflect.Term,sym: quotes.reflect.Symbol): AnyRef =  {
    import quotes.reflect._
    val className = sym.fullName + "$"  // assume that java and scala encoding are same.
    val clazz = java.lang.Class.forName(className) 
    val moduleField = try {
      clazz.getField("MODULE$")
    }catch{
      case ex: Exception =>
        throw CompileTimeEvalException(s"Can't get ModuleField for lass ${className}", applyTerm.asExpr, ex)
    }
    val instance = moduleField.get(null)
    if (instance == null) {
      throw CompileTimeEvalException(s"module is null for lass ${className}", applyTerm.asExpr)
    }
    instance
  }  

  def applyJavaReflectedModuleField(using Quotes)(applyTerm: quotes.reflect.Term, 
                                                  qualSym: quotes.reflect.Symbol, 
                                                  memberName: String, 
                                                  args: List[quotes.reflect.Term]): quotes.reflect.Term = {
    val module = retrieveRuntimeModule(applyTerm, qualSym) 
    javaReflectionCall(applyTerm, module, memberName, args)
  }

  def applyJavaReflectModule(using Quotes)(applyTerm: quotes.reflect.Term , 
                                           funSym: quotes.reflect.Symbol, 
                                           args: List[quotes.reflect.Term]): quotes.reflect.Term = {
    applyJavaReflectedModuleField(applyTerm, funSym, "apply", args)
  }


  def applyPrimitiveOrStringField(using Quotes)(applyTerm: quotes.reflect.Term, 
                                                qual: quotes.reflect.Term,
                                                memberName: String, 
                                                args: List[quotes.reflect.Term]): quotes.reflect.Term = {
    val jvmQual = termToJvm(qual)
    javaReflectionCall(applyTerm, jvmQual, memberName, args) 
  }


  def javaReflectionCall(using Quotes)(term: quotes.reflect.Term,
                                       qual: AnyRef, 
                                       name: String, 
                                       args:List[quotes.reflect.Term]): quotes.reflect.Term = {
      import quotes.reflect._
      val preparedArgs = args.map(t => termToJvm(t)).toArray

      val qualClass = qual.getClass
    
      val argsTypes = new Array[Class[?]](preparedArgs.length)
      var i=0
      while(i < argsTypes.length) {
          argsTypes(i) = preparedArgs(i).getClass()
          i = i+1
      }
      val method = try {
          qualClass.getMethod(name, argsTypes: _*)
      } catch {
          case ex: NoSuchMethodException => 
            throw CompileTimeEvalException(s"Can't find methid $name of object ${qual} with argumentTypes: ${argsTypes.toList}", term.asExpr, ex)
          case ex: SecurityException =>
            throw CompileTimeEvalException(s"Can't get method $name of object ${qual}", term.asExpr, ex)
      }

      val result = try {
          method.invoke(qual, preparedArgs: _*)
      } catch {
          case ex: Exception =>
            throw CompileTimeEvalException(s"Can't invoke methid $name of object ${qual}", term.asExpr, ex)
      }

      jvmToTerm(term, result)

  }


  def jvmToTerm(using Quotes)(applyTerm: quotes.reflect.Term, obj: AnyRef): quotes.reflect.Term = {
    import quotes.reflect._
    if (obj.isInstanceOf[String]) {
      Literal(StringConstant(obj.asInstanceOf[String])) 
    } else if (obj.asInstanceOf[java.lang.Boolean]) {
      val v = obj.asInstanceOf[java.lang.Boolean].booleanValue()
      Literal(BooleanConstant(v))
    } else if (obj.isInstanceOf[java.lang.Character]) {
      val v = obj.asInstanceOf[java.lang.Character].charValue()
      Literal(CharConstant(v))
    } else if (obj.isInstanceOf[java.lang.Byte]) {
      val v = obj.asInstanceOf[java.lang.Byte].byteValue()
      Literal(ByteConstant(v))
    } else if (obj.isInstanceOf[java.lang.Short]) {
      val v = obj.asInstanceOf[java.lang.Short].shortValue()
      Literal(ShortConstant(v))
    } else if (obj.isInstanceOf[java.lang.Integer]) {
      val v = obj.asInstanceOf[java.lang.Integer].intValue()
      Literal(IntConstant(v))
    } else if (obj.isInstanceOf[java.lang.Long]) {
      val v = obj.asInstanceOf[java.lang.Long].intValue()
      Literal(LongConstant(v))
    } else if (obj.isInstanceOf[java.lang.Float]) {
      val v = obj.asInstanceOf[java.lang.Float].floatValue()
      Literal(FloatConstant(v))
    } else if (obj.isInstanceOf[java.lang.Double]) {
      val v = obj.asInstanceOf[java.lang.Double].floatValue()
      Literal(DoubleConstant(v))
    } else {
      throw CompileTimeEvalException(s"return value of an external function ($obj) is not primitiva or string", applyTerm.asExpr)
    }
  }

  def termToJvm(using Quotes)(x: quotes.reflect.Term): AnyRef = {
    import quotes.reflect._
    x.tpe match
      case Literal(StringConstant(v)) => v
      case Literal(BooleanConstant(v)) => java.lang.Boolean.valueOf(v)
      case Literal(CharConstant(v)) => java.lang.Character.valueOf(v)
      case Literal(ByteConstant(v)) => java.lang.Byte.valueOf(v)
      case Literal(ShortConstant(v)) => java.lang.Short.valueOf(v)
      case Literal(IntConstant(v)) => java.lang.Integer.valueOf(v)
      case Literal(LongConstant(v)) => java.lang.Long.valueOf(v)
      case Literal(FloatConstant(v)) => java.lang.Float.valueOf(v)
      case Literal(DoubleConstant(v)) => java.lang.Double.valueOf(v)
      case other =>
        throw CompileTimeEvalException(s"Can't interpret ${x} as primitive (type-${x.tpe.widen.show})",x.asExpr)
  }
    
  def termToOptString(using Quotes)(x:quotes.reflect.Term): Option[String] = {
    import quotes.reflect._
    x match
      case Literal(NullConstant()) => None
      case Literal(StringConstant(v)) => Some(v)
      case other =>
        throw CompileTimeEvalException(s"term should return string or null term we have ${x},", x.asExpr)
  }

  def termToString(using Quotes)(x:quotes.reflect.Term): String = {
    import quotes.reflect._
    x match
      case Literal(StringConstant(v)) => v
      case other =>
        throw CompileTimeEvalException(s"term should return string, we have ${x},", x.asExpr)
  }

  def isPrimitiveOrString(using Quotes)(term: quotes.reflect.Term): Boolean = {
    import  quotes.reflect._
    val tpe = term.tpe.widen
    tpe =:= TypeRepr.of[String] ||
    tpe =:= TypeRepr.of[Byte] ||
    tpe =:= TypeRepr.of[Short] ||
    tpe =:= TypeRepr.of[Int] ||
    tpe =:= TypeRepr.of[Long] ||
    tpe =:= TypeRepr.of[Float] ||
    tpe =:= TypeRepr.of[Double] ||
    tpe =:= TypeRepr.of[Char] ||
    tpe =:= TypeRepr.of[Boolean] ||
    tpe =:= TypeRepr.of[Unit]
  }
  

}

