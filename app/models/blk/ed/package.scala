package models.blk

import models.im.MImgT
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.15 12:07
  */
package object ed {

  /** Модель сохраненных картинок карточки-блока. */
  type Imgs_t               = io.suggest.ym.model.common.EMImg.Imgs_t

  /** Тип ключа в карте картинок блока. */
  type BimKey_t             = String
  type BlockImgEntry        = (BimKey_t, MImgT)
  type BlockImgMap          = Map[BimKey_t, MImgT]

  /** Тип маппинга формы рекламной карточки. */
  type AdFormM              = Form[AdFormResult]

}
