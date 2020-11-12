package io.suggest.common.geom.d2

import play.api.data.Mapping

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 15:32
  * Description: Поддержка ES для кросс-платформенной модели MSize2di.
  */
object MSize2diJvm {

  /** Form-маппинг для SysImgMake. Код вынесен из BlockMetaJvm. */
  def size2dFormMapping: Mapping[MSize2di] = {
    import play.api.data.Forms._
    val sizeMapping = number(1, 5000)
    mapping(
      "width"   -> sizeMapping,
      "height"  -> sizeMapping,
    )
    { MSize2di.apply }
    { MSize2di.unapply }
  }

}
