package models.adv.js

import models.adv.js.ctx.MJsCtx
import play.api.libs.json.{JsString, Json}
import Answer._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 16:31
 * Description: Общение на тему HandleTarget: ask и answer.
 */

case class HandleTargetAsk(
  mctx0     : MJsCtx,
  replyTo   : Option[String],
  sendMode  : CmdSendMode = CmdSendModes.Queued
) extends IJsonActionCmd with IJsonActionCtxPatcher {

  override def action = MJsActions.HandleTarget

}
