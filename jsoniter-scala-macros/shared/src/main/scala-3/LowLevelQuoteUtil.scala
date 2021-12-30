package com.github.plokhotnyuk.jsoniter_scala.macros

import scala.quoted.*

object LowLevelQuoteUtil {

   /**
    * When we mix generation via trees and via quotes, we can have incorrect owners inside tree,
    * when tree with owner Symbol.currentOwner was inserted inside quote in other scope. 
    *
    * We call deepChangeOwner when all val-s which defined in term is not used outside term
    * and over
    **/
   def deepChangeOwner(using Quotes)(tree: quotes.reflect.Tree, owner: quotes.reflect.Symbol, traceFlag: Boolean): quotes.reflect.Tree = {
    import quotes.reflect.*  

    val mapper = new TreeMap() {
        override def transformTree(tree: Tree)(owner: Symbol):Tree = {
          try {
            super.transformTree(tree)(owner)
          }catch{
            case ex: IllegalStateException =>
              println(ex.getMessage())
              println(s"tree=$tree")
              throw ex
          }
        }

        override def transformStatement(tree: Statement)(owner: Symbol): Statement = {
          tree match
            case d: Definition =>
              if (d.symbol.owner != owner) {
                throw new IllegalStateException("Invalid owner in definition, which we nof fixed")
              }
              d
            case _ =>  
              super.transformStatement(tree)(owner)    
        }

        override def transformTerm(tree:Term)(owner: Symbol):Term = {
          var mismatch = false
          if (tree.symbol.exists) {
            if (tree.symbol.maybeOwner.exists) {
              if (tree.symbol.owner != owner) {
                if (false & traceFlag) {
                  println(s"owner mismatch for ${tree.show}, expectd owner: ${owner}, have ${tree.symbol.maybeOwner}, fixing")
                }
                mismatch = true
              }
            }
          }
      
          tree match {
            case Inlined(orgin, bindings, body) =>
              if (bindings.isEmpty) {
                transformTerm(body)(owner)
              } else {
                val r = if (mismatch) {
                  if (traceFlag) {
                    println(s"fixing mismatch for $tree")
                  }
                  tree.changeOwner(owner)
                } else tree
                super.transformTerm(r)(owner)
              }
            case bl@Block(statements, expr) =>
              var needTopLevelChange = false
              for{ s <- statements } {
                s match {
                  case d: Definition =>
                    if (d.symbol.owner != owner) {
                      needTopLevelChange = true
                    }
                  case other =>
                }
              }
              val r = if (needTopLevelChange) {
                         bl.changeOwner(owner)
                      } else bl
              val nStatements = r.statements.map{ s =>
                s match {
                  case t: Term =>
                    transformTerm(t)(owner)
                  case other => other
                }
              }
              val nExpr = transformTerm(r.expr)(owner)
              Block.copy(r)(nStatements,nExpr)
            case tif@If(cond, ifTrue, ifFalse) =>
              If.copy(tif)(transformTerm(cond)(owner),
                           transformTerm(ifTrue)(owner),
                           transformTerm(ifFalse)(owner)
              )
            case _ =>
              val r = if (mismatch) {
                if (traceFlag) {
                  println(s"fixing mismatch for ${tree.show}")
                }
                tree.changeOwner(owner)
              } else tree
              super.transformTerm(r)(owner)
          }
        }

        def checkInvalidOwner(term: Term, owner: Symbol):Boolean =
          checkOwner(term, owner, traceFlag, false, true) 

    }
    mapper.transformTree(tree)(owner)

   }

   /*
   def findAnyOwner(using Quotes)(tree: quotes.reflect.Tree): Option[quotes.reflect.Symbol] = {
    import quotes.reflect.*
    tree match {
        case td:Definition => Some(td.symbol.owner)
        case Block(statements, exp) =>
          statements.find(x => findAnyOwner(x).isDefined) match {
            case Some(s) => findAnyOwner(s)
            case None => findAnyOwner(exp)
          }
        case Inlined(x,b,v) =>
          findAnyOwner(v)
        case If(cond, a, b) =>
          findAnyOwner(cond).orElse(findAnyOwner(a)).orElse(findAnyOwner(b))
        case _ =>
          ???
    }      

   }
   */

   def checkOwner(using Quotes)(term: quotes.reflect.Term, ownerToCheck: quotes.reflect.Symbol, traceFlag: Boolean = true, throwFlag: Boolean = true, onlyFirst: Boolean = false): Boolean = {
    import quotes.reflect.*    
  
    var topLevelFound = false
    val treeTraverser = new TreeTraverser() {

      var wasException = false

      override def traverseTree(tree: Tree)(owner: Symbol): Unit = {
        if (topLevelFound && onlyFirst) {
          return
        }
        var foundInvalidOwner = false
        tree match
          case d: Definition =>
            if (tree.symbol.owner != owner) {
              foundInvalidOwner = true
              topLevelFound = true
              println(s"checkOwner: owner mismatch for ${tree.show}, expectd owner: ${owner}, have ${tree.symbol.maybeOwner}")
              if (!wasException) {
                  wasException = true
                  throw new IllegalStateException(s"invlid owner, expected: ${owner}, have ${tree.symbol.owner}")
              }     
            }
          case _ =>
        try {    
          if (!foundInvalidOwner) {
            traverseTreeChildren(tree)(owner)
          }
        }catch{
          case ex: IllegalStateException =>
            if (traceFlag) {
              println(s"in tree:  $tree\n")
            }
            if (throwFlag) {
              throw ex
            }
        }
     }
    }
    treeTraverser.foldTree((),term)(ownerToCheck)
    topLevelFound
   }

 



}