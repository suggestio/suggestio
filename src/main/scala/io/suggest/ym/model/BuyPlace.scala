package io.suggest.ym.model

import io.suggest.model.EsModelMinimalT
import MCompany.CompanyId_t
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.03.14 18:38
 * Description: Описываем общее между [[MMart]], [[MShop]] и возможными другими сущностями.
 */
trait BuyPlaceT[T <: BuyPlaceT[T]] extends EsModelMinimalT[T] {

  var logoImgId: Option[String]
  var name: String
  var companyId: CompanyId_t
  var dateCreated   : DateTime

}
