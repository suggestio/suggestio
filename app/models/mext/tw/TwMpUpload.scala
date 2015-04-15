package models.mext.tw

import models.mext._
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.{WSRequestHolder, WSClient, WSResponse}
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:15
 * Description: Поддержка аплоада media для твиттера.
 */
trait TwMpUpload extends MpUploadSupportDflt with PlayMacroLogsImpl { this: TwitterService =>
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

  override def mpFieldNameDflt = "media"

  /** Приведение ответа после аплода к внутреннему списку attachments. */
  override def resp2attachments(resp: WSResponse): Seq[TwMediaAtt] = {
    resp.json
      .asOpt[TwMediaAtt]      // TODO Может надо тут .as[] использовать?
      .toSeq
  }

}
