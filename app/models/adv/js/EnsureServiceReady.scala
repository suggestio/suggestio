package models.adv.js

import models.adv.MExtService
import models.adv.js.ctx.JsCtx_t
import play.api.libs.json._
import play.api.libs.functional.syntax._
import ServiceStatic._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.14 16:32
 * Description: Модели для работы с инициализацией js-api одного сервиса.
 */

sealed trait EnsureServiceReadyAction extends IAction {
  override def action = "ensureServiceReady"

  def CTX2    = "ctx2"
  def PARAMS  = "params"
}


/** Генерация js-кода, который занимается сборкой и отправкой запроса инициализации в клиент сервиса. */
case class EnsureServiceReadyAsk(service: MExtService, ctx1: JsCtx_t) extends CallbackServiceAskBuilder with EnsureServiceReadyAction {

  override val PARAMS = super.PARAMS
  override val CTX2   = super.CTX2

  override def onSuccessArgsList: List[String] = {
    List(CTX2, PARAMS)
  }
  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    super.onSuccessArgs(sb)
      .append(',')
      .append(JsString(CTX2)).append(':').append(CTX2)
      .append(',')
      .append(JsString(PARAMS)).append(':').append(PARAMS)
  }

  /**
   * Генерация основного js-кода.
   * Этог метод должен быть перезаписан в наследии, чтобы добиться добавления js-кода между начальным SioPR и
   * финальным .execute().
   * @param sb Начальный аккамулятор.
   * @return Финальный аккамулятор.
   */
  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append(".prepareEnsureServiceReady(")
      .append(service.strId)
      .append(',')
      .append(ctx1)
      .append(')')
  }

}


case class ServicePictureParams(needStorage: Boolean, widthMax: Int, heightMax: Int)
object ServicePictureParams {
  /** Парсер из json. */
  implicit val sppReads: Reads[ServicePictureParams] = {
    val s =
      (JsPath \ "needStorage").read[Boolean] and
      (JsPath \ "widthMax").read[Int] and
      (JsPath \ "heightMax").read[Int]
    s.apply(ServicePictureParams.apply _)
  }

}

case class ServiceParams(threadSafe: Boolean, picture: ServicePictureParams)
object ServiceParams {
  /** Парсер из json. */
  implicit val spReads: Reads[ServiceParams] = {
    val s =
      (JsPath \ "threadSafe").read[Boolean] and
      (JsPath \ "picture").read[ServicePictureParams]
    s(ServiceParams.apply _)
  }
}


case class EnsureServiceReadySuccess(
  service : MExtService,
  ctx2    : JsCtx_t,
  params  : ServiceParams
)
object EnsureServiceReadySuccess extends StaticUnapplier with EnsureServiceReadyAction {
  override type T = EnsureServiceReadySuccess
  override type Tu = (MExtService, JsCtx_t, ServiceParams)
  override def statusExpected = "success"

  /** Маппер из json'а. */
  implicit val esrsReads: Reads[EnsureServiceReadySuccess] = {
    val s =
      serviceFieldReads and
      (JsPath \ CTX2).read[JsCtx_t] and
      (JsPath \ PARAMS).read[ServiceParams]
    s(EnsureServiceReadySuccess.apply _)
  }

  /** Этот элемент точно подходит. Нужно десериализовать данные из него. */
  override def fromJs(json: JsValue): Tu = {
    val v = json.validate[EnsureServiceReadySuccess].get
    (v.service, v.ctx2, v.params)
  }
}



/** Ошибка получена. */
object EnsureServiceReadyError extends StaticServiceErrorUnapplier with EnsureServiceReadyAction {
  override type T = EnsureServiceReadyError
}
case class EnsureServiceReadyError(service: MExtService, reason: String)

