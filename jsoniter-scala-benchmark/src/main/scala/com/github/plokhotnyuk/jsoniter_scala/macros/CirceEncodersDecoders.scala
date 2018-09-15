package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time.{MonthDay, Year, ZoneOffset}

import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._

import scala.util.control.NonFatal

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true)
  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
  implicit val config: Configuration = Configuration.default.withDiscriminator("type")
  implicit val aEncoder: Encoder[A] = deriveEncoder
  implicit val aDecoder: Decoder[A] = deriveDecoder
  implicit val bEncoder: Encoder[B] = deriveEncoder
  implicit val bDecoder: Decoder[B] = deriveDecoder
  implicit val cEncoder: Encoder[C] = deriveEncoder
  implicit val cDecoder: Decoder[C] = deriveDecoder
  implicit val adtEncoder: Encoder[ADTBase] = deriveEncoder
  implicit val adtDecoder: Decoder[ADTBase] = deriveDecoder
  implicit val enumEncoder: Encoder[SuitEnum] = Encoder.enumEncoder(SuitEnum)
  implicit val enumDecoder: Decoder[SuitEnum] = Decoder.enumDecoder(SuitEnum)
  implicit val suitEncoder: Encoder[Suit] = Encoder.encodeString.contramap(_.name)
  implicit val suitDecoder: Decoder[Suit] = Decoder.decodeString.emap { str =>
    try Right(Suit.valueOf(str)) catch {
      case NonFatal(_) => Left("Suit")
    }
  }
  implicit val monthDayEncoder: Encoder[MonthDay] = Encoder.encodeString.contramap(_.toString)
  implicit val monthDayDecoder: Decoder[MonthDay] = Decoder.decodeString.emap { str =>
    try Right(MonthDay.parse(str)) catch {
      case NonFatal(_) => Left("MonthDay")
    }
  }
  implicit val yearEncoder: Encoder[Year] = Encoder.encodeString.contramap(_.toString)
  implicit val yearDecoder: Decoder[Year] = Decoder.decodeString.emap { str =>
    try Right(Year.parse(str)) catch {
      case NonFatal(_) => Left("Year")
    }
  }
  implicit val zoneOffsetEncoder: Encoder[ZoneOffset] = Encoder.encodeString.contramap(_.toString)
  implicit val zoneOffsetDecoder: Decoder[ZoneOffset] = Decoder.decodeString.emap { str =>
    try Right(ZoneOffset.of(str)) catch {
      case NonFatal(_) => Left("ZoneOffset")
    }
  }
  // GeoJSON
  implicit val featureEncoder: Encoder[Feature] = deriveEncoder
  implicit val featureDecoder: Decoder[Feature] = deriveDecoder
  implicit val featureCollectionEncoder: Encoder[FeatureCollection] = deriveEncoder
  implicit val featureCollectionDecoder: Decoder[FeatureCollection] = deriveDecoder
  implicit val geoJSONEncoder: Encoder[GeoJSON] = deriveEncoder
  implicit val geoJSONDecoder: Decoder[GeoJSON] = deriveDecoder
  implicit val pointEncoder: Encoder[Point] = deriveEncoder
  implicit val pointDecoder: Decoder[Point] = deriveDecoder
  implicit val multiPointEncoder: Encoder[MultiPoint] = deriveEncoder
  implicit val multiPointDecoder: Decoder[MultiPoint] = deriveDecoder
  implicit val lineStringEncoder: Encoder[LineString] = deriveEncoder
  implicit val lineStringDecoder: Decoder[LineString] = deriveDecoder
  implicit val multiLineStringEncoder: Encoder[MultiLineString] = deriveEncoder
  implicit val multiLineStringDecoder: Decoder[MultiLineString] = deriveDecoder
  implicit val polygonEncoder: Encoder[Polygon] = deriveEncoder
  implicit val polygonDecoder: Decoder[Polygon] = deriveDecoder
  implicit val multiPolygonEncoder: Encoder[MultiPolygon] = deriveEncoder
  implicit val multiPolygonDecoder: Decoder[MultiPolygon] = deriveDecoder
  implicit val geometryCollectionEncoder: Encoder[GeometryCollection] = deriveEncoder
  implicit val geometryCollectionDecoder: Decoder[GeometryCollection] = deriveDecoder
  implicit val geometryEncoder: Encoder[Geometry] = deriveEncoder
  implicit val geometryDecoder: Decoder[Geometry] = deriveDecoder
  // Derivation for Enum ADTs borrowed from:
  // https://stackoverflow.com/questions/37011894/circe-instances-for-encoding-decoding-sealed-trait-instances-of-arity-0
  import shapeless._
  import shapeless.labelled.{ FieldType, field }

  trait IsEnum[C <: Coproduct] {
    def to(c: C): String

    def from(s: String): Option[C]
  }

  object IsEnum {
    implicit val cnilIsEnum: IsEnum[CNil] = new IsEnum[CNil] {
      def to(c: CNil): String = sys.error("Impossible")

      def from(s: String): Option[CNil] = None
    }

    implicit def cconsIsEnum[K <: Symbol, H <: Product, T <: Coproduct](implicit witK: Witness.Aux[K],
                                                                        witH: Witness.Aux[H],
                                                                        gen: Generic.Aux[H, HNil],
                                                                        tie: IsEnum[T]): IsEnum[FieldType[K, H] :+: T] =
      new IsEnum[FieldType[K, H] :+: T] {
        def to(c: FieldType[K, H] :+: T): String = c match {
          case Inl(h) => witK.value.name
          case Inr(t) => tie.to(t)
        }

        def from(s: String): Option[FieldType[K, H] :+: T] =
          if (s == witK.value.name) Some(Inl(field[K](witH.value)))
          else tie.from(s).map(Inr(_))
      }
  }

  implicit def encodeEnum[A, C <: Coproduct](implicit gen: LabelledGeneric.Aux[A, C], rie: IsEnum[C]): Encoder[A] =
    Encoder[String].contramap[A](a => rie.to(gen.to(a)))

  implicit def decodeEnum[A, C <: Coproduct](implicit gen: LabelledGeneric.Aux[A, C], rie: IsEnum[C]): Decoder[A] =
    Decoder[String].emap(s => rie.from(s).map(gen.from).fold[Either[String, A]](Left("enum"))(Right.apply))
}