package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticMutAkvT, EsModelPlayJsonT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.search.sort.SortBuilder
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import play.api.libs.json.JsString
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.search.aggregations.{Aggregations, AggregationBuilders}
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.14 17:01
 * Description: Поле producerId содержит id создателя контента (рекламы)
 * (создатель - узел сети [[io.suggest.ym.model.MAdnNode]]), т.е. id источника контента
 * и автора одновременно.
 */
object EMProducerId {

  /** Имя поля, которое должно содержать id продьюсера. */
  val PRODUCER_ID_ESFN   = "producerId"

  def producerIdAgg = AggregationBuilders.terms(PRODUCER_ID_ESFN).field(PRODUCER_ID_ESFN)
  def extractProducerIdAgg(aggs: Aggregations): Map[String, Long] = {
    aggs.get[Terms](PRODUCER_ID_ESFN)
      .getBuckets
      .iterator()
      .foldLeft [List[(String, Long)]] (Nil) {
        (acc, e) => e.getKey -> e.getDocCount :: acc
      }
      .toMap
  }


  /**
   * Сгенерить es query для поиска по id продьюсера.
   * @param producerId id продьюсера.
   * @return QueryBuilder.
   */
  def producerIdQuery(producerId: String) = QueryBuilders.termQuery(PRODUCER_ID_ESFN, producerId)

}

import EMProducerId._


trait EMProducerIdStatic extends EsModelStaticMutAkvT {
  override type T <: EMProducerIdMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(PRODUCER_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PRODUCER_ID_ESFN, value) =>
        acc.producerId = stringParser(value)
    }
  }

  /**
   * Найти все рекламные карточки, принадлежащие (созданные) указанным узлом рекламной сети.
   * @param adnId id узла, создавшего рекламные карточки.
   * @return Список результатов.
   */
  def findForProducer(adnId: String, withSorter: Option[SortBuilder] = None)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val req = prepareSearch
      .setQuery( producerIdQuery(adnId) )
    if (withSorter.isDefined)
      req.addSort(withSorter.get)
    req
      .execute()
      .map { searchResp2list }
  }

  /**
   * Реалтаймовый поиск по создателю.
   * @param producerId id продьюсера.
   * @return Список MAd.
   */
  def findForProducerRt(producerId: String, maxResults: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[List[T]] = {
    findQueryRt(producerIdQuery(producerId), maxResults)
  }

  /**
   * Найти все документы с указанным producerId, и удалить по одному.
   * Это важно, если delete() порождает какое-то событие или имеет иные сайд-эффекты.
   * @param producerId id продьюсера.
   * @return Фьючерс для синхронизации.
   */
  def deleteByProducerId1by1(producerId: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    // TODO Надо бы стандартизировать сайд-эффекты удаления и тут удалять через bulk + вызовы.
    findForProducer(producerId).flatMap { docs =>
      Future.traverse(docs) { _.delete }
    }
  }
}


trait EMProducerIdI extends EsModelPlayJsonT {
  override type T <: EMProducerIdI

  /** Кто является изготовителем этой рекламной карточки? */
  def producerId: String
}


trait EMProducerId extends EMProducerIdI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    PRODUCER_ID_ESFN -> JsString(producerId) :: super.writeJsonFields(acc)
  }

}

trait EMProducerIdMut extends EMProducerId {
  override type T <: EMProducerIdMut
  var producerId: String
}
