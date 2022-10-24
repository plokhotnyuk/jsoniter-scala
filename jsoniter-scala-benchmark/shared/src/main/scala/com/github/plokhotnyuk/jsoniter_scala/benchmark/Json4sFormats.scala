package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.json4s._
import java.time._
import java.util.{Base64, UUID}
import scala.reflect.ClassTag

object Json4sFormats {
  implicit val customFormats: Formats = DefaultFormats +
    StringifiedFormats.stringified[Char](x => if (x.length == 1) x.charAt(0) else sys.error("char")) +
    StringifiedFormats.stringified[UUID](UUID.fromString) +
    StringifiedFormats.stringified[Suit](Suit.valueOf) +
    StringifiedFormats.stringified[SuitADT] {
      val suite = Map(
        "Hearts" -> Hearts,
        "Spades" -> Spades,
        "Diamonds" -> Diamonds,
        "Clubs" -> Clubs)
      x => suite(x)
    } +
    StringifiedFormats.stringified[SuitEnum.SuitEnum] {
      val ec = new java.util.concurrent.ConcurrentHashMap[String, SuitEnum.SuitEnum]
      x => {
        var v = ec.get(x)
        if (v eq null) {
          v = SuitEnum.values.iterator.find(_.toString == x).getOrElse(sys.error("SuitEnum"))
          ec.put(x, v)
        }
        v
      }
    }
}

object BigDecimalJson4sFormat {
  implicit val bigDecimalFormats: Formats = DefaultFormats.withBigDecimal.withBigInt
}

object ADTJson4sFormats {
  implicit val adtFormats: Formats = new DefaultFormats {
    override val typeHints: TypeHints = new SimpleTypeHints(List(classOf[X], classOf[Y], classOf[Z]))
  }
}

object AnyValsJson4sFormats {
  implicit val anyValsFormats: Formats = Json4sFormats.customFormats +
    new CustomSerializer[ByteVal](_ => ({
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

object JavaTimeJson4sFormats {
  implicit val javaTimeFormats: Formats = DefaultFormats +
    StringifiedFormats.stringified[Duration](Duration.parse) +
    StringifiedFormats.stringified[Instant](Instant.parse) +
    StringifiedFormats.stringified[LocalDate](LocalDate.parse) +
    StringifiedFormats.stringified[LocalDateTime](LocalDateTime.parse) +
    StringifiedFormats.stringified[LocalTime](LocalTime.parse) +
    StringifiedFormats.stringified[MonthDay](MonthDay.parse) +
    StringifiedFormats.stringified[OffsetDateTime](OffsetDateTime.parse) +
    StringifiedFormats.stringified[OffsetTime](OffsetTime.parse) +
    StringifiedFormats.stringified[Period](Period.parse) +
    StringifiedFormats.stringified[Year](Year.parse) +
    StringifiedFormats.stringified[YearMonth](YearMonth.parse) +
    StringifiedFormats.stringified[ZonedDateTime](ZonedDateTime.parse) +
    StringifiedFormats.stringified[ZoneId](ZoneId.of) +
    StringifiedFormats.stringified[ZoneOffset](ZoneOffset.of)
}

object GeoJsonJson4sFormats {
  implicit val geoJsonFormats: Formats = new DefaultFormats {
    override val typeHints: TypeHints = new SimpleTypeHints(List(
        classOf[GeoJSON.Point], classOf[GeoJSON.MultiPoint], classOf[GeoJSON.LineString],
        classOf[GeoJSON.MultiLineString], classOf[GeoJSON.Polygon], classOf[GeoJSON.MultiPolygon],
        classOf[GeoJSON.GeometryCollection], classOf[GeoJSON.Feature], classOf[GeoJSON.FeatureCollection]))
  } + new CustomSerializer[Tuple2[Double, Double]](_ => ({
    case JArray(JDouble(x) :: JDouble(y) :: Nil) => new Tuple2[Double, Double](x, y)
  }, {
    case x: Tuple2[Double, Double] => JArray(JDouble(x._1) :: JDouble(x._2) :: Nil)
  }))
}

object GitHubActionsAPIJson4sFormats {
  implicit val gitHubActionsAPIFormats: Formats = Json4sFormats.customFormats +
    StringifiedFormats.stringified[Instant](Instant.parse) +
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
      case js: JString => f(js.s)
    }, {
      case x: A => new JString(g(x))
    }))
}

class SimpleTypeHints(override val hints: List[Class[_]],
                      override val typeHintFieldName: String = "type") extends TypeHints {
  override def hintFor(clazz: Class[_]): Option[String] = new Some(clazz.getSimpleName)

  override def classFor(hint: String, parent: Class[_]): Option[Class[_]] = hints.collectFirst {
    case clazz if clazz.getSimpleName == hint => clazz
  }
}

object Json4sJacksonMappers {
  import com.fasterxml.jackson.core._
  import com.fasterxml.jackson.core.util._
  import com.fasterxml.jackson.core.json.JsonWriteFeature
  import com.fasterxml.jackson.databind._
  import org.json4s.jackson.Json4sScalaModule

  private[this] def mapper(indentOutput: Boolean = false, escapeNonAscii: Boolean = false,
                           useBigNumber: Boolean = false): ObjectMapper = {
    val jsonFactory = new JsonFactoryBuilder()
      .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false)
      .configure(JsonWriteFeature.ESCAPE_NON_ASCII, escapeNonAscii)
      .configure(StreamReadFeature.USE_FAST_DOUBLE_PARSER, true)
      .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER, true)
      .build()
    new ObjectMapper(jsonFactory)
      .registerModule(new Json4sScalaModule)
      .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, useBigNumber)
      .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, useBigNumber)
      .configure(SerializationFeature.INDENT_OUTPUT, indentOutput)
      .setDefaultPrettyPrinter {
        val indenter = new DefaultIndenter("  ", "\n")
        new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter)
      }
  }

  val mapper: ObjectMapper = mapper()
  val bigNumberMapper: ObjectMapper = mapper(useBigNumber = true)
  val prettyPrintMapper: ObjectMapper = mapper(indentOutput = true)
  val escapeNonAsciiMapper: ObjectMapper = mapper(escapeNonAscii = true)
  val jValueType: JavaType = mapper.constructType(classOf[JValue])
}