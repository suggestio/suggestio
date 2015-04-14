package models.mext.tw

import models.mext.{IMpUploadArgs, MpUploadSupportDflt}
import play.api.libs.oauth.{OAuthCalculator, ConsumerKey}
import play.api.libs.ws.{WSRequestHolder, WSClient, WSResponse}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:15
 * Description: Поддержка аплоада media для твиттера.
 */
class TwMpUpload(consumerKey: ConsumerKey) extends MpUploadSupportDflt {
  /**
   * Твиттер поддерживает upload по одному и тому же ресурсу.
   * @param args ignored.
   * @return Upload resource URL string.
   */
  override def getUploadUrl(args: IMpUploadArgs): String = {
    "https://upload.twitter.com/1.1/media/upload.json"
  }

  /** Является ли ответ по запросу правильным. false - если ошибка. */
  override def isRespOk(args: IMpUploadArgs, resp: WSResponse): Boolean = {
    val s = resp.status
    s >= 200 && s <= 299
  }

  /** Создание экземпляра нового реквеста требует цифровую подпись OAuth. */
  override def newRequest(args: IMpUploadArgs)(implicit ws: WSClient): WSRequestHolder = {
    super.newRequest(args)
      .sign( OAuthCalculator(consumerKey, args.oa1AcTok.get) )
  }

}
