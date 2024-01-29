//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.28.0"
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.28.0"

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make

val user = readFromString("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""")
val json = writeToString(User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))

println(user)
println(json)
