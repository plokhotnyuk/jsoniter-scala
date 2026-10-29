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
  * Configuration of [[com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter]]: formatting of JSON output and the size
  * of the internal byte buffer, which the writer allocates once and reuses.
  * <br/>
  * The defaults suit most cases. Change them to:
  * <ul>
  * <li>turn on pretty printing with an `indentionStep` greater than 0</li>
  * <li>turn on escaping of non-ASCII chars for ASCII-only output</li>
  * <li>increase `preferredBufSize` to avoid reallocation of the byte buffer when the output exceeds 32Kb</li>
  * </ul>
  * The `with...` methods throw `IllegalArgumentException` for values outside of the ranges below.
  *
  * @param throwWriterExceptionWithStackTrace turns on stack traces of serialization exceptions for debugging (`false`
  *                                           by default)
  * @param indentionStep the number of spaces per indentation level, or 0 for compact output (0 by default, not
  *                      negative)
  * @param escapeUnicode turns on hexadecimal escaping of all non-ASCII chars (`false` by default)
  * @param preferredBufSize the preferred size (in bytes) of the byte buffer for all outputs except byte arrays and
  *                         heap byte buffers passed as arguments (32768 by default, at least 1)
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