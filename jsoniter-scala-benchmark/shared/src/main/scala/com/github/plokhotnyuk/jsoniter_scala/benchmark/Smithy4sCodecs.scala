package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.core._
import smithy4s.http.json._
import smithy4s.Schema
import smithy4s.api.Discriminated
import smithy4s.schema.Schema._

object Smithy4sCodecs {
  val exceptionWithoutDumpConfig: ReaderConfig = ReaderConfig.withAppendHexDumpToParseException(false)
  val exceptionWithStacktraceConfig: ReaderConfig = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)
  val tooLongStringConfig: ReaderConfig = ReaderConfig.withPreferredCharBufSize(1024 * 1024)
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2).withPreferredBufSize(32768)
  implicit val adtSchema: Schema[ADTBase] = recursive {
    val xAlt = struct(int.required[X]("a", _.a))(X.apply).oneOf[ADTBase]("X")
    val yAlt = struct(string.required[Y]("b", _.b))(Y.apply).oneOf[ADTBase]("Y")
    val zAlt = struct(adtSchema.required[Z]("l", _.l), adtSchema.required[Z]("r", _.r))(Z.apply).oneOf[ADTBase]("Z")
    union(xAlt, yAlt, zAlt) {
      case x: X => xAlt(x)
      case y: Y => yAlt(y)
      case z: Z => zAlt(z)
    }.addHints(Discriminated("type"))
  }
  implicit val adtJCodec: JCodec[ADTBase] = JCodec.deriveJCodecFromSchema(adtSchema)
  val anyValsSchema: Schema[AnyVals] =
    struct(
      byte.required[AnyVals]("b", _.b.a).addHints(smithy.api.Required()),
      short.required[AnyVals]("s", _.s.a).addHints(smithy.api.Required()),
      int.required[AnyVals]("i", _.i.a).addHints(smithy.api.Required()),
      long.required[AnyVals]("l", _.l.a).addHints(smithy.api.Required()),
      boolean.required[AnyVals]("bl", _.bl.a).addHints(smithy.api.Required()),
      string.required[AnyVals]("ch", _.ch.a.toString).addHints(smithy.api.Required()),
      double.required[AnyVals]("dbl", _.dbl.a).addHints(smithy.api.Required()),
      float.required[AnyVals]("f", _.f.a).addHints(smithy.api.Required())
    )((b, s, i, l, bl, st, dbl, f) => AnyVals.apply(ByteVal(b), ShortVal(s), IntVal(i), LongVal(l), BooleanVal(bl),
      CharVal({
        if (st.length == 1) st.charAt(0)
        else sys.error("illegal char")
      }), DoubleVal(dbl), FloatVal(f)))
  implicit val anyValsJCodec: JCodec[AnyVals] = JCodec.deriveJCodecFromSchema(anyValsSchema)
  val extractFieldsSchema: Schema[ExtractFields] =
    struct(
      string.required[ExtractFields]("s", _.s).addHints(smithy.api.Required()),
      int.required[ExtractFields]("i", _.i).addHints(smithy.api.Required()),
    )((s, i) => ExtractFields.apply(s, i))
  implicit val extractFieldsJCodec: JCodec[ExtractFields] =
    JCodec.deriveJCodecFromSchema(extractFieldsSchema)
  val googleMapsAPISchema: Schema[GoogleMapsAPI.DistanceMatrix] = {
    val valueSchema: Schema[GoogleMapsAPI.Value] =
      struct(
        string.required[GoogleMapsAPI.Value]("text", _.text),
        int.required[GoogleMapsAPI.Value]("value", _.value),
      )(GoogleMapsAPI.Value.apply)
    val elementsSchema: Schema[GoogleMapsAPI.Elements] =
      struct(
        valueSchema.required[GoogleMapsAPI.Elements]("distance", _.distance),
        valueSchema.required[GoogleMapsAPI.Elements]("duration", _.duration),
        string.required[GoogleMapsAPI.Elements]("status", _.status)
      )(GoogleMapsAPI.Elements.apply)
    val rowsSchema: Schema[GoogleMapsAPI.Rows] =
      struct(
        list(elementsSchema).required[GoogleMapsAPI.Rows]("elements", _.elements.toList),
      )(elements => GoogleMapsAPI.Rows.apply(elements.toVector))
    struct(
      list(string).required[GoogleMapsAPI.DistanceMatrix]("destination_addresses", _.destination_addresses.toList),
      list(string).required[GoogleMapsAPI.DistanceMatrix]("origin_addresses", _.origin_addresses.toList),
      list(rowsSchema).required[GoogleMapsAPI.DistanceMatrix]("rows", _.rows.toList),
      string.required[GoogleMapsAPI.DistanceMatrix]("status", _.status),
    ) { (destination_addresses, origin_addresses, rows, status) =>
      GoogleMapsAPI.DistanceMatrix.apply(
        destination_addresses.toVector,
        origin_addresses.toVector,
        rows.toVector,
        status
      )
    }
  }
  implicit val googleMapsAPIJCodec: JCodec[GoogleMapsAPI.DistanceMatrix] =
    JCodec.deriveJCodecFromSchema(googleMapsAPISchema)
  val intJCodec: JCodec[Int] = JCodec.deriveJCodecFromSchema(int)
  val listOfBooleansSchema: Schema[List[Boolean]] = list(boolean)
  implicit val listOfBooleansJCodec: JCodec[List[Boolean]] = JCodec.deriveJCodecFromSchema(listOfBooleansSchema)
  val mapOfIntsToBooleansSchema: Schema[Map[Int, Boolean]] = map(int, boolean)
  implicit val mapOfIntsToBooleansJCodec: JCodec[Map[Int, Boolean]] =
    JCodec.deriveJCodecFromSchema(mapOfIntsToBooleansSchema)
  val missingRequiredFieldsSchema: Schema[MissingRequiredFields] =
    struct(
      string.required[MissingRequiredFields]("s", _.s).addHints(smithy.api.Required()),
      int.required[MissingRequiredFields]("i", _.i).addHints(smithy.api.Required()),
    )((s, i) => MissingRequiredFields.apply(s, i))
  implicit val missingRequiredFieldsJCodec: JCodec[MissingRequiredFields] =
    JCodec.deriveJCodecFromSchema(missingRequiredFieldsSchema)
  val nestedStructsSchema: Schema[NestedStructs] =
    recursive(struct(nestedStructsSchema.optional[NestedStructs]("n", _.n))(NestedStructs.apply))
  implicit val nestedStructsJCodec: JCodec[NestedStructs] = JCodec.deriveJCodecFromSchema(nestedStructsSchema)
  val primitivesSchema: Schema[Primitives] =
    struct(
      byte.required[Primitives]("b", _.b).addHints(smithy.api.Required()),
      short.required[Primitives]("s", _.s).addHints(smithy.api.Required()),
      int.required[Primitives]("i", _.i).addHints(smithy.api.Required()),
      long.required[Primitives]("l", _.l).addHints(smithy.api.Required()),
      boolean.required[Primitives]("bl", _.bl).addHints(smithy.api.Required()),
      string.required[Primitives]("ch", _.ch.toString).addHints(smithy.api.Required()),
      double.required[Primitives]("dbl", _.dbl).addHints(smithy.api.Required()),
      float.required[Primitives]("f", _.f).addHints(smithy.api.Required())
    )((b, s, i, l, bl, st, dbl, f) => Primitives.apply(b, s, i, l, bl, {
      if (st.length == 1) st.charAt(0)
      else sys.error("illegal char")
    }, dbl, f))
  implicit val primitivesJCodec: JCodec[Primitives] = JCodec.deriveJCodecFromSchema(primitivesSchema)
  val setOfIntsSchema: Schema[Set[Int]] = set(int)
  implicit val setOfIntsJCodec: JCodec[Set[Int]] = JCodec.deriveJCodecFromSchema(setOfIntsSchema)
  val vectorOfBooleansSchema: Schema[Vector[Boolean]] =
    bijection[List[Boolean], Vector[Boolean]](list(boolean), _.toVector, _.toList)
  implicit val vectorOfBooleansJCodec: JCodec[Vector[Boolean]] = JCodec.deriveJCodecFromSchema(vectorOfBooleansSchema)
}
