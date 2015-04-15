package models.mext.fb

import _root_.util.adv.ExtServiceActor
import io.suggest.adv.ext.model.im.INamedSize2di
import models.blk.SzMult_t
import models.im.OutImgFmts
import models.MAd
import io.suggest.adv.ext.model.im.FbWallImgSizesScalaEnumT

import models.mext.{ILoginProvider, IExtService}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:21
 * Description: Абстрактная реализация facebook-сервиса.
 */
trait FacebookService extends IExtService with FbLoginProvider {

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

  /** Поддержка логина через facebook. */
  override def loginProvider = Some(this)

}

/** Реализация модели размеров картинок фейсбука. */
object FbImgSizes extends FbWallImgSizesScalaEnumT
