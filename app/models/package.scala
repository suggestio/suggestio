import io.suggest.ym
import io.suggest.ym.model.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:47
 * Description: После переноса ряда моделей в sioutil, тут появились костыли.
 */

package object models {

  type MCompany             = ym.model.MCompany
  val  MCompany             = ym.model.MCompany
  type CompanyId_t          = MCompany.CompanyId_t
  
  val  MAdnNode             = ym.model.MAdnNode
  type MAdnNode             = ym.model.MAdnNode

  type MShopPriceList       = ym.model.MShopPriceList
  val  MShopPriceList       = ym.model.MShopPriceList

  type MShopPromoOffer      = ym.model.MShopPromoOffer
  val  MShopPromoOffer      = ym.model.MShopPromoOffer

  type MYmCategory          = ym.model.MYmCategory
  val  MYmCategory          = ym.model.MYmCategory

  val  AdShowLevels         = ym.model.AdShowLevels
  type AdShowLevel          = AdShowLevels.AdShowLevel
  
  val  MAd                  = ym.model.MAd
  type MAd                  = ym.model.MAd

  val  AdnMMetadata         = ym.model.common.AdnMMetadata
  type AdnMMetadata         = ym.model.common.AdnMMetadata

  val  AdNetMemberInfo      = ym.model.common.AdNetMemberInfo
  type AdNetMemberInfo      = ym.model.common.AdNetMemberInfo

  val  AdNetMemberTypes     = ym.model.common.AdNetMemberTypes
  type AdNetMemberType      = AdNetMemberTypes.AdNetMemberType

  val  AdnMPubSettingsLevels = ym.model.common.AdnMemberShowLevels
  type AdnMPubSettingsLevels = ym.model.common.AdnMemberShowLevels


  // Рекламота
  type MAdT                 = ym.model.ad.MAdT

  val  AdReceiverInfo       = ym.model.common.AdReceiverInfo
  type AdReceiverInfo       = ym.model.common.AdReceiverInfo

  val  MWelcomeAd           = ym.model.MWelcomeAd
  type MWelcomeAd           = ym.model.MWelcomeAd

  val  AdOfferTypes         = ym.model.AdOfferTypes
  type AdOfferType          = AdOfferTypes.AdOfferType

  type AdOfferT             = ym.model.ad.AdOfferT

  val  AOBlock              = ym.model.ad.AOBlock
  type AOBlock              = ym.model.ad.AOBlock

  val  AOTextAlignValues    = common.AOTextAlignValues
  type AOTextAlignValue     = AOTextAlignValues.TextAlignValue

  val  AOFloatField         = ym.model.ad.AOFloatField
  type AOFloatField         = ym.model.ad.AOFloatField
  val  AOStringField        = ym.model.ad.AOStringField
  type AOStringField        = ym.model.ad.AOStringField

  val  TextAlign            = ym.model.common.TextAlign
  type TextAlign            = ym.model.common.TextAlign

  val  TextAlignPhone       = ym.model.common.TextAlignPhone
  type TextAlignPhone       = ym.model.common.TextAlignPhone

  val  TextAlignTablet      = ym.model.common.TextAlignTablet
  type TextAlignTablet      = ym.model.common.TextAlignTablet

  val  AOFieldFont          = ym.model.ad.AOFieldFont
  type AOFieldFont          = ym.model.ad.AOFieldFont

  val  AdPanelSettings      = ym.model.common.AdPanelSettings 
  type AdPanelSettings      = ym.model.common.AdPanelSettings

  val  AOPriceField         = ym.model.ad.AOPriceField
  type AOPriceField         = ym.model.ad.AOPriceField

  val  MImgInfo             = ym.model.common.MImgInfo
  type MImgInfo             = ym.model.common.MImgInfo

  val  MImgInfoMeta         = ym.model.common.MImgInfoMeta
  type MImgInfoMeta         = ym.model.common.MImgInfoMeta

  val  MInx                 = io.suggest.model.inx2.MInx
  type MInxT                = io.suggest.model.inx2.MInxT

  val  MMartInx             = io.suggest.model.inx2.MMartInx
  type MMartInx             = io.suggest.model.inx2.MMartInx

  val  MAdStat              = ym.model.stat.MAdStat
  type MAdStat              = ym.model.stat.MAdStat
  type AdFreqs_t            = MAdStat.AdFreqs_t

  val  AdStatActions        = ym.model.stat.AdStatActions
  type AdStatAction         = ym.model.stat.AdStatAction

  type CollectMMCatsAcc_t   = MMartCategory.CollectMMCatsAcc_t

  type BTariffType          = BTariffTypes.BTariffType

  val  Tariff               = ym.model.common.Tariff
  type Tariff               = ym.model.common.Tariff

  val  TariffTypes          = ym.model.common.TariffTypes
  type TariffType           = TariffTypes.TariffType

  type SelectPolicy         = SelectPolicies.SelectPolicy

  type Context              = util.Context
  type BlockData            = common.IBlockMeta with ym.model.ad.IOffers

  val  BlocksConf           = util.blocks.BlocksConf
  type BlockConf            = BlocksConf.BlockConf

  type BlockFieldT          = util.blocks.BlockFieldT
  type BfInt                = util.blocks.BfInt
  type BfDiscount           = util.blocks.BfDiscount
  type BfText               = util.blocks.BfText
  type BfPrice              = util.blocks.BfPrice
  type BfString             = util.blocks.BfString
  type BfImage              = util.blocks.BfImage

}
