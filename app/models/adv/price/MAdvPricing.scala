package models.adv.price

import models.{IPrice, MPrice}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 14:46
 * Description: Информация о стоимости размения.
 */

trait IAdvPricing {
  def prices          : Iterable[IPrice]
  def hasEnoughtMoney : Boolean
}


case class MAdvPricing(
  prices          : Iterable[MPrice],
  hasEnoughtMoney : Boolean = true
)
  extends IAdvPricing
