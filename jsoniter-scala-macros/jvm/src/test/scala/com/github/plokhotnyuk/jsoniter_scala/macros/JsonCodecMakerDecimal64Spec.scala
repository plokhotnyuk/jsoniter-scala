package com.github.plokhotnyuk.jsoniter_scala.macros

import com.epam.deltix.dfp.{Decimal64, Decimal64Utils}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

class JsonCodecMakerDecimal64Spec extends VerifyingSpec {
  import Decimal64Codec._

  "Decimal64Codec" should {
    "deserialize both numeric and string representation of numbers to canonized Decimal64 values" in {
      verifyDeser(make[List[Decimal64]],
        List(Decimal64.ONE_TENTH, Decimal64.MILLION, Decimal64.fromLong(4503599627370497L), Decimal64.parse("1.23456789e+309"), Decimal64.MAX_VALUE),
        """[0.1,"001000000",4503599627370497,1.23456789e+309,"9999999999999999e+369"]""")
    }
    "serialize Decimal64 values into numeric or string representation depending on number of mantissa bits of canonized in-memory representation" in {
      verifySer(make[List[Decimal64]],
        List(Decimal64.ONE_TENTH, Decimal64.MILLION, Decimal64.fromLong(4503599627370497L), Decimal64.parse("1.23456789e+309"), Decimal64.MAX_VALUE),
        """[0.1,1E+6,"4503599627370497","1.23456789E+309","9.999999999999999E+384"]""")
    }
  }
}

object Decimal64Codec {
  implicit val codec: JsonValueCodec[Decimal64] = new JsonValueCodec[Decimal64] {
    override def decodeValue(in: JsonReader, default: Decimal64): Decimal64 =
      Decimal64.fromUnderlying(Decimal64Utils.canonize(Decimal64Utils.fromBigDecimal((if (in.isNextToken('"')) {
        in.rollbackToken()
        in.readStringAsBigDecimal(null)
      } else {
        in.rollbackToken()
        in.readBigDecimal(null)
      }).bigDecimal)))

    override def encodeValue(x: Decimal64, out: JsonWriter): Unit = {
      val cx = Decimal64Utils.canonize(Decimal64.toUnderlying(x))
      val bd = new BigDecimal(Decimal64Utils.toBigDecimal(cx))
      val m = Decimal64Utils.getUnscaledValue(cx)
      val s = Decimal64Utils.getScale(cx)
      if (m >= -4503599627370496L && m <= 4503599627370496L && s >= -256 && s <= 256) out.writeVal(bd)
      else out.writeValAsString(bd)
    }

    override def nullValue: Decimal64 = null
  }
}
