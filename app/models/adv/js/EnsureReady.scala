package models.adv.js

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:06
 * Description: Протокол взаимодействия с ensureReady-логикой. Эта логика инициализирует состояние js-сервера
 * в браузере клиента, инициализирует начальное состояние и подтверждает общую исправность js-сервера.
 */
sealed trait EnsureReadyAction extends IAction {
  override def action: String = "ensureReady"
  def CTX1 = "ctx1"
}


/**
 * Запрос инициализации js-компонента с кодогенератором.
 * @param ctx1 Начальное состояние.
 */
case class EnsureReadyAsk(ctx1: JsObject) extends AskBuilder with EnsureReadyAction {

  override val CTX1 = super.CTX1

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
      .append(".prepareEnsureReady(").append(ctx1).append(')')
  }

}


/** Компаньон положительного ответа на запрос инициализации. */
object EnsureReadySuccess extends StaticUnapplier with EnsureReadyAction {
  override type T = EnsureReadySuccess
  override type Tu = JsObject

  override def statusExpected: String = "success"

  implicit val ersReads: Reads[EnsureReadySuccess] = {
    val v = (JsPath \ CTX1).read[JsObject]
    v.map { EnsureReadySuccess.apply }
  }

  override def fromJs(args: JsValue): Tu = {
    args.validate[EnsureReadySuccess]
      .get
      .ctx1
  }
}

/** Положительный ответ на запрос инициализации. Обычно используется через unapply() в ws-акторе. */
case class EnsureReadySuccess(ctx1: JsObject)


/** Ошибка получена. */
object EnsureReadyError extends StaticErrorUnapplier with EnsureReadyAction {
  override type T = EnsureReadyError
}
case class EnsureReadyError(reason: String)
