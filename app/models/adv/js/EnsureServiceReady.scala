package models.adv.js

import models.adv.MExtService
import models.adv.js.ctx.JsCtx_t
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.14 16:32
 * Description: Модели для работы с инициализацией js-api одного сервиса.
 */

object EnsureServiceReady extends IAction {
  override def action = "ensureServiceReady"
}


/** Генерация js-кода, который занимается сборкой и отправкой запроса инициализации в клиент сервиса. */
case class EnsureServiceReadyAsk(service: MExtService, ctx1: JsCtx_t) extends ServiceAskBuilder {

  override def action: String = EnsureServiceReady.action

  /**
   * Генерация основного js-кода.
   * Этог метод должен быть перезаписан в наследии, чтобы добиться добавления js-кода между начальным SioPR и
   * финальным .execute().
   * @param sb Начальный аккамулятор.
   * @return Финальный аккамулятор.
   */
  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append(".prepareEnsureServiceReady(")
      .append(JsString(service.strId))
      .append(',')
      .append(ctx1)
      .append(')')
  }

}

