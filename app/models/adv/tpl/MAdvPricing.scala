package models.adv.tpl

import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 14:46
 * Description: Информация о стоимости размения.
 */

trait IAdvPricing {
  def prices          : Iterable[(Currency, Float)]
  def hasEnoughtMoney : Boolean
}


case class MAdvPricing(
  prices          : Iterable[(Currency, Float)],
  hasEnoughtMoney : Boolean
)
  extends IAdvPricing
