package util.blocks

import BlocksUtil._
import models._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:27
 * Description: Утиль для сборки блоков, содержащих поле discountBf.
 */

object Discount {
  val BF_NAME_DFLT = "discount"
  val DISCOUNT_BF_VALUE_DFLT: Option[AOFloatField] = Some(AOFloatField(50F, defaultFont))
  val DISCOUNT_MIN_DFLT = -99F
  val DISCOUNT_MAX_DFLT = 100F
  val DISCOUNT_BF_DFLT = BfDiscount(
    name = BF_NAME_DFLT,
    min = DISCOUNT_MIN_DFLT,
    max = DISCOUNT_MAX_DFLT,
    defaultValue = DISCOUNT_BF_VALUE_DFLT
  )
}

/** Базовый интерфейсный трейт для реализаций поля discountBf. */
trait DiscountT extends ValT {
  def discountBf: BfDiscount
  abstract override def blockFieldsRev: List[BlockFieldT] = discountBf :: super.blockFieldsRev
}

/** Статическая реализация discount-поля. Достаточна для большинства случаев. */
trait DiscountStatic extends DiscountT {
  def discountBf = Discount.DISCOUNT_BF_DFLT
}
