package com.github.plokhotnyuk.jsoniter_scala.core

import io.circe._
import java.time.OffsetDateTime

object CirceCodecs {
  implicit val offsetDateTimeC3C: Codec[OffsetDateTime] =
    stringCodec("offset date time", _.readOffsetDateTime(_), _.writeVal(_))

  private[this] val pool = new ThreadLocal[(Array[Byte], JsonReader, JsonWriter)] {
    override def initialValue(): (Array[Byte], JsonReader, JsonWriter) = {
      val buf = new Array[Byte](64)
      (buf, new JsonReader(buf, charBuf = new Array[Char](64)), new JsonWriter(buf))
    }
  }

  private[this] def stringCodec[A](name: String, read: (JsonReader, A) => A, write: (JsonWriter, A) => Unit): Codec[A] =
    new JsonValueCodec[A] with Codec[A] {
      def apply(x: A): Json = {
        val (buf, _, writer) = pool.get
        val len = writer.write(this, x, buf, 0, buf.length, WriterConfig)
        Json.fromString(new String(buf, 0, 1, len - 2))
      }

      def apply(c: HCursor): Decoder.Result[A] =
        c.value.asString.fold[Decoder.Result[A]](Left(DecodingFailure(name, c.history))) { s =>
          val (buf, reader, _) = pool.get
          val len = s.length
          var i = 0
          buf(0) = '"'
          while (i < len) {
            buf(i + 1) = s.charAt(i).toByte
            i += 1
          }
          buf(i + 1) = '"'
          try {
            new scala.util.Right(reader.read(this, buf, 0, len + 2, ReaderConfig))
          } catch {
            case _: JsonReaderException => Left(DecodingFailure(name, c.history))
          }
        }

      override def decodeValue(in: JsonReader, default: A): A = read(in, default)

      override def encodeValue(x: A, out: JsonWriter): Unit = write(out, x)

      override val nullValue: A = null.asInstanceOf[A]
    }
}
