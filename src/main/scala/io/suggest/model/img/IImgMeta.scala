package io.suggest.model.img

import io.suggest.common.geom.d2.{ISize2diWrap, ISize2di}
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 17:15
 * Description: Очень абстрактный доступ к метаданным изображения.
 */
trait IImgMeta extends ISize2di {

  def dateCreated: DateTime

}


case class ImgSzDated(
  sz          : ISize2di,
  dateCreated : DateTime
)
  extends IImgMeta
  with ISize2diWrap
{
  override def _sz2dUnderlying = sz
}
