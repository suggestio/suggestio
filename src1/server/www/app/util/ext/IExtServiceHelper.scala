package util.ext

import java.net.URL

import io.suggest.common.geom.d2.INamedSize2di
import io.suggest.ext.svc.MExtService
import io.suggest.img.{MImgFmt, MImgFmts}
import io.suggest.model.n2.node.MNode
import io.suggest.text.util.UrlUtil
import models.blk.{OneAdWideQsArgs, SzMult_t, szMulted}
import models.mproj.IMCommonDi
import models.IRenderable
import util.adv.IAdvUtilDi

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 10:56
  * Description: Базовый трейт инжектируемых хелперов для внешних сервисов.
  * Появился при распиливании растолстевшего [[models.mext.IExtService]] на данные модели и логику,
  * которая перешла сюда в ходе DI-рефакторинга.
  */
trait IExtServiceHelper
  extends IMCommonDi
  with IAdvUtilDi
{

  import mCommonDi._

  def mExtService: MExtService

  /** id приложения на стороне сервиса. */
  lazy val APP_ID_OPT: Option[String] = {
    configuration.getOptional[String](s"ext.adv.${mExtService.value}.api.id")
  }

  /** Тестирование хоста на принадлежность к текущему сервису. */
  def isForHost(host: String): Boolean

  /** Нормализация ссылки цели, т.е. выбрасывание ненужного. */
  def normalizeTargetUrl(url: URL): String = {
    UrlUtil.normalize(url.toExternalForm)
  }

  /**
    * Клиент прислал upload-ссылку. Нужно её проверить на валидность.
    *
    * @param url Ссылка.
    * @return true если upload-ссылка корректная. Иначе false.
    */
  def checkImgUploadUrl(url: String): Boolean = false

  /**
    * Максимальные размеры картинки при постинге в соц.сеть в css-пикселях.
    *
    * @return None если нет размеров, и нужно постить исходную карточку без трансформации.
    */
  def advPostMaxSz(tgUrl: String): INamedSize2di

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  def postImgSzWithName(n: String): Option[INamedSize2di]

  /**
    * Мультипликатор размера для экспортируемых на сервис карточек.
    *
    * @return SzMult_t.
    */
  def szMult: SzMult_t = szMultDflt

  /** Дефолтовое значение szMult, если в конфиге не задано. */
  def szMultDflt: SzMult_t = 1.0F

  /** Разрешен ли и необходим ли wide-постинг? Без учета szMult, т.к. обычно он отличается от заявленного. */
  def advExtWidePosting(tgUrl: String, mad: MNode, szMult: SzMult_t = szMult): Option[OneAdWideQsArgs] = {
    if (isAdvExtWide(mad)) {
      val sz = advPostMaxSz(tgUrl)
      val v = OneAdWideQsArgs(
        width = szMulted(sz.whPx.width, szMult)
      )
      Some(v)
    } else {
      None
    }
  }

  /** Исповедовать ли широкое отображение при рендере карточки для размещения карточки на сервисе? */
  def isAdvExtWide(mad: MNode): Boolean = {
    advUtil.getAdvMainBlockMeta(mad)
      .exists(_.wide)
  }

  /** Формат рендера в картинку загружаемой карточки. */
  def imgFmt = imgFmtDflt

  /** Дефолтовый формат изображения, если не задан в конфиге. */
  def imgFmtDflt: MImgFmt = MImgFmts.JPEG

  /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
  def oauth1Support: Option[IOAuth1Support] = None

  /**
   * Генератор HTML-мета-тегов для описания рекламной карточки.
   *
   * @param mad Экземпляр рекламной карточки.
   * @return экземпляры моделй, готовых для запуска рендера.
   */
  def adMetaTagsRender(mad: MNode): Future[List[IRenderable]] = {
    Future.successful(Nil)
  }

  /**
    * Вернуть поддержку multipart, если есть.
    *
    * @return Some(), если сервис поддерживает multi-part upload.
    *         None, если сервис не поддерживает загрузку по multipart.
    */
  def maybeMpUpload: Option[IExtMpUploadSupport] = None

}
