package models.adv.js

import models.adv.js.ctx.{MJsCtx, JsCtx_t}
import play.api.libs.json.{Json, JsString}
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
 * @param mctx0 Начальное состояние.
 * @param replyTo Кому ответ отправлять?
 */
case class EnsureReadyAsk(
  mctx0     : MJsCtx,
  replyTo   : Option[String]
) extends IJsonActionCmd with IJsonActionCtxPatcher {

  override def action = MJsActions.EnsureReady

}
