//> using scala 3
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.31.3"
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.31.3"

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

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

given JsonValueCodec[Seq[OrderPayment]] = JsonCodecMaker.make

@main def gettingStarted: Unit =
  val json = writeToString(report)
  println(json)
  val parsedReport = readFromString(json)
  println(parsedReport)
