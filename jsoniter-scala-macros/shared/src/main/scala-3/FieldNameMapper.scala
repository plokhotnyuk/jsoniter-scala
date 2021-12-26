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
                               reason: Throwable = null
                               ) extends RuntimeException(message, reason)

object CompileTimeEval {

  def evalApplyString(fun: Expr[PartialFunction[String,String]], input:String)(using Quotes): Option[String] = {
    val bubble = new QuoteScope()
    bubble.evalApplyString(fun, input)
  }
    
  def evalExpr[T:Type](expr:Expr[T])(using Quotes):Expr[T] = {
    val bubble = new QuoteScope()
    bubble.evalExpr(expr)
  }

  //def evalTerm(using Quotes)(ft: quotes.reflect.Term, bindings: Map[Symbol, Term], optDefault: Option[Term]): Term = {
  //}


  class QuoteScope(using Quotes) {

    import quotes.reflect._

    def evalApplyString(fun: Expr[PartialFunction[String,String]], input:String): Option[String] =
      if (fun.asTerm.tpe <:< TypeRepr.of[Map[String,String]]) {
        evalApplyStringMap(fun.asExprOf[Map[String,String]], input)
      } else {
        evalApplyStringTerm(fun.asTerm, input) 
      }    
 
    def evalApplyStringMap(m: Expr[Map[String,String]], input: String): Option[String] =
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

    def evalExpr[T:Type](expr:Expr[T]):Expr[T] = {
      evalTerm(expr.asTerm, Map.empty, None).asExprOf[T]
    }
  
    private def evalApplyStringTerm(ft: Term, input:String,
                                bindings: Map[Symbol, Term] = Map.empty[Symbol, Term]): Option[String] =
      ft match
        case Inlined(oring, inlineBindings, body) => evalApplyStringTerm(body, input, addBindings(ft, bindings, inlineBindings))
        case _ =>
          val inputLiteral = Literal(StringConstant(input))
          val nullTerm = Literal(NullConstant())
          val termResult = ft match
            case Lambda(params, body) => 
              params match
                case List(param) => 
                  evalTerm(body,bindings.updated(param.symbol,inputLiteral),Some(nullTerm))
                case _ => 
                  throw CompileTimeEvalException(s"Expected that partial function have one parameter ${ft.show}", ft.asExpr)
            case other =>
              if (ft.tpe <:< TypeRepr.of[PartialFunction[_,_]]) {
                 val isDefinedTerm = try {
                   Apply(Select.unique(ft,"isDefinedAt"),List(inputLiteral))
                 }catch{
                   case ex: Throwable =>
                    throw CompileTimeEvalException(s"Can't create isDefinedAt call for ${ft}: ${ex.getMessage}",ft.asExpr, ex)
                 }
                 val applyTerm = try {
                   Apply(Select.unique(ft,"apply"),List(inputLiteral)) 
                 }catch{
                  case ex: Throwable =>
                    throw CompileTimeEvalException(s"Can't create apply call for ${ft}: ${ex.getMessage}",ft.asExpr, ex)
                 }
                 if (evalCondition(isDefinedTerm, bindings)) {
                   evalTerm(applyTerm, bindings, None)
                 } else {
                   nullTerm
                 } 
              } else if (ft.tpe <:< TypeRepr.of[Function[_,_]]) {
                val applyTerm = try {
                  Apply(Select.unique(ft,"apply"),List(inputLiteral))
                }catch{
                  case ex: Throwable =>
                    throw CompileTimeEvalException(s"Can't create apply call for ${ft}: ${ex.getMessage}",ft.asExpr, ex)
                }
                evalApply(applyTerm, bindings)
              } else {
                throw CompileTimeEvalException(s"PartialFunction[String,String] or Function[String,String] is required, we have ${ft.tpe.show}",ft.asExpr)
              }
          termToOptString(termResult)
      

    private def evalTerm(ft: quotes.reflect.Term, bindings: Map[Symbol, Term], 
                             optDefault: Option[Term]): Term = {
      import quotes.reflect._
      try{
        ft match {
          case Inlined(origin, inlineBindings, body) =>
            evalTerm(body, addBindings(ft,bindings,inlineBindings), optDefault)
          case id@Ident(_) =>
            bindings.get(id.symbol) match
              case Some(term) => evalTerm(term, bindings, optDefault)
              case None => throw CompileTimeEvalException(s"Unknown symbol: $id, bindigns=${bindings}", ft.asExpr)
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
          case Typed(expr,tpt) => evalTerm(expr,bindings, optDefault)
          case other =>
              throw CompileTimeEvalException(s"Unsupported constant expression: $other", ft.asExpr)
        }
      }catch{
        case ex:CompileTimeEvalException =>
          if (true) {
            // TODO: debug flag.
            println(s"in ${ft.show}")
          }
          throw ex
      } 
    }

    private def evalMatch(t: Match, 
                  bindings: Map[Symbol, Term], 
                  optDefault:Option[Term]): Term = {
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
    }

    private def evalCaseDef(m: Term,
                    caseDef: CaseDef, 
                    scrutinee: Term, 
                    bindings: Map[Symbol, Term],
                    optDefault: Option[Term]): Option[Term] =
      evalCaseDefPattern(m, caseDef.pattern, scrutinee, bindings).flatMap{ newBinding =>
        caseDef.guard match
          case Some(guard) => 
            if (evalCondition(guard, newBinding)) {
              Some(evalTerm(caseDef.rhs, newBinding, optDefault))
            } else None
          case None =>
            Some(evalTerm(caseDef.rhs, newBinding, optDefault))
      }

    private def evalCaseDefPattern(m: Term, 
                          pattern: Tree, 
                          scrutinee: Term, 
                          bindings: Map[Symbol, Term]):  Option[Map[Symbol, Term]] = {
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
          Some(bindings.updated(pattern.symbol,scrutinee))
        case lit@Literal(constant) => 
          if (evalLitEquals(lit,scrutinee,bindings)) {
            Some(bindings)
          } else None
        case other =>
          throw CompileTimeEvalException(s"pattern ${other} is not supported in compile-time evaluation", m.asExpr) 
    }  


    private def evalApply(applyTerm: Apply, 
                  bindings: Map[Symbol, Term]): Term = {
      val evaluatedArgs = applyTerm.args.map(x => evalTerm(x,bindings,None))
      val retval: Option[Term] = None
      applyTerm.fun match
        case Select(qual, memberName) =>
          if (memberName == "isDefinedAt" || memberName == "apply") {
            qual match
              case Apply(Select(frs, "andThen"),List(snd)) =>
                println("catched-and-then")
                ???
              case Select(nestQual, nestMember) =>
                val nestQualSym = nestQual.symbol
                if (nestQualSym.flags.is(Flags.Module)) {
                  if (nestQualSym.fullName == "com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker") {
                    applyJsonCodeMakerField(applyTerm, nestMember, memberName, evaluatedArgs)      
                  } else {
                    val module = retrieveRuntimeModule(applyTerm, nestQualSym)
                    val obj = retrieveRuntimeField(applyTerm, module, nestMember)
                    javaReflectionCall(applyTerm, obj, memberName, evaluatedArgs)  
                  }
                } else {
                  throw new CompileTimeEvalException(s"expeted that $nestQual is module", applyTerm.asExpr)
                }
              case _ =>
                // TODO: test  (Function instance passed as module)
                if (qual.symbol.flags.is(Flags.Module)) {
                  applyJavaReflectedModuleField(applyTerm, qual.symbol, memberName, evaluatedArgs)
                } else {
                  throw new CompileTimeEvalException(s"expected that $qual is module", applyTerm.asExpr)
                }
          } else {
            val qualSym = qual.symbol
            if (qualSym.flags.is(Flags.Module)) {
              if (qualSym.fullName.equals("com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker")) {
                // call of member fiedl without isDefined or Apply.
                //  in principle, this is impossible for correct terms, but prorgram 
                applyJsonCodeMakerField(applyTerm, memberName, "apply", evaluatedArgs)
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

    private def evalCondition(term: Term, bindings: Map[Symbol, Term]): Boolean =
      evalTerm(term, bindings, None) match
        case Literal(BooleanConstant(v)) => v
        case other => throw CompileTimeEvalException(s"Condition should return boolean value, we have ${other.show}", term.asExpr)

    private def evalBlock(block: Block,
                  bindings: Map[Symbol, Term], 
                  optDefault: Option[Term]): Term = {
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

    private def evalLitEquals(lhs: Literal, rhs: Term, 
                               bindings: Map[Symbol, Term] ): Boolean = {
      rhs match
        case Literal(rconst) => lhs.constant.value == rconst.value
        case _ => 
          evalTerm(rhs,bindings,None) match
            case Literal(rconst) => lhs.constant.value == rconst.value
            case other => false // throw evaluation exception ?
    }

    private def addBindings(m: Term, x: Map[Symbol, Term], 
                                bindings:List[Definition]): Map[Symbol, Term] = {
      var r = x
      var c = bindings
      while(! c.isEmpty) {
        val h = c.head
        c = c.tail
        r = addDefinition(m, r, h)
      }
      r
    }

    private def addDefinition(m: Term, x: Map[quotes.reflect.Symbol, quotes.reflect.Term], 
                                binding: quotes.reflect.Definition): Map[quotes.reflect.Symbol, quotes.reflect.Term] = {
      binding match
        case vd@ValDef(name, tpt, optRhs) =>
          optRhs match
            case Some(rhs) => x.updated(vd.symbol, rhs)
            case None => x
        case other => 
          throw CompileTimeEvalException(s"definitions other then ValDefs is not supported, we have $other ",m.asExpr)
    }

    private def applyJsonCodeMakerField(t: Term,
                                         fieldName: String, 
                                         operation: String,
                                         args: List[Term] ): Term = {
      val arg = termToString(args.head)
      val field: PartialFunction[String,String] = fieldName match {
        case "partialIdentity" => JsonCodecMaker.partialIdentity
        case "enforceCamelCase" => JsonCodecMaker.enforceCamelCase
        case "EnforcePascalCase" => JsonCodecMaker.EnforcePascalCase
        case "enforce_snake_case" => JsonCodecMaker.enforce_snake_case
        case "enforce-kebab-case" => JsonCodecMaker.`enforce-kebab-case`
        case _ => throw CompileTimeEvalException(s"Unknonwn JsonCodeMaker parial function field: ${fieldName}",t.asExpr)
      }
      operation match {
        case "isDefinedAt" => 
          Literal(BooleanConstant(field.isDefinedAt(arg)))
        case "apply" => 
          jvmToTerm(t, field.apply(arg))
        case _ =>
          throw CompileTimeEvalException(s"Expected that operation shpuld be isDefnedAt or Apply, we have: ${operation}",t.asExpr)
      }
    }

 
    private def retrieveRuntimeModule(applyTerm: Term, sym: Symbol): AnyRef =  {
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

    private def retrieveRuntimeField(applyTerm: Term, obj: AnyRef, name: String): AnyRef =  {
      // field is scala field, we search access method with the same name
      // TODO: error handling
      val method = obj.getClass.getMethod(name)
      method.invoke(obj) 
    }
  

    private def applyJavaReflectedModuleField(applyTerm: Term, 
                                      qualSym: Symbol, 
                                      memberName: String, 
                                      args: List[Term]): Term = {
      println(s"applyJavaReflectionModuleField: qualSym.fullName=${qualSym.fullName}")
      val module = retrieveRuntimeModule(applyTerm, qualSym) 
      javaReflectionCall(applyTerm, module, memberName, args)
    }

    private def applyJavaReflectModule(applyTerm: Term , 
                              funSym: Symbol, 
                              args: List[Term]): Term = {
      applyJavaReflectedModuleField(applyTerm, funSym, "apply", args)
    }


    private def applyPrimitiveOrStringField(applyTerm: Term, 
                                    qual: Term,
                                    memberName: String, 
                                    args: List[Term]): Term = {
      val jvmQual = termToJvm(qual)
      javaReflectionCall(applyTerm, jvmQual, memberName, args) 
    }

    
    private[QuoteScope] sealed trait ApplyFun
    private[QuoteScope] object ApplyFun {
      case class CallIsDefined(fun: ApplyFun, module: AnyRef) extends ApplyFun
      case class CallApply(fun: ApplyFun, module: AnyRef) extends ApplyFun
      case class Module(fullName: String) extends ApplyFun
      case class FieldOf(fun: ApplyFun, name: String) extends ApplyFun
      case class AndThen(frs: ApplyFun, snd: ApplyFun) extends ApplyFun
      case class ApplyLambda(lambda: Block) extends ApplyFun
    }
    

    private def evalApplyFun(posTerm: Term, fun: Term, value: Term, bindigns: Map[Symbol,Term]): ApplyFun =
     ???


    class JvmReflectionMethodCallException(msg: String, ex: Exception) extends RuntimeException(msg, ex) 

    sealed trait JvmReflectionMethodCall {
      def process(): AnyRef

      protected def retrieveArgTypes(args: Array[AnyRef]): Array[java.lang.Class[?]] = {
        val argsTypes = new Array[Class[?]](args.length)
        var i=0
        while(i < argsTypes.length) {
          argsTypes(i) = args(i).getClass()
          i = i+1
        }
        argsTypes
      }

    }

    case class DirectJvmReflectionMethodCall(obj: AnyRef, name: String, args: Array[AnyRef]) extends JvmReflectionMethodCall
    {

      def process(): AnyRef = {
        val argsTypes = retrieveArgTypes(args)
        val method = try {
          obj.getClass.getMethod(name, argsTypes: _*)
        } catch {
          case ex: NoSuchMethodException => 
            throw JvmReflectionMethodCallException(s"Can't find method $name of object ${obj} (class ${obj.getClass}) with argumentTypes: ${argsTypes.toList}", ex)
          case ex: SecurityException =>
            throw JvmReflectionMethodCallException(s"Can't get method $name of object ${obj} (class ${obj.getClass}", ex)
        }

        val result = try {
          method.invoke(obj, args: _*)
        } catch {
          case ex: Exception =>
            throw JvmReflectionMethodCallException(s"Can't invoke methid $name of object ${obj}", ex)
        }
        result
      }
    }

    sealed trait PrependedArgumentJvmReflectionMethodCall extends JvmReflectionMethodCall {

     def prependArgument(obj: AnyRef, args: Array[AnyRef]): Array[AnyRef] = {
       val retval = new Array[AnyRef](args.length + 1)
       retval(0) = obj
       System.arraycopy(args,0,retval,1,args.length)
       retval
     }

    }  
  
    case class HelperObjectJvmReflectionMethodCall(helperObj: AnyRef, obj: AnyRef, name: String, args: Array[AnyRef]) extends PrependedArgumentJvmReflectionMethodCall {

      def process(): AnyRef = {
        val nArgs = prependArgument(obj, args)
        val argsTypes = retrieveArgTypes(nArgs)
        val method = try {
          helperObj.getClass().getMethod(name, argsTypes: _*)
        } catch {
          case ex: NoSuchMethodException => 
            throw JvmReflectionMethodCallException(s"Can't find method $name of object ${helperObj} (class ${helperObj.getClass}) with argumentTypes: ${argsTypes.toList}", ex)
          case ex: SecurityException =>
            throw JvmReflectionMethodCallException(s"Can't get method $name of object ${helperObj} (class ${helperObj.getClass}", ex)
        }

        val result = try {
          method.invoke(helperObj, nArgs: _*)
        } catch {
          case ex: Exception =>
            throw JvmReflectionMethodCallException(s"Can't invoke methid $name of object ${helperObj} (class ${helperObj.getClass})", ex)
        }

        result
      }

    }

    case class StringConcatJvmReflectionMethodCall(obj: String, args: Array[AnyRef]) extends JvmReflectionMethodCall {

      def process(): AnyRef = {
        var r = obj
        var i = 0
        while(i < args.length) {
          r = r.concat(args(i).toString)
          i = i + 1
        }
        r
      }

    }
  

    private def javaReflectionCall(term: Term,
                                       qual: AnyRef, 
                                       name: String, 
                                       args:List[Term]): Term = {
      val preparedArgs = args.map(t => termToJvm(t)).toArray
      val call = prepareJvmReflectionMethodCall(term, qual, name, preparedArgs)
      val result = try {
          call.process()
      } catch {
          case ex: JvmReflectionMethodCallException =>
            throw CompileTimeEvalException(ex.getMessage, term.asExpr, ex.getCause)
      }
      jvmToTerm(term, result)
    }


    private def  prepareJvmReflectionMethodCall(t: Term, x: AnyRef,  name: String, args:Array[AnyRef]): JvmReflectionMethodCall = {
      name match
        case "+" =>
          if (x.isInstanceOf[java.lang.String]) {
            StringConcatJvmReflectionMethodCall(x.asInstanceOf[java.lang.String], args)
          } else if (x.isInstanceOf[java.lang.Integer] || x.isInstanceOf[java.lang.Long]) {
            HelperObjectJvmReflectionMethodCall(x,x,"sum", args)
          } else {
            throw CompileTimeEvalException(s"Can't find substitute for opeation $name of object ${x} (class ${x.getClass})", t.asExpr)
          }
        case _ =>
          DirectJvmReflectionMethodCall(x,name, args)
    }

    private def jvmToTerm(applyTerm: Term, obj: AnyRef): Term = {
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

    private def termToJvm(x: Term): AnyRef = {
      x match
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
    
    private def termToOptString(x:Term): Option[String] = {
      x match
        case Literal(NullConstant()) => None
        case Literal(StringConstant(v)) => Some(v)
        case other =>
          throw CompileTimeEvalException(s"term should return string or null term we have ${x},", x.asExpr)
    }

    private def termToString(x:quotes.reflect.Term): String = {
      x match
        case Literal(StringConstant(v)) => v
        case other =>
          throw CompileTimeEvalException(s"term should return string, we have ${x},", x.asExpr)
    }

    private def isPrimitiveOrString(term: Term): Boolean = {
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
  
}

