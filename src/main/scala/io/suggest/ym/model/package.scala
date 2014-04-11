package io.suggest.ym


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 13:30
 * Description:
 */
package object model {

  val  AdShowLevels       = common.AdShowLevels
  type AdShowLevel        = AdShowLevels.AdShowLevel

  val  AdOfferTypes       = common.AdOfferTypes
  type AdOfferType        = AdOfferTypes.AdOfferType

  val  AdNetMemberTypes   = common.AdNetMemberTypes
  type AdNetMemberType    = AdNetMemberTypes.AdNetMemberType

  type CompanyId_t        = MCompany.CompanyId_t

}
