import io.suggest.ym.model.common
import io.suggest.ym.model.common.{AdPanelSettings, TextAlignPhone, TextAlignTablet, TextAlign}

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

  val  MWelcomeAd       = io.suggest.ym.model.MWelcomeAd
  type MWelcomeAd       = io.suggest.ym.model.MWelcomeAd

  val MMartAdOfferTypes = io.suggest.ym.model.MMartAdOfferTypes
  type MMartAdOfferType = io.suggest.ym.model.MMartAdOfferType

  type MMartAdOfferT    = io.suggest.ym.model.ad.AdOfferT

  val  MMartAdProduct   = io.suggest.ym.model.ad.AOProduct
  type MMartAdProduct   = io.suggest.ym.model.ad.AOProduct

  val  MMartAdDiscount  = io.suggest.ym.model.ad.AODiscount
  type MMartAdDiscount  = io.suggest.ym.model.ad.AODiscount

  val  MMartAdText      = io.suggest.ym.model.ad.AOText
  type MMartAdText      = io.suggest.ym.model.ad.AOText

  val TextAlignValues   = io.suggest.ym.model.ad.AOTextAlign

  val  MMartAdTextAlign = MMartAdTextAlign
  type MMartAdTextAlign = common.TextAlign

  val  DiscountTemplate = io.suggest.ym.model.ad.AODiscountTemplate
  type DiscountTemplate = io.suggest.ym.model.ad.AODiscountTemplate

  val  MMAdFloatField   = io.suggest.ym.model.ad.AOFloatField
  type MMAdFloatField   = io.suggest.ym.model.ad.AOFloatField
  val  MMAdStringField  = io.suggest.ym.model.ad.AOStringField
  type MMAdStringField  = io.suggest.ym.model.ad.AOStringField

  val  MMartAdTAPhone   = MMartAdTAPhone
  type MMartAdTAPhone   = common.TextAlignPhone

  val  MMartAdTATablet  = MMartAdTATablet
  type MMartAdTATablet  = common.TextAlignTablet

  val  MMAdFieldFont    = io.suggest.ym.model.ad.AOFieldFont
  type MMAdFieldFont    = io.suggest.ym.model.ad.AOFieldFont

  val  MMartAdPanelSettings = MMartAdPanelSettings
  type MMartAdPanelSettings = common.AdPanelSettings

  val  MMAdPrice        = io.suggest.ym.model.ad.AOPriceField
  type MMAdPrice        = io.suggest.ym.model.ad.AOPriceField

  val  MImgInfo         = io.suggest.ym.model.common.MImgInfo
  type MImgInfo         = io.suggest.ym.model.common.MImgInfo

  val  MInx             = io.suggest.model.inx2.MInx
  type MInxT            = io.suggest.model.inx2.MInxT

  val  MMartInx         = io.suggest.model.inx2.MMartInx
  type MMartInx         = io.suggest.model.inx2.MMartInx

  val  MMartAdIndexed   = io.suggest.ym.model.MMartAdIndexed
  type MMartAdIndexed   = io.suggest.ym.model.MMartAdIndexed

  type MAdnNode         = io.suggest.ym.model.MAdnNode

  val  MAdStat          = io.suggest.ym.model.stat.MAdStat
  type MAdStat          = io.suggest.ym.model.stat.MAdStat
  type AdFreqs_t        = MAdStat.AdFreqs_t

  val  AdStatActions    = io.suggest.ym.model.stat.AdStatActions
  type AdStatAction     = io.suggest.ym.model.stat.AdStatAction
}
