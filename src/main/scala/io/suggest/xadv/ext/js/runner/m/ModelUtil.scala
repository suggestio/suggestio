package io.suggest.xadv.ext.js.runner.m

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 14:01
 * Description: утиль для ускорения построения моделей.
 */

/** Добавить метод fromString() в статическую модель, поддерживающую десериализацию fromDyn(). */
trait FromStringT {

  type T

  def fromDyn(raw: js.Dynamic): T

  def fromString(s: String): T = {
    fromDyn( js.JSON.parse(s) )
  }

}

trait IToJson {
  def toJson: js.Dynamic
}

