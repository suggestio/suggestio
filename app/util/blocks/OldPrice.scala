package util.blocks

import models._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:23
 * Description: Утиль для сборки блоков, содержащих поле старой цены oldPriceBf.
 */

object OldPrice {
  val BF_NAME_DFLT = "oldPrice"
  val BF_OLD_PRICE_DFLT = BfPrice(BF_NAME_DFLT)
}

/** Базовый интерфейсный трейт для реализаций oldPriceBf.
  * Добавляет поле в список полей формы редактора. */
trait OldPriceT extends ValT {
  def oldPriceBf: BfPrice
  abstract override def blockFieldsRev: List[BlockFieldT] = oldPriceBf :: super.blockFieldsRev
}

/** Статическая реализация поля. Используется общий статический инстанс поля oldPriceBf. */
trait OldPriceStatic extends OldPriceT {
  override def oldPriceBf = OldPrice.BF_OLD_PRICE_DFLT
}

/** Динамическая реализация oldPriceBf. */
trait OldPrice extends OldPriceT {
  def oldPriceDefaultValue: Option[AOPriceField] = None
  def oldPriceFontSizes: Set[Int] = Set.empty
  def oldPriceWithCoords: Boolean = false
  override def oldPriceBf = BfPrice(
    name = OldPrice.BF_NAME_DFLT,
    defaultValue = oldPriceDefaultValue,
    withFontSizes = oldPriceFontSizes,
    withCoords = oldPriceWithCoords
  )
}

