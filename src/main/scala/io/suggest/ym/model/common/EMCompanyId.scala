package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModel, EsModelT}
import io.suggest.ym.model.MCompany.CompanyId_t
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:49
 * Description: Аддон для ES-моделей, содержащий поле companyId.
 */

trait EMCompanyIdStatic[T <: EMCompanyId[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldString(COMPANY_ID_ESFN,  index = FieldIndexingVariants.not_analyzed,  include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (COMPANY_ID_ESFN, value)    => acc.companyId = companyIdParser(value)
    }
  }
}


trait EMCompanyId[T <: EMCompanyId[T]] extends EsModelT[T] {

  def companyId: CompanyId_t
  def companyId_=(companyId: CompanyId_t)

  abstract override def writeJsonFields(acc: XContentBuilder) = {
    super.writeJsonFields(acc)
    acc.field(COMPANY_ID_ESFN, companyId)
  }
}
