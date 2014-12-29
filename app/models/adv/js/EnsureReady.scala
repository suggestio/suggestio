package models.adv.js

import play.api.libs.json.{JsString, JsValue, JsObject}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:06
 * Description: Протокол взаимодействия с ensureReady-логикой. Эта логика инициализирует состояние js-сервера
 * в браузере клиента, инициализирует начальное состояние и подтверждает общую исправность js-сервера.
 */
trait EnsureReadyAction extends IAction {
  override def action: String = "ensureReady"
  def CTX1 = "ctx1"
}


/**
 * Запрос инициализации js-компонента с кодогенератором.
 * @param ctx0 Начальное состояние.
 */
case class EnsureReadyAsk(ctx0: JsObject) extends AskBuilder with EnsureReadyAction {
  override def onSuccessArgsList = List(CTX1)
  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    sb.append(JsString(CTX1))
      .append(':')
      .append(CTX1)
  }

  /**
   * Генерация основного js-кода.
   * Этог метод должен быть перезаписан в наследии, чтобы добиться добавления js-кода между начальным SioPR и
   * финальным .execute().
   * @param sb Начальный аккамулятор.
   * @return Финальный аккамулятор.
   */
  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append('.').append(action).append('(').append(ctx0).append(')')
  }

}


/** Компаньон положительного ответа на запрос инициализации. */
object EnsureReadySuccess extends StaticUnapplier with EnsureReadyAction {
  override type T = EnsureReadySuccess
  override type Tu = JsObject

  override def statusExpected: String = "success"
  override def fromJs(args: JsValue): Tu = {
    args \ CTX1 match {
      case jso: JsObject => jso
      case _             => JsObject(Nil)
    }
  }
}

/** Положительный ответ на запрос инициализации. Обычно используется через unapply() в ws-акторе. */
case class EnsureReadySuccess(ctx1: JsObject)
