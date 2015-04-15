package models.mext

import java.net.URL

import _root_.util.adv.IServiceActorCompanion
import _root_.util.blocks.BgImg
import io.suggest.adv.ext.model.im.INamedSize2di
import io.suggest.util.UrlUtil
import models.adv.MExtTarget
import models.blk.{OneAdWideQsArgs, SzMult_t}
import models.im.{OutImgFmt, OutImgFmts}
import models.{IRenderable, MAd}
import play.api.Play._
import play.api.i18n.Messages
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:09
 * Description: Интерфейс одного экземпляра мега-модели внешних сервисов.
 */

trait IExtService {

  /** Строковой id. */
  def strId: String

  /** id приложения на стороне сервиса. */
  def APP_ID_OPT = configuration.getString(s"ext.adv.$strId.api.id")

  /** Отображамое имя, заданное через код в messages. */
  def nameI18N: String

  /** Код локализованного предложения "Я в фейсбук" */
  def iAtServiceI18N: String = "adv.ext.i.at." + strId

  /** Тестирование хоста на принадлежность к текущему сервису. */
  def isForHost(host: String): Boolean

  /** Нормализация ссылки цели, т.е. выбрасывание ненужного. */
  def normalizeTargetUrl(url: URL): String = {
    UrlUtil.normalize(url.toExternalForm)
  }

  /** Дефолтовая цель размещения, если есть. При создании узлов дефолтовые цели создаются автоматом. */
  def dfltTargetUrl: Option[String]

  /**
   * Создавать ли экземпляр этой модели для новых узлов?
   * @param adnId id узла.
   * @param lang язык. Для связи с Messages().
   * @return Some с экземпляром [[MExtTarget]].
   *         None, если по дефолту таргет создавать не надо.
   */
  def dfltTarget(adnId: String)(implicit lang: Messages): Option[MExtTarget]

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
  def szMult: SzMult_t = configuration.getDouble(s"ext.adv.$strId.szMult")
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

  /** Исповедовать ли широкое отображение при рендере карточки для размещения карточки на сервисе? */
  def isAdvExtWide(mad: MAd): Boolean = {
    mad.blockMeta.wide
  }

  /** Формат рендера в картинку загружаемой карточки. */
  def imgFmt: OutImgFmt = configuration.getString(s"ext.adv.$strId.fmt")
    .fold(imgFmtDflt)(OutImgFmts.withName)

  /** Дефолтовый формат изображения, если не задан в конфиге. */
  def imgFmtDflt: OutImgFmt = OutImgFmts.JPEG

  /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
  def oauth1Support: Option[IOAuth1Support] = None

  /** Какой ext-adv-service-актор надо использовать для взаимодействия с данным сервисом? */
  def extAdvServiceActor: IServiceActorCompanion

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  def myUserName: Option[String] = None

  /**
   * Генератор HTML-мета-тегов для описания рекламной карточки.
   * @param mad Экземпляр рекламной карточки.
   * @return экземпляры моделй, готовых для запуска рендера.
   */
  def adMetaTagsRender(mad: MAd)(implicit ec: ExecutionContext): Future[List[IRenderable]] = {
    Future successful Nil
  }

  /**
   * Вернуть поддержку multipart, если есть.
   * @return Some(), если сервис поддерживает multi-part upload.
   *         None, если сервис не поддерживает загрузку по multipart.
   */
  def maybeMpUpload: Option[IMpUploadSupport] = None

  /**
   * Если логин через этот сервис поддерживается, то тут API.
   * @return Some() если логин на suggest.io возможен через указанный сервис.
   */
  def loginProvider: Option[ILoginProvider] = None

}
