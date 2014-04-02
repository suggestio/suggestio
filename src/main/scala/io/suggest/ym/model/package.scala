package io.suggest.ym

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 13:30
 * Description:
 */
package object model {

  type AdShowLevel = AdShowLevels.AdShowLevel
  type MMartAdOfferType = MMartAdOfferTypes.MMartAdOfferType

  val  AdProducerTypes = common.AdProducerTypes
  type AdProducerType  = AdProducerTypes.AdProducerType

  type CompanyId_t = MCompany.CompanyId_t
}
