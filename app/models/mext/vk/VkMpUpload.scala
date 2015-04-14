package models.mext.vk

import models.mext.{IMpUploadArgs, MpUploadSupportDflt}
import play.api.libs.ws.WSResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:12
 * Description: Поддержка аплода для вконтакта.
 */
object VkMpUpload extends MpUploadSupportDflt {
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
}
