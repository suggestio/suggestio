package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticMutAkvT, EsModel, EsModelPlayJsonT}
import EsModel.{FieldsJsonAcc, stringParser}
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import io.suggest.ym.model.MCompanySel
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:49
 * Description: Аддон для ES-моделей, содержащий поле companyId.
 */

object EMCompanyId {
  val COMPANY_ID_ESFN = "companyId"
}


import EMCompanyId._


trait EMCompanyIdStatic extends EsModelStaticMutAkvT {
  override type T <: EMCompanyId

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(COMPANY_ID_ESFN,  index = FieldIndexingVariants.not_analyzed,  include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (COMPANY_ID_ESFN, value)    => acc.companyId = stringParser(value)
    }
  }


  /** Генерация es query для поиска по компании. */
  def companyIdQuery(companyId: String) = QueryBuilders.termQuery(COMPANY_ID_ESFN, companyId)


  /**
   * Вернуть все ТЦ, находящиеся во владении указанной конторы.
   * @param companyId id конторы.
   * @return Список ТЦ в неопределённом порядке.
   */
  def findByCompanyId(companyId: String, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
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
  def countByCompanyId(companyId: String)(implicit ec:ExecutionContext, client: Client): Future[Long] = {
    countByQuery( companyIdQuery(companyId) )
  }

}


trait EMCompanyId extends EsModelPlayJsonT with MCompanySel {
  override type T <: EMCompanyId

  var companyId: String

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    COMPANY_ID_ESFN -> JsString(companyId) :: super.writeJsonFields(acc)
  }

}


// Аддоны для dyn-search.
/** Поддержка поиска по полю companyId. */
trait CompanyIdsDsa extends DynSearchArgs {

  /** id компаний, под которые копаем узлы ADN. */
  def companyIds: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt .map[QueryBuilder] { qb =>
      // Отрабатываем companyId
      if (companyIds.isEmpty) {
        qb
      } else {
        val cf = FilterBuilders.termsFilter(COMPANY_ID_ESFN, companyIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, cf)
      }
    }.orElse {
      if (companyIds.isEmpty) {
        None
      } else {
        val cq = QueryBuilders.termsQuery(COMPANY_ID_ESFN, companyIds : _*)
          .minimumMatch(1)
        Some(cq)
      }
    }
  }

  override def sbInitSize: Int = {
    collStringSize(companyIds, super.sbInitSize)
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("companyIds", companyIds, super.toStringBuilder)
  }
}


trait CompanyIdsDsaDflt extends CompanyIdsDsa {
  override def companyIds: Seq[String] = Seq.empty
}


trait CompanyIdsDsaWrapper extends CompanyIdsDsa with DynSearchArgsWrapper {
  override type WT <: CompanyIdsDsa
  override def companyIds = _dsArgsUnderlying.companyIds
}
