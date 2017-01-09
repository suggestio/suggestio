package util.ext.fb

import com.google.inject.{Inject, Singleton}
import io.suggest.common.geom.d2.INamedSize2di
import models.MNode
import models.blk.SzMult_t
import models.im.OutImgFmts
import models.mext.MExtServices
import models.mext.fb.FbImgSizes
import models.mproj.ICommonDi
import util.ext.IExtServiceHelper

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 11:06
  * Description: Класс с утилью для взаимодействия с facebook.
  */
@Singleton
class FacebookHelper @Inject() (
  override val mCommonDi: ICommonDi
)
  extends IExtServiceHelper
{

  override def mExtService = MExtServices.FACEBOOK

  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?facebook\\.(com|net)$".r
      .pattern
      .matcher(host)
      .matches()
  }
  /** Параметры картинки для размещения. */
  override def advPostMaxSz(tgUrl: String): INamedSize2di = {
    FbImgSizes.FbPostLink
  }

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

}
