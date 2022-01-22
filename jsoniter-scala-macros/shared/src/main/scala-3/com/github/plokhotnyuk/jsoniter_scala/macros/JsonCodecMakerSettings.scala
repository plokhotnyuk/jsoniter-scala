package com.github.plokhotnyuk.jsoniter_scala.macros

import scala.compiletime._

/**
 * Object contains compile-time settings for JsonCodeMaker generator.
 * To activate settings, add appropriative implicit object to the current 
 * compilation context
 **/
object JsonCodecMakerSettings {

  /**
   * Use to enable printing of codec during compilation:
   *```
   *given JsonCodecMakerSetting.PrintCodec
   *
   *val codec = CodecMakerConfig[MyClass]
   *```
   **/
  class PrintCodec

  /**
   * Use to print additional debug code during derivation of codecs
    *```
    *given JsonCodecMakerSetting.Trace
    *
    *val codec = CodecMakerConfig[MyClass]
    *```
   **/
  class Trace
}