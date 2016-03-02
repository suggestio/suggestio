package io.suggest.common.menum

import io.suggest.primo.TypeT

import scala.collection.immutable.SortedSet

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 12:20
 * Description: Утиль для scala Enumeration.
 */

/** Добавить неявный конвертер value к типу Enum'а. */
// TODO Может стоит скрестить это с ILightEnumeration?
trait EnumValue2Val extends Enumeration with TypeT {
  import scala.language.implicitConversions

  override type T <: Value

  implicit def value2val(x: Value): T = x.asInstanceOf[T]

  def valuesT = values.asInstanceOf[SortedSet[T]]

}


trait IMaybeWithName extends TypeT {
  def maybeWithName(n: String): Option[T]
}


/** Добавить метод value2val к scala enum'у. */
trait EnumMaybeWithName extends EnumValue2Val with IMaybeWithName {

  override def maybeWithName(n: String): Option[T] = {
    values
      .find(_.toString == n)
      .map(value2val)
  }

}


trait EnumMaybeWithId extends EnumValue2Val {

  def maybeWithId(id: Int): Option[T] = {
    values
      .iterator
      .map(value2val)
      .find(_.id == id)
  }

}


trait IVeryLightEnumeration extends TypeT {
  protected trait ValT
  override type T <: ValT
}
/** Enumeration'ы НЕ привязанные к scala-коллекциям, исповедуют совместимый с оригиналом интерфейс. */
trait ILightEnumeration
  extends IVeryLightEnumeration
  with IMaybeWithName

/** Дефолтовая реализация ILightEnumeration. */
trait LightEnumeration extends ILightEnumeration {
  def withName(n: String): T = {
    maybeWithName(n).get
  }
}
