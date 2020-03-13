package io.suggest.util

import java.lang.reflect.{Type, ParameterizedType}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 19:41
 * Description: Код взят из http://stackoverflow.com/a/14166997 и допилен под текущие задачи.
 */

object JacksonWrapper {

  def mapper : ObjectMapper = {
    new ObjectMapper()
      .registerModule(DefaultScalaModule)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  }

  def prettyWriter = mapper.writer().withDefaultPrettyPrinter()

  /** Распарсить строку json и отрендерить назад в красивую строку. */
  def prettify(jsonStr: String): String = {
    val json = mapper.readValue(jsonStr, classOf[Object])
    prettyWriter.writeValueAsString(json)
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
