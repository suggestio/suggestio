package util.ext

import io.suggest.ahc.util.NingUtil.ningFut2wsScalaFut
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.common.html.HtmlConstants
import io.suggest.di.IWsClient
import io.suggest.util.logs.IMacroLogs
import models.mctx.IContextUtilDi
import models.mext.{IExtPostInfo, IOAuth1MkPostArgs, MExtPostInfo}
import models.msc.SiteQsArgs
import play.api.Configuration
import play.api.libs.json.JsValue
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.api.libs.oauth._
import util.n2u.IN2NodesUtilDi
import util.{FormUtil, TplDataFormatUtil}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:16
 * Description: Трейты поддержки OAuth1 в хелперах ext-сервисов.
 */


/** Поддержка OAuth1 на сервисе описывается этим интерфейсом. */
trait IOAuth1Support {

  /** Доступ к oauth-клиенту для логина и получения access_token'а. */
  def client: OAuth

  /** Быстрый доступ к ключу сервиса. Обычно перезаписывается в реализациях и не зависит от клиента. */
  def consumerKey: ConsumerKey = client.info.key

  /** В каких размерах должно открываться окно авторизации OAuth1. */
  def popupWndSz: ISize2di = MSize2di(height = 400, width = 400)

  /** Проверка валидности access_token'a силами модели. */
  def isAcTokValid(acTok: RequestToken): Future[Boolean]

  /** Калькулятор oauth1-сигнатур запросов. */
  def sigCalc(acTok: RequestToken) = new OAuthCalculator(consumerKey, acTok)

  /** Необходимо ли делать mp-upload карточки на сервер перед вызовом mkPost?
    * Если true, то текущий сервис должен поддерживать mpUpload. */
  def isMkPostNeedMpUpload: Boolean

  /**
   * Запостить твит через OAuth1.
   * @param args Данные для постинга.
   * @return Фьючерс с результатом работы.
   */
  def mkPost(args: IOAuth1MkPostArgs): Future[IExtPostInfo]

}


/** Реализация поддержки OAuth1 для сервиса. */
trait OAuth1Support
  extends IOAuth1Support
  with IMacroLogs
  with IContextUtilDi
  with IN2NodesUtilDi
  with IWsClient
{

  protected[this] def configuration: Configuration
  implicit protected[this] def ec: ExecutionContext

  /** Сколько текста можно напихать в начало твита? */
  def LEAD_TEXT_LEN: Int

  /** Добавлять URL в твит? По логике да, но при отладке бывают проблемы с длиной твита и т.д, и URL лучше спилить. */
  def WITH_URL: Boolean

  /** URL ресурс API твиттинга. */
  def MK_MESSAGE_URL: String

  /** URL для проверки валидности access_token'а. */
  def AC_TOK_VERIFY_URL: String

  /**
   * Приведение html-текста из rich descr к тексту твита необходимой длины.
   * @param s Исходный текст rich descr.
   * @return строка, которая после добавления ссылки будет э
   */
  def rdescr2tweetLeadingText(s: String): String = {
    val s1 = FormUtil.stripHtml(s)
      .replaceAll("(?U)\\s+", " ")
      .replace("...", HtmlConstants.ELLIPSIS)
      .trim
    TplDataFormatUtil.strLimitLenNoTrailingWordPart(s1, LEAD_TEXT_LEN, hard = true)
  }


  /** Префикс ключей конфигурации. Конфиг расшарен с secure-social. */
  protected[this] def ssConfPrefix: String

  /** 2015.apr.14: 28cdf84ad875 twitter cards отнесены в печку, т.к. отображаются скрытыми.
    * Загрузка картинки будет идти напрямую в твиттер и затем публикация твита со встроенным media. */
  override def isMkPostNeedMpUpload = true

  /** Ключи приложения для доступа к public API. */
  override lazy val consumerKey: ConsumerKey = {
    val cp = ssConfPrefix
    ConsumerKey(
      key     = configuration.getOptional[String](cp + ".consumerKey").get,
      secret  = configuration.getOptional[String](cp + ".consumerSecret").get
    )
  }

  protected def REQUEST_TOKEN_URL_DFLT: String
  protected def ACCESS_TOKEN_URL_DFLT: String
  protected def AUTHORIZATION_URL: String

  /** Синхронный OAuth1-клиент для твиттера */
  override lazy val client: OAuth = {
    val cp = ssConfPrefix
    OAuth(
      ServiceInfo(
        requestTokenURL  = configuration.getOptional[String](cp + ".requestTokenUrl")
          .getOrElse( REQUEST_TOKEN_URL_DFLT ),
        accessTokenURL   = configuration.getOptional[String](cp + ".accessTokenUrl")
          .getOrElse( ACCESS_TOKEN_URL_DFLT ),
        // securesocial должна по идее использовать /authentificate, а не authorize. Поэтому, отвязываем значение.
        authorizationURL = AUTHORIZATION_URL,
        consumerKey
      ),
      use10a = true
    )
  }

  /**
   * Проверка валидности access_tokena силами модели.
   * @param acTok Проверяемый access_token.
   * @return Фьючерс с true, если токен точно валиден сейчас.
   * @see [[https://dev.twitter.com/rest/reference/get/account/verify_credentials]]
   */
  override def isAcTokValid(acTok: RequestToken): Future[Boolean] = {
    wsClient.url( AC_TOK_VERIFY_URL )
      .sign( sigCalc(acTok) )
      .get()
      .map { resp =>
        val res = resp.status == 200
        if (!res)
          LOGGER.debug(s"Server said, that access_token ${acTok.token} invalid: HTTP ${resp.status} ${resp.statusText}\n ${resp.body}")
        res
      }
  }

  /**
   * Сделать твит.
   * @param args Данные для постинга.
   * @see [[https://dev.twitter.com/rest/reference/post/statuses/update]]
   * @return Фьючерс с результатом работы.
   */
  override def mkPost(args: IOAuth1MkPostArgs): Future[MExtPostInfo] = {
    import args._
    val ning = wsClient.underlying[AsyncHttpClient]
    val nreq = ning.preparePost(MK_MESSAGE_URL)

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
      val call = controllers.sc.routes.ScSite.geoSite(jsSt, siteArgs)
      ctxUtil.toScAbsUrl( call )
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
      val v = medias
        .iterator
        .map(_.strId)
        .mkString(",")
      nreq.addFormParam("media_ids", v)
    }
    // Начать постинг.
    val req = nreq
      .setSignatureCalculator( sigCalc(acTok) )
      .build()

    LOGGER.trace("Start POSTing to: " + req.getUrl)

    ning.executeRequest(req)
      .map { resp =>
        if (resp.status == 200) {
          val tweetId = (resp.body[JsValue] \ "id_str").as[String]
          LOGGER.trace("New tweet posted: " + tweetId + " resp.body =\n  " + resp.body)
          MExtPostInfo( tweetId )
        } else {
          throw new IllegalArgumentException(s"Tweet not POSTed: HTTP ${resp.status}: ${resp.body}")
        }
      }
  }

}
