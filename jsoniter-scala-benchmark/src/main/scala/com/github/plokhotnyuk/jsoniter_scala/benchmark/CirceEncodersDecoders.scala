package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._

import scala.util.control.NonFatal

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true)
  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
  implicit val config: Configuration = Configuration.default.withDiscriminator("type")
  implicit val adtEncoder: Encoder[ADTBase] = {
    implicit val aEncoder: Encoder[X] = deriveEncoder
    implicit val bEncoder: Encoder[Y] = deriveEncoder
    implicit val cEncoder: Encoder[Z] = deriveEncoder
    deriveEncoder
  }
  implicit val adtDecoder: Decoder[ADTBase] = {
    implicit val aDecoder: Decoder[X] = deriveDecoder
    implicit val bDecoder: Decoder[Y] = deriveDecoder
    implicit val cDecoder: Decoder[Z] = deriveDecoder
    deriveDecoder
  }
  implicit val anyValsEncoder: Encoder[AnyVals] = {
    implicit val byteValEncoder: Encoder[ByteVal] = deriveUnwrappedEncoder
    implicit val shortValEncoder: Encoder[ShortVal] = deriveUnwrappedEncoder
    implicit val intValEncoder: Encoder[IntVal] = deriveUnwrappedEncoder
    implicit val longValEncoder: Encoder[LongVal] = deriveUnwrappedEncoder
    implicit val booleanValEncoder: Encoder[BooleanVal] = deriveUnwrappedEncoder
    implicit val charValEncoder: Encoder[CharVal] = deriveUnwrappedEncoder
    implicit val doubleValEncoder: Encoder[DoubleVal] = deriveUnwrappedEncoder
    implicit val floatValEncoder: Encoder[FloatVal] = deriveUnwrappedEncoder
    deriveEncoder
  }
  implicit val anyValsDecoder: Decoder[AnyVals] = {
    implicit val byteValDecoder: Decoder[ByteVal] = deriveUnwrappedDecoder
    implicit val shortValDecoder: Decoder[ShortVal] = deriveUnwrappedDecoder
    implicit val intValDecoder: Decoder[IntVal] = deriveUnwrappedDecoder
    implicit val longValDecoder: Decoder[LongVal] = deriveUnwrappedDecoder
    implicit val booleanValDecoder: Decoder[BooleanVal] = deriveUnwrappedDecoder
    implicit val charValDecoder: Decoder[CharVal] = deriveUnwrappedDecoder
    implicit val doubleValDecoder: Decoder[DoubleVal] = deriveUnwrappedDecoder
    implicit val floatValDecoder: Decoder[FloatVal] = deriveUnwrappedDecoder
    deriveDecoder
  }
  implicit val bigIntEncoder: Encoder[BigInt] = Encoder.encodeJsonNumber
    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val enumEncoder: Encoder[SuitEnum] = Encoder.enumEncoder(SuitEnum)
  implicit val enumDecoder: Decoder[SuitEnum] = Decoder.enumDecoder(SuitEnum)
  implicit val suitEncoder: Encoder[Suit] = Encoder.encodeString.contramap(_.name)
  implicit val suitDecoder: Decoder[Suit] = Decoder.decodeString.emap { str =>
    try Right(Suit.valueOf(str)) catch {
      case NonFatal(_) => Left("Suit")
    }
  }
  implicit val geometryEncoder: Encoder[Geometry] = {
    implicit val pointEncoder: Encoder[Point] = deriveEncoder
    implicit val multiPointEncoder: Encoder[MultiPoint] = deriveEncoder
    implicit val lineStringEncoder: Encoder[LineString] = deriveEncoder
    implicit val multiLineStringEncoder: Encoder[MultiLineString] = deriveEncoder
    implicit val polygonEncoder: Encoder[Polygon] = deriveEncoder
    implicit val multiPolygonEncoder: Encoder[MultiPolygon] = deriveEncoder
    implicit val geometryCollectionEncoder: Encoder[GeometryCollection] = deriveEncoder
    deriveEncoder
  }
  implicit val geometryDecoder: Decoder[Geometry] = {
    implicit val pointDecoder: Decoder[Point] = deriveDecoder
    implicit val multiPointDecoder: Decoder[MultiPoint] = deriveDecoder
    implicit val lineStringDecoder: Decoder[LineString] = deriveDecoder
    implicit val multiLineStringDecoder: Decoder[MultiLineString] = deriveDecoder
    implicit val polygonDecoder: Decoder[Polygon] = deriveDecoder
    implicit val multiPolygonDecoder: Decoder[MultiPolygon] = deriveDecoder
    implicit val geometryCollectionDecoder: Decoder[GeometryCollection] = deriveDecoder
    deriveDecoder
  }
  implicit val geoJSONEncoder: Encoder[GeoJSON] = {
    implicit val featureEncoder: Encoder[Feature] = deriveEncoder
    implicit val featureCollectionEncoder: Encoder[FeatureCollection] = deriveEncoder
    deriveEncoder
  }
  implicit val geoJSONDecoder: Decoder[GeoJSON] = {
    implicit val featureDecoder: Decoder[Feature] = deriveDecoder
    implicit val featureCollectionDecoder: Decoder[FeatureCollection] = deriveDecoder
    deriveDecoder
  }
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
    Decoder[String].emap(s => rie.from(s).fold[Either[String, A]](Left("enum"))(c => Right(gen.from(c))))
}