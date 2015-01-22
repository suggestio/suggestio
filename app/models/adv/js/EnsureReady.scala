package models.adv.js

import models.adv.js.ctx.JsCtx_t
import play.api.libs.json.JsString
import Answer._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:06
 * Description: Протокол взаимодействия с ensureReady-логикой. Эта логика инициализирует состояние js-сервера
 * в браузере клиента, инициализирует начальное состояние и подтверждает общую исправность js-сервера.
 */

/**
 * Запрос инициализации js-компонента с кодогенератором.
 * @param ctx1 Начальное состояние.
 */
case class EnsureReadyAsk(ctx1: JsCtx_t) extends JsBuilder {

  override def js: String = {
    new StringBuilder(128)
      .append("SioPR.ensureReady(")
      .append(ctx1)
      .append(',')
      .append("function(ctx2,sendF){sendF({")
      .append(JsString(CTX2_FN)).append(":ctx2")
      .append("});});")
      .toString()
  }

}
