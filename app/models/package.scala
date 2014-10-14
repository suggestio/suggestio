import controllers.routes
import io.suggest.model.geo
import io.suggest.ym
import io.suggest.ym.model.common
import io.suggest.ym.model.common.EMImg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:47
 * Description: После переноса ряда моделей в sioutil, тут появились костыли.
 */

package object models {

  type MCompany             = ym.model.MCompany
  val  MCompany             = ym.model.MCompany

  type MCompanyMeta         = ym.model.common.MCompanyMeta
  val  MCompanyMeta         = ym.model.common.MCompanyMeta

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

  val  AdnRights            = ym.model.common.AdnRights
  type AdnRight             = AdnRights.AdnRight

  val  AdNetMemberTypes     = ym.model.common.AdNetMemberTypes
  type AdNetMemberType      = AdNetMemberTypes.AdNetMemberType

  val  AdnMPubSettingsLevels = ym.model.common.AdnMemberShowLevels
  type AdnMPubSettingsLevels = ym.model.common.AdnMemberShowLevels


  // Рекламота
  type MAdT                 = ym.model.ad.MAdT

  val  AdReceiverInfo       = ym.model.common.AdReceiverInfo
  type AdReceiverInfo       = ym.model.common.AdReceiverInfo

  type Receivers_t          = ym.model.common.EMReceivers.Receivers_t

  val  MWelcomeAd           = ym.model.MWelcomeAd
  type MWelcomeAd           = ym.model.MWelcomeAd

  val  AdOfferTypes         = ym.model.AdOfferTypes
  type AdOfferType          = AdOfferTypes.AdOfferType

  type AdOfferT             = ym.model.ad.AdOfferT

  val  AOBlock              = ym.model.ad.AOBlock
  type AOBlock              = ym.model.ad.AOBlock

  val  AOFloatField         = ym.model.ad.AOFloatField
  type AOFloatField         = ym.model.ad.AOFloatField
  val  AOStringField        = ym.model.ad.AOStringField
  type AOStringField        = ym.model.ad.AOStringField

  val  AOFieldFont          = ym.model.ad.AOFieldFont
  type AOFieldFont          = ym.model.ad.AOFieldFont

  type AOValueField         = ym.model.ad.AOValueField

  val  AdPanelSettings      = ym.model.common.AdPanelSettings 
  type AdPanelSettings      = ym.model.common.AdPanelSettings

  val  AOPriceField         = ym.model.ad.AOPriceField
  type AOPriceField         = ym.model.ad.AOPriceField

  val  MImgInfo             = ym.model.common.MImgInfo
  type MImgInfo             = ym.model.common.MImgInfo
  type MImgInfoT            = ym.model.common.MImgInfoT

  type ImgCrop              = io.suggest.img.ImgCrop
  val  ImgCrop              = io.suggest.img.ImgCrop

  type MImgSizeT            = ym.model.common.MImgSizeT

  val  MImgInfoMeta         = ym.model.common.MImgInfoMeta
  type MImgInfoMeta         = ym.model.common.MImgInfoMeta

  val  MInx                 = io.suggest.model.inx2.MInx
  type MInxT                = io.suggest.model.inx2.MInxT

  val  MMartInx             = io.suggest.model.inx2.MMartInx
  type MMartInx             = io.suggest.model.inx2.MMartInx

  val  MAdStat              = ym.model.stat.MAdStat
  type MAdStat              = ym.model.stat.MAdStat
  type AdFreqs_t            = MAdStat.AdFreqs_t

  type CollectMMCatsAcc_t   = MMartCategory.CollectMMCatsAcc_t

  type BTariffType          = BTariffTypes.BTariffType

  val  Tariff               = ym.model.common.Tariff
  type Tariff               = ym.model.common.Tariff

  val  TariffTypes          = ym.model.common.TariffTypes
  type TariffType           = TariffTypes.TariffType

  type SelectPolicy         = SelectPolicies.SelectPolicy

  type Context              = util.Context
  type BlockData            = common.IBlockMeta with ym.model.ad.IOffers with common.IColors

  val  BlocksConf           = util.blocks.BlocksConf
  type BlockConf            = BlocksConf.BlockConf

  type BlockFieldT          = util.blocks.BlockFieldT
  type BlockAOValueFieldT   = util.blocks.BlockAOValueFieldT
  type BfHeight             = util.blocks.BfHeight
  type BfWidth              = util.blocks.BfWidth
  type BfDiscount           = util.blocks.BfDiscount
  type BfText               = util.blocks.BfText
  type BfPrice              = util.blocks.BfPrice
  type BfString             = util.blocks.BfString
  type BfImage              = util.blocks.BfImage
  type BfColor              = util.blocks.BfColor

  //type ICoords2D            = ym.model.ad.ICoords2D
  type Coords2D             = ym.model.ad.Coords2D
  val  Coords2D             = ym.model.ad.Coords2D

  val  TextAligns           = ym.model.ad.TextAligns
  type TextAlign            = TextAligns.TextAlign

  val  DisableReason        = ym.model.common.DisableReason
  type DisableReason        = ym.model.common.DisableReason

  type MAdvMode             = MAdvModes.MAdvMode
  type AudienceSize         = AudienceSizes.AudienceSize
  type InviteReqType        = InviteReqTypes.InviteReqType

  type AdnShownType         = AdnShownTypes.AdnShownType

  type Imgs_t               = EMImg.Imgs_t

  type GeoPoint             = geo.GeoPoint
  val  GeoPoint             = geo.GeoPoint

  val  SinkShowLevels       = ym.model.common.SinkShowLevels
  type SinkShowLevel        = SinkShowLevels.SinkShowLevel

  val  AdnSinks             = ym.model.common.AdnSinks
  type AdnSink              = AdnSinks.AdnSink

  type QuickAdvPeriod       = QuickAdvPeriods.QuickAdvPeriod
  type MNodeAdsMode         = MNodeAdsModes.MNodeAdsMode
  type NodeRightPanelLink   = NodeRightPanelLinks.NodeRightPanelLink
  type BillingRightPanelLink= BillingRightPanelLinks.BillingRightPanelLink
  type LkLeftPanelLink      = LkLeftPanelLinks.LkLeftPanelLink

  val  NodeGeoLevels        = ym.model.NodeGeoLevels
  type NodeGeoLevel         = NodeGeoLevels.NodeGeoLevel

  val  MAdnNodeGeo          = ym.model.MAdnNodeGeo
  type MAdnNodeGeo          = ym.model.MAdnNodeGeo

  val  MAdnNodeGeoIndexed   = ym.model.MAdnNodeGeoIndexed
  type MAdnNodeGeoIndexed   = ym.model.MAdnNodeGeoIndexed

  val  AdnNodeGeodata       = ym.model.common.AdnNodeGeodata
  type AdnNodeGeodata       = ym.model.common.AdnNodeGeodata

  type AdnNodesSearchArgs   = ym.model.common.AdnNodesSearchArgs
  type AdnNodesSearchArgsT  = ym.model.common.AdnNodesSearchArgsT


  // cassandra img models
  type MImgThumb2           = io.suggest.model.MImgThumb2
  val  MImgThumb2           = io.suggest.model.MImgThumb2

  type MUserImgMeta2        = io.suggest.model.MUserImgMeta2
  val  MUserImgMeta2        = io.suggest.model.MUserImgMeta2

  type MUserImg2            = io.suggest.model.MUserImg2
  val  MUserImg2            = io.suggest.model.MUserImg2

  /** Вызов на главную страницу. */
  def MAIN_PAGE_CALL        = routes.MarketShowcase.geoSite()
}
