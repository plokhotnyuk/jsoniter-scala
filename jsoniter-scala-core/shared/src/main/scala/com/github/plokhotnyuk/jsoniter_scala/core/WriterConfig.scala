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

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter]] that contains params for formatting of
  * output JSON and for tuning of preferred size for internal byte buffer that is created on the writer instantiation
  * and reused in runtime for serialization of messages using any type of output except pre-allocated byte arrays or
  * heap byte buffers supplied as arguments.
  * <br/>
  * All configuration params already initialized to default values, but in some cases they should be altered:
  * <ul>
  * <li>turn on pretty printing by specifying of indention step that is greater than 0</li>
  * <li>turn on escaping of Unicode characters to serialize with only ASCII characters</li>
  * <li>increase preferred size of an internal byte buffer to reduce allocation rate of grown and then reduced
  * internal buffers when serialized output size is greater than 32Kb</li>
  * </ul>
  * @param throwWriterExceptionWithStackTrace a flag that allows to turn on a stack traces for debugging purposes in
  *                                           development
  * @param indentionStep a size of indention for pretty-printed formatting or 0 for compact output
  * @param escapeUnicode a flag to turn on hexadecimal escaping of all non-ASCII chars
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when writing to any type of output
  *                         except pre-allocated byte arrays or heap byte buffers supplied as arguments
  */
class WriterConfig private (val indentionStep: Int, val preferredBufSize: Int, val escapeUnicode: Boolean,
                            val throwWriterExceptionWithStackTrace: Boolean) extends Serializable {
  def withThrowWriterExceptionWithStackTrace(throwWriterExceptionWithStackTrace: Boolean): WriterConfig =
    copy(throwWriterExceptionWithStackTrace = throwWriterExceptionWithStackTrace)

  def withIndentionStep(indentionStep: Int): WriterConfig = {
    if (indentionStep < 0) throw new IllegalArgumentException("'indentionStep' should be not less than 0")
    copy(indentionStep = indentionStep)
  }

  def withEscapeUnicode(escapeUnicode: Boolean): WriterConfig =
    copy(escapeUnicode = escapeUnicode)

  def withPreferredBufSize(preferredBufSize: Int): WriterConfig = {
    if (preferredBufSize <= 0) throw new IllegalArgumentException("'preferredBufSize' should be not less than 1")
    copy(preferredBufSize = preferredBufSize)
  }

  private[this] def copy(indentionStep: Int = indentionStep, preferredBufSize: Int = preferredBufSize,
                         throwWriterExceptionWithStackTrace: Boolean = throwWriterExceptionWithStackTrace,
                         escapeUnicode: Boolean = escapeUnicode): WriterConfig =
    new WriterConfig(
      indentionStep = indentionStep,
      preferredBufSize = preferredBufSize,
      escapeUnicode = escapeUnicode,
      throwWriterExceptionWithStackTrace = throwWriterExceptionWithStackTrace)
}

object WriterConfig extends WriterConfig(indentionStep = 0, preferredBufSize = 32768, escapeUnicode = false,
  throwWriterExceptionWithStackTrace = false)