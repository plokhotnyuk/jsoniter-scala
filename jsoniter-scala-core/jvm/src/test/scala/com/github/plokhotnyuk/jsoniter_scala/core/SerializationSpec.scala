package com.github.plokhotnyuk.jsoniter_scala.core

import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import java.io._

class SerializationSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "JsonValueCodec" should {
    "be serializable" in {
      readFromStream(new ByteArrayInputStream(prettyJson))(serDeser(codec)) shouldBe user
    }
  }
  "JsonKeyCodec" should {
    "be serializable" in {
      val out = new ByteArrayOutputStream()
      val codec: JsonKeyCodec[Int] = new JsonKeyCodec[Int] {
        override def decodeKey(in: JsonReader): Int = in.readKeyAsInt()
        override def encodeKey(x: Int, out: JsonWriter): Unit = out.writeKey(x)
      }
      serDeser(codec) shouldNot be(null)
    }
  }
  "ReaderConfig" should {
    "be serializable" in {
      def check(expected: ReaderConfig): Unit = {
        val actual = serDeser(expected)
        actual.throwReaderExceptionWithStackTrace shouldBe expected.throwReaderExceptionWithStackTrace
        actual.appendHexDumpToParseException shouldBe expected.appendHexDumpToParseException
        actual.preferredBufSize shouldBe expected.preferredBufSize
        actual.preferredCharBufSize shouldBe expected.preferredCharBufSize
        actual.hexDumpSize shouldBe expected.hexDumpSize
      }

      check(ReaderConfig.withThrowReaderExceptionWithStackTrace(true))
      check(ReaderConfig.withAppendHexDumpToParseException(false))
      check(ReaderConfig.withPreferredBufSize(100))
      check(ReaderConfig.withPreferredCharBufSize(100))
      check(ReaderConfig.withHexDumpSize(10))
    }
  }
  "WriterConfig" should {
    "be serializable" in {
      def check(expected: WriterConfig): Unit = {
        val actual = serDeser(expected)
        actual.throwWriterExceptionWithStackTrace shouldBe expected.throwWriterExceptionWithStackTrace
        actual.indentionStep shouldBe expected.indentionStep
        actual.escapeUnicode shouldBe expected.escapeUnicode
        actual.preferredBufSize shouldBe expected.preferredBufSize
      }

      check(WriterConfig.withThrowWriterExceptionWithStackTrace(true))
      check(WriterConfig.withIndentionStep(8))
      check(WriterConfig.withEscapeUnicode(true))
      check(WriterConfig.withPreferredBufSize(100))
    }
  }

  def serDeser[A](x: A): A = {
    val out = new ByteArrayOutputStream()
    new ObjectOutputStream(out).writeObject(x)
    new ObjectInputStream(new ByteArrayInputStream(out.toByteArray)).readObject().asInstanceOf[A]
  }
}
