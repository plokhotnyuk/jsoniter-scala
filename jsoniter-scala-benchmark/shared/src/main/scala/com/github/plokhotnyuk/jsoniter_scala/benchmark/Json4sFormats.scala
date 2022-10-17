package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import org.json4s._
import java.time._
import java.util.{Base64, UUID}
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.ClassTag

object Json4sFormats {
  implicit val customFormats: Formats =
    new DefaultFormats  {
      override val typeHints: TypeHints =
        ShortTypeHints(List(classOf[ADTBase], classOf[GeoJSON.GeoJSON], classOf[GeoJSON.SimpleGeoJSON]), "type")
    }.skippingEmptyValues +
    StringifiedFormats.stringified[Char](x => if (x.length == 1) x.charAt(0) else sys.error("char")) +
    StringifiedFormats.stringified[Duration](Duration.parse) +
    StringifiedFormats.stringified[Instant](Instant.parse) +
    StringifiedFormats.stringified[LocalDate](LocalDate.parse) +
    StringifiedFormats.stringified[LocalDateTime](LocalDateTime.parse) +
    StringifiedFormats.stringified[LocalTime](LocalTime.parse) +
    StringifiedFormats.stringified[MonthDay](MonthDay.parse) +
    StringifiedFormats.stringified[OffsetDateTime](OffsetDateTime.parse) +
    StringifiedFormats.stringified[OffsetTime](OffsetTime.parse) +
    StringifiedFormats.stringified[Period](Period.parse) +
    StringifiedFormats.stringified[UUID](UUID.fromString) +
    StringifiedFormats.stringified[Year](Year.parse) +
    StringifiedFormats.stringified[YearMonth](YearMonth.parse) +
    StringifiedFormats.stringified[ZonedDateTime](ZonedDateTime.parse) +
    StringifiedFormats.stringified[ZoneId](ZoneId.of) +
    StringifiedFormats.stringified[ZoneOffset](ZoneOffset.of) +
    StringifiedFormats.stringified[Suit](Suit.valueOf) +
    StringifiedFormats.stringified[SuitADT] {
      val suite = Map(
        "Hearts" -> Hearts,
        "Spades" -> Spades,
        "Diamonds" -> Diamonds,
        "Clubs" -> Clubs)
      x => suite(x)
    } +
    StringifiedFormats.stringified[SuitEnum] {
      val ec = new ConcurrentHashMap[String, SuitEnum]
      x => {
        var v = ec.get(x)
        if (v eq null) {
          v = SuitEnum.values.iterator.find(_.toString == x).getOrElse(sys.error("SuitEnum"))
          ec.put(x, v)
        }
        v
      }
    } + new CustomSerializer[Tuple2[Double, Double]](_ => ({
      case JArray(JDouble(x) :: JDouble(y) :: Nil) => new Tuple2[Double, Double](x, y)
    }, {
      case x: Tuple2[Double, Double] => JArray(JDouble(x._1) :: JDouble(x._2) :: Nil)
    })) + new CustomSerializer[ByteVal](_ => ({
      case JInt(x) if x.isValidByte => new ByteVal(x.toByte)
    }, {
      case x: ByteVal => JInt(x.a)
    })) + new CustomSerializer[ShortVal](_ => ({
      case JInt(x) if x.isValidShort => new ShortVal(x.toShort)
    }, {
      case x: ShortVal => JInt(x.a)
    })) + new CustomSerializer[IntVal](_ => ({
      case JInt(x) if x.isValidInt => new IntVal(x.toInt)
    }, {
      case x: IntVal => JInt(x.a)
    })) + new CustomSerializer[LongVal](_ => ({
      case JInt(x) if x.isValidLong => new LongVal(x.toInt)
    }, {
      case x: LongVal => JInt(x.a)
    })) + new CustomSerializer[FloatVal](_ => ({
      case JDecimal(x) => new FloatVal(x.toFloat)
    }, {
      case x: FloatVal => JDecimal(x.a)
    })) + new CustomSerializer[DoubleVal](_ => ({
      case JDouble(x) => new DoubleVal(x.toFloat)
    }, {
      case x: DoubleVal => JDecimal(x.a)
    })) + new CustomSerializer[CharVal](_ => ({
      case JString(x) if x.length == 1 => new CharVal(x.charAt(0))
    }, {
      case x: CharVal => JString(x.a.toString)
    }))
}

object GitHubActionsAPIJson4sFormats {
  implicit val gitHubActionsAPIFormats: Formats = Json4sFormats.customFormats +
    StringifiedFormats.stringified[Boolean](java.lang.Boolean.parseBoolean)
}

object Base64Json4sFormats {
  implicit val base64Formats: Formats = DefaultFormats +
    StringifiedFormats.stringified[Array[Byte]](Base64.getDecoder.decode, Base64.getEncoder.encodeToString)
}

object EscapeUnicodeJson4sFormats {
  implicit val escapeUnicodeFormats: Formats = DefaultFormats.withEscapeUnicode
}

object StringifiedFormats {
  def stringified[A: ClassTag](f: String => A, g: A => String = (x: A) => x.toString): CustomSerializer[A] =
    new CustomSerializer[A](_ => ({
      case JString(s) => f(s)
    }, {
      case x: A => JString(g(x))
    }))
}