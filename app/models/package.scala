import controllers.routes
import io.suggest.model.geo
import io.suggest.model.n2

import io.suggest.ym
import models.adv.MAdvModes
import models.mbill.BTariffTypes
import models.usr.EmailPwConfirmInfo
import play.api.data.Form

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

  type MYmCategory          = ym.model.MYmCategory
  val  MYmCategory          = ym.model.MYmCategory

  type AdShowLevel          = io.suggest.model.sc.common.AdShowLevel
  val  AdShowLevels         = io.suggest.model.sc.common.AdShowLevels

  val  AdnRights            = ym.model.common.AdnRights
  type AdnRight             = ym.model.common.AdnRight


  val  AdReceiverInfo       = ym.model.common.AdReceiverInfo
  type AdReceiverInfo       = ym.model.common.AdReceiverInfo

  type Receivers_t          = n2.edge.NodeEdgesMap_t

  type AdOfferT             = ym.model.ad.AdOfferT

  val  TextEnt              = io.suggest.model.n2.ad.ent.text.TextEnt
  type TextEnt              = io.suggest.model.n2.ad.ent.text.TextEnt

  val  EntFont              = io.suggest.model.n2.ad.ent.text.EntFont
  type EntFont              = io.suggest.model.n2.ad.ent.text.EntFont

  type ValueEnt             = io.suggest.model.n2.ad.ent.text.ValueEnt

  val  MImgInfo             = ym.model.common.MImgInfo
  type MImgInfo             = ym.model.common.MImgInfo
  type MImgInfoT            = ym.model.common.MImgInfoT

  type ImgCrop              = io.suggest.img.ImgCrop
  val  ImgCrop              = io.suggest.img.ImgCrop

  type ISize2di             = io.suggest.common.geom.d2.ISize2di

  type Size2di              = io.suggest.common.geom.d2.Size2di
  val  Size2di              = io.suggest.common.geom.d2.Size2di

  type MImgSizeT            = ym.model.common.MImgSizeT

  val  MImgInfoMeta         = ym.model.common.MImgInfoMeta
  type MImgInfoMeta         = ym.model.common.MImgInfoMeta

  val  MAdStat              = ym.model.stat.MAdStat
  type MAdStat              = ym.model.stat.MAdStat
  type AdFreqs_t            = MAdStat.AdFreqs_t

  type BTariffType          = BTariffTypes.BTariffType

  val  Tariff               = ym.model.common.Tariff
  type Tariff               = ym.model.common.Tariff

  val  TariffTypes          = ym.model.common.TariffTypes
  type TariffType           = TariffTypes.TariffType

  type SelectPolicy         = SelectPolicies.T

  val  BlocksConf           = util.blocks.BlocksConf
  type BlockConf            = BlocksConf.T

  type BlockFieldT          = util.blocks.BlockFieldT
  type BlockAOValueFieldT   = util.blocks.BlockAOValueFieldT
  type BfHeight             = util.blocks.BfHeight
  type BfWidth              = util.blocks.BfWidth
  type BfText               = util.blocks.BfText
  type BfString             = util.blocks.BfString
  type BfImage              = util.blocks.BfImage
  type BfColor              = util.blocks.BfColor
  type BfCheckbox           = util.blocks.BfCheckbox
  type BfNoValueT           = util.blocks.BfNoValueT

  type ICoords2di           = io.suggest.common.geom.coord.ICoords2di
  type Coords2d             = io.suggest.model.n2.ad.ent.Coords2d
  val  Coords2d             = io.suggest.model.n2.ad.ent.Coords2d

  val  TextAligns           = io.suggest.model.n2.ad.ent.text.TextAligns
  type TextAlign            = io.suggest.model.n2.ad.ent.text.TextAlign

  val  DisableReason        = ym.model.common.DisableReason
  type DisableReason        = ym.model.common.DisableReason

  type MAdvMode             = MAdvModes.T
  type AudienceSize         = AudienceSizes.T
  type InviteReqType        = InviteReqTypes.T

  type AdnShownType         = AdnShownTypes.T

  type GeoPoint             = geo.GeoPoint
  val  GeoPoint             = geo.GeoPoint

  type SinkShowLevel        = io.suggest.model.sc.common.SinkShowLevel
  val  SinkShowLevels       = io.suggest.model.sc.common.SinkShowLevels

  val  AdnSinks             = ym.model.common.AdnSinks
  type AdnSink              = ym.model.common.AdnSink

  type MNodeAdsMode         = MNodeAdsModes.T
  type NodeRightPanelLink   = NodeRightPanelLinks.T
  type BillingRightPanelLink= BillingRightPanelLinks.T
  type LkLeftPanelLink      = LkLeftPanelLinks.T

  val  NodeGeoLevels        = ym.model.NodeGeoLevels
  type NodeGeoLevel         = ym.model.NodeGeoLevel

  val  AdnNodeGeodata       = ym.model.common.AdnNodeGeodata
  type AdnNodeGeodata       = ym.model.common.AdnNodeGeodata

  type AdnNodesSearchArgs   = ym.model.common.AdnNodesSearchArgsImpl
  type AdnNodesSearchArgsT  = ym.model.common.AdnNodesSearchArgsT


  // cassandra img models
  type MUserImgMeta2        = io.suggest.model.MUserImgMeta2
  val  MUserImgMeta2        = io.suggest.model.MUserImgMeta2


  /** Вызов на главную страницу. */
  def MAIN_PAGE_CALL        = routes.MarketShowcase.geoSite()

  type IImgMeta             = io.suggest.model.img.IImgMeta

  type MHand                = MHands.T


  /** Тип формы для регистрации по email (шаг 1 - указание email). */
  type EmailPwRegReqForm_t  = Form[String]

  /** Тип формы для восстановления пароля. */
  type EmailPwRecoverForm_t = Form[String]

  /** Тип формы сброса забытого пароля. */
  type PwResetForm_t        = Form[String]

  /** Тип формы для второго шагоа регистрации по email: заполнение данных о себе. */
  type EmailPwConfirmForm_t = Form[EmailPwConfirmInfo]

  /** Тип формы для регистрации через внешнего провайдера. */
  type ExtRegConfirmForm_t  = Form[String]

  /** Тип формы создания нового узла-магазина силами юзера. */
  type UsrCreateNodeForm_t  = Form[String]

  type MColorData           = n2.node.meta.colors.MColorData

  type MNode                = n2.node.MNode
  val  MNode                = n2.node.MNode

  type MNodeType            = n2.node.MNodeType
  val  MNodeTypes           = n2.node.MNodeTypes

  type MEdge                = n2.edge.MEdge
  val  MEdge                = n2.edge.MEdge
  type IEdge                = n2.edge.IEdge

  type MPredicate           = n2.edge.MPredicate
  val  MPredicates          = n2.edge.MPredicates

  val  MMeta                = n2.node.meta.MMeta
  type MMeta                = n2.node.meta.MMeta

  val  MPersonMeta          = n2.node.meta.MPersonMeta
  type MPersonMeta          = n2.node.meta.MPersonMeta

  val  MMedia               = n2.media.MMedia
  type MMedia               = n2.media.MMedia

  type MGeoShape            = n2.geo.MGeoShape
  val  MGeoShape            = n2.geo.MGeoShape

  val  MEntity              = n2.ad.ent.MEntity
  type MEntity              = n2.ad.ent.MEntity

  type IPrice               = io.suggest.mbill2.m.price.IPrice
  type MPrice               = io.suggest.mbill2.m.price.MPrice
  val  MPrice               = io.suggest.mbill2.m.price.MPrice

  type MDailyTf             = io.suggest.model.n2.bill.tariff.daily.MDailyTf

}
