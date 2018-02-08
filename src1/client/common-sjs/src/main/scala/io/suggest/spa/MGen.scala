package io.suggest.spa

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.16 10:38
  * Description: Модель/статическая утиль для random seed (generation).
  */
object MGen {

  /** Распарсить сериализованный вариант. */
  def parse(genStr: String): Option[Long] = {
    Try(genStr.toLong).toOption
  }

  /** Сериализовать значение generation. */
  // TODO Это только для sc-v2, потом - удалить.
  def serialize2js(gen: Long): Any = {
    gen
  }

  def serialize(gen: Long): String = {
    gen.toString
  }

  /** Сгенерить новое значение generation. */
  def random: Long = {
    (js.Math.random() * 1000000000).toLong
  }

}
