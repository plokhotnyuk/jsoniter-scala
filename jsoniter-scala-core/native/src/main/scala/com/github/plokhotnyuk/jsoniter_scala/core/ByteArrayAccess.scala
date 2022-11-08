package com.github.plokhotnyuk.jsoniter_scala.core

import scala.scalanative.runtime.ByteArray
import scala.scalanative.runtime.LLVMIntrinsics._
import scala.scalanative.unsafe._

private object ByteArrayAccess {
  
  @inline private[this] def toPtr(buf: Array[Byte]): Ptr[Byte] =
    buf.asInstanceOf[ByteArray].at(0)

  @inline def setLong(buf: Array[Byte], pos: Int, value: Long): Unit =
    toPtr(buf).asInstanceOf[Ptr[Long]](pos) = value

  @inline def getLong(buf: Array[Byte], pos: Int): Long =
    toPtr(buf).asInstanceOf[Ptr[Long]](pos)

  @inline def setInt(buf: Array[Byte], pos: Int, value: Int): Unit =
    toPtr(buf).asInstanceOf[Ptr[Long]](pos) = value

  @inline def getInt(buf: Array[Byte], pos: Int): Int =
    toPtr(buf).asInstanceOf[Ptr[Int]](pos)

  @inline def setShort(buf: Array[Byte], pos: Int, value: Short): Unit =
    toPtr(buf).asInstanceOf[Ptr[Short]](pos) = value

  @inline def getIntReversed(buf: Array[Byte], pos: Int): Int =
    `llvm.bswap.i32`(getInt(buf, pos))

}
