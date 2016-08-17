package util.ext.vk

import io.suggest.ahc.upload.IMpUploadArgs
import models.mext._
import play.api.libs.ws.WSResponse
import util.ext.ExtServiceHelperMpUpload

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:12
 * Description: Поддержка аплода для вконтакта.
 */
trait VkMpUpload
  extends ExtServiceHelperMpUpload
{

  /**
   * vk используется динамические URL для подгрузки. URL должен быть задан в аргументах.
   * @param args Аргументы upload.
   * @return Содержимое args.url.get.
   */
  override def getUploadUrl(args: IMpUploadArgs): String = {
    args.url.get
  }

  /** Является ли ответ по запросу правильным. false - если ошибка. */
  override def isRespOk(args: IMpUploadArgs, resp: WSResponse): Boolean = {
    val s = resp.status
    s >= 200 && s <= 299
  }

  override def mpFieldNameDflt: String = "photo"

  /** У вконтакта формат такой, что он парсится на клиенте, и этот метод никогда не вызывается вроде. */
  override def resp2attachments(resp: WSResponse): Seq[IPostAttachmentId] = {
    Seq(
      StringIdPostAttachment(resp.body)
    )
  }

}
