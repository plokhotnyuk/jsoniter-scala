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
  * Configuration of [[com.github.plokhotnyuk.jsoniter_scala.core.JsonReader]]: parse exceptions and sizes of internal
  * buffers, which the reader allocates once and reuses.
  * <br/>
  * The defaults suit most cases. Change them for performance:
  * <ul>
  * <li>turn off stack traces when exceptions are frequent, e.g. under DoS attacks, see
  * [[https://shipilev.net/blog/2014/exceptional-performance/]]</li>
  * <li>turn off hex dumps to shorten exception messages</li>
  * <li>increase `preferredBufSize` to avoid reallocation of the byte buffer when parsing large (>16Kb) numbers, raw
  * values, or ADT instances with the discriminator field not at the start of the JSON object</li>
  * <li>increase `preferredCharBufSize` to avoid reallocation of the char buffer when parsing large (>4Kb) strings,
  * including Base16 and Base64 encoded ones</li>
  * </ul>
  * The `with...` methods throw `IllegalArgumentException` for values outside of the ranges below.
  *
  * @param throwReaderExceptionWithStackTrace turns on stack traces of parse exceptions for debugging (`false` by
  *                                           default)
  * @param appendHexDumpToParseException turns on the hex dump of the input around the error in parse exceptions (`true`
  *                                      by default)
  * @param maxBufSize the max size (in bytes) of the byte buffer for parsing from [[java.io.InputStream]] or
  *                   [[java.nio.DirectByteBuffer]] (33554432 by default, from `preferredBufSize` to 2147483645)
  * @param maxCharBufSize the max size (in chars) of the char buffer for parsing strings, including Base16 and Base64
  *                       encoded ones (4194304 by default, from `preferredCharBufSize` to 2147483645)
  * @param preferredBufSize the preferred size (in bytes) of the byte buffer for parsing from [[java.io.InputStream]]
  *                         or [[java.nio.DirectByteBuffer]] (32768 by default, from 12 to `maxBufSize`)
  * @param preferredCharBufSize the preferred size (in chars) of the char buffer for parsing strings (4096 by
  *                             default, from 0 to `maxCharBufSize`)
  * @param checkForEndOfInput turns on a parse exception for non-whitespace bytes after the parsed value (`true` by
  *                           default)
  * @param hexDumpSize the number of 16-byte lines in the hex dump before and after the line with the error (2 by
  *                    default, at least 1)
  */
class ReaderConfig private (val preferredBufSize: Int, val preferredCharBufSize: Int, val maxBufSize: Int,
                            val maxCharBufSize: Int, val checkForEndOfInput: Boolean,
                            val throwReaderExceptionWithStackTrace: Boolean, val appendHexDumpToParseException: Boolean,
                            val hexDumpSize: Int) extends Serializable {
  def withThrowReaderExceptionWithStackTrace(throwReaderExceptionWithStackTrace: Boolean): ReaderConfig =
    copy(throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace)

  def withAppendHexDumpToParseException(appendHexDumpToParseException: Boolean): ReaderConfig =
    copy(appendHexDumpToParseException = appendHexDumpToParseException)

  def withMaxBufSize(maxBufSize: Int): ReaderConfig = {
    if (maxBufSize < preferredBufSize) throw new IllegalArgumentException("'maxBufSize' should be not less than 'preferredBufSize'")
    if (maxBufSize > 2147483645) throw new IllegalArgumentException("'maxBufSize' should be not greater than 2147483645")
    copy(maxBufSize = maxBufSize)
  }

  def withMaxCharBufSize(maxCharBufSize: Int): ReaderConfig = {
    if (maxCharBufSize < preferredCharBufSize) throw new IllegalArgumentException("'maxCharBufSize' should be not less than 'preferredCharBufSize'")
    if (maxCharBufSize > 2147483645) throw new IllegalArgumentException("'maxCharBufSize' should be not greater than 2147483645")
    copy(maxCharBufSize = maxCharBufSize)
  }

  def withPreferredBufSize(preferredBufSize: Int): ReaderConfig = {
    if (preferredBufSize < 12) throw new IllegalArgumentException("'preferredBufSize' should be not less than 12")
    if (preferredBufSize > maxBufSize) throw new IllegalArgumentException("'preferredBufSize' should be not greater than 'maxBufSize'")
    copy(preferredBufSize = preferredBufSize)
  }

  def withPreferredCharBufSize(preferredCharBufSize: Int): ReaderConfig = {
    if (preferredCharBufSize < 0) throw new IllegalArgumentException("'preferredCharBufSize' should be not less than 0")
    if (preferredCharBufSize > maxCharBufSize) throw new IllegalArgumentException("'preferredCharBufSize' should be not greater than 'maxCharBufSize'")
    copy(preferredCharBufSize = preferredCharBufSize)
  }

  def withCheckForEndOfInput(checkForEndOfInput: Boolean): ReaderConfig =
    copy(checkForEndOfInput = checkForEndOfInput)

  def withHexDumpSize(hexDumpSize: Int): ReaderConfig = {
    if (hexDumpSize < 1) throw new IllegalArgumentException("'hexDumpSize' should be not less than 1")
    copy(hexDumpSize = hexDumpSize)
  }

  private[this] def copy(preferredBufSize: Int = preferredBufSize, preferredCharBufSize: Int = preferredCharBufSize,
                         maxBufSize: Int = maxBufSize, maxCharBufSize: Int = maxCharBufSize,
                         checkForEndOfInput: Boolean = checkForEndOfInput,
                         throwReaderExceptionWithStackTrace: Boolean = throwReaderExceptionWithStackTrace,
                         appendHexDumpToParseException: Boolean = appendHexDumpToParseException,
                         hexDumpSize: Int = hexDumpSize): ReaderConfig =
    new ReaderConfig(throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace,
      appendHexDumpToParseException = appendHexDumpToParseException, maxBufSize = maxBufSize,
      maxCharBufSize = maxCharBufSize, preferredBufSize = preferredBufSize, preferredCharBufSize = preferredCharBufSize,
      checkForEndOfInput = checkForEndOfInput, hexDumpSize = hexDumpSize)
}

object ReaderConfig extends ReaderConfig(preferredBufSize = 32768, preferredCharBufSize = 4096, maxBufSize = 33554432,
  maxCharBufSize = 4194304, throwReaderExceptionWithStackTrace = false, appendHexDumpToParseException = true,
  checkForEndOfInput = true, hexDumpSize = 2)
