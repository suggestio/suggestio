package util.blocks

import models._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:20
 * Description: Утиль для поля priceBf, содержащего актуальную цену товара/услуги в блоке.
 */

object Price {
  val BF_NAME_DFLT = "price"
  val BF_PRICE_DFLT = BfPrice(BF_NAME_DFLT)
}


/** Базовый трейт для трейтов-реализаций bfPrice. Добавляет поле в форму редактора. */
trait PriceT extends ValT {
  def priceBf: BfPrice
  abstract override def blockFieldsRev: List[BlockFieldT] = priceBf :: super.blockFieldsRev
}


/** Статическая реализация priceBf. Инстанс расшарен между разными блоками. */
trait PriceStatic extends PriceT {
  override final def priceBf = Price.BF_PRICE_DFLT
}


/** Динамическая реализация priceBf. Инстанс собирается на основе параметров,
  * задаваемых через соответсвующие методы. */
trait Price extends PriceT {
  def priceDefaultValue: Option[AOPriceField] = None
  def priceFontSizes: Set[Int] = Set.empty
  def priceWithCoords: Boolean = false
  override def priceBf = BfPrice(
    name = Price.BF_NAME_DFLT,
    defaultValue = priceDefaultValue,
    withFontSizes = priceFontSizes,
    withCoords = priceWithCoords
  )
}

