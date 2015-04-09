package models.adv

import java.net.URL
import _root_.util.PlayLazyMacroLogsImpl
import _root_.util.adv.{OAuth1ServiceActor, ExtServiceActor, IServiceActorCompanion}
import _root_.util.blocks.BgImg
import io.suggest.adv.ext.model._, MServices._
import io.suggest.adv.ext.model.im.{VkWallImgSizesScalaEnumT, FbWallImgSizesScalaEnumT, INamedSize2di}
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.geo.GeoPoint
import io.suggest.util.UrlUtil
import io.suggest.ym.model.common.MImgInfoMeta
import models.{MImgSizeT, MAd}
import models.adv.js.ctx.MJsCtx
import models.blk.{OneAdWideQsArgs, SzMult_t}
import models.im.{OutImgFmts, OutImgFmt}
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play._
import play.api.libs.oauth._
import play.api.libs.ws.WSClient
import org.apache.http.client.utils.URIBuilder

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Модель сервисов для внешнего размещения рекламных карточек.
 * TODO Нужно вынести эту модель в отдельный пакет в models, разбить на куски и наверное объеденить с моделью IdProviders.
 */
object MExtServices extends MServicesT with PlayLazyMacroLogsImpl {

  override type T = Val

  /** Экземпляр модели. */
  protected abstract class Val(strId: String) extends super.Val(strId) {

    /** id приложения на стороне сервиса. */
    val APP_ID_OPT = configuration.getString(s"ext.adv.$strId.api.id")

    def nameI18N: String
    def iAtServiceI18N: String = "adv.ext.i.at." + strId

    def isForHost(host: String): Boolean
    def normalizeTargetUrl(url: URL): String = {
      UrlUtil.normalize(url.toExternalForm)
    }

    def dfltTargetUrl: Option[String]

    /**
     * Создавать ли экземпляр этой модели для новых узлов?
     * @param adnId id узла.
     * @param lang язык. Для связи с Messages().
     * @return Some с экземпляром [[MExtTarget]].
     *         None, если по дефолту таргет создавать не надо.
     */
    def dfltTarget(adnId: String)(implicit lang: Messages): Option[MExtTarget] = {
      dfltTargetUrl.map { url =>
        MExtTarget(
          url     = url,
          adnId   = adnId,
          service = this,
          name    = Some(Messages(iAtServiceI18N))
        )
      }
    }

    /**
     * Бывает нужно закинуть в контекст какие-то данные для доступа к сервису или иные параметры.
     * @param mctx Исходный JSON контекст.
     * @return Обновлённый JSON контекст.
     */
    def prepareContext(mctx: MJsCtx): MJsCtx = {
      mctx.copy(
        service = Some(this)
      )
    }

    /**
     * Клиент прислал upload-ссылку. Нужно её проверить на валидность.
     * @param url Ссылка.
     * @return true если upload-ссылка корректная. Иначе false.
     */
    def checkImgUploadUrl(url: String): Boolean = false

    /**
     * Максимальные размеры картинки при постинге в соц.сеть в css-пикселях.
     * @return None если нет размеров, и нужно постить исходную карточку без трансформации.
     */
    def advPostMaxSz(tgUrl: String): INamedSize2di
    
    /** Найти стандартный (в рамках сервиса) размер картинки. */
    def postImgSzWithName(n: String): Option[INamedSize2di]

    /**
     * Мультипликатор размера для экспортируемых на сервис карточек.
     * @return SzMult_t.
     */
    val szMult: SzMult_t = configuration.getDouble(s"ext.adv.$strId.szMult")
      .fold(szMultDflt)(_.toFloat)

    /** Дефолтовое значение szMult, если в конфиге не задано. */
    def szMultDflt: SzMult_t = 1.0F

    /** Разрешен ли и необходим ли wide-постинг? Без учета szMult, т.к. обычно он отличается от заявленного. */
    def advExtWidePosting(tgUrl: String, mad: MAd, szMult: SzMult_t = szMult): Option[OneAdWideQsArgs] = {
      if (isAdvExtWide(mad)) {
        val sz = advPostMaxSz(tgUrl)
        val v = OneAdWideQsArgs(
          width = BgImg.szMulted(sz.width, szMult)
        )
        Some(v)
      } else {
        None
      }
    }
    def isAdvExtWide(mad: MAd): Boolean = {
      mad.blockMeta.wide
    }

    /** Формат рендера в картинку загружаемой карточки. */
    val imgFmt: OutImgFmt = configuration.getString(s"ext.adv.$strId.fmt")
      .fold(imgFmtDflt)(OutImgFmts.withName)

    /** Дефолтовый формат изображения, если не задан в конфиге. */
    def imgFmtDflt: OutImgFmt = OutImgFmts.JPEG

    /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
    def oauth1Support: Option[IOAuth1Support] = None

    /** Какой ext-adv-service-актор надо использовать для взаимодействия с данным сервисом? */
    def extAdvServiceActor: IServiceActorCompanion

    /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
    def myUserName: Option[String] = None
  }



  /** Сервис вконтакта. */
  val VKONTAKTE: T = new Val(VKONTAKTE_ID) {
    override def nameI18N = "VKontakte"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?vk(ontakte)?\\.(com|ru|me)$".r.pattern.matcher(host).find()
    }

    override def checkImgUploadUrl(url: String): Boolean = {
      val v = try {
        new URL(url)
          .getHost
          .contains(".vk")
      } catch {
        case ex: Throwable => false
      }
      v || super.checkImgUploadUrl(url)
    }

    override def dfltTargetUrl = Some("https://vk.com/")

    /**
     * Максимальные размеры картинки при постинге во вконтакт в соц.сеть в css-пикселях.
     * Система будет пытаться вписать картинку в этот размер.
     * У вконтакта экспирементальная макс.ширина - 601px почему-то.
     * @see [[https://vk.com/vkrazmer]]
     * @see [[https://pp.vk.me/c617930/v617930261/4b62/S2KQ45_JHM0.jpg]] хрень?
     * @return Экземпляр 2D-размеров.
     */
    override def advPostMaxSz(tgUrl: String) = VkImgSizes.VkWallDflt

    /** Найти стандартный (в рамках сервиса) размер картинки. */
    override def postImgSzWithName(n: String): Option[INamedSize2di] = {
      VkImgSizes.maybeWithName(n)
    }

    /** VK работает через openapi.js. */
    override def extAdvServiceActor = ExtServiceActor
  }


  /** Сервис фейсбука. */
  val FACEBOOK: T = new Val(FACEBOOK_ID) {
    override def nameI18N = "Facebook"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?facebook\\.(com|net)$".r.pattern.matcher(host).matches()
    }
    override def dfltTargetUrl = Some("https://facebook.com/me")

    /** Параметры картинки для размещения. */
    override def advPostMaxSz(tgUrl: String): INamedSize2di = FbImgSizes.FbPostLink

    /** Найти стандартный (в рамках сервиса) размер картинки. */
    override def postImgSzWithName(n: String): Option[INamedSize2di] = {
      FbImgSizes.maybeWithName(n)
    }

    /** В фейсбук если не постить горизонтально, то будет фотография на пасспорт вместо иллюстрации. */
    override def isAdvExtWide(mad: MAd) = true

    // akamaihd пересжимает jpeg в jpeg, png в png. Если ШГ, то надо слать увеличенный jpeg.
    override def imgFmtDflt = OutImgFmts.PNG

    /** Дефолтовое значение szMult, если в конфиге не задано. */
    override def szMultDflt: SzMult_t = 1.0F

    /** FB работает через js API. */
    override def extAdvServiceActor = ExtServiceActor
  }


  /** Сервис твиттера. */
  val TWITTER: T = new Val(TWITTER_ID) {
    override def nameI18N = "Twitter"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?twitter\\.com".r.pattern.matcher(host).matches()
    }
    override def dfltTargetUrl = None

    /** Найти стандартный (в рамках сервиса) размер картинки. */
    override def postImgSzWithName(n: String) = ???  // TODO

    override def advPostMaxSz(tgUrl: String) = ???    // TODO

    /** Префикс ключей конфигурации. Конфиг расшарен с secure-social. */
    def confPrefix = "securesocial.twitter"

    /** twitter работает через OAuth1. */
    override def extAdvServiceActor = OAuth1ServiceActor

    /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
    override def oauth1Support = Some(OAuth1Support)

    /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
    override def myUserName = Some("@suggest_io")

    /** Реализация поддержки OAuth1. */
    object OAuth1Support extends IOAuth1Support {
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
       * Запостить твит через OAuth1.
       * @param mad Рекламная карточка.
       * @param acTok access_token.
       * @param geo Необязательная геоточка, к которой привязан твит.
       * @see [[https://dev.twitter.com/rest/reference/post/statuses/update]]
       * @return Фьючерс с результатом работы.
       */
      override def mkPost(mad: MAd, acTok: RequestToken, geo: Option[GeoPoint] = None)
                         (implicit ws: WSClient, ec: ExecutionContext): Future[TweetInfo] = {
        val b = new URIBuilder(MK_TWEET_URL)
        b.addParameter("status", "Hello, world!")   // TODO Генерить текст твита из описания карточки со ссылкой на страницу.
        if (geo.isDefined) {
          val g = geo.get
          b.addParameter("lat", g.lat.toString)
          b.addParameter("lon", g.lon.toString)
        }
        ws.url(b.build().toASCIIString)
          .sign( sigCalc(acTok) )
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

      case class TweetInfo(id: String) extends IExtPostInfo {
        override def url: String = "https://twitter.com/" // TODO Надо что-то типа https://twitter.com/Flickr/status/423511451970445312
      }

    }

  } // TWITTER


  /**
   * Поиск подходящего сервиса для указанного хоста.
   * @param host Хостнейм искомого сервиса.
   * @return Сервис, если такой есть.
   */
  def findForHost(host: String): Option[MExtService] = {
    values
      .find(_.isForHost(host))
      .map(value2val)
  }


  /** Десериализация из JSON. Всё можно прочитать по имени. */
  implicit def reads: Reads[T] = {
    (__ \ NAME_FN)
      .read[String]
      .map { withName }
  }

  /** Сериализация в JSON. */
  implicit def writes: Writes[T] = (
    (__ \ NAME_FN).write[String] and
    (__ \ APP_ID_FN).writeNullable[String]
  ){ s => (s.strId, s.APP_ID_OPT) }


  /** Поддержка OAuth1 задается реализацией этого интерфейса. */
  sealed trait IOAuth1Support {
    /** Доступ к oauth-клиенту для логина и получения access_token'а. */
    def client: OAuth

    /** Быстрый доступ к ключу сервиса. Обычно перезаписывается в реализациях и не зависит от клиента. */
    def consumerKey: ConsumerKey = client.info.key

    /** В каких размерах должно открываться окно авторизации OAuth1. */
    def popupWndSz: MImgSizeT = MImgInfoMeta(height = 400, width = 400)

    /** Проверка валидности access_token'a силами модели. */
    def isAcTokValid(acTok: RequestToken)(implicit ws: WSClient, ec: ExecutionContext): Future[Boolean]

    def sigCalc(acTok: RequestToken) = OAuthCalculator(consumerKey, acTok)

    /** Сделать пост в сервисе. */
    def mkPost(mad: MAd, acTok: RequestToken, geo: Option[GeoPoint] = None)
              (implicit ws: WSClient, ec: ExecutionContext): Future[IExtPostInfo]
  }

}

/** Абстрактные метаданные по посту на внешнем сервисе. */
sealed trait IExtPostInfo {
  def id: String
  def url: String
}

/** Реализация модели размеров картинок фейсбука. */
object FbImgSizes extends FbWallImgSizesScalaEnumT

/** Реализация модели размеров картинок vk. */
object VkImgSizes extends VkWallImgSizesScalaEnumT

