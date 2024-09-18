//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.30.11"

import com.github.plokhotnyuk.jsoniter_scala.core._

val jsonCodec: JsonValueCodec[Unit] = new JsonValueCodec[Unit] {
  override def decodeValue(in: JsonReader, default: Unit): Unit = decode(in, 1024) // Max depth is 1024

  override def encodeValue(x: Unit, out: JsonWriter): Unit = ()

  override def nullValue: Unit = ()

  private[this] def decode(in: JsonReader, depth: Int): Unit = {
    val b = in.nextToken()
    if (b == '"') {
      in.rollbackToken()
      val _ = in.readStringAsCharBuf()
    } else if (b == 'f' || b == 't') {
      in.rollbackToken()
      val _ = in.readBoolean()
    } else if ((b >= '0' && b <= '9') || b == '-') {
      in.rollbackToken()
      val _ = in.readFloat()
    } else if (b == '[') {
      val depthM1 = depth - 1
      if (depthM1 < 0) in.decodeError("depth limit exceeded")
      if (!in.isNextToken(']')) {
        in.rollbackToken()
        while ({
          decode(in, depthM1)
          in.isNextToken(',')
        }) ()
        if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
      }
    } else if (b == '{') {
      val depthM1 = depth - 1
      if (depthM1 < 0) in.decodeError("depth limit exceeded")
      if (!in.isNextToken('}')) {
        in.rollbackToken()
        while ({
          in.readKeyAsCharBuf()
          decode(in, depthM1)
          in.isNextToken(',')
        }) ()
        if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
      }
    } else in.readNullOrError(nullValue, "expected JSON value")
  }
}

try readFromStream(System.in)(jsonCodec) catch {
  case ex: Throwable => ex.printStackTrace(System.err)
}
