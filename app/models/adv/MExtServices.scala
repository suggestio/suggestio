package models.adv

import java.net.URL
import _root_.util.blocks.{BlocksConf, BgImg}
import io.suggest.adv.ext.model._, MServices._
import io.suggest.adv.ext.model.im.{VkWallImgSizesScalaEnumT, FbWallImgSizesScalaEnumT, INamedSize2di, ISize2di}
import io.suggest.util.UrlUtil
import io.suggest.ym.model.common.{MImgInfoMeta, MImgSizeT}
import models.{MAdT, MAd}
import models.adv.js.ctx.MJsCtx
import models.blk.{OneAdWideQsArgs, SzMult_t}
import models.im.{OutImgFmts, OutImgFmt}
import play.api.i18n.{Messages, Lang}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:13
 * Description: Модель сервисов для внешнего размещения рекламных карточек.
 * TODO ext.adv api v2: Модель осталась для совместимости, должна быть удалена или же стать неким каталогом скриптов,
 * либо ещё что-то...
 */
object MExtServices extends MServicesT {

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
    def dfltTarget(adnId: String)(implicit lang: Lang): Option[MExtTarget] = {
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
  }


  /** Сервис фейсбука. */
  val FACEBOOK: T = new Val(FACEBOOK_ID) {
    override def nameI18N = "Facebook"
    override def isForHost(host: String): Boolean = {
      "(?i)(www\\.)?facebook\\.(com|net)$".r.pattern.matcher(host).matches()
    }
    override def dfltTargetUrl = Some("https://facebook.com/me")

    /** Параметры картинки для размещения. */
    override def advPostMaxSz(tgUrl: String): INamedSize2di = {
      // TODO Узнать и добавить размеры для корня и /me.
      val path = new URL(tgUrl).getPath
      if ( (path startsWith "/me") || (path == "/") ||
           (path startsWith  "/groups/") || (tgUrl startsWith "/pages/") ||
           (tgUrl startsWith "/events/") || !(tgUrl startsWith "/profile.php") ) {
        FbImgSizes.FbCommunityLink
      } else {
        FbImgSizes.FbUserLink
      }
    }

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
  }


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

}


/** Реализация модели размеров картинок фейсбука. */
object FbImgSizes extends FbWallImgSizesScalaEnumT

/** Реализация модели размеров картинок vk. */
object VkImgSizes extends VkWallImgSizesScalaEnumT

