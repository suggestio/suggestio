package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.JsCommand

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 18:56
 * Description: Обертка ответа на запрос. Этот ответ отправляется на сервер.
 */

object MAnswer {
  implicit def answer2dyn(ans: MAnswer): js.Dynamic = ans.toJson
}

case class MAnswer(
  replyTo: Option[String],
  mctx: MJsCtx
) {

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    if (replyTo.isDefined)
      lit.updateDynamic(JsCommand.REPLY_TO_FN)(replyTo.get)
    lit.updateDynamic(JsCommand.MCTX_FN)(mctx.toJson)
    lit
  }

}
