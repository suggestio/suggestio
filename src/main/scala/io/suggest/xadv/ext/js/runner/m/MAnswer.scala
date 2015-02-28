package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.JsCommand

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 18:56
 * Description: Обертка ответа на запрос. Этот ответ отправляется на сервер.
 */

case class MAnswer(
  replyTo : Option[String],
  mctx    : MJsCtx
) {

  def toJson: js.Dictionary[js.Any] = {
    val d = js.Dictionary[js.Any](
      JsCommand.MCTX_FN -> mctx.toJson
    )
    if (replyTo.isDefined)
      d.update(JsCommand.REPLY_TO_FN, replyTo.get)
    d
  }

}
