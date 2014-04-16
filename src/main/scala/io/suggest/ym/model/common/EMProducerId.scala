package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.search.sort.SortBuilder
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import play.api.libs.json.JsString
import io.suggest.event.SioNotifierStaticClientI

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

  /**
   * Сгенерить es query для поиска по id продьюсера.
   * @param producerId id продьюсера.
   * @return QueryBuilder.
   */
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

  /**
   * Реалтаймовый поиск по создателю.
   * @param producerId id продьюсера.
   * @return Список MAd.
   */
  def findForProducerRt(producerId: String, maxResults: Int = 100)(implicit ec: ExecutionContext, client: Client): Future[List[T]] = {
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


trait EMProducerId[T <: EMProducerId[T]] extends EsModelT[T] {

  /** Кто является изготовителем этой рекламной карточки? */
  def producerId: String

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    PRODUCER_ID_ESFN -> JsString(producerId) :: super.writeJsonFields(acc)
  }

}

trait EMProducerIdMut[T <: EMProducerIdMut[T]] extends EMProducerId[T] {
  var producerId: String
}
