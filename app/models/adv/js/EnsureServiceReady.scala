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

sealed trait EnsureServiceReadyAction extends IAction {
  override def action = "ensureServiceReady"
}


/** Генерация js-кода, который занимается сборкой и отправкой запроса инициализации в клиент сервиса. */
case class EnsureServiceReadyAsk(service: MExtService, ctx1: JsCtx_t) extends CallbackServiceAskBuilder
with EnsureServiceReadyAction with Ctx2Fields {

  override val CTX2   = super.CTX2

  override def onSuccessArgsList: List[String] = {
    List(CTX2)
  }
  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    super.onSuccessArgs(sb)
      .append(',')
      .append(JsString(CTX2)).append(':').append(CTX2)
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
      .append(".prepareEnsureServiceReady(")
      .append(JsString(service.strId))
      .append(',')
      .append(ctx1)
      .append(')')
  }

}


case class EnsureServiceReadySuccess(
  service : MExtService,
  ctx2    : JsCtx_t
) extends ISuccess
object EnsureServiceReadySuccess extends ServiceAndCtxStaticUnapplier with EnsureServiceReadyAction {
  override type T = EnsureServiceReadySuccess
}



/** Ошибка получена. */
object EnsureServiceReadyError extends StaticServiceErrorUnapplier with EnsureServiceReadyAction {
  override type T = EnsureServiceReadyError
}
case class EnsureServiceReadyError(service: MExtService, reason: String)

