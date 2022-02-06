package com.github.plokhotnyuk.jsoniter_scala.macros

import scala.quoted._
import scala.util._

sealed trait NameMapper:
  def apply(input: String)(using Quotes): Option[String]

class PartialFunctionWrapper(fun: PartialFunction[String, String]) extends NameMapper:
  def apply(input: String)(using Quotes): Option[String] = fun.lift(input)

class ExprPartialFunctionWrapper(fun: Expr[PartialFunction[String, String]]) extends NameMapper:
  def apply(input: String)(using Quotes): Option[String] = CompileTimeEval.evalApplyString(fun, input)

case class FromExprException(name: String, expr: Expr[Any]) extends RuntimeException

object NameMapper {
  inline given Conversion[PartialFunction[String, String], NameMapper] = PartialFunctionWrapper(_)

  inline given Conversion[Function[String, String], NameMapper] = f => PartialFunctionWrapper( {case x => f(x) })

  given FromExpr[NameMapper] with {
    def unapply(x: Expr[NameMapper])(using Quotes): Option[NameMapper] = {
      import quotes.reflect._

      if (x.asTerm.tpe <:< TypeRepr.of[PartialFunctionWrapper]) {
        PartialFunctionWrapper.toExprWrapper(x.asExprOf[PartialFunctionWrapper])
      } else if (x.asTerm.tpe <:< TypeRepr.of[ExprPartialFunctionWrapper]) {
        throw new FromExprException("Double application of FromExpr ", x)
      } else throw new FromExprException("FieldNameMapper", x)
    }
  }
}

object PartialFunctionWrapper {
  def toExprWrapper(x: Expr[PartialFunctionWrapper])(using Quotes): Option[ExprPartialFunctionWrapper] =
    x match
      case '{ PartialFunctionWrapper($fun) } => Some(ExprPartialFunctionWrapper(fun))
      case _ => throw new FromExprException("FieldNameExpr", x)
}

private[macros] object CompileTimeEval {
  case class CompileTimeEvalException(message: String, expr: Expr[Any], reason: Throwable = null)
    extends RuntimeException(message, reason)

  def evalApplyString(fun: Expr[PartialFunction[String, String]], input: String)(using Quotes): Option[String] =
    new QuoteScope().evalApplyString(fun, input)

  def evalExpr[T: Type](expr: Expr[T])(using Quotes): Expr[T] = new QuoteScope().evalExpr(expr)

  class QuoteScope(using Quotes) {
    import quotes.reflect._

    def evalApplyString(fun: Expr[PartialFunction[String, String]], input: String): Option[String] =
      if (fun.asTerm.tpe <:< TypeRepr.of[Map[String, String]]) {
        evalApplyStringMap(fun.asExprOf[Map[String, String]], input)
      } else evalApplyStringTerm(fun.asTerm, input, Map.empty)

    def evalApplyStringMap(m: Expr[Map[String, String]], input: String): Option[String] =
      m match
        case '{ Map(${Varargs(args)}) } =>
          for (a <- args) {
            if (a.asTerm.tpe <:<  TypeRepr.of[(String, String)]) {
              val kv = a.asExprOf[(String, String)]
              summon[FromExpr[(String, String)]].unapply(kv) match
                case Some((k, v)) => if (k == input) return Some(v)
                case None => throw  CompileTimeEvalException(s"Can't eval ${a.show} at compile time", a)
            } else throw CompileTimeEvalException(s"Can't case ${a.show} to (String, String)", a)
          }
          None
        case _ => throw CompileTimeEvalException(s"Map ${m.show} should be a constrictoor literal, we have ${m.asTerm}", m)

    def evalExpr[T: Type](expr: Expr[T]): Expr[T] = evalTerm(expr.asTerm, Map.empty, None).asExprOf[T]

    private def evalApplyStringTerm(ft: Term, input: String, bindings: Map[Symbol, Term]): Option[String] =
      ft match
        case Inlined(oring, inlineBindings, body) =>
          evalApplyStringTerm(body, input, addBindings(ft, bindings, inlineBindings))
        case _ =>
          val inputLiteral = Literal(StringConstant(input))
          val nullTerm = Literal(NullConstant())
          val termResult = ft match
            case Lambda(params, body) =>
              params match
                case List(param) => evalTerm(body, bindings.updated(param.symbol, inputLiteral), Some(nullTerm))
                case _ =>
                  throw CompileTimeEvalException(s"Expected that partial function have one parameter ${ft.show}", ft.asExpr)
            case other =>
              if (ft.tpe <:< TypeRepr.of[PartialFunction[_, _]]) {
                 val isDefinedTerm = try {
                   Apply(Select.unique(ft, "isDefinedAt"), List(inputLiteral))
                 } catch {
                   case ex: Throwable =>
                    throw CompileTimeEvalException(s"Can't create isDefinedAt call for ${ft}: ${ex.getMessage}", ft.asExpr, ex)
                 }
                 val applyTerm = try {
                   Apply(Select.unique(ft, "apply"), List(inputLiteral))
                 } catch {
                  case ex: Throwable =>
                    throw CompileTimeEvalException(s"Can't create apply call for ${ft}: ${ex.getMessage}", ft.asExpr, ex)
                 }
                 if (evalCondition(isDefinedTerm, bindings)) evalTerm(applyTerm, bindings, None)
                 else nullTerm
              } else if (ft.tpe <:< TypeRepr.of[Function[_, _]]) {
                val applyTerm = try {
                  Apply(Select.unique(ft, "apply"), List(inputLiteral))
                } catch {
                  case ex: Throwable =>
                    throw CompileTimeEvalException(s"Can't create apply call for ${ft}: ${ex.getMessage}", ft.asExpr, ex)
                }
                evalApply(applyTerm, bindings)
              } else {
                throw CompileTimeEvalException(s"PartialFunction[String, String] or Function[String, String] is required, we have ${ft.tpe.show}", ft.asExpr)
              }
          termToOptString(termResult)

    private def evalTerm(ft: quotes.reflect.Term, bindings: Map[Symbol, Term], optDefault: Option[Term]): Term = {
      import quotes.reflect._

      ft match {
        case Inlined(origin, inlineBindings, body) =>
          evalTerm(body, addBindings(ft, bindings, inlineBindings), optDefault)
        case id@Ident(_) =>
          bindings.get(id.symbol) match
            case Some(term) => evalTerm(term, bindings, optDefault)
            case None => throw CompileTimeEvalException(s"Unknown symbol: $id, bindigns=${bindings}", ft.asExpr)
        case m@Match(scrutinee, caseDefs ) => evalMatch(m, bindings, optDefault)
        case If(cond, ifTrue, ifFalse) =>
          if (evalCondition(cond, bindings)) evalTerm(ifTrue, bindings, optDefault)
          else evalTerm(ifFalse, bindings, optDefault)
        case app@Apply(fun, args) => evalApply(app, bindings)
        case block@Block(statements, exprs) => evalBlock(block, bindings, optDefault)
        case lt@Literal(_) => lt
        case Typed(expr, tpt) => evalTerm(expr, bindings, optDefault)
        case other => throw CompileTimeEvalException(s"Unsupported constant expression: $other", ft.asExpr)
      }
    }

    private def evalMatch(t: Match, bindings: Map[Symbol, Term], optDefault: Option[Term]): Term = {
      val scrutinee = evalTerm(t.scrutinee, bindings, None)
      var result: Option[Term] = None
      var cases = t.cases
      while (!cases.isEmpty && result.isEmpty) {
        val c = cases.head
        cases = cases.tail
        result = evalCaseDef(t, c, scrutinee, bindings, optDefault)
      }
      result match
        case Some(value) => value
        case None => optDefault.getOrElse(throw CompileTimeEvalException(s"Match failed and no default: scrutinee=${scrutinee}\nmatch: ${t.show}\nbindings: ${bindings}\nmatch tree: ${t}\n", t.asExpr))
    }

    private def evalCaseDef(m: Term, caseDef: CaseDef, scrutinee: Term, bindings: Map[Symbol, Term],
                            optDefault: Option[Term]): Option[Term] =
      evalCaseDefPattern(m, caseDef.pattern, scrutinee, bindings).flatMap { newBinding =>
        caseDef.guard match
          case Some(guard) =>
            if (evalCondition(guard, newBinding)) Some(evalTerm(caseDef.rhs, newBinding, optDefault))
            else None
          case None =>
            Some(evalTerm(caseDef.rhs, newBinding, optDefault))
      }

    private def evalCaseDefPattern(m: Term, pattern: Tree, scrutinee: Term,
                                   bindings: Map[Symbol, Term]):  Option[Map[Symbol, Term]] = {
      pattern match
        case TypedOrTest(v, tpt) =>
          evalCaseDefPattern(m, v, scrutinee, bindings).flatMap { newBinding =>
            if (scrutinee.tpe <:< tpt.tpe) Some(newBinding)
            else None
          }
        case b@Bind(name, tree) =>
          evalCaseDefPattern(m, tree, scrutinee, bindings).map { newBindings =>
            tree match
              case ct: Term => newBindings.updated(b.symbol, ct)
              case _ => newBindings.updated(b.symbol, scrutinee)
          }
        case Unapply(fun, implicits, patterns) =>
          // TODO: implement regexpr ?
          throw CompileTimeEvalException(s"Unapply ${fun} is not supported in compile-time pattern", m.asExpr)
        case Alternatives(cases) =>
          var c = cases
          var retval: Option[Map[quotes.reflect.Symbol, quotes.reflect.Term]] = None
          while (!c.isEmpty) {
            val h = c.head
            c = c.tail
            retval = evalCaseDefPattern(m, h, scrutinee, bindings)
          }
          retval
        case Wildcard() =>
          Some(bindings.updated(pattern.symbol, scrutinee))
        case lit@Literal(constant) =>
          if (evalLitEquals(lit, scrutinee, bindings)) Some(bindings)
          else None
        case other =>
          throw CompileTimeEvalException(s"Pattern ${other} is not supported in compile-time evaluation", m.asExpr)
    }

    private def evalApply(applyTerm: Apply, bindings: Map[Symbol, Term]): Term =
      evalApply2(applyTerm, applyTerm.fun, applyTerm.args.map(x => evalTerm(x, bindings, None)), bindings)

    private def evalApply2(posTerm: Term, fun: Term, args: List[Term], bindings: Map[Symbol, Term]): Term =
      fun match
        case Inlined(origin, inlineBindings, body) =>
          evalApply2(posTerm, body, args, addBindings(posTerm, bindings, inlineBindings))
        case Select(qual, memberName) =>
          evalApplySelect(posTerm, qual, memberName, args, bindings)
        case other =>
          val funSym = other.symbol
          if (funSym.flags.is(Flags.Module)) applyJavaReflectModule(posTerm, funSym, args) // TODO: test
          else throw new CompileTimeEvalException(s"Expected that $funSym is a module", posTerm.asExpr)

    private def evalApplySelect(posTerm: Term, qual: Term, memberName: String, args: List[Term],
                                bindings: Map[Symbol, Term]): Term = {
      def runIsDefinedAt(fun: Term): Boolean =
        evalApplySelect(posTerm, fun, "isDefinedAt", args, bindings) match
          case Literal(BooleanConstant(v)) => v
          case other => throw new CompileTimeEvalException(
            s"Expected that isDefined returns boolean, we have ${other}\nfun = ${fun.show}", posTerm.asExpr)

      qual match
        case Inlined(origin, inlineBindings, body) =>
          evalApplySelect(body, body, memberName, args, addBindings(posTerm, bindings, inlineBindings))
        case Select(qual1, name1) =>
          if (qual1.symbol.fullName == "com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker") {
            applyJsonCodeMakerField(posTerm, name1, memberName, args)
          } else jvmToTerm(posTerm, javaReflectionCall(posTerm, evalQualJvm(qual, bindings), memberName, args))
        case Lambda(params, body) =>
          if (memberName == "apply") {
            val nApply = Select.unique(qual,"apply").appliedToArgs(args) // TODO: pass symbol instead using Select.unique
            Term.betaReduce(nApply) match
              case Some(term) => evalTerm(term, bindings, None)
              case None => throw new CompileTimeEvalException(s"Can't beta-reduce ${nApply}", posTerm.asExpr)
          } else {
            throw new CompileTimeEvalException(s"Expected that lambda call memeber name is 'apply', we have $memberName", posTerm.asExpr)
          }
        case Apply(TypeApply(Select(frs, "andThen"), List(stringTpt)), List(snd)) =>
          if (memberName == "isDefinedAt") {
            if (runIsDefinedAt(frs)) {
              val r1 = evalApplySelect(posTerm, frs, "apply", args, bindings)
              evalApplySelect(posTerm, snd, "isDefinedAt", List(r1), bindings)
            } else Literal(BooleanConstant(false))
          } else if (memberName == "apply") {
            val r1 = evalApplySelect(posTerm, frs, "apply", args, bindings)
            evalApplySelect(posTerm, snd, "apply", List(r1), bindings)
          } else {
            throw new CompileTimeEvalException(s"Expected that parial function methods are 'isDefinedAt' and 'apply', we have $memberName", posTerm.asExpr)
          }
        case Apply(TypeApply(Select(frs, "orElse"), List(stringTpt)), List(snd)) =>
          if (memberName == "isDefinedAt") {
            if (runIsDefinedAt(frs)) Literal(BooleanConstant(true))
            else evalApplySelect(posTerm, snd, "isDefinedAt", args, bindings)
          } else if (memberName == "apply") {
            if (runIsDefinedAt(frs)) evalApplySelect(posTerm, frs, "apply", args, bindings)
            else evalApplySelect(posTerm, snd, "apply", args, bindings)
          } else {
            throw new CompileTimeEvalException(s"expected that parial function methods are 'isDefinedAt' and 'apply', we have $memberName", posTerm.asExpr)
          }
        case other =>
          try {
            jvmToTerm(posTerm, javaReflectionCall(posTerm, evalQualJvm(qual, bindings), memberName, args))
          } catch {
            case ex: Throwable =>
              // TODO: introduce some tracing flag
              println(s"Exception during reflectionCall, retrace, qual=$qual")
              val ref = evalQualJvm(qual, bindings, true)
              println(s"ref = $ref")
              throw ex;
          }
    }

    private def evalQualJvm(qual: Term, bindings: Map[Symbol, Term], trace: Boolean = false): AnyRef = {
      if (trace) println(s"evalQualJvn, qual=${qual}")
      if (qual.symbol.flags.is(Flags.Module)) {
        if (trace) println("evalQualJvm: module")
        retrieveRuntimeModule(qual, qual.symbol)
      } else {
        qual match
          case Inlined(origin, inlineBindings, body) =>
            if (trace) println("inlined")
            evalQualJvm(body, addBindings(body, bindings, inlineBindings))
          case id@Ident(_) =>
            bindings.get(id.symbol) match
              case Some(value) =>
                val r1 = evalTerm(value, bindings, None)
                termToJvm(r1)
              case None =>
                throw new CompileTimeEvalException(s"Can't interpret '${id.show}' as constant expression", qual.asExpr)
          case Select(qual1, name) =>
            val ref1 = evalQualJvm(qual1, bindings)
            retrieveRuntimeField(qual, ref1, name)
          case Apply(Select(qual1, name), args) =>
            if (trace) println(s"evalQualJvm: reduceApply. qual1=${qual1}")
            val ref1 = evalQualJvm(qual1, bindings, trace)
            if (trace) println(s"evalQualJvm: after reduceApply. ref1=${ref1} (class ${ref1.getClass})")
            val r = javaReflectionCall(qual, ref1, name, args)
            if (trace) println(s"After javaReflectionCall, r=$r")
            r
          case If(cond, ifTrue, ifFalse) =>
            if (evalCondition(cond, bindings)) evalQualJvm(ifTrue, bindings)
            else evalQualJvm(ifFalse, bindings)
          case _ =>
            // this can be constant expression
            val t = evalTerm(qual, bindings, None)
            if (trace) println(s"Before termToJvm, t=$t")
            val r = termToJvm(t)
            if (trace) println(s"After termToJvm, r=$r (class = ${r.getClass})")
            r
      }
    }

    private def evalCondition(term: Term, bindings: Map[Symbol, Term]): Boolean =
      evalTerm(term, bindings, None) match
        case Literal(BooleanConstant(v)) => v
        case other => throw CompileTimeEvalException(s"Condition should return boolean value, we have ${other.show}", term.asExpr)

    private def evalBlock(block: Block, bindings: Map[Symbol, Term], optDefault: Option[Term]): Term = {
      var statements = block.statements
      var b = bindings
      while (!statements.isEmpty) {
       val h = statements.head
       statements = statements.tail
       h match
        case dfn: Definition => b = addDefinition(block, b, dfn)
        case bt: Term => throw CompileTimeEvalException(s"Term as non-last block statement have no sence", bt.asExpr)
        case _ => // ignore Import and Export
      }
      evalTerm(block.expr, b, optDefault)
    }

    private def evalLitEquals(lhs: Literal, rhs: Term, bindings: Map[Symbol, Term] ): Boolean =
      rhs match
        case Literal(rconst) => lhs.constant.value == rconst.value
        case _ =>
          evalTerm(rhs, bindings, None) match
            case Literal(rconst) => lhs.constant.value == rconst.value
            case other => false // throw evaluation exception ?

    private def addBindings(m: Term, x: Map[Symbol, Term], bindings: List[Definition]): Map[Symbol, Term] = {
      var r = x
      var c = bindings
      while (!c.isEmpty) {
        val h = c.head
        c = c.tail
        r = addDefinition(m, r, h)
      }
      r
    }

    private def addDefinition(m: Term, x: Map[quotes.reflect.Symbol, quotes.reflect.Term],
                              binding: quotes.reflect.Definition): Map[quotes.reflect.Symbol, quotes.reflect.Term] =
      binding match
        case vd@ValDef(name, tpt, optRhs) =>
          optRhs match
            case Some(rhs) => x.updated(vd.symbol, rhs)
            case None => x
        case other =>
          throw CompileTimeEvalException(s"Definitions other then ValDefs is not supported, we have $other ", m.asExpr)

    private def applyJsonCodeMakerField(t: Term, fieldName: String, operation: String, args: List[Term] ): Term = {
      val arg = termToString(args.head)
      val field: PartialFunction[String, String] = fieldName match {
        case "partialIdentity" => JsonCodecMaker.partialIdentity
        case "enforceCamelCase" => JsonCodecMaker.enforceCamelCase
        case "EnforcePascalCase" => JsonCodecMaker.EnforcePascalCase
        case "enforce_snake_case" => JsonCodecMaker.enforce_snake_case
        case "enforce-kebab-case" => JsonCodecMaker.`enforce-kebab-case`
        case _ => throw CompileTimeEvalException(s"Unknonwn JsonCodeMaker parial function field: ${fieldName}", t.asExpr)
      }
      operation match {
        case "isDefinedAt" => Literal(BooleanConstant(field.isDefinedAt(arg)))
        case "apply" => jvmToTerm(t, field.apply(arg))
        case _ => throw CompileTimeEvalException(s"Expected isDefinedAt or Apply operations, we have: ${operation}", t.asExpr)
      }
    }

    private def retrieveRuntimeModule(applyTerm: Term, sym: Symbol): AnyRef =  {
      val className = sym.fullName + "$"  // assume that java and scala encoding are same.
      val clazz = java.lang.Class.forName(className)
      val moduleField = try {
        clazz.getField("MODULE$")
      } catch{
        case ex: Exception =>
          throw CompileTimeEvalException(s"Can't get ModuleField for lass ${className}", applyTerm.asExpr, ex)
      }
      val instance = moduleField.get(null)
      if (instance == null) {
        throw CompileTimeEvalException(s"Module is null for lass ${className}", applyTerm.asExpr)
      }
      instance
    }

    // Field is a Scala field, we search for an access method with the same name
    private def retrieveRuntimeField(applyTerm: Term, obj: AnyRef, name: String): AnyRef =
      try { // TODO: error handling
        obj.getClass.getMethod(name).invoke(obj)
      } catch{
        case ex: NoSuchMethodException =>
          obj.getClass.getField(name).get(obj)
      }

    private def applyJavaReflectedModuleField(applyTerm: Term, qualSym: Symbol, memberName: String,
                                              args: List[Term]): Term =
      jvmToTerm(applyTerm, javaReflectionCall(applyTerm, retrieveRuntimeModule(applyTerm, qualSym), memberName, args))

    private def applyJavaReflectModule(applyTerm: Term, funSym: Symbol, args: List[Term]): Term =
      applyJavaReflectedModuleField(applyTerm, funSym, "apply", args)

    private def applyPrimitiveOrStringField(applyTerm: Term, qual: Term, memberName: String, args: List[Term]): Term =
      jvmToTerm(applyTerm, javaReflectionCall(applyTerm, termToJvm(qual), memberName, args))

    class JvmReflectionMethodCallException(msg: String, ex: Exception) extends RuntimeException(msg, ex)

    sealed trait JvmReflectionMethodCall {
      def process(): AnyRef

      protected def retrieveArgTypes(args: Array[AnyRef]): Array[java.lang.Class[_]] = {
        val argsTypes = new Array[Class[_]](args.length)
        var i = 0
        while (i < argsTypes.length) {
          argsTypes(i) = args(i).getClass()
          i += 1
        }
        argsTypes
      }
    }

    case class DirectJvmReflectionMethodCall(obj: AnyRef, name: String,
                                             args: Array[AnyRef]) extends JvmReflectionMethodCall {
      def process(): AnyRef = {
        val argsTypes = retrieveArgTypes(args)
        val method = try {
          obj.getClass.getMethod(name, argsTypes: _*)
        } catch {
          case ex: NoSuchMethodException =>
            throw JvmReflectionMethodCallException(s"Can't find method $name of object $obj (class ${obj.getClass}) with argument types: ${argsTypes.toList}", ex)
          case ex: SecurityException =>
            throw JvmReflectionMethodCallException(s"Can't get method $name of object $obj (class ${obj.getClass})", ex)
        }
        try {
          method.invoke(obj, args: _*)
        } catch {
          case ex: Exception =>
            throw JvmReflectionMethodCallException(s"Can't invoke method $name of object $obj (class ${obj.getClass})", ex)
        }
      }
    }

    sealed trait PrependedArgumentJvmReflectionMethodCall extends JvmReflectionMethodCall {
      def prependArgument(obj: AnyRef, args: Array[AnyRef]): Array[AnyRef] = {
        val retval = new Array[AnyRef](args.length + 1)
        retval(0) = obj
        System.arraycopy(args, 0, retval, 1, args.length)
        retval
      }
    }

    case class HelperObjectJvmReflectionMethodCall(helperObj: AnyRef, obj: AnyRef, name: String,
                                                   args: Array[AnyRef]) extends PrependedArgumentJvmReflectionMethodCall {
      def process(): AnyRef = {
        val nArgs = prependArgument(obj, args)
        val argsTypes = retrieveArgTypes(nArgs)
        val method = try {
          helperObj.getClass().getMethod(name, argsTypes: _*)
        } catch {
          case ex: NoSuchMethodException =>
            throw JvmReflectionMethodCallException(s"Can't find method $name of object ${helperObj} (class ${helperObj.getClass}) with argument types: ${argsTypes.toList}", ex)
          case ex: SecurityException =>
            throw JvmReflectionMethodCallException(s"Can't get method $name of object ${helperObj} (class ${helperObj.getClass})", ex)
        }
        try {
          method.invoke(helperObj, nArgs: _*)
        } catch {
          case ex: Exception =>
            throw JvmReflectionMethodCallException(s"Can't invoke method $name of object ${helperObj} (class ${helperObj.getClass})", ex)
        }
      }
    }

    case class StringConcatJvmReflectionMethodCall(obj: String, args: Array[AnyRef]) extends JvmReflectionMethodCall {
      def process(): AnyRef = {
        var r = obj
        var i = 0
        while (i < args.length) {
          r = r.concat(args(i).toString)
          i += 1
        }
        r
      }
    }

    private def javaReflectionCall(term: Term, qual: AnyRef, name: String, args: List[Term]): AnyRef = {
      val preparedArgs = args.map(t => termToJvm(t)).toArray
      val call = prepareJvmReflectionMethodCall(term, qual, name, preparedArgs)
      try {
        call.process()
      } catch {
          case ex: JvmReflectionMethodCallException =>
            throw CompileTimeEvalException(ex.getMessage, term.asExpr, ex.getCause)
      }
    }

    private def  prepareJvmReflectionMethodCall(t: Term, x: AnyRef,  name: String, args: Array[AnyRef]): JvmReflectionMethodCall =
      name match
        case "+" =>
          if (x.isInstanceOf[java.lang.String]) {
            StringConcatJvmReflectionMethodCall(x.asInstanceOf[java.lang.String], args)
          } else if (x.isInstanceOf[java.lang.Integer] || x.isInstanceOf[java.lang.Long]) {
            HelperObjectJvmReflectionMethodCall(x, x, "sum", args)
          } else {
            throw CompileTimeEvalException(s"Can't find substitute for opeation $name of object ${x} (class ${x.getClass})", t.asExpr)
          }
        case _ =>
          DirectJvmReflectionMethodCall(x, name, args)

    private def jvmToTerm(applyTerm: Term, obj: AnyRef): Term =
      if (obj.isInstanceOf[String]) {
        Literal(StringConstant(obj.asInstanceOf[String]))
      } else if (obj.asInstanceOf[java.lang.Boolean]) {
        Literal(BooleanConstant(obj.asInstanceOf[java.lang.Boolean].booleanValue()))
      } else if (obj.isInstanceOf[java.lang.Character]) {
        Literal(CharConstant(obj.asInstanceOf[java.lang.Character].charValue()))
      } else if (obj.isInstanceOf[java.lang.Byte]) {
        Literal(ByteConstant(obj.asInstanceOf[java.lang.Byte].byteValue()))
      } else if (obj.isInstanceOf[java.lang.Short]) {
        Literal(ShortConstant(obj.asInstanceOf[java.lang.Short].shortValue()))
      } else if (obj.isInstanceOf[java.lang.Integer]) {
        Literal(IntConstant(obj.asInstanceOf[java.lang.Integer].intValue()))
      } else if (obj.isInstanceOf[java.lang.Long]) {
        Literal(LongConstant(obj.asInstanceOf[java.lang.Long].intValue()))
      } else if (obj.isInstanceOf[java.lang.Float]) {
        Literal(FloatConstant(obj.asInstanceOf[java.lang.Float].floatValue()))
      } else if (obj.isInstanceOf[java.lang.Double]) {
        Literal(DoubleConstant(obj.asInstanceOf[java.lang.Double].floatValue()))
      } else {
        throw CompileTimeEvalException(s"Return value of an external function ($obj) is not primitive or string", applyTerm.asExpr)
      }

    private def termToJvm(x: Term): AnyRef =
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
        case id@Ident(name) if (id.symbol.flags.is(Flags.Module)) =>
          retrieveRuntimeModule(x, id.symbol)
        case _ =>
          throw CompileTimeEvalException(s"Can't interpret ${x} as primitive (type ${x.tpe.widen.show})", x.asExpr)

    private def termToOptString(x: Term): Option[String] =
      x match
        case Literal(NullConstant()) => None
        case Literal(StringConstant(v)) => Some(v)
        case _ => throw CompileTimeEvalException(s"Term should return string or null term, we have ${x}", x.asExpr)

    private def termToString(x: quotes.reflect.Term): String =
      x match
        case Literal(StringConstant(v)) => v
        case _ => throw CompileTimeEvalException(s"Term should return string, we have ${x}", x.asExpr)

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