package models.mext.tw

import io.suggest.ahc.util.NingUtil
import models.mctx.Context
import models.msc.SiteQsArgs
import util.n2u.N2NodesUtil
import util.{FormUtil, PlayMacroLogsI, TplDataFormatUtil}
import models.mext.{IExtPostInfo, IOAuth1Support, IOa1MkPostArgs}
import play.api.libs.oauth._
import play.api.libs.ws.WSClient
import play.api.Play.{configuration, current}
import NingUtil.ningFut2wsScalaFut
import org.asynchttpclient.AsyncHttpClient

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:16
 * Description: Реализация поддержки OAuth1.
 */

object OAuth1Support {

  /** Сколько текста можно напихать в начало твита? */
  // TODO Нужно нормальную длину узнать. Там какой-то гемор с ссылками, что даже 100 символов - многовато.
  val LEAD_TEXT_LEN = configuration.getInt("ext.tw.tweet.lead.text.maxlen") getOrElse 90

  /** Добавлять URL в твит? По логике да, но при отладке бывают проблемы с длиной твита и т.д, и URL лучше спилить. */
  val WITH_URL = configuration.getBoolean("ext.tw.tweet.with.url") getOrElse true

  /** URL ресурс API твиттинга. */
  def MK_TWEET_URL = "https://api.twitter.com/1.1/statuses/update.json"

  /**
   * Приведение html-текста из rich descr к тексту твита необходимой длины.
   * @param s Исходный текст rich descr.
   * @return строка, которая после добавления ссылки будет э
   */
  def rdescr2tweetLeadingText(s: String): String = {
    val s1 = FormUtil.stripHtml(s)
      .replaceAll("(?U)\\s+", " ")
      .replaceAllLiterally("...", TplDataFormatUtil.ELLIPSIS)
      .trim
    TplDataFormatUtil.strLimitLenNoTrailingWordPart(s1, LEAD_TEXT_LEN, hard = true)
  }

  /** URL для проверки валидности access_token'а. */
  def AC_TOK_VERIFY_URL = "https://api.twitter.com/1.1/account/verify_credentials.json?include_entities=false&skip_status=true"

}


import OAuth1Support._


trait OAuth1Support extends IOAuth1Support with PlayMacroLogsI { this: TwitterService =>

  protected val n2NodesUtil = current.injector.instanceOf[N2NodesUtil]

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

  /**
   * Сделать твит.
   * @param args Данные для постинга.
   * @see [[https://dev.twitter.com/rest/reference/post/statuses/update]]
   * @return Фьючерс с результатом работы.
   */
  override def mkPost(args: IOa1MkPostArgs)(implicit ws: WSClient, ec: ExecutionContext): Future[TweetInfo] = {
    import args._
    val ning = ws.underlying[AsyncHttpClient]
    val nreq = ning.preparePost(MK_TWEET_URL)

    // Собираем читабельный текст твита.
    val tweetTextOpt = mad.ad.richDescr
      .map { rd => rdescr2tweetLeadingText(rd.text) }
      .filter { !_.isEmpty }
    LOGGER.trace {
      tweetTextOpt match {
        case Some(tt) => s"Tweet readable text lenght = ${tt.length}: $tt"
        case None     => "Tweet text is empty. Only link will be tweeted."
      }
    }
    // Собираем ссылку в твите.
    val siteArgs = SiteQsArgs(
      povAdId = mad.id
    )
    val jsSt = {
      val b = returnTo.builder()
        .setAdnId( args.mnode.id.get )
        .setFocusedAdId( args.mad.id.get )
      for (producerId <- n2NodesUtil.madProducerId(args.mad)) {
        b.setFocusedProducerId(producerId)
      }
      b.toJsState
    }
    // twitter не трогает ссылки, до которых не может достучаться. Нужно помнить об этом.
    val tweetUrl = if (WITH_URL) {
      val urlPrefix = /*Context devReplaceLocalHostW127001*/ Context.SC_URL_PREFIX
      urlPrefix + controllers.routes.Sc.geoSite(jsSt, siteArgs)
    } else {
      ""
    }
    val fullTweetText = tweetTextOpt.fold(tweetUrl)(_ + " " + tweetUrl)
    nreq.addFormParam("status", fullTweetText)
    if (geo.isDefined) {
      val g = geo.get
      nreq.addFormParam("lat", g.lat.toString)
          .addFormParam("lon", g.lon.toString)
    }
    // Приаттачить аттачменты к твиту.
    val medias = args.attachments
    if (medias.nonEmpty) {
      val v = medias.toIterator.map(_.strId).mkString(",")
      nreq.addFormParam("media_ids", v)
    }
    // Начать постинг.
    val req = nreq
      .setSignatureCalculator( sigCalc(acTok) )
      .build()
    LOGGER.trace("Tweet POSTing to: " + req.getUrl)
    ning.executeRequest(req)
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
