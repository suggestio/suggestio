package models.blk

import models.MPredicate
import models.im.MImgT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.15 12:07
  */
package object ed {

  /** Тип ключа в карте картинок блока. */
  type BimKey_t             = MPredicate
  type BlockImgMap          = Map[BimKey_t, MImgT]

}
