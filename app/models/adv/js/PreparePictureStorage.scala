package models.adv.js

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

case class PreparePictureStorageAsk(
  service : MExtService,
  ctx     : JsCtx_t,
  name    : String = "suggest.io",
  descr   : Option[String] = None
)
  extends CallbackServiceAskBuilder
  with ServiceCall
  with PreparePictureStorageAction
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
    super.buildJsCodeBody(sb)
      .append(".prepareHasPictureStorage(").append(ctx).append(')')
      .append(".setName(").append(JsString(name)).append(')')
    if (descr.isDefined)
      sb.append(".setDescription(").append(JsString(descr.get)).append(')')
    sb
  }
}


case class PreparePictureStorageSuccess(service: MExtService, ctx2: JsCtx_t) extends ISuccess
object PreparePictureStorageSuccess extends ServiceAndCtxStaticUnapplier with PreparePictureStorageAction {
  override type T = PreparePictureStorageSuccess
}


case class PreparePictureStorageError(service: MExtService, reason: String)
object PreparePictureStorageError extends StaticServiceErrorUnapplier with PreparePictureStorageAction {
  override type T = PreparePictureStorageError
}
