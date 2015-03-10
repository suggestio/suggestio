package models.im

import models.ImgCrop

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.03.15 17:33
 * Description: Модель для представления инфы по кроппингу.
 */
case class ImgCropInfo(
  crop      : ImgCrop,
  isCenter  : Boolean
)
