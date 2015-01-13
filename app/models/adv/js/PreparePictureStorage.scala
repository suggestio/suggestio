package models.adv.js

import ServiceStatic._
import models.adv.MExtService
import models.adv.js.ctx.JsCtx_t
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.14 11:14
 * Description: Модели для взаимодействий на языке хранилищь.
 */
trait PreparePictureStorageAction extends IAction {
  override def action: String = "hasPictureStorage"
}
trait PreparePictureStorageFields {
  def IS_EXISTS = "isExists"
}

case class PreparePictureStorageAsk(
  service : MExtService,
  ctx     : JsCtx_t,
  name    : String = "suggest.io",
  descr   : Option[String] = None
)
  extends CallbackServiceAskBuilder
  with ServiceCall
  with PreparePictureStorageAction
  with PreparePictureStorageFields
{
  override val IS_EXISTS = super.IS_EXISTS

  override def onSuccessArgsList: List[String] = List(IS_EXISTS)
  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    super.onSuccessArgs(sb)
      .append(',')
      .append(JsString(IS_EXISTS)).append(':').append(IS_EXISTS)
  }

  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append(".prepareHasPictureStorage(").append(ctx).append(')')
      .append(".setName(").append(JsString(name)).append(')')
    if (descr.isDefined)
      sb.append(".setDescription(").append(JsString(descr.get)).append(')')
    sb
  }
}


case class PreparePictureStorageSuccess(service: MExtService, ctx2: JsCtx_t)
object PreparePictureStorageSuccess extends StaticUnapplier with PreparePictureStorageAction with PreparePictureStorageFields {
  override type T = PreparePictureStorageSuccess
  override type Tu = (MExtService, JsCtx_t)
  override def statusExpected = "success"

  implicit val hpsReads: Reads[T] = {
    val s =
      serviceFieldReads and
      (JsPath \ IS_EXISTS).read[JsObject]
    s(PreparePictureStorageSuccess.apply _)
  }

  /** Этот элемент точно подходит. Нужно десериализовать данные из него. */
  override def fromJs(json: JsValue): Tu = {
    val v = json.validate[T].get
    (v.service, v.ctx2)
  }
}


case class PreparePictureStorageError(service: MExtService, reason: String)
object PreparePictureStorageError extends StaticServiceErrorUnapplier with PreparePictureStorageAction {
  override type T = PreparePictureStorageAction
}
