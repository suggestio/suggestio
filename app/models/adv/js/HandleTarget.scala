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

case class HandleTargetAsk(mctx: MJsCtx, replyTo: Option[String]) extends JsBuilder {

  override def js: String = {
    val sb = new StringBuilder(512)
      .append("SioPR.handleTarget(")
      .append( Json.toJson(mctx) )
      .append(',')
      .append("function(ctx2,sendF){sendF({")
    if (replyTo.isDefined)
      sb.append(JsString(REPLY_TO_FN)).append(':').append(JsString(replyTo.get)).append(',')
    sb.append(JsString(CTX2_FN)).append(":ctx2")
      .append("});});")
      .toString()
  }

}
