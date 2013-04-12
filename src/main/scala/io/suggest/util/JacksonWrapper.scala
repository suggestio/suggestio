package io.suggest.util

import java.lang.reflect.{Type, ParameterizedType}
import com.fasterxml.jackson.core.`type`.TypeReference
import java.io.{OutputStream, InputStream, StringWriter}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 19:41
 * Description: Код взят из http://stackoverflow.com/a/14166997 и допилен под текущие задачи.
 */

object JacksonWrapper {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def serialize(value: Any): String = {
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

  def serialize(os:OutputStream, value:Any) {
    mapper.writeValue(os, value)
  }

  def serializePretty(os:OutputStream, value:Any) {
    val writer = mapper.writer().withDefaultPrettyPrinter()
    writer.writeValue(os, value)
  }

  def deserialize[T: Manifest](value: String) : T = {
    mapper.readValue(value, typeReference[T])
  }

  def deserialize[T: Manifest](stream: InputStream) : T = {
    mapper.readValue(stream, typeReference[T])
  }

  def convert[T: Manifest](fromValue : Any) : T = {
    mapper.convertValue(fromValue, typeReference[T])
  }

  def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType = typeFromManifest(manifest[T])
  }

  final def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) {
      // тут было m.erasure, но компилятор жаловался на deprecated и настаивал на вызове runtimeClass()
      m.runtimeClass
    } else {
      new ParameterizedType {
        def getRawType = m.runtimeClass
        def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray
        def getOwnerType = null
      }
    }
  }
}