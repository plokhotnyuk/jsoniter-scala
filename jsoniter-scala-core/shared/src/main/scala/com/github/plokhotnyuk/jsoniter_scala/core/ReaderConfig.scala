package com.github.plokhotnyuk.jsoniter_scala.core

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonReader]] that contains flags for tuning of
  * parsing exceptions and preferred sizes for internal buffers that are created on the reader instantiation and reused
  * in runtime for parsing of messages.
  * <br/>
  * All configuration params already initialized by recommended default values, but in some cases they should be altered
  * for performance reasons:
  * <ul>
  * <li>turn off stack traces for parsing exceptions to greatly reduce impact on performance for cases when exceptions
  * can be not exceptional (e.g. under DoS attacks over open to the world systems), see more details here:
  * [[https://shipilev.net/blog/2014/exceptional-performance/]]</li>
  * <li>turn off appending of hex dump to minimize length of exception message</li>
  * <li>increase preferred size of an internal byte buffer for parsing from [[java.io.InputStream]] or
  * [[java.nio.DirectByteBuffer]] to reduce allocation rate of grown and then reduced buffers during parsing of large
  * (>16Kb) numbers (including stringified), raw values, or ADT instances with the discriminator field doesn't
  * appear in the beginning of the JSON object</li>
  * <li>increase preferred size of an internal char buffer to reduce allocation rate of grown and then reduced
  * buffers when large (>4Kb) string instances need to be parsed including those one which use Base16 or Base64
  * encodings</li>
  * </ul>
  * @param throwReaderExceptionWithStackTrace a flag that allows to turn on a stack traces for debugging purposes in
  *                                           development
  * @param appendHexDumpToParseException a flag that allows to turn off hex dumping of affected by error part of
  *                                      an internal byte buffer
  * @param maxBufSize a max size (in bytes) of an internal byte buffer when parsing from [[java.io.InputStream]] or
  *                   [[java.nio.DirectByteBuffer]]
  * @param maxCharBufSize a max size (in chars) of an internal char buffer for parsing of string values including
  *                       those one which use Base16 or Base64 encodings
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when parsing from
  *                         [[java.io.InputStream]] or [[java.nio.DirectByteBuffer]]
  * @param preferredCharBufSize a preferred size (in chars) of an internal char buffer for parsing of string values
  * @param checkForEndOfInput a flag to check and raise an error if some non whitespace bytes will be detected after
  *                           successful parsing of the value
  * @param hexDumpSize a size of the hex dump in 16-byte lines before and after the 16-byte line where an error occurs
  * @param allowComments a flag to allow one-line and multi-line JavaScript-like comments
  */
class ReaderConfig private (val preferredBufSize: Int, val preferredCharBufSize: Int, val maxBufSize: Int,
                            val maxCharBufSize: Int, val checkForEndOfInput: Boolean,
                            val throwReaderExceptionWithStackTrace: Boolean, val appendHexDumpToParseException: Boolean,
                            val hexDumpSize: Int, val allowComments: Boolean) extends Serializable {
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

  def withCheckForEndOfInput(checkForEndOfInput: Boolean): ReaderConfig = copy(checkForEndOfInput = checkForEndOfInput)

  def withHexDumpSize(hexDumpSize: Int): ReaderConfig = {
    if (hexDumpSize < 1) throw new IllegalArgumentException("'hexDumpSize' should be not less than 1")
    copy(hexDumpSize = hexDumpSize)
  }

  def withAllowComments(allowComments: Boolean): ReaderConfig = copy(allowComments = allowComments)

  private[this] def copy(preferredBufSize: Int = preferredBufSize, preferredCharBufSize: Int = preferredCharBufSize,
                         maxBufSize: Int = maxBufSize, maxCharBufSize: Int = maxCharBufSize,
                         checkForEndOfInput: Boolean = checkForEndOfInput,
                         throwReaderExceptionWithStackTrace: Boolean = throwReaderExceptionWithStackTrace,
                         appendHexDumpToParseException: Boolean = appendHexDumpToParseException,
                         hexDumpSize: Int = hexDumpSize, allowComments: Boolean = allowComments): ReaderConfig =
    new ReaderConfig(throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace,
      appendHexDumpToParseException = appendHexDumpToParseException, maxBufSize = maxBufSize,
      maxCharBufSize = maxCharBufSize, preferredBufSize = preferredBufSize, preferredCharBufSize = preferredCharBufSize,
      checkForEndOfInput = checkForEndOfInput, hexDumpSize = hexDumpSize, allowComments = allowComments)
}

object ReaderConfig extends ReaderConfig(preferredBufSize = 32768, preferredCharBufSize = 4096, maxBufSize = 33554432,
  maxCharBufSize = 4194304, throwReaderExceptionWithStackTrace = false, appendHexDumpToParseException = true,
  checkForEndOfInput = true, hexDumpSize = 2, allowComments = false)