package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.apache.fory.Fory
//import org.apache.fory.config.Language
//import org.apache.fory.serializer.scala.ScalaDispatcher

object ForySerDesers {
  val forySerDeser: Fory =  Fory
    .builder()
//    .withLanguage(Language.JAVA)
    .withScalaOptimizationEnabled(true)
    .requireClassRegistration(false)
//    .withRefTracking(true)
    .suppressClassRegistrationWarnings(false)
    .build()

 // forySerDeser.getClassResolver.setSerializerFactory(new ScalaDispatcher())
}

