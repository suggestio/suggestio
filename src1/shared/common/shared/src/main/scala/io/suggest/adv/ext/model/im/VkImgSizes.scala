package io.suggest.adv.ext.model.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.geom.d2.{INamedSize2di, MSize2di}
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.15 17:50
 * Description: Модели размеров настенных картинок для vk-сервиса.
 */
object VkImgSizes extends StringEnum[VkImgSize] {

  case object VkWallDflt extends VkImgSize("vk1") {
    override def whPx = MSize2di(
      height = 700,
      width  = 1100
    )
  }

  override def values = findValues

}


sealed abstract class VkImgSize(override val value: String) extends StringEnumEntry with INamedSize2di {
  @deprecated def szAlias: String = value
}

object VkImgSize {

  implicit def vkImgSizeFormat: Format[VkImgSize] =
    EnumeratumUtil.valueEnumEntryFormat( VkImgSizes )

}
