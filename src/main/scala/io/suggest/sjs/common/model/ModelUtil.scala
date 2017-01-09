package io.suggest.sjs.common.model

import scala.scalajs.js.{Dictionary, Any, JSON}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 14:01
 * Description: утиль для ускорения построения моделей.
 */

trait FromJsonT {
  type T
  def fromJson(raw: Any): T
}

trait MaybeFromJsonT extends FromJsonT {
  def maybeFromJson(raw: Any): Option[T]

  override def fromJson(raw: Any): T = {
    maybeFromJson(raw).get
  }
}


/** Добавить метод fromString() в статическую модель, поддерживающую десериализацию fromDyn(). */
trait FromStringT extends FromJsonT {
  def fromString(s: String): T = {
    fromJson( JSON.parse(s) )
  }
}


/** Интерфейс сериализации в JSON. */
trait IToJson {
  def toJson: Any

  override def toString: String = {
    toJson.toString
  }
}

/** Интерфейс сериализации в JSON object. */
trait IToJsonDict extends IToJson {
  def toJson: Dictionary[Any]
}


/** Пустая реализация [[IToJsonDict]]. */
trait ToJsonDictDummyT extends IToJsonDict {
  override def toJson: Dictionary[Any] = {
    Dictionary.empty
  }
}
class ToJsonDictDummy
  extends ToJsonDictDummyT
