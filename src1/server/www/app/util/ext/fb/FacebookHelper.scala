package util.ext.fb

import io.suggest.adv.ext.model.im.FbImgSizes
import javax.inject.Inject
import io.suggest.common.geom.d2.INamedSize2di
import io.suggest.ext.svc.MExtServices
import io.suggest.img.MImgFormats
import io.suggest.n2.node.MNode
import models.blk.SzMult_t
import models.mproj.ICommonDi
import util.adv.AdvUtil
import util.ext.IExtServiceHelper

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 11:06
  * Description: Класс с утилью для взаимодействия с facebook.
  */
class FacebookHelper @Inject() (
                                 override val advUtil: AdvUtil,
                                 override val mCommonDi: ICommonDi
                               )
  extends IExtServiceHelper
{

  override def mExtService = MExtServices.FaceBook

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
    FbImgSizes.withValueOpt(n)
  }

  /** В фейсбук если не постить горизонтально, то будет фотография на пасспорт вместо иллюстрации. */
  override def isAdvExtWide(mad: MNode) = true

  // akamaihd пересжимает jpeg в jpeg, png в png. Если ШГ, то надо слать увеличенный jpeg.
  override def imgFmtDflt = MImgFormats.PNG

  /** Дефолтовое значение szMult, если в конфиге не задано. */
  override def szMultDflt: SzMult_t = 1.0F

}
