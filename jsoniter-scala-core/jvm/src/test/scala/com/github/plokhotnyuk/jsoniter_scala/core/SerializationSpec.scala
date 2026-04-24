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
