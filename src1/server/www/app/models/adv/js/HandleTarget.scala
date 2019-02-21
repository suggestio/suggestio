package models.adv.js

import models.adv.js.ctx.MJsCtx
import io.suggest.adv.ext.model.ctx.MAskActions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 16:31
 * Description: Общение на тему HandleTarget: ask и answer.
 */

case class HandleTargetAsk(
  mctx0     : MJsCtx,
  replyTo   : Option[String]
) extends IJsonActionCmd with IJsonActionCtxPatcher {

  override def sendMode = CmdSendModes.Async
  override def action = MAskActions.HandleTarget

}
