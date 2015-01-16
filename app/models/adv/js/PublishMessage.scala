package models.adv.js

import models.adv.js.ctx.JsCtx_t
import models.adv.{JsExtTargetT, MExtService}
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.01.15 13:01
 * Description: Модель запроса публикации сообщения и результаты запроса.
 */
object PublishMessage extends IAction {
  override def action: String = "publishMessage"
}


/**
 * Экземпляр запроса публикации сообщения в цели.
 * @param service Сервис.
 * @param ctx Исходный js-контекст.
 * @param text Текст сообщения
 * @param pictures Картинки.
 */
case class PublishMessageAsk(
  service     : MExtService,
  ctx         : JsCtx_t,
  text        : Option[String] = None,
  pictures    : Seq[String] = Seq.empty
)
  extends ServiceAskBuilder
  with InServiceAskBuilder
{

  override def action: String = PublishMessage.action

  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    val sb1 = super.buildJsCodeBody(sb)
      .append(".preparePublishMessage(").append(ctx).append(')')
    if (text.isDefined)
      sb1.append(".setText(").append(JsString(text.get)).append(')')
    pictures.foreach { pic =>
      sb1.append(".addPicture(").append(JsString(pic)).append(')')
    }
    sb1
  }

}

