package util.ext.tw

import io.suggest.ahc.upload.MpUploadArgs
import models.mext.tw.TwMediaAtt
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.ws.WSResponse
import util.ext.{ExtServiceHelperMpUpload, IOAuth1Support}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 15:15
 * Description: Поддержка аплоада media для твиттера.
 */
trait TwMpUpload
  extends ExtServiceHelperMpUpload
  with IOAuth1Support
{

  /**
   * Твиттер поддерживает upload по одному и тому же ресурсу.
   * @param args ignored.
   * @return Upload resource URL string.
   */
  override def getUploadUrl(args: MpUploadArgs): String = {
    "https://upload.twitter.com/1.1/media/upload.json"
  }

  /** Является ли ответ по запросу правильным. false - если ошибка. */
  override def isRespOk(args: MpUploadArgs, resp: WSResponse): Boolean = {
    val s = resp.status
    s >= 200 && s <= 299
  }

  /** Создание экземпляра нового реквеста. */
  override def newRequest(args: MpUploadArgs, client: AsyncHttpClient) = {
    super.newRequest(args, client)
      .setSignatureCalculator( new OAuthCalculator(consumerKey, args.oa1AcTok.get) )
  }

  override def mpFieldNameDflt = "media"

  /** Приведение ответа после аплода к внутреннему списку attachments. */
  override def resp2attachments(resp: WSResponse): Seq[TwMediaAtt] = {
    resp.json
      .asOpt[TwMediaAtt]      // TODO Может надо тут .as[] использовать?
      .toSeq
  }

}
