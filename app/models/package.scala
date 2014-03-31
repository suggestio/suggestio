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
  type MartId_t         = MMart.MartId_t

  type MShop            = io.suggest.ym.model.MShop
  val  MShop            = io.suggest.ym.model.MShop
  type ShopId_t         = MShop.ShopId_t

  type MShopPriceList   = io.suggest.ym.model.MShopPriceList
  val  MShopPriceList   = io.suggest.ym.model.MShopPriceList

  type MShopPromoOffer  = io.suggest.ym.model.MShopPromoOffer
  val  MShopPromoOffer  = io.suggest.ym.model.MShopPromoOffer

  type MYmCategory      = io.suggest.ym.model.MYmCategory
  val  MYmCategory      = io.suggest.ym.model.MYmCategory

  val  AdShowLevels     = io.suggest.ym.model.AdShowLevels
  type AdShowLevel      = AdShowLevels.AdShowLevel

  // Начинаем MMartAd и смежные объекты.
  val  MMartAd          = io.suggest.ym.model.MMartAd
  type MMartAd          = io.suggest.ym.model.MMartAd
  type MMartAdT         = io.suggest.ym.model.MMartAdT[_]

  val MMartAdOfferTypes = io.suggest.ym.model.MMartAdOfferTypes
  type MMartAdOfferType = io.suggest.ym.model.MMartAdOfferType

  type MMartAdOfferT    = io.suggest.ym.model.MMartAdOfferT

  val  MMartAdProduct   = io.suggest.ym.model.MMartAdProduct
  type MMartAdProduct   = io.suggest.ym.model.MMartAdProduct

  val  MMartAdDiscount  = io.suggest.ym.model.MMartAdDiscount
  type MMartAdDiscount  = io.suggest.ym.model.MMartAdDiscount

  val  MMartAdText      = io.suggest.ym.model.MMartAdText
  type MMartAdText      = io.suggest.ym.model.MMartAdText

  val TextAlignValues   = io.suggest.ym.model.TextAlignValues

  val  MMartAdTextAlign = io.suggest.ym.model.MMartAdTextAlign
  type MMartAdTextAlign = io.suggest.ym.model.MMartAdTextAlign

  val  DiscountTemplate = io.suggest.ym.model.MMartAdDiscountTemplate
  type DiscountTemplate = io.suggest.ym.model.MMartAdDiscountTemplate

  val  MMAdFloatField   = io.suggest.ym.model.MMAdFloatField
  type MMAdFloatField   = io.suggest.ym.model.MMAdFloatField
  val  MMAdStringField  = io.suggest.ym.model.MMAdStringField
  type MMAdStringField  = io.suggest.ym.model.MMAdStringField

  val  MMartAdTAPhone   = io.suggest.ym.model.MMartAdTAPhone
  type MMartAdTAPhone   = io.suggest.ym.model.MMartAdTAPhone

  val  MMartAdTATablet  = io.suggest.ym.model.MMartAdTATablet
  type MMartAdTATablet  = io.suggest.ym.model.MMartAdTATablet

  val  MMAdFieldFont    = io.suggest.ym.model.MMAdFieldFont
  type MMAdFieldFont    = io.suggest.ym.model.MMAdFieldFont

  val  MMartAdPanelSettings = io.suggest.ym.model.MMartAdPanelSettings
  type MMartAdPanelSettings = io.suggest.ym.model.MMartAdPanelSettings

  val  MMAdPrice        = io.suggest.ym.model.MMAdPrice
  type MMAdPrice        = io.suggest.ym.model.MMAdPrice

  val  MImgInfo         = io.suggest.ym.model.MImgInfo
  type MImgInfo         = io.suggest.ym.model.MImgInfo

  val  MInx             = io.suggest.model.inx2.MInx
  type MInxT            = io.suggest.model.inx2.MInxT

  val  MMartInx         = io.suggest.model.inx2.MMartInx
  type MMartInx         = io.suggest.model.inx2.MMartInx

  val  MMartAdIndexed   = io.suggest.ym.model.MMartAdIndexed
  type MMartAdIndexed   = io.suggest.ym.model.MMartAdIndexed

  type BuyPlaceT[T <: BuyPlaceT[T]]  = io.suggest.ym.model.BuyPlaceT[T]

  val  MAdStat          = io.suggest.ym.model.stat.MAdStat
  type MAdStat          = io.suggest.ym.model.stat.MAdStat

  val  AdStatActions    = io.suggest.ym.model.stat.AdStatActions
  type AdStatAction     = io.suggest.ym.model.stat.AdStatAction
}
