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
trait PublishMessageAction extends IAction {
  override def action: String = "publishMessage"
}


/**
 * Экземпляр запроса публикации сообщения в цели.
 * @param service Сервис.
 * @param ctx Исходный js-контекст.
 * @param target Описание цели.
 * @param onClickUrl Ссылка при клике юзера (куда юзер должен переходить).
 * @param text Текст сообщения
 * @param pictures Картинки.
 */
case class PublishMessageAsk(
  service     : MExtService,
  ctx         : JsCtx_t,
  target      : JsExtTargetT,
  onClickUrl  : String,
  text        : Option[String] = None,
  pictures    : Seq[String] = Seq.empty
)
  extends CallbackServiceAskBuilder
  with ServiceCall
  with PublishMessageAction
  with Ctx2Fields
{

  override val CTX2 = super.CTX2

  override def onSuccessArgsList: List[String] = List(CTX2)

  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    super.onSuccessArgs(sb)
      .append(',')
      .append(JsString(CTX2)).append(':').append(CTX2)
  }

  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    val sb1 = super.buildJsCodeBody(sb)
      .append(".preparePublishMessage(").append(ctx).append(')')
      .append(".setTarget(").append(target.toJsTargetPlayJson).append(')')
      .append(".setUrl(").append(JsString(onClickUrl)).append(')')
    if (text.isDefined)
      sb1.append(".setText(").append(JsString(text.get)).append(')')
    pictures.foreach { pic =>
      sb1.append(".addPicture(").append(JsString(pic)).append(')')
    }
    sb1
  }

}


/** Распарсенный экземпляр положительного ответа. */
case class PublishMessageSuccess(
  service   : MExtService,
  ctx2      : JsCtx_t
) extends ISuccess

object PublishMessageSuccess extends ServiceAndCtxStaticUnapplier with PublishMessageAction with Ctx2Fields {
  override type T = PublishMessageSuccess
}


/** Распарсенный экземпляр отрицательного результата исполнения. */
case class PublishMessageError(service: MExtService, reason: String) extends IError
object PublishMessageError extends StaticServiceErrorUnapplier with PublishMessageAction {
  override type T = PublishMessageError
}

