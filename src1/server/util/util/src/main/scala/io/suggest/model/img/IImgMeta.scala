package io.suggest.model.img

import java.time.OffsetDateTime

import io.suggest.common.geom.d2.{ISize2di, ISize2diWrap}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 17:15
 * Description: Очень абстрактный доступ к метаданным изображения.
 */
trait IImgMeta extends ISize2di {

  def dateCreated: OffsetDateTime

}


case class ImgSzDated(
  sz          : ISize2di,
  dateCreated : OffsetDateTime
)
  extends IImgMeta
  with ISize2diWrap
{
  override def _sz2dUnderlying = sz
}
