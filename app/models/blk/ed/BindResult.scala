package models.blk.ed

import models.blk.BlockMeta
import models.{BfImage, MEntity}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.15 11:32
 * Description: Модель контейнера результата маппинга данных блока формы редактирования карточки.
 */

case class BindResult(
  entites   : List[MEntity],
  blockMeta : BlockMeta,
  bim       : BlockImgMap
) {

  def unapplyBIM(bfi: BfImage): BlockImgMap = {
    bim.filter(_._1 == bfi.name)
  }

  def flatMapFirstOffer[T](f: MEntity => Option[T]): Option[T] = {
    entites
      .headOption
      .flatMap(f)
  }

}

