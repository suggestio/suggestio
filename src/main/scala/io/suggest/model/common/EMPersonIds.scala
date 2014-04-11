package io.suggest.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json.{JsString, JsValue, JsArray}
import EsModel.asJsonStrArray

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:12
 * Description: Аддон для ES-моделей, имеющих поле person_id во множественном числе.
 */

trait EMPersonIdsStatic[T <: EMPersonIds[T]] extends EsModelStaticT[T] {

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
    val personIdQuery = QueryBuilders.termQuery(PERSON_ID_ESFN, personId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(personIdQuery)
      .setSize(maxResults)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
  }

}

trait EMPersonIds[T <: EMPersonIds[T]] extends EsModelT[T] {

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
