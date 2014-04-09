package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModel, EsModelT}
import io.suggest.ym.model.MCompany.CompanyId_t
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.ym.model.MCompanySel

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


  /** Генерация es query для поиска по компании. */
  def companyIdQuery(companyId: CompanyId_t) = QueryBuilders.termQuery(COMPANY_ID_ESFN, companyId)


  /**
   * Вернуть все ТЦ, находящиеся во владении указанной конторы.
   * @param companyId id конторы.
   * @return Список ТЦ в неопределённом порядке.
   */
  def findByCompanyId(companyId: CompanyId_t, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                     (implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery(companyIdQuery(companyId))
      .execute()
      .map { searchResp2list }
  }


  /** Посчитать кол-во документов в хранилище для указанного id компании в поле companyId
    * @param companyId id компании.
    * @return Long.
    */
  def countByCompanyId(companyId: CompanyId_t)(implicit ec:ExecutionContext, client: Client): Future[Long] = {
    prepareCount
      .setQuery(companyIdQuery(companyId))
      .execute()
      .map { _.getCount }
  }

}


trait EMCompanyId[T <: EMCompanyId[T]] extends EsModelT[T] with MCompanySel {

  var companyId: CompanyId_t

  abstract override def writeJsonFields(acc: XContentBuilder) = {
    super.writeJsonFields(acc)
    acc.field(COMPANY_ID_ESFN, companyId)
  }
}
