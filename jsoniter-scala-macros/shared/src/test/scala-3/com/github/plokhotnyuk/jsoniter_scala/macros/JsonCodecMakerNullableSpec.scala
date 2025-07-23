package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

case class NullableProperty(a: Int | Null)

given nullableValueCodec: JsonValueCodec[Int | Null] = new JsonValueCodec[Int | Null] {
  def decodeValue(in: JsonReader, default: Int | Null): Int | Null = {
    if (in.isNextToken('n')) {
      in.rollbackToken()
      in.readRawValAsBytes()
      null
    } else {
      in.rollbackToken()
      in.readInt()
    }
  }

  def encodeValue(x: Int | Null, out: JsonWriter): Unit = {
    if (x == null) {
      out.writeNull()
    } else {
      out.writeVal(x.asInstanceOf[Int])
    }
  }

  val nullValue: Int | Null = null
}

// (CodecMakerConfig.withDiscriminatorFieldName(None))
class JsonCodecMakerNullableSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize case class with nullable values (default behavior)" in {
      verifySerDeser(make[NullableProperty], NullableProperty(a = null),
        """{"a":null}""")
    }
    "serialize and deserialize case class with nullable values (transient null behavior)" in {
      verifySerDeser(make[NullableProperty](CodecMakerConfig.withTransientNull(true)), NullableProperty(a = null),
        """{}""")
    }
  }
}