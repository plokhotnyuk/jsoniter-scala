package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import scala.quoted.*


// @plokhotnyuk: this is used by `JsonCodecMakerCustomSpec` to swap out the different implementations
// without requiring a major refactor. This is in order to ensure compatibility with existing tests.
// The reason for including this file in `main` instead of `test` is because the incremental compiler
// will crash when `originalResolverImpl` and `customResolverImpl` are used while in the same compile-unit
// as the tests. 
object makerImpl {
  import JsonCodecMaker.Impl
  import JsonCodecMaker.Impl.FieldTypeResolver

  // @plokhotnyuk: the default implementation
  object jsonCodecMakerImpl {
    export JsonCodecMaker.{ 
      make,
      makeWithoutDiscriminator,
      makeWithRequiredCollectionFields,
      makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName,
      makeWithRequiredDefaultFields,
      makeCirceLike,
      makeCirceLikeSnakeCased
    }
  }

  object defaultAPI extends Impl.MacroAPI(Impl.FieldTypeResolver.original)

  // @plokhotnyuk: stubbed-out equivalent of the default implementation
  object originalResolverImpl {

    inline def make[A]: JsonValueCodec[A] = ${defaultAPI.makeWithDefaultConfig}

    inline def makeWithoutDiscriminator[A]: JsonValueCodec[A] = ${defaultAPI.makeWithoutDiscriminator}

    inline def makeWithRequiredCollectionFields[A]: JsonValueCodec[A] = ${defaultAPI.makeWithRequiredCollectionFields}

    inline def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]: JsonValueCodec[A] =
      ${defaultAPI.makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName}

    inline def makeWithRequiredDefaultFields[A]: JsonValueCodec[A] = ${defaultAPI.makeWithRequiredDefaultFields}

    inline def makeCirceLike[A]: JsonValueCodec[A] = ${defaultAPI.makeCirceLike}

    inline def makeCirceLikeSnakeCased[A]: JsonValueCodec[A] = ${defaultAPI.makeCirceLikeSnakeCased}

    inline def make[A](inline config: CodecMakerConfig): JsonValueCodec[A] = ${defaultAPI.makeWithSpecifiedConfig('config)}
  }

  // @plokhotnyuk: this is the actual fix
  object customAPI extends Impl.MacroAPI(
    (tpe, symbol) => {
      import quotes.reflect.*
      val tst = symbol.typeRef.translucentSuperType.typeSymbol
      if (tst.flags.is(Flags.JavaDefined) || tst.flags.is(Flags.Scala2x)) {
        FieldTypeResolver.original(tpe, symbol)
      } else {
        tpe match {
          case TypeRef(q, n) if q.typeSymbol.flags.is(Flags.Module) => {
            val trmRef = q.typeSymbol.termRef
            def recurse(t: TypeRepr): TypeRepr = t match {
              case TermRef(q, n) => TermRef(recurse(q), n)
              case tr @ TypeRef(q, n) => recurse(q).select(tr.typeSymbol)
              case tt @ ThisType(tref) => {
                if (!tref.typeSymbol.flags.is(Flags.Module) && (trmRef.baseClasses.contains(tref.typeSymbol))) {
                  // replacing ThisType with its concrete enclosing module
                  trmRef
                } else {
                  tt
                }
              }
              case _ => t
            } 
            recurse(symbol.typeRef.translucentSuperType).dealias
          }
          case _ => {
            FieldTypeResolver.original(tpe, symbol)
          }
        }
      }
    })

  // @plokhotnyuk: stubbed-out custom implementation
  object customResolverImpl {

    inline def make[A]: JsonValueCodec[A] = ${customAPI.makeWithDefaultConfig}

    inline def makeWithoutDiscriminator[A]: JsonValueCodec[A] = ${customAPI.makeWithoutDiscriminator}

    inline def makeWithRequiredCollectionFields[A]: JsonValueCodec[A] = ${customAPI.makeWithRequiredCollectionFields}

    inline def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]: JsonValueCodec[A] =
      ${customAPI.makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName}

    inline def makeWithRequiredDefaultFields[A]: JsonValueCodec[A] = ${customAPI.makeWithRequiredDefaultFields}

    inline def makeCirceLike[A]: JsonValueCodec[A] = ${customAPI.makeCirceLike}

    inline def makeCirceLikeSnakeCased[A]: JsonValueCodec[A] = ${customAPI.makeCirceLikeSnakeCased}

    inline def make[A](inline config: CodecMakerConfig): JsonValueCodec[A] = ${customAPI.makeWithSpecifiedConfig('config)}
  }
}