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
object PreparePictureStorage extends IAction {
  override def action: String = "preparePictureStorage"
}

case class PreparePictureStorageAsk(
  service : MExtService,
  ctx     : JsCtx_t,
  name    : String = "suggest.io",
  descr   : Option[String] = None
)
  extends ServiceAskBuilder
  with InServiceAskBuilder
{
  override def action: String = PreparePictureStorage.action

  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append(".preparePictureStorage(").append(ctx).append(')')
      .append(".setName(").append(JsString(name)).append(')')
    if (descr.isDefined)
      sb.append(".setDescription(").append(JsString(descr.get)).append(')')
    sb
  }
}
