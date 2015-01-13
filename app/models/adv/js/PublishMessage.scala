package models.adv.js

import models.adv.js.ctx.JsCtx_t
import models.adv.{JsPublishTargetT, MExtService}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.01.15 13:01
 * Description: Модель запроса публикации сообщения и результаты запроса.
 */
trait PublishMessageAction extends IAction {
  override def action: String = "publishMessage"
}

/** Поля для рендера js-кода. */
trait PublishMessageFields {
  def CTX2    = "ctx2"
  def POST_ID = "postId"
}


case class PublishMessageAsk(
  service     : MExtService,
  ctx         : JsCtx_t,
  target      : JsPublishTargetT,
  url         : String,
  text        : Option[String] = None,
  pictures    : Seq[String] = Seq.empty
) extends AskBuilder with PublishMessageAction with PublishMessageFields {

  override val POST_ID = super.POST_ID
  override val CTX2 = super.CTX2

  override def onSuccessArgsList: List[String] = ???
  override def onSuccessArgs(sb: StringBuilder): StringBuilder = ???

  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
    ???
  }
}
