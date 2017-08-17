package models.blk.ed

import io.suggest.ad.blk.BlockMeta
import io.suggest.ad.blk.ent.MEntity
import models.BfImage

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.15 11:32
 * Description: Модель контейнера результата маппинга данных блока формы редактирования карточки.
 */

case class BindResult(
  entites   : List[MEntity],
  blockMeta : BlockMeta,
  bim       : BlockImgMap,
  href      : Option[String]
) {

  def unapplyBIM(bfi: BfImage): BlockImgMap = {
    bim.filter(_._1 == bfi.bimKey)
  }

  def flatMapFirstOffer[T](f: MEntity => Option[T]): Option[T] = {
    entites
      .headOption
      .flatMap(f)
  }

}

