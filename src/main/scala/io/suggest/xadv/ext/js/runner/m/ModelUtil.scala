package io.suggest.xadv.ext.js.runner.m

import scala.scalajs.js
import scala.scalajs.js.Dictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 14:01
 * Description: утиль для ускорения построения моделей.
 */

trait FromJsonT {
  type T
  def fromJson(raw: js.Any): T
}

trait MaybeFromJsonT extends FromJsonT {
  def maybeFromJson(raw: js.Any): Option[T]

  override def fromJson(raw: js.Any): T = {
    maybeFromJson(raw).get
  }
}


/** Добавить метод fromString() в статическую модель, поддерживающую десериализацию fromDyn(). */
trait FromStringT extends FromJsonT {
  def fromString(s: String): T = {
    fromJson( js.JSON.parse(s) )
  }
}


/** Интерфейс сериализации в JSON. */
trait IToJson {
  def toJson: js.Any
}

/** Интерфейс сериализации в JSON object. */
trait IToJsonDict extends IToJson {
  def toJson: js.Dictionary[js.Any]
}


/** Пустая реализация [[IToJsonDict]]. */
class ToJsonDictDummy extends IToJsonDict {
  override def toJson: Dictionary[js.Any] = {
    Dictionary.empty
  }
}
