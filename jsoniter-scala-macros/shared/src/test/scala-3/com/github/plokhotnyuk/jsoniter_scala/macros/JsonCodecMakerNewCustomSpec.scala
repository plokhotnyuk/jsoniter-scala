package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{Objects, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core._
// import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException
import java.util.concurrent.ThreadLocalRandom
import scala.annotation.switch
import scala.util.control.NonFatal
import scala.util.hashing.MurmurHash3

// @plokhotnyuk: compile-only tests - the use-cases behind this PR
object newCustom {

  // won't work
  // val impl = makerImpl.jsonCodecMakerImpl

  // won't work
  // val impl = makerImpl.originalResolverImpl

  // works!
  val impl = makerImpl.customResolverImpl

  trait Aggregate {
    type Props

    implicit def propsCodec: JsonValueCodec[Props]
  }

  trait Events { self: Aggregate =>
    case class MyEvent(props: Props)

    // Works here
    // implicit val myEventCodec: JsonValueCodec[MyEvent] = impl.make
  }

  object Person extends Aggregate with Events {
    case class Props(name: String, age: Int)

    implicit val propsCodec: JsonValueCodec[Props] = impl.make

    // FIXME: Doesn't work here
    // Scala 3: No implicit 'com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[_ >: scala.Nothing <: scala.Any]' defined for 'Events.this.Props'.
    // Scala 2: Only sealed traits or abstract classes are supported as an ADT base. Please consider sealing the 'Events.this.Props' or provide a custom implicitly accessible codec for it.
    
    // @plokhotnyuk: Scala 3: now works with `customResolverImpl`
    val myEventCodec: JsonValueCodec[MyEvent] = impl.make
  }

  object tagged {
    case class Wrap[+V](v: V)
    opaque type Wrapped[+V, +Tag] = Wrap[V]
    def wrap[Tag]: [V] => V => Tagged[V, Tag] = [V] => (v: V) => Wrap(v)

    opaque type Tagged[+V, +Tag] = Any
    type @@[+V, +Tag] = V & Tagged[V, Tag]

    def tag[Tag]: [V] => V => V @@ Tag =
      [V] => (v: V) => v

    inline def derive[F[_], V, T](using ev: F[V]): F[V @@ T] =
      ev.asInstanceOf[F[V @@ T]]
  }

  import tagged.*

  inline given [T <: Long]: JsonValueCodec[T] = ???

  trait BaseType { self =>
    type AID = Long @@ self.type
  }

  trait CustomerType extends BaseType {
    case class State(customerInfo: String)
    val stateCodec = impl.make[State]
  }

  trait OrderType[C <: CustomerType](val c: C) {
    case class State(cust: c.AID)

    // @plokhotnyuk: don't need this yet, but could add support later with a bit more work on `customResolverImpl`
    // val stateCodec = impl.make[State]
  }

  object Customer extends CustomerType
  object Order extends OrderType[Customer.type](Customer) {

    // @plokhotnyuk: works with `customResolverImpl`
    val orderStateCodec = impl.make[State]
  }

}

// class JsonCodecMakerNewCustomSpec extends VerifyingSpec {
//   import newCustom.*
//   //
// }