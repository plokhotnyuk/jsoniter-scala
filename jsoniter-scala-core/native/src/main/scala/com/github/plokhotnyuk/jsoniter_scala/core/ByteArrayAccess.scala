/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.core

import scala.scalanative.annotation.alwaysinline
import scala.scalanative.runtime.Intrinsics._
import scala.scalanative.runtime.LLVMIntrinsics._
import scala.scalanative.runtime.RawPtr
import scala.scalanative.meta.LinktimeInfo.isMultithreadingEnabled

private[core] object ByteArrayAccess {
  // Borrowed from https://github.com/scala-native/scala-native/blob/2bb9cc6f032a5b00083d0a6bbc96aba2632f61d4/nativelib/src/main/scala/scala/scalanative/runtime/MemoryLayout.scala
  @alwaysinline
  private[this] def PtrSize: Int = castRawSizeToInt(sizeOf[RawPtr])

  @alwaysinline
  private[this] def ValuesOffset: Int =
    (if (isMultithreadingEnabled) PtrSize
    else 0) + PtrSize + 8

  @alwaysinline
  private[this] def toPtr(buf: Array[Byte], pos: Int): RawPtr = elemRawPtr(castObjectToRawPtr(buf), pos + ValuesOffset)

  @alwaysinline
  def setLong(buf: Array[Byte], pos: Int, value: Long): Unit = storeLong(toPtr(buf, pos), value)

  @alwaysinline
  def getLong(buf: Array[Byte], pos: Int): Long = loadLong(toPtr(buf, pos))

  @alwaysinline
  def setInt(buf: Array[Byte], pos: Int, value: Int): Unit = storeInt(toPtr(buf, pos), value)

  @alwaysinline
  def getInt(buf: Array[Byte], pos: Int): Int = loadInt(toPtr(buf, pos))

  @alwaysinline
  def setShort(buf: Array[Byte], pos: Int, value: Short): Unit = storeShort(toPtr(buf, pos), value)

  @alwaysinline
  def setLongReversed(buf: Array[Byte], pos: Int, value: Long): Unit = storeLong(toPtr(buf, pos), `llvm.bswap.i64`(value))

  @alwaysinline
  def getIntReversed(buf: Array[Byte], pos: Int): Int = `llvm.bswap.i32`(getInt(buf, pos))
}