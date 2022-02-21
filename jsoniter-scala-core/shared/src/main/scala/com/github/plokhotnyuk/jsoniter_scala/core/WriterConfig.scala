package com.github.plokhotnyuk.jsoniter_scala.core

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter]] that contains params for formatting of
  * output JSON and for tuning of preferred size for internal byte buffer that is created on the writer instantiation
  * and reused in runtime for serialization of messages using [[java.io.OutputStream]] or [[java.nio.DirectByteBuffer]].
  * <br/>
  * All configuration params already initialized to default values, but in some cases they should be altered:
  * <ul>
  * <li>turn on pretty printing by specifying of indention step that is greater than 0</li>
  * <li>turn on escaping of Unicode characters to serialize with only ASCII characters</li>
  * <li>increase preferred size of an internal byte buffer to reduce allocation rate of grown and then reduced buffers
  * when writing to [[java.io.OutputStream]] or [[java.nio.DirectByteBuffer]] lot of large (>16Kb)
  * [[scala.math.BigDecimal]], [[scala.math.BigInt]] or other non escaped ASCII strings written using
  * `JsonWriter.writeNonEscapedAsciiKey` or `JsonWriter.writeNonEscapedAsciiVal` </li>
  * </ul>
  * @param throwWriterExceptionWithStackTrace a flag that allows to turn on a stack traces for debugging purposes in
  *                                           development
  * @param indentionStep a size of indention for pretty-printed formatting or 0 for compact output
  * @param escapeUnicode a flag to turn on hexadecimal escaping of all non-ASCII chars
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when writing to
  *                         [[java.io.OutputStream]] or [[java.nio.DirectByteBuffer]]
  */
class WriterConfig private (
                             val throwWriterExceptionWithStackTrace: Boolean,
                             val indentionStep: Int,
                             val escapeUnicode: Boolean,
                             val preferredBufSize: Int) extends Serializable {
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

  private[this] def copy(throwWriterExceptionWithStackTrace: Boolean = throwWriterExceptionWithStackTrace,
                         indentionStep: Int = indentionStep,
                         escapeUnicode: Boolean = escapeUnicode,
                         preferredBufSize: Int = preferredBufSize): WriterConfig =
    new WriterConfig(
      throwWriterExceptionWithStackTrace = throwWriterExceptionWithStackTrace,
      indentionStep = indentionStep,
      escapeUnicode = escapeUnicode,
      preferredBufSize = preferredBufSize)
}

object WriterConfig extends WriterConfig(
  throwWriterExceptionWithStackTrace = false,
  indentionStep = 0,
  escapeUnicode = false,
  preferredBufSize = 32768)