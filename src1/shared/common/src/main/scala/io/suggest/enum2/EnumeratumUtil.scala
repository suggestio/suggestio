package io.suggest.enum2

import enumeratum._
import enumeratum.values.{ValueEnum, ValueEnumEntry}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, _}

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.06.17 18:32
  * Description: Серверная утиль для enumeratum.
  * Например, нормальная поддержка play-json для кросс-платформенных моделей.
  */
object EnumeratumUtil {

  /** Поддержка JSON-десериализации из строки.
    *
    * @param model Enum-модель, содержащая все инстансы.
    * @tparam T Тип одного элемента модели.
    * @return JSON Reads.
    */
  def enumEntryReads[T <: EnumEntry](model: Enum[T]): Reads[T] = {
    implicitly[Reads[String]]
      .map( model.withNameOption )
      .filter( JsonValidationError("error.unknown.name") )(_.nonEmpty)
      .map(_.get)
  }


  /** Поддержка JSON-сериализации в строку.
    *
    * @tparam T Тип элемента enum-модели.
    * @return JSON Writes.
    */
  implicit def enumEntryWrites[T <: EnumEntry]: Writes[T] = {
    implicitly[Writes[String]]
      .contramap(_.toString)
  }


  /** JSON Format на базе Reads и Writes.
    *
    * @param model Enum-модель, содержащая все элементы.
    * @tparam T Тип одного элемента модели.
    * @return JSON Format.
    */
  def enumEntryFormat[T <: EnumEntry](model: Enum[T]): Format[T] = {
    Format(enumEntryReads(model), enumEntryWrites)
  }


  /** Просто функция для доступа к value-ключу. */
  def valueF[ValueType]: ValueEnumEntry[ValueType] => ValueType = {
    _.value
  }


  /** При рендере html-шаблонов в play с помощью @select() бывает удобно сериализовать модель в список option'ов. */
  def toSelectOptions[EE <: EnumEntry](m: Enum[EE]): Seq[(String, String)] = {
    toSelectOptions(
      m.values.iterator.map(_.entryName)
    )
  }
  def toSelectOptions[V, VEE <: ValueEnumEntry[V]](m: ValueEnum[V, VEE]): Seq[(String, String)] = {
    toSelectOptions(
      m.values.map(_.value)
    )
  }
  def toSelectOptions[T](items: TraversableOnce[T]): Seq[(String, String)] = {
    items
      .map { raw =>
        val s = raw.toString
        s -> s
      }
      .toSeq
  }

}
