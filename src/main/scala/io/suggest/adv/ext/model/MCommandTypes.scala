package io.suggest.adv.ext.model

import io.suggest.model.{LightEnumeration, ILightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 16:01
 * Description: Стройматериалы для реализации модели типов js-комманд от sio-сервера.
 */

/** Типы команд. */
object JsCommandTypes {
  def CTYPE_JS      = "js"
  def CTYPE_ACTION  = "action"
}


import JsCommandTypes._


/** Абстрактная модель. */
trait MCommandTypesBaseT extends ILightEnumeration {

  protected trait ValT extends super.ValT {
    def ctype: String
  }

  override type T <: ValT

  /** Команда содержит js-код, который надо тупо исполнить. */
  val JavaScript: T

  /** Команда содержит json-описание действия, которое должен понять и исполнить js-модуль ext.adv. */
  val Action: T

}


/** Абстрактная реализация легковесной модели MCommandTypes без использования коллекция и т.д. */
trait MCommandTypesLightT extends MCommandTypesBaseT with LightEnumeration {
  protected sealed class Val(val ctype: String) extends ValT

  override type T = Val

  override val JavaScript: T  = new Val(CTYPE_JS)
  override val Action: T      = new Val(CTYPE_ACTION)

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case JavaScript.ctype => Some(JavaScript)
      case Action.ctype     => Some(Action)
      case _                => None
    }
  }
}
