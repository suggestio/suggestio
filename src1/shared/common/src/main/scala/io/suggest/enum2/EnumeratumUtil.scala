package io.suggest.enum2

import enumeratum._
import enumeratum.values.{ValueEnum, ValueEnumEntry}
import io.suggest.common.empty.EmptyUtil
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
    _optReads2reads {
      implicitly[Reads[String]]
        .map(model.withNameOption)
    }
  }

  def valueEnumEntryReads[V, VEE <: ValueEnumEntry[V]](model: ValueEnum[V, VEE])(implicit rawReads: Reads[V]): Reads[VEE] = {
    _optReads2reads {
      rawReads
        .map( model.withValueOpt )
    }
  }

  private def _optReads2reads[T](rOpt: Reads[Option[T]]): Reads[T] = {
    rOpt
      .filter( JsonValidationError("error.unknown.id") )(_.nonEmpty)
      .map( EmptyUtil.getF )
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

  implicit def valueEnumEntryWrites[T, VEE <: ValueEnumEntry[T]](implicit writes: Writes[T]): Writes[VEE] = {
    writes
      .contramap( valueF )
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
  def valueEnumEntryFormat[V, VEE <: ValueEnumEntry[V]](model: ValueEnum[V, VEE])(implicit format: Format[V]): Format[VEE] = {
    Format(valueEnumEntryReads(model), valueEnumEntryWrites)
  }


  /** Просто функция для доступа к value-ключу. */
  def valueF[ValueType]: ValueEnumEntry[ValueType] => ValueType = {
    _.value
  }

  /** Просто функция для доступа к value-ключу. */
  def entryNameF: EnumEntry => String = {
    _.entryName
  }

  /** При рендере html-шаблонов в play с помощью @select() бывает удобно сериализовать модель в список option'ов. */
  def toSelectOptions[EE <: EnumEntry](m: Enum[EE]): Seq[(String, String)] = {
    toSelectOptions(
      m.values.iterator.map( entryNameF )
    )
  }
  def toSelectOptions[V, VEE <: ValueEnumEntry[V]](m: ValueEnum[V, VEE]): Seq[(String, String)] = {
    toSelectOptions(
      m.values.map( valueF )
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
