# Getting started
Let's start our adventure with jsoniter-scala from parsing and serialization of some complex data structure of nested 
collections and case classes.

## Prerequisites
In all tutorials we will use [scala-cli](https://scala-cli.virtuslab.org) for running Scala scripts, so you'll need to install it upfront.

You can use any text editor to copy and paste provided code snippets. Please, also, check Scala CLI Cookbooks 
documentation if you use [VSCode](https://scala-cli.virtuslab.org/docs/cookbooks/ide/vscode), [Intellij IDEA](https://scala-cli.virtuslab.org/docs/cookbooks/ide/intellij), or [emacs](https://scala-cli.virtuslab.org/docs/cookbooks/ide/emacs) editors.

Support of Scala CLI by IDEs is improving over time, so that [IntelliJ IDEA](https://www.jetbrains.com/idea/) with the latest version of Scala 
plugin you can try its [improved support for Scala CLI projects](https://blog.jetbrains.com/scala/2024/11/13/intellij-scala-plugin-2024-3-is-out/#scala-cli).

Latest versions of jsoniter-scala libraries require JDK 11 or above. You can use any of its distributions.

In all tutorials we will start from a file with the following dependencies and imports:

```scala
//> using scala 3
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.31.3"
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.31.3"

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
```

Please just copy and paste code snippets from the subsequent steps and run by pressing some `Run` button/keystroke in 
your IDE or run it using `scala-cli <file_name>` command from the terminal.

Make sure to preserve indentation when copying and pasting because we will use indenting syntax of Scala 3.

## Definition of a complex data structure

Let's imagine that we need to generate a JSON representation of some report for a US shop.

Below is a definition of its data structures and instantiation of some sample data: 

```scala
enum Category extends Enum[Category]:
  case Electronics, Fashion, HomeGoods

case class Product(id: Long, name: String, category: Category, price: BigDecimal, description: String)

enum OrderStatus extends Enum[OrderStatus]:
  case Pending, Shipped, Delivered, Cancelled

case class OrderItem(product: Product, quantity: Int)

case class Order(id: Long, customer: Customer, items: Seq[OrderItem], status: OrderStatus)

case class Customer(id: Long, name: String, email: String, address: Address)

case class Address(street: String, city: String, state: String, zip: String)

enum PaymentMethod:
  case CreditCard(cardNumber: Long, validThru: java.time.YearMonth) extends PaymentMethod
  case PayPal(id: String) extends PaymentMethod

case class Payment(method: PaymentMethod, amount: BigDecimal, timestamp: java.time.Instant)

case class OrderPayment(order: Order, payment: Payment)

val product1 = Product(
  id = 1L,
  name = "Apple iPhone 16",
  category = Category.Electronics,
  price = BigDecimal(999.99),
  description = "A high-end smartphone with advanced camera and AI capabilities"
)
val product2 = Product(
  id = 2L,
  name = "Nike Air Max 270",
  category = Category.Fashion,
  price = BigDecimal(129.99),
  description = "A stylish and comfortable sneaker with a full-length air unit"
)
val product3 = Product(
  id = 3L,
  name = "KitchenAid Stand Mixer",
  category = Category.HomeGoods,
  price = BigDecimal(299.99),
  description = "A versatile and powerful stand mixer for baking and cooking"
)
val customer1 = Customer(
  id = 1L,
  name = "John Doe",
  email = "john.doe@example.com",
  address = Address(
    street = "123 Main St",
    city = "Anytown",
    state = "CA",
    zip = "12345"
  )
)
val customer2 = Customer(
  id = 2L,
  name = "Jane Smith",
  email = "jane.smith@example.com",
  address = Address(
    street = "456 Elm St",
    city = "Othertown",
    state = "NY",
    zip = "67890"
  )
)
val order1 = Order(
  id = 1L,
  customer = customer1,
  items = Seq(
    OrderItem(product1, 1),
    OrderItem(product2, 2)
  ),
  status = OrderStatus.Pending
)
val order2 = Order(
  id = 2L,
  customer = customer2,
  items = Seq(
    OrderItem(product3, 1)
  ),
  status = OrderStatus.Shipped
)
val paymentMethod1 = PaymentMethod.CreditCard(
  cardNumber = 1234_5678_9012_3456L,
  validThru = java.time.YearMonth.parse("2026-12")
)
val paymentMethod2 = PaymentMethod.PayPal(
  id = "jane.smith@example.com"
)
val payment1 = Payment(
  method = paymentMethod1,
  amount = BigDecimal(1259.97),
  timestamp = java.time.Instant.parse("2025-01-03T12:30:45Z")
)
val payment2 = Payment(
  method = paymentMethod2,
  amount = BigDecimal(299.99),
  timestamp = java.time.Instant.parse("2025-01-15T19:10:55Z")
)
val orderPayment1 = OrderPayment(
  order = order1,
  payment = payment1
)
val orderPayment2 = OrderPayment(
  order = order2,
  payment = payment2
)
val report = Seq(
  orderPayment1,
  orderPayment2
)
```

## Defining the codec

Now we need to derive a codec for the report type (`List[OrderPayment]` type in our case). We will use 
`JsonCodecMaker.make` macros for that:
```scala
given JsonValueCodec[Seq[OrderPayment]] = JsonCodecMaker.make
```

An instance of this codec (also known as a type-class instance) is getting to be visible in the scope of subsequent
calls of parsing and serialization methods.

## Serialization

Now we are ready to serialize the report. For that we need to define some entry point method and call `writeToString`.
We will also print resulting JSON to the system output to see it as an output:

```scala
@main def gettingStarted: Unit =
  val json = writeToString(report)
  println(json)
```

From this moment you can run the script and see a long JSON string on the screen.

## Parsing

Having the JSON string in memory you can parse it using following lines that should be pasted to the end of the
`gettingStarted` method:
```scala
  val parsedReport = readFromString(json)
  println(parsedReport)
```

Let's rerun the script and get additionally printed `toString` representation of a report parsed from JSON string.

If something gone wrong you can pick and run [the final version of a script for this tutorial](1-getting-started.scala).

## Challenge
Experiment with the script to use different types of collections instead of `Seq` and try to find any collection type 
from the standard Scala library that is not supported by `JsonCodecMaker.make` macros to derive codec for serialization
and parsing.

## Recap
In this tutorial we learned basics for parsing and serialization of complex nested data structures using `scala-cli`.
Having definition of data structures and their instances in memory we need to:
1. Add dependency usage and package imports of `core` and `macros` modules of jsoniter-scala 
2. Define `given` type-class instance of `JsonValueCodec` for top-level data structure
3. Call `writeToString` to serialize an instance of complex data structure to JSON representation
4. Call `readFromString` to parse a complex data structure from JSON representation
