package util.ext

import io.suggest.ahc.upload.IMpUploadSupport
import models.mext.IPostAttachmentId
import play.api.libs.ws.WSResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 22:08
 * Description: Расширение интерфейса подсистем систем аплоада в ext-services.
 */
trait IExtMpUploadSupport extends IMpUploadSupport {

  /** Приведение ответа после аплода к внутреннему списку attachments. */
  def resp2attachments(resp: WSResponse): Seq[IPostAttachmentId]

}
