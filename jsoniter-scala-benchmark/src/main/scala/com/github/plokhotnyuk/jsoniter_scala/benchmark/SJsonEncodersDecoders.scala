package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.ByteBuffer
import java.time.{Duration, Instant}
import java.util.Base64

import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON.SimpleGeometry
import org.typelevel.jawn.SupportParser
import sjsonnew._

import scala.collection.immutable.{BitSet, IndexedSeq, IntMap}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object SJsonEncodersDecoders extends BasicJsonProtocol {
//  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true, predictSize = true)
//  val prettyPrinter: Printer = Printer.spaces2.copy(dropNullValues = true, reuseWriters = true, predictSize = true)
//  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
//  implicit val config: Configuration = Configuration.default.withDefaults.withDiscriminator("type")
  implicit val adtSjsonFormat: JsonFormat[ADTBase] = new JsonFormat[ADTBase]() {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): ADTBase = jsOpt match {
      case Some(js) =>
        unbuilder.beginObject(js)
        val tpe = unbuilder.readField[String]("type")
        val result = tpe match {
          case "X" =>
            val a = unbuilder.readField[Int]("a")
            X(a)
          case "Y" =>
            val b = unbuilder.readField[String]("b")
            Y(b)
          case "Z" =>
            val l = unbuilder.readField[ADTBase]("l")
            val r = unbuilder.readField[ADTBase]("r")
            Z(l, r)
          case t => 
            deserializationError(s"Unexpected type $tpe")
        }
        unbuilder.endObject()
        result
      case None =>
        deserializationError("Expected JsObject but found None")
    }
  
    override def write[J](obj: ADTBase, builder: Builder[J]): Unit = {
      builder.beginObject()
      obj match {
        case X(a) =>
          builder.addField("type", "X")
          builder.addField("a", a)
        case Y(b) =>
          builder.addField("type", "Y")
          builder.addField("b", b)
        case Z(l, r) =>
          builder.addField("type", "Z")
          builder.addField("l", l)
          builder.addField("r", r)
      }
      builder.endObject()
    }
  }

  implicit val durationSjsonFormat: JsonFormat[Duration] = projectFormat((d: Duration) => d.toString, Duration.parse)
  implicit val instantSjsonFormat: JsonFormat[Instant] = projectFormat((d: Instant) => d.toString, Instant.parse)
  
  implicit val arrayBufferSjsonFormat: RootJsonFormat[ArrayBuffer[Boolean]] = viaSeq[mutable.ArrayBuffer[Boolean], Boolean](s => mutable.ArrayBuffer(s :_*))
//  implicit val anyValsC3c: Codec[AnyVals] = {
//    implicit def valueClassEncoder[A <: AnyVal : UnwrappedEncoder]: Encoder[A] = implicitly
//
//    implicit def valueClassDecoder[A <: AnyVal : UnwrappedDecoder]: Decoder[A] = implicitly
//
//    deriveConfiguredCodec[AnyVals]
//  }
//  val (base64D5r: Decoder[Array[Byte]], base64E5r: Encoder[Array[Byte]]) =
//    (Decoder.decodeString.map[Array[Byte]](Base64.getDecoder.decode),
//      Encoder.encodeString.contramap[Array[Byte]](Base64.getEncoder.encodeToString))
//  implicit val bidRequestC3c: Codec[OpenRTB.BidRequest] = {
//
//    deriveConfiguredCodec[OpenRTB.BidRequest]
//  }
//  implicit val bigIntE5r: Encoder[BigInt] = encodeJsonNumber
//    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val bitSetSjsonFormat: JsonFormat[BitSet] = projectFormat((b: BitSet) => b.toArray[Int], (arr: Array[Int]) => BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is unsafe an option for open systems */)))
//  implicit val (bitSetD5r: Decoder[BitSet], bitSetE5r: Encoder[BitSet]) =
//    (Decoder.decodeArray[Int].map(arr => BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is unsafe an option for open systems */))),
//      Encoder.encodeSet[Int].contramapArray((m: BitSet) => m))
implicit val mutableBitSetSjsonFormat: JsonFormat[mutable.BitSet] = projectFormat((b: mutable.BitSet) => b.toArray[Int], (arr: Array[Int]) => mutable.BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is unsafe an option for open systems */)))
//  implicit val (mutableBitSetD5r: Decoder[mutable.BitSet], mutableBitSetE5r: Encoder[mutable.BitSet]) =
//    (Decoder.decodeArray[Int].map(arr => mutable.BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is unsafe an option for open systems */))),
//      Encoder.encodeSeq[Int].contramapArray((m: mutable.BitSet) => m.toVector))
//  implicit val distanceMatrixC3c: Codec[GoogleMapsAPI.DistanceMatrix] = {
//
//    deriveConfiguredCodec[GoogleMapsAPI.DistanceMatrix]
//  }
//  implicit val gitHubActionsAPIC3c: Codec[GitHubActionsAPI.Response] = {
//    implicit val c1: Codec[GitHubActionsAPI.Artifact] =
//    Codec.forProduct9("id", "node_id", "name", "size_in_bytes", "url", "archive_download_url",
//      "expired", "created_at", "expires_at") {
//      (id: Long, node_id: String, name: String, size_in_bytes: Long, url: String, archive_download_url: String,
//      expired: String, created_at: Instant, expired_at: Instant) =>
//        GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url,
//          expired.toBoolean, created_at, expired_at)
//    } { a =>
//      (a.id, a.node_id, a.name, a.size_in_bytes, a.url, a.archive_download_url,
//      a.expired.toString, a.created_at, a.expires_at)
//    }
//    deriveConfiguredCodec[GitHubActionsAPI.Response]
//  }
//  implicit val extractFieldsC3c: Codec[ExtractFields] = deriveConfiguredCodec[ExtractFields]
//  implicit val geoJSONC3c: Codec[GeoJSON.GeoJSON] = {
//    implicit val c1: Codec[GeoJSON.SimpleGeometry] = deriveConfiguredCodec[GeoJSON.SimpleGeometry]
//    implicit val c2: Codec[GeoJSON.Geometry] = deriveConfiguredCodec[GeoJSON.Geometry]
//    implicit val c3: Codec[GeoJSON.SimpleGeoJSON] = deriveConfiguredCodec[GeoJSON.SimpleGeoJSON]
//    deriveConfiguredCodec[GeoJSON.GeoJSON]
//  }
implicit val gitHubActionsAPISjsonFormat: JsonFormat[GitHubActionsAPI.Response] = {
  implicit val jf1: RootJsonFormat[GitHubActionsAPI.Artifact] = new RootJsonFormat[GitHubActionsAPI.Artifact] {
    override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GitHubActionsAPI.Artifact = jsOpt match {
      case Some(js) =>
        unbuilder.beginObject(js)
        val a = GitHubActionsAPI.Artifact(
          unbuilder.readField[Long]("id"),
          unbuilder.readField[String]("node_id"),
          unbuilder.readField[String]("name"),
          unbuilder.readField[Long]("size_in_bytes"),
          unbuilder.readField[String]("url"),
          unbuilder.readField[String]("archive_download_url"),
          unbuilder.readField[String]("expired").toBoolean,
          unbuilder.readField[Instant]("created_at"),
          unbuilder.readField[Instant]("expires_at")
        )
        unbuilder.endObject()
        a
      case None =>
        deserializationError("Expected JsObject but found None")
        
    }

    override def write[J](obj: GitHubActionsAPI.Artifact, builder: Builder[J]): Unit = {
      builder.beginObject()
      builder.addField("id", obj.id)
      builder.addField("node_id", obj.node_id)
      builder.addField("name", obj.name)
      builder.addField("size_in_bytes", obj.size_in_bytes)
      builder.addField("url", obj.url)
      builder.addField("archive_download_url", obj.archive_download_url)
      builder.addField("expired", obj.expired.toString)
      builder.addField("created_at", obj.created_at.toString)
      builder.addField("expires_at", obj.expires_at.toString)
      builder.endObject()
    }
  }
  isolistFormat(LList.iso(
    { p: GitHubActionsAPI.Response => ("total_count", p.total_count) :*: ("artifacts", p.artifacts) :*: LNil },
    { in: Int :*: Seq[GitHubActionsAPI.Artifact] :*: LNil => GitHubActionsAPI.Response(in.head, in.tail.head) }
  ))
}
  def readADT[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): String = jsOpt match {
    case Some(js) =>
      unbuilder.beginPreObject(js)
      val tpe = unbuilder.lookupField("type") match {
        case Some(value) => unbuilder.readString(value)
        case None => deserializationError("Field not found: type")
      }
      unbuilder.endPreObject()
      tpe
    case None => deserializationError("Expected JsObject but found None")
  }
  def writeADT[J, T <: Product](obj: T, builder: Builder[J]): T = {
    builder.beginPreObject()
    builder.addField("type", obj.productPrefix)
    builder.endPreObject()
    obj
  }
  implicit val geoJSONSjsonFormat: RootJsonFormat[GeoJSON.GeoJSON] = {
    implicit lazy val jf1: JsonFormat[GeoJSON.Point] = isolistFormat(LList.iso(
      { p: GeoJSON.Point => ("coordinates", p.coordinates) :*: LNil },
      { in: (Double, Double) :*: LNil => GeoJSON.Point(in.head) }
    ))
    implicit lazy val jf2: JsonFormat[GeoJSON.MultiPoint] = isolistFormat(LList.iso(
      { p: GeoJSON.MultiPoint => ("coordinates", p.coordinates) :*: LNil },
      { in: IndexedSeq[(Double, Double)] :*: LNil => GeoJSON.MultiPoint(in.head) }
    ))
    implicit lazy val jf3: JsonFormat[GeoJSON.LineString] = isolistFormat(LList.iso(
      { p: GeoJSON.LineString => ("coordinates", p.coordinates) :*: LNil },
      { in: IndexedSeq[(Double, Double)] :*: LNil => GeoJSON.LineString(in.head) }
    ))
    implicit lazy val jf4: JsonFormat[GeoJSON.MultiLineString] = isolistFormat(LList.iso(
      { p: GeoJSON.MultiLineString => ("coordinates", p.coordinates) :*: LNil },
      { in: IndexedSeq[IndexedSeq[(Double, Double)]] :*: LNil => GeoJSON.MultiLineString(in.head) }
    ))
    implicit lazy val jf5: JsonFormat[GeoJSON.Polygon] = new RootJsonFormat[GeoJSON.Polygon] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.Polygon = jsOpt match {
        case Some(js) =>
          unbuilder.beginObject(js)
          val f = GeoJSON.Polygon(
            unbuilder.readField[IndexedSeq[IndexedSeq[(Double, Double)]]]("coordinates")
          )
          unbuilder.endObject()
          f
        case None => deserializationError("Expected JsObject but found None")
      }
      override def write[J](obj: GeoJSON.Polygon, builder: Builder[J]): Unit = {
        builder.beginObject()
        builder.addField("coordinates", obj.coordinates)
        builder.endObject()
      }
    }
    implicit lazy val jf6: JsonFormat[GeoJSON.MultiPolygon] = isolistFormat(LList.iso(
      { p: GeoJSON.MultiPolygon => ("coordinates", p.coordinates) :*: LNil },
      { in: IndexedSeq[IndexedSeq[IndexedSeq[(Double, Double)]]] :*: LNil => GeoJSON.MultiPolygon(in.head) }
    ))
    implicit lazy val jf7: RootJsonFormat[GeoJSON.SimpleGeometry] = new RootJsonFormat[GeoJSON.SimpleGeometry] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.SimpleGeometry = readADT(jsOpt, unbuilder) match {
        case "Point" => jsonReader[GeoJSON.Point].read(jsOpt, unbuilder)
        case "MultiPoint" => jsonReader[GeoJSON.MultiPoint].read(jsOpt, unbuilder)
        case "LineString" => jsonReader[GeoJSON.LineString].read(jsOpt, unbuilder)
        case "MultiLineString" => jsonReader[GeoJSON.MultiLineString].read(jsOpt, unbuilder)
        case "Polygon" => jsonReader[GeoJSON.Polygon].read(jsOpt, unbuilder)
        case "MultiPolygon" => jsonReader[GeoJSON.MultiPolygon].read(jsOpt, unbuilder)
      }

      override def write[J](obj: GeoJSON.SimpleGeometry, builder: Builder[J]): Unit = writeADT(obj, builder) match {
        case x: GeoJSON.Point => jsonWriter[GeoJSON.Point].write(x, builder)
        case x: GeoJSON.MultiPoint => jsonWriter[GeoJSON.MultiPoint].write(x, builder)
        case x: GeoJSON.LineString => jsonWriter[GeoJSON.LineString].write(x, builder)
        case x: GeoJSON.MultiLineString => jsonWriter[GeoJSON.MultiLineString].write(x, builder)
        case x: GeoJSON.Polygon => jsonWriter[GeoJSON.Polygon].write(x, builder)
        case x: GeoJSON.MultiPolygon => jsonWriter[GeoJSON.MultiPolygon].write(x, builder)
      }
      
      
    }
    implicit lazy val jf8: JsonFormat[GeoJSON.GeometryCollection] = isolistFormat(LList.iso(
      { p: GeoJSON.GeometryCollection => ("geometries", p.geometries) :*: LNil },
      { in: IndexedSeq[GeoJSON.SimpleGeometry] :*: LNil => GeoJSON.GeometryCollection(in.head) }
    ))
    implicit lazy val jf9: RootJsonFormat[GeoJSON.Geometry] = new RootJsonFormat[GeoJSON.Geometry] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.Geometry = readADT(jsOpt, unbuilder) match {
        case "Point" => jsonReader[GeoJSON.Point].read(jsOpt, unbuilder)
        case "MultiPoint" => jsonReader[GeoJSON.MultiPoint].read(jsOpt, unbuilder)
        case "LineString" => jsonReader[GeoJSON.LineString].read(jsOpt, unbuilder)
        case "MultiLineString" => jsonReader[GeoJSON.MultiLineString].read(jsOpt, unbuilder)
        case "Polygon" => jsonReader[GeoJSON.Polygon].read(jsOpt, unbuilder)
        case "MultiPolygon" => jsonReader[GeoJSON.MultiPolygon].read(jsOpt, unbuilder)
        case "GeometryCollection" => jsonReader[GeoJSON.GeometryCollection].read(jsOpt, unbuilder)
      }

      override def write[J](obj: GeoJSON.Geometry, builder: Builder[J]): Unit = writeADT(obj, builder) match {
        case x: GeoJSON.Point => jsonWriter[GeoJSON.Point].write(x, builder)
        case x: GeoJSON.MultiPoint => jsonWriter[GeoJSON.MultiPoint].write(x, builder)
        case x: GeoJSON.LineString => jsonWriter[GeoJSON.LineString].write(x, builder)
        case x: GeoJSON.MultiLineString => jsonWriter[GeoJSON.MultiLineString].write(x, builder)
        case x: GeoJSON.Polygon => jsonWriter[GeoJSON.Polygon].write(x, builder)
        case x: GeoJSON.MultiPolygon => jsonWriter[GeoJSON.MultiPolygon].write(x, builder)
        case x: GeoJSON.GeometryCollection => jsonWriter[GeoJSON.GeometryCollection].write(x, builder)
      }
    }
    implicit lazy val jf10: JsonFormat[GeoJSON.Feature] = new RootJsonFormat[GeoJSON.Feature] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.Feature = jsOpt match {
        case Some(js) =>
          unbuilder.beginObject(js)
          val f = GeoJSON.Feature(
            unbuilder.readField[Map[String, String]]("properties"),
            unbuilder.readField[GeoJSON.Geometry]("geometry"),
            unbuilder.readField[Option[(Double, Double, Double, Double)]]("bbox")
          )
          unbuilder.endObject()
          f
        case None => deserializationError("Expected JsObject but found None")
      }
      override def write[J](obj: GeoJSON.Feature, builder: Builder[J]): Unit = {
        builder.beginObject()
        builder.addField("properties", obj.properties)
        builder.addField("geometry", obj.geometry)
        builder.addField("bbox", obj.bbox)
        builder.endObject()
      }
    }
    implicit lazy val jf12: RootJsonFormat[GeoJSON.SimpleGeoJSON] = new RootJsonFormat[GeoJSON.SimpleGeoJSON] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.SimpleGeoJSON = readADT(jsOpt, unbuilder) match {
        case "Feature" => jsonReader[GeoJSON.Feature].read(jsOpt, unbuilder)
      }

      override def write[J](obj: GeoJSON.SimpleGeoJSON, builder: Builder[J]): Unit = writeADT(obj, builder) match {
        case x: GeoJSON.Feature => jsonWriter[GeoJSON.Feature].write(x, builder)
      }
    }
    implicit lazy val jf13: RootJsonFormat[GeoJSON.FeatureCollection] = new RootJsonFormat[GeoJSON.FeatureCollection] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.FeatureCollection = jsOpt match {
        case Some(js) => 
          unbuilder.beginObject(js)
          val f = GeoJSON.FeatureCollection(
            unbuilder.readField[IndexedSeq[GeoJSON.SimpleGeoJSON]]("features"),
            unbuilder.readField[Option[(Double, Double, Double, Double)]]("bbox")
          )
          unbuilder.endObject()
          f
        case None => deserializationError("Expected JsObject but found None")
      }
      override def write[J](obj: GeoJSON.FeatureCollection, builder: Builder[J]): Unit = {
        builder.beginObject()
        builder.addField("features", obj.features)
        builder.addField("bbox", obj.bbox)
        builder.endObject()
      }
    } 
    implicit lazy val jf14: RootJsonFormat[GeoJSON.GeoJSON] = new RootJsonFormat[GeoJSON.GeoJSON] {
      override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): GeoJSON.GeoJSON = readADT(jsOpt, unbuilder) match {
        case "Feature" => jsonReader[GeoJSON.Feature].read(jsOpt, unbuilder)
        case "FeatureCollection" => jsonReader[GeoJSON.FeatureCollection].read(jsOpt, unbuilder)
      }

      override def write[J](obj: GeoJSON.GeoJSON, builder: Builder[J]): Unit = writeADT(obj, builder) match {
        case x: GeoJSON.Feature => jsonWriter[GeoJSON.Feature].write(x, builder)
        case y: GeoJSON.FeatureCollection => jsonWriter[GeoJSON.FeatureCollection].write(y, builder)
      }
    }
    jf14
  }
  implicit val intMapSjsonFormat: JsonFormat[IntMap[Boolean]] = projectFormat[IntMap[Boolean], Map[Int,Boolean]](identity, IntMap.from)
//  implicit val (intMapD5r: Decoder[IntMap[Boolean]], intMapE5r: Encoder[IntMap[Boolean]]) =
//    (Decoder.decodeMap[Int, Boolean].map(_.foldLeft(IntMap.empty[Boolean])((m, p) => m.updated(p._1, p._2))),
//      Encoder.encodeMap[Int, Boolean].contramapObject((m: IntMap[Boolean]) => m))
  /** Supplies the JsonFormat for mutable.Maps. */
  implicit def mutableMapFormat[K: JsonKeyFormat, V: JsonFormat]: RootJsonFormat[mutable.Map[K, V]] = new RootJsonFormat[mutable.Map[K, V]] {
    lazy val keyFormat = implicitly[JsonKeyFormat[K]]
    lazy val valueFormat = implicitly[JsonFormat[V]]
    def write[J](m: mutable.Map[K, V], builder: Builder[J]): Unit =
    {
      builder.beginObject()
      m foreach {
        case (k, v) =>
          builder.writeString(keyFormat.write(k))
          valueFormat.write(v, builder)
      }
      builder.endObject()
    }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): mutable.Map[K, V] =
      jsOpt match {
        case Some(js) =>
          val size = unbuilder.beginObject(js)
          val mapBuilder = mutable.Map.newBuilder[K, V]
          mapBuilder.sizeHint(size)
          (1 to size).toList map { _ =>
            val (k, v) = unbuilder.nextFieldOpt
            mapBuilder.addOne(
              keyFormat.read(k) -> valueFormat.read(v, unbuilder)
            )
          }
          unbuilder.endObject
          mapBuilder.result()
        case None => mutable.Map()
      }
  }
  /** Supplies the JsonFormat for mutable.Sets. */
  implicit def mutableSetFormat[T :JsonFormat]: RootJsonFormat[mutable.Set[T]]               = viaSeq[mutable.Set[T], T](seq => mutable.Set(seq :_*))
  implicit val longMapSjsonFormat: JsonFormat[mutable.LongMap[Boolean]] = projectFormat[mutable.LongMap[Boolean], Map[Long,Boolean]](_.toMap, mutable.LongMap.from)
//  implicit val (longMapD5r: Decoder[mutable.LongMap[Boolean]], longMapE5r: Encoder[mutable.LongMap[Boolean]]) =
//    (Decoder.decodeMap[Long, Boolean].map(_.foldLeft(new mutable.LongMap[Boolean])((m, p) => m += (p._1, p._2))),
//      Encoder.encodeMapLike[Long, Boolean, mutable.Map].contramapObject((m: mutable.LongMap[Boolean]) => m))
  implicit lazy val MissingRequiredFieldsSjsonFormat: JsonFormat[MissingRequiredFields] = isolistFormat(LList.iso(
    { p: MissingRequiredFields => ("s", p.s) :*: ("i", p.i) :*: LNil },
    { in: String :*: Int :*: LNil => MissingRequiredFields(in.head, in.tail.head) }
  ))
//  implicit val missingRequiredFieldsC3c: Codec[MissingRequiredFields] = deriveConfiguredCodec[MissingRequiredFields]
//  implicit lazy val NestedStructsSjsonFormat: JsonFormat[NestedStructs] = lazyFormat(isolistFormat(LList.iso(
//    { p: NestedStructs => ("n", p.n) :*: LNil },
//    { in: Option[NestedStructs] :*: LNil => NestedStructs(in.head) }
//  )))
  implicit lazy val NestedStructsSjsonFormat: JsonFormat[NestedStructs] = new RootJsonFormat[NestedStructs] {
  override def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): NestedStructs = jsOpt match {
    case Some(js) =>
      unbuilder.beginObject(js)
      val n = NestedStructs(unbuilder.readField[Option[NestedStructs]]("n"))
      unbuilder.endObject()
      n
    case None =>
      deserializationError("Expected JsObject but found None")
  }
  override def write[J](obj: NestedStructs, builder: Builder[J]): Unit = {
    builder.beginObject()
    builder.addField("n", obj.n)
    builder.endObject()
  }
}
//  implicit val nestedStructsC3c: Codec[NestedStructs] = deriveConfiguredCodec[NestedStructs]
  implicit val enumADTSjsonFormat: JsonFormat[SuitADT] = projectFormat((s: SuitADT) => s.toString, {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s: String => suite(s)
  })
//  implicit val (suitEnumDecoder: Decoder[SuitEnum.Value], suitEnumEncoder: Encoder[SuitEnum.Value]) =
//    (decodeEnumeration(SuitEnum), encodeEnumeration(SuitEnum))
  implicit val suitEnumSjsonFormat: JsonFormat[SuitEnum.Value] = projectFormat((s: SuitEnum.Value) => s.toString, SuitEnum.withName)
  implicit val suitJavaEnumSjsonFormat: JsonFormat[Suit] = projectFormat((s: Suit) => s.toString, Suit.valueOf)
  implicit lazy val PrimitivesSjsonFormat: JsonFormat[Primitives] = isolistFormat(LList.iso(
    { p: Primitives => ("b", p.b) :*: ("s", p.s) :*: ("i", p.i) :*: ("l", p.l) :*: ("bl", p.bl) :*: ("ch", p.ch) :*: ("dbl", p.dbl) :*: ("f", p.f) :*: LNil },
    { in: Byte :*: Short :*: Int :*: Long :*: Boolean :*: Char :*: Double :*: Float :*: LNil => Primitives(in.head, in.tail.head, in.tail.tail.head, in.tail.tail.tail.head, in.tail.tail.tail.tail.head, in.tail.tail.tail.tail.tail.head, in.tail.tail.tail.tail.tail.tail.head, in.tail.tail.tail.tail.tail.tail.tail.head) }
  ))
//  implicit val primitivesC3c: Codec[Primitives] = deriveConfiguredCodec[Primitives]
//  implicit val tweetC3c: Codec[TwitterAPI.Tweet] = {
//
//    deriveConfiguredCodec[TwitterAPI.Tweet]
//  }
  
  implicit class Jawn1SupportParser[J](private val Parser: SupportParser[J]) extends AnyVal {
    def parseFromByteArray(byteArray: Array[Byte]) = Parser.parseFromByteBuffer(ByteBuffer.wrap(byteArray))
  }
}
