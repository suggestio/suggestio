package io.suggest.enum

import enumeratum._
import play.api.data.validation.ValidationError
import play.api.libs.json.Writes
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.06.17 18:32
  * Description: Серверная утиль для enumeratum.
  * Например, нормальная поддержка play-json для кросс-платформенных моделей.
  */
object EnumeratumJvmUtil {

  /** Поддержка JSON-десериализации из строки.
    *
    * @param model Enum-модель, содержащая все инстансы.
    * @tparam T Тип одного элемента модели.
    * @return JSON Reads.
    */
  def enumEntryReads[T <: EnumEntry](model: Enum[T]): Reads[T] = {
    __.read[String]
      .map( model.withNameOption )
      .filter( ValidationError("error.unknown.name") )(_.nonEmpty)
      .map(_.get)
  }


  /** Поддержка JSON-сериализации в строку.
    *
    * @tparam T Тип элемента enum-модели.
    * @return JSON Writes.
    */
  implicit def enumEntryWrites[T <: EnumEntry]: Writes[T] = {
    __.write[String]
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

}
