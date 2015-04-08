package io.suggest.adv.ext.model

import io.suggest.model.{EnumMaybeWithName, LightEnumeration, ILightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 16:01
 * Description: Стройматериалы для реализации модели типов js-комманд от sio-сервера.
 */

/** Типы команд. */
object JsCommandTypes {

  /** Код команды для запуска произвольного js в браузере, сгенеренного сервером. */
  def CTYPE_JS            = "js"

  /** Код команды на запуск adv-ext-экшена в указанной обстановке. */
  def CTYPE_ACTION        = "action"

}


import JsCommandTypes._


/** Абстрактная модель. */
trait MCommandTypesBaseT extends ILightEnumeration {

  protected trait ValT extends super.ValT {
    val ctype: String
    override def toString = ctype
  }

  override type T <: ValT

  /** Команда содержит js-код, который надо тупо исполнить. */
  val JavaScript: T

  /** Команда содержит json-описание действия, которое должен понять и исполнить js-модуль ext.adv. */
  val Action: T

}


/** Абстрактная легковесная реализация модели MCommandTypes без использования коллекций и т.д. */
trait MCommandTypesLightT extends MCommandTypesBaseT with LightEnumeration {
  override def maybeWithName(n: String): Option[T] = {
    n match {
      case JavaScript.ctype => Some(JavaScript)
      case Action.ctype     => Some(Action)
      case _                => None
    }
  }
}


/** Трейт реализации модели [[MCommandTypesBaseT]] на базе scala.Enumeration. */
trait MCommandTypesT extends Enumeration with MCommandTypesBaseT with EnumMaybeWithName {
  protected class Val(val ctype: String) extends super.Val(ctype) with ValT

  override type T = Val

  override val JavaScript: T  = new Val(CTYPE_JS)
  override val Action: T      = new Val(CTYPE_ACTION)
}

