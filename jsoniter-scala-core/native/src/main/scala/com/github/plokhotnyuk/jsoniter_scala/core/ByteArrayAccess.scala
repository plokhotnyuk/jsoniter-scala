package com.github.plokhotnyuk.jsoniter_scala.core

import scala.scalanative.runtime.Intrinsics._
import scala.scalanative.runtime.LLVMIntrinsics._
import scala.scalanative.runtime.RawPtr

private[core] object ByteArrayAccess {
  @inline
  private[this] def toPtr(buf: Array[Byte], pos: Int): RawPtr = elemRawPtr(castObjectToRawPtr(buf), pos + 16)

  @inline
  def setLong(buf: Array[Byte], pos: Int, value: Long): Unit = storeLong(toPtr(buf, pos), value)

  @inline
  def getLong(buf: Array[Byte], pos: Int): Long = loadLong(toPtr(buf, pos))

  @inline
  def setInt(buf: Array[Byte], pos: Int, value: Int): Unit = storeInt(toPtr(buf, pos), value)

  @inline
  def getInt(buf: Array[Byte], pos: Int): Int = loadInt(toPtr(buf, pos))

  @inline
  def setShort(buf: Array[Byte], pos: Int, value: Short): Unit = storeShort(toPtr(buf, pos), value)

  @inline
  def getIntReversed(buf: Array[Byte], pos: Int): Int = `llvm.bswap.i32`(getInt(buf, pos))
}