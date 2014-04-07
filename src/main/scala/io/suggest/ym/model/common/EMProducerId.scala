package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.search.sort.SortBuilder
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.model.EsModel.stringParser

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
}

import EMProducerId._


trait EMProducerIdStatic[T <: EMProducerIdMut[T]] extends EsModelStaticT[T] {
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


  def producerIdQuery(producerId: String) = QueryBuilders.termQuery(PRODUCER_ID_ESFN, producerId)

  /**
   * Найти все рекламные карточки, принадлежащие (созданные) указанным узлом рекламной сети.
   * @param adnId id узла, создавшего рекламные карточки.
   * @return Список результатов.
   */
  def findForProducer(adnId: String, withSorter: Option[SortBuilder] = None)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val req = prepareSearch
      .setQuery(producerIdQuery(adnId))
    if (withSorter.isDefined)
      req.addSort(withSorter.get)
    req
      .execute()
      .map { searchResp2list }
  }

}

trait EMProducerId[T <: EMProducerId[T]] extends EsModelT[T] {

  /** Кто является изготовителем этой рекламной карточки? */
  def producerId: String

  abstract override def writeJsonFields(acc: XContentBuilder) = {
    super.writeJsonFields(acc)
    acc.field(PRODUCER_ID_ESFN, producerId)
  }
}

trait EMProducerIdMut[T <: EMProducerIdMut[T]] extends EMProducerId[T] {
  var producerId: String
}
