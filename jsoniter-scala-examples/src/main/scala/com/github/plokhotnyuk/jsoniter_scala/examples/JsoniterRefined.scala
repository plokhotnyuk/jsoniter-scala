package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, JsonReader, JsonWriter}
import eu.timepit.refined.api.{RefType, Validate}

import scala.language.higherKinds
import scala.reflect.runtime.universe.WeakTypeTag

// TODO: how to deal with refined collections? implicit JsonCodec[T] seems not to be visible for primitive types ...
trait JsoniterRefined {

  // ===========================================================================================================================================================
  // INT
  // ===========================================================================================================================================================
  implicit def intCodec[F[_, _], P](
                                     implicit refType: RefType[F],
                                     validate: Validate[Int, P],
                                     typeTag: WeakTypeTag[F[Int, P]]
                                   ): JsonCodec[F[Int, P]] = {

    new JsonCodec[F[Int, P]] {

      override def decodeKey(in: JsonReader): F[Int, P] = {
        val unrefined = in.readKeyAsInt()

        refType refine unrefined match {
          case Left(because) =>
            in decodeError s"Cannot convert '$unrefined' to ${typeTag.tpe.toString} because: $because"
          case Right(refined) =>
            refined
        }
      }

      override def encodeKey(refined: F[Int, P], out: JsonWriter): Unit = {
        val unrefined = refType unwrap refined
        out writeKey unrefined
      }

      override def decodeValue(in: JsonReader,
                               default: F[Int, P]): F[Int, P] = {
        if (in isNextToken 'n') {
          in.readNullOrError(default, "expected int value or null")
        } else {
          in.rollbackToken()
          val unrefined = in.readInt()
          refType refine unrefined match {
            case Left(because) =>
              in decodeError s"Cannot convert '$unrefined' to ${typeTag.tpe.toString} because: $because"
            case Right(refined) =>
              refined
          }
        }
      }

      override def encodeValue(refined: F[Int, P], out: JsonWriter): Unit = {
        if (refined != null) {
          val unrefined = refType unwrap refined
          out writeVal unrefined
        } else {
          out.writeNull()
        }

      }

      override def nullValue: F[Int, P] = null.asInstanceOf[F[Int, P]]
    }

  }

  // ===========================================================================================================================================================
  // STRING
  // ===========================================================================================================================================================
  implicit def stringCodec[F[_, _], P](
                                        implicit refType: RefType[F],
                                        validate: Validate[String, P],
                                        typeTag: WeakTypeTag[F[String, P]]
                                      ): JsonCodec[F[String, P]] = {

    new JsonCodec[F[String, P]] {

      override def decodeKey(in: JsonReader): F[String, P] = {
        val unrefined = in.readKeyAsString()

        refType refine unrefined match {
          case Left(because) =>
            in decodeError s"Cannot convert '$unrefined' to ${typeTag.tpe.toString} because: $because"
          case Right(refined) =>
            refined
        }
      }

      override def encodeKey(refined: F[String, P], out: JsonWriter): Unit = {
        val unrefined = refType unwrap refined
        out writeKey unrefined
      }

      override def decodeValue(in: JsonReader,
                               default: F[String, P]): F[String, P] = {
        if (in isNextToken 'n') {
          in.readNullOrError(default, "expected String value or null")
        } else {
          in.rollbackToken()
          // TODO: default correct?
          val unrefined = in.readString(refType unwrap default)
          refType refine unrefined match {
            case Left(because) =>
              in decodeError s"Cannot convert '$unrefined' to ${typeTag.tpe.toString} because: $because"
            case Right(refined) =>
              refined
          }
        }
      }

      override def encodeValue(refined: F[String, P], out: JsonWriter): Unit = {
        if (refined != null) {
          val unrefined = refType unwrap refined
          out writeVal unrefined
        } else {
          out.writeNull()
        }

      }

      override def nullValue: F[String, P] = null.asInstanceOf[F[String, P]]
    }

  }


}