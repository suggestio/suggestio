package io.suggest.model.common

import io.suggest.model._
import io.suggest.model.EsModel.{FieldsJsonAcc, asJsonStrArray}
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:12
 * Description: Аддон для ES-моделей, имеющих поле person_id во множественном числе.
 */

object EMPersonIds {
  val PERSON_ID_ESFN = "personId"

  def personIdQuery(personId: String) = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
}

import EMPersonIds._

trait EMPersonIdsStatic extends EsModelStaticT {

  override type T <: EMPersonIds

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(PERSON_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PERSON_ID_ESFN, value) =>
        value match {
          case personIdsRaw: java.lang.Iterable[_] =>
            acc.personIds = personIdsRaw.foldLeft[List[String]] (Nil) { (acc, e) =>
              e.toString :: acc
            }.toSet
        }
    }
  }


  /**
   * Найти экземпляры модели, относящиеся к указанному юзеру.
   * @param personId id юзера.
   * @return Список найденных результатов.
   */
  def findByPersonId(personId: String, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                    (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( personIdQuery(personId) )
      .setSize(maxResults)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
  }

  def countByPersonId(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    countByQuery(personIdQuery(personId))
  }
}

trait EMPersonIds extends EsModelT {

  override type T <: EMPersonIds

  var personIds: Set[String]

  def mainPersonId = personIds.lastOption

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (!personIds.isEmpty) {
      val personIdsJson = asJsonStrArray(personIds)
      (PERSON_ID_ESFN, personIdsJson) :: acc0
    } else {
      acc0
    }
  }

}
