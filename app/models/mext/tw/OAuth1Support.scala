package models.mext.tw

import models.msc.SiteQsArgs
import util.{PlayMacroLogsI, FormUtil, TplDataFormatUtil}
import models.Context
import models.mext.{IOa1MkPostArgs, IExtPostInfo, IOAuth1Support}
import org.apache.http.client.utils.URIBuilder
import play.api.Play._
import play.api.libs.oauth._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:16
 * Description: Реализация поддержки OAuth1.
 */

trait OAuth1Support extends IOAuth1Support with PlayMacroLogsI { this: TwitterService =>

  /** 2015.apr.14: 28cdf84ad875 twitter cards отнесены в печку, т.к. отображаются скрытыми.
    * Загрузка картинки будет идти напрямую в твиттер и затем публикация твита со встроенным media. */
  override def isMkPostNeedMpUpload = true

  /** Ключи приложения для доступа к public API. */
  override lazy val consumerKey: ConsumerKey = {
    val cp = confPrefix
    ConsumerKey(
      key     = configuration.getString(cp + ".consumerKey").get,
      secret  = configuration.getString(cp + ".consumerSecret").get
    )
  }

  /** Синхронный OAuth1-клиент для твиттера */
  override lazy val client: OAuth = {
    val cp = confPrefix
    OAuth(
      ServiceInfo(
        requestTokenURL  = configuration.getString(cp + ".requestTokenUrl")  getOrElse "https://api.twitter.com/oauth/request_token",
        accessTokenURL   = configuration.getString(cp + ".accessTokenUrl")   getOrElse "https://api.twitter.com/oauth/access_token",
        // securesocial должна по идее использовать /authentificate, а не authorize. Поэтому, отвязываем значение.
        authorizationURL = /*configuration.getString(cp + ".authorizationUrl") getOrElse*/ "https://api.twitter.com/oauth/authorize",
        consumerKey
      ),
      use10a = true
    )
  }

  /** URL для проверки валидности access_token'а. */
  def AC_TOK_VERIFY_URL = "https://api.twitter.com/1.1/account/verify_credentials.json?include_entities=false&skip_status=true"

  /**
   * Проверка валидности access_tokena силами модели.
   * @param acTok Проверяемый access_token.
   * @param ws http-клиент.
   * @return Фьючерс с true, если токен точно валиден сейчас.
   * @see [[https://dev.twitter.com/rest/reference/get/account/verify_credentials]]
   */
  override def isAcTokValid(acTok: RequestToken)(implicit ws: WSClient, ec: ExecutionContext): Future[Boolean] = {
    ws.url( AC_TOK_VERIFY_URL )
      .sign( sigCalc(acTok) )
      .get()
      .map { resp =>
        val res = resp.status == 200
        if (!res)
          LOGGER.debug(s"Twitter server said, that access_token ${acTok.token} invalid: HTTP ${resp.status} ${resp.statusText}\n ${resp.body}")
        res
      }
  }

  /** URL ресурс API твиттинга. */
  def MK_TWEET_URL = "https://api.twitter.com/1.1/statuses/update.json"

  /**
   * Сделать твит.
   * @param args Данные для постинга.
   * @see [[https://dev.twitter.com/rest/reference/post/statuses/update]]
   * @return Фьючерс с результатом работы.
   */
  override def mkPost(args: IOa1MkPostArgs)(implicit ws: WSClient, ec: ExecutionContext): Future[TweetInfo] = {
    import args._
    val b = new URIBuilder(MK_TWEET_URL)
    // Собираем читабельный текст твита.
    val tweetText = mad.richDescrOpt
      .map { rd =>
        val s0 = FormUtil.strTrimSanitizeF(rd.text)
        TplDataFormatUtil.strLimitLenNoTrailingWordPart(s0, 115)
      }
      .getOrElse("")
    // Собираем ссылку в твите.
    val siteArgs = SiteQsArgs(
      povAdId = mad.id
    )
    val jsSt = returnTo.builder()
      .setAdnId( args.mnode.id.get )
      .setFocusedAdId( args.mad.id.get )
      .setFocusedProducerId( args.mad.producerId )
      .toJsState
    // twitter не трогает ссылки, до которых не может достучаться. Нужно помнить об этом.
    val urlPrefix = /*Context devReplaceLocalHostW127001*/ Context.SC_URL_PREFIX
    val tweetUrl = urlPrefix + controllers.routes.MarketShowcase.geoSite(jsSt, siteArgs)
    val fullTweetText = Seq(tweetText, tweetUrl).mkString(" ")
    b.addParameter("status", fullTweetText)
    if (geo.isDefined) {
      val g = geo.get
      b.addParameter("lat", g.lat.toString)
      b.addParameter("lon", g.lon.toString)
    }
    // Приаттачить аттачменты к твиту.
    val medias = args.attachments
    if (medias.nonEmpty) {
      val v = medias.toIterator.map(_.strId).mkString(",")
      b.addParameter("media_ids", v)
    }
    val url = b.build().toASCIIString
    // Начать постинг.
    val req = ws.url(url)
      .sign( sigCalc(acTok) )
    LOGGER.trace("Tweet POSTing to: " + req.url)
    req
      .execute("POST")
      .map { resp =>
        if (resp.status == 200) {
          val tweetId = (resp.json \ "id_str").as[String]
          LOGGER.trace("New tweet posted: " + tweetId + " resp.body =\n  " + resp.body)
          TweetInfo( tweetId )
        } else {
          throw new IllegalArgumentException(s"Tweet not POSTed: HTTP ${resp.status}: ${resp.body}")
        }
      }
  }

}

/** Инфа по одному твиту. Потом наверное будет вынесена в отдельный файл модели. */
case class TweetInfo(id: String) extends IExtPostInfo {
  override def url: String = {
    "https://twitter.com/"
  } // TODO Надо что-то типа https://twitter.com/Flickr/status/423511451970445312
}
