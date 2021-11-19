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
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when parsing from
  *                         [[java.io.InputStream]]
  * @param preferredCharBufSize a preferred size (in chars) of an internal char buffer for parsing of string values
  * @param checkForEndOfInput a flag to check and raise an error if some non whitespace bytes will be detected after
  *                           successful parsing of the value
  * @param hexDumpSize a size of the hex dump in 16-byte lines before and after the 16-byte line where an error occurs
  */
class ReaderConfig private (
                             val throwReaderExceptionWithStackTrace: Boolean,
                             val appendHexDumpToParseException: Boolean,
                             val preferredBufSize: Int,
                             val preferredCharBufSize: Int,
                             val checkForEndOfInput: Boolean,
                             val hexDumpSize: Int) extends Serializable {
  def withThrowReaderExceptionWithStackTrace(throwReaderExceptionWithStackTrace: Boolean): ReaderConfig =
    copy(throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace)

  def withAppendHexDumpToParseException(appendHexDumpToParseException: Boolean): ReaderConfig =
    copy(appendHexDumpToParseException = appendHexDumpToParseException)

  def withPreferredBufSize(preferredBufSize: Int): ReaderConfig = {
    if (preferredBufSize < 12) throw new IllegalArgumentException("'preferredBufSize' should be not less than 12")
    copy(preferredBufSize = preferredBufSize)
  }

  def withPreferredCharBufSize(preferredCharBufSize: Int): ReaderConfig = {
    if (preferredCharBufSize < 0) throw new IllegalArgumentException("'preferredCharBufSize' should be not less than 0")
    copy(preferredCharBufSize = preferredCharBufSize)
  }

  def withCheckForEndOfInput(checkForEndOfInput: Boolean): ReaderConfig =
    copy(checkForEndOfInput = checkForEndOfInput)

  def withHexDumpSize(hexDumpSize: Int): ReaderConfig = {
    if (hexDumpSize < 1) throw new IllegalArgumentException("'hexDumpSize' should be not less than 1")
    copy(hexDumpSize = hexDumpSize)
  }

  private[this] def copy(throwReaderExceptionWithStackTrace: Boolean = throwReaderExceptionWithStackTrace,
                         appendHexDumpToParseException: Boolean = appendHexDumpToParseException,
                         preferredBufSize: Int = preferredBufSize,
                         preferredCharBufSize: Int = preferredCharBufSize,
                         checkForEndOfInput: Boolean = checkForEndOfInput,
                         hexDumpSize: Int = hexDumpSize): ReaderConfig =
    new ReaderConfig(
      throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace,
      appendHexDumpToParseException = appendHexDumpToParseException,
      preferredBufSize = preferredBufSize,
      preferredCharBufSize = preferredCharBufSize,
      checkForEndOfInput = checkForEndOfInput,
      hexDumpSize = hexDumpSize)
}

object ReaderConfig extends ReaderConfig(
  throwReaderExceptionWithStackTrace = false,
  appendHexDumpToParseException = true,
  preferredBufSize = 16384,
  preferredCharBufSize = 4096,
  checkForEndOfInput = true,
  hexDumpSize = 2)
