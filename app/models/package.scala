/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:47
 * Description: После переноса ряда моделей в sioutil, тут появились костыли.
 */

package object models {

  type MCompany         = io.suggest.ym.model.MCompany
  val  MCompany         = io.suggest.ym.model.MCompany

  type MMart            = io.suggest.ym.model.MMart
  val  MMart            = io.suggest.ym.model.MMart

  type MShop            = io.suggest.ym.model.MShop
  val  MShop            = io.suggest.ym.model.MShop

  type MShopPriceList   = io.suggest.ym.model.MShopPriceList
  val  MShopPriceList   = io.suggest.ym.model.MShopPriceList

  type MShopPromoOffer  = io.suggest.ym.model.MShopPromoOffer
  val  MShopPromoOffer  = io.suggest.ym.model.MShopPromoOffer

  type MYmCategory      = io.suggest.ym.model.MYmCategory
  val  MYmCategory      = io.suggest.ym.model.MYmCategory

  val  AdShowLevels     = io.suggest.ym.model.AdShowLevels
  type AdShowLevel      = AdShowLevels.AdShowLevel
}
