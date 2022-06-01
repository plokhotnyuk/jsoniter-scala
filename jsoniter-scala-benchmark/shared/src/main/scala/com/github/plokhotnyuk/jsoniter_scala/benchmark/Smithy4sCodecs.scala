package com.github.plokhotnyuk.jsoniter_scala.benchmark

import smithy4s.http.json._
import smithy4s.{ByteArray, Schema}
import smithy4s.api.Discriminated
import smithy4s.schema.Schema._

object Smithy4sCodecs {
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
  implicit val anyValsJCodec: JCodec[AnyVals] = JCodec.deriveJCodecFromSchema(struct(
    byte.required[AnyVals]("b", _.b.a),
    short.required[AnyVals]("s", _.s.a),
    int.required[AnyVals]("i", _.i.a),
    long.required[AnyVals]("l", _.l.a),
    boolean.required[AnyVals]("bl", _.bl.a),
    string.required[AnyVals]("ch", _.ch.a.toString),
    double.required[AnyVals]("dbl", _.dbl.a),
    float.required[AnyVals]("f", _.f.a)
  )((b, s, i, l, bl, st, dbl, f) => AnyVals(ByteVal(b), ShortVal(s), IntVal(i), LongVal(l), BooleanVal(bl),
    CharVal({
      if (st.length == 1) st.charAt(0)
      else sys.error("illegal char")
    }), DoubleVal(dbl), FloatVal(f))))
  val base64JCodec: JCodec[Array[Byte]] =
    JCodec.deriveJCodecFromSchema(bijection[ByteArray, Array[Byte]](bytes, _.array, ByteArray.apply))
  val bigDecimalJCodec: JCodec[BigDecimal] = JCodec.deriveJCodecFromSchema(bigdecimal)
  val bigIntJCodec: JCodec[BigInt] = JCodec.deriveJCodecFromSchema(bigint)
  implicit val extractFieldsJCodec: JCodec[ExtractFields] = JCodec.deriveJCodecFromSchema(struct(
    string.required[ExtractFields]("s", _.s),
    int.required[ExtractFields]("i", _.i),
  )(ExtractFields.apply))
  implicit val googleMapsAPIJCodec: JCodec[GoogleMapsAPI.DistanceMatrix] = JCodec.deriveJCodecFromSchema({
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
      GoogleMapsAPI.DistanceMatrix(destination_addresses.toVector, origin_addresses.toVector, rows.toVector, status)
    }
  })
  val intJCodec: JCodec[Int] = JCodec.deriveJCodecFromSchema(int)
  implicit val listOfBooleansJCodec: JCodec[List[Boolean]] = JCodec.deriveJCodecFromSchema(list(boolean))
  implicit val mapOfIntsToBooleansJCodec: JCodec[Map[Int, Boolean]] = JCodec.deriveJCodecFromSchema(map(int, boolean))
  implicit val missingRequiredFieldsJCodec: JCodec[MissingRequiredFields] =
    JCodec.deriveJCodecFromSchema(struct(
      string.required[MissingRequiredFields]("s", _.s),
      int.required[MissingRequiredFields]("i", _.i),
    )(MissingRequiredFields.apply))
  val nestedStructsSchema: Schema[NestedStructs] =
    recursive(struct(nestedStructsSchema.optional[NestedStructs]("n", _.n))(NestedStructs.apply))
  implicit val nestedStructsJCodec: JCodec[NestedStructs] = JCodec.deriveJCodecFromSchema(nestedStructsSchema)
  implicit val primitivesJCodec: JCodec[Primitives] = JCodec.deriveJCodecFromSchema(struct(
    byte.required[Primitives]("b", _.b),
    short.required[Primitives]("s", _.s),
    int.required[Primitives]("i", _.i),
    long.required[Primitives]("l", _.l),
    boolean.required[Primitives]("bl", _.bl),
    string.required[Primitives]("ch", _.ch.toString),
    double.required[Primitives]("dbl", _.dbl),
    float.required[Primitives]("f", _.f)
  )((b, s, i, l, bl, st, dbl, f) => Primitives(b, s, i, l, bl, {
    if (st.length == 1) st.charAt(0)
    else sys.error("illegal char")
  }, dbl, f)))
  implicit val setOfIntsJCodec: JCodec[Set[Int]] = JCodec.deriveJCodecFromSchema(set(int))
  val stringJCodec: JCodec[String] = JCodec.deriveJCodecFromSchema(string)
  implicit val vectorOfBooleansJCodec: JCodec[Vector[Boolean]] =
    JCodec.deriveJCodecFromSchema(bijection[List[Boolean], Vector[Boolean]](list(boolean), _.toVector, _.toList))
}
