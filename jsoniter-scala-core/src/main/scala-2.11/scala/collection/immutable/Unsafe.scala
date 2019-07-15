package scala.collection.immutable

object Unsafe {
  final def append[T](last: ::[T], x: T): ::[T] = {
    val next = new ::(x, Nil)
    last.tl = next
    next
  }
}
