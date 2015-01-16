package models.adv.js

import models.adv.js.ctx.JsCtx_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:06
 * Description: Протокол взаимодействия с ensureReady-логикой. Эта логика инициализирует состояние js-сервера
 * в браузере клиента, инициализирует начальное состояние и подтверждает общую исправность js-сервера.
 */
object EnsureReady extends IAction {
  override val action: String = "ensureReady"
}


/**
 * Запрос инициализации js-компонента с кодогенератором.
 * @param ctx1 Начальное состояние.
 */
case class EnsureReadyAsk(ctx1: JsCtx_t) extends AskBuilder {

  override def action: String = EnsureReady.action

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

