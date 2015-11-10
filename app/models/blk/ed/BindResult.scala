package models.blk.ed

import io.suggest.ym.model.ad.IOffers
import io.suggest.ym.model.common.IEMBlockMeta
import models.{AOBlock, BfImage}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.15 11:32
 * Description: Модель контейнера результата маппинга данных блока формы редактирования карточки.
 */

case class BindResult(
  bd  : IEMBlockMeta with IOffers,
  bim : BlockImgMap
) {

  def unapplyBIM(bfi: BfImage): BlockImgMap = {
    bim.filter(_._1 == bfi.name)
  }

  def flatMapFirstOffer[T](f: AOBlock => Option[T]): Option[T] = {
    bd.offers
      .headOption
      .flatMap(f)
  }

}

