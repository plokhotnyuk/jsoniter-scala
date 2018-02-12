package com.github.plokhotnyuk.jsoniter_scala.core

import sun.misc.Unsafe

import scala.util.Try

// FIXME: remove when perf. degradation of String.charAt when iterating through strings will be fixed in JDK 9:
// https://bugs.openjdk.java.net/browse/JDK-8013655
object UnsafeUtils {
  private[this] final val (unsafe, stringValueOffset, stringCoderOffset) = Try {
    val u = {
      val unsafeField = classOf[Unsafe].getDeclaredField("theUnsafe")
      unsafeField.setAccessible(true)
      unsafeField.get(null).asInstanceOf[Unsafe]
    }
    (u,
      u.objectFieldOffset(classOf[String].getDeclaredField("value")),
      u.objectFieldOffset(classOf[String].getDeclaredField("coder")))
  }.getOrElse((null, 0L, 0L))

  private[jsoniter_scala] final def getLatin1Array(s: String): Array[Byte] =
    if (stringCoderOffset == 0 || unsafe.getByte(s, stringCoderOffset) != 0) null
    else unsafe.getObject(s, stringValueOffset).asInstanceOf[Array[Byte]]
}
