package models.mext.fb

import io.suggest.adv.ext.model.im.FbWallImgSizesScalaEnumT
import io.suggest.common.geom.d2.INamedSize2di
import models.MNode
import models.blk.SzMult_t
import models.im.OutImgFmts
import models.mext.IJsActorExtService
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:21
 * Description: Абстрактная реализация facebook-сервиса.
 */
trait FacebookService extends IJsActorExtService with FbLoginProvider with PlayMacroLogsImpl {

  /** URL главной страницы сервиса. */
  override def mainPageUrl: String = "https://facebook.com/"

  /** Если не задать класс для логгера, то в логах будет "MExtServices$anon$2". */
  override protected def _loggerClass = classOf[FacebookService]

  override def nameI18N = "Facebook"

  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?facebook\\.(com|net)$".r.pattern.matcher(host).matches()
  }
  override def dfltTargetUrl = Some(mainPageUrl + "me")

  /** Параметры картинки для размещения. */
  override def advPostMaxSz(tgUrl: String): INamedSize2di = FbImgSizes.FbPostLink

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  override def postImgSzWithName(n: String): Option[INamedSize2di] = {
    FbImgSizes.maybeWithName(n)
  }

  /** В фейсбук если не постить горизонтально, то будет фотография на пасспорт вместо иллюстрации. */
  override def isAdvExtWide(mad: MNode) = true

  // akamaihd пересжимает jpeg в jpeg, png в png. Если ШГ, то надо слать увеличенный jpeg.
  override def imgFmtDflt = OutImgFmts.PNG

  /** Дефолтовое значение szMult, если в конфиге не задано. */
  override def szMultDflt: SzMult_t = 1.0F

  /** Поддержка логина через facebook. */
  override def loginProvider = Some(this)

}

/** Реализация модели размеров картинок фейсбука. */
object FbImgSizes extends FbWallImgSizesScalaEnumT
