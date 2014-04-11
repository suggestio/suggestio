package io.suggest.model.inx2

import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.action.search.SearchResponse
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.event.SioNotifierStaticClientI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.14 19:34
 * Description: Подобие EsModel, но с учетом того, что информация об индексе, в котором лежат данные,
 * приходит в качестве параметра вызова.
 */
trait EsModelInx2StaticT[T, InxT <: MInxT] {

  protected def dummy(id: String, inx2: InxT): T

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  def deserializeOne(id: String, m: collection.Map[String, AnyRef], inx2: InxT): T = {
    val acc = dummy(id, inx2)
    m foreach applyKeyValue(acc)
    acc
  }

  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit]

  /** Список результатов с source внутри перегнать в распарсенный список. */
  def searchResp2list(searchResp: SearchResponse, inx2: InxT): Seq[T] = {
    searchResp.getHits.getHits.toSeq.map { hit =>
      deserializeOne(hit.getId, hit.getSource, inx2)
    }
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @return Список магазинов в порядке их создания.
   */
  def getAll(inx2: InxT)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    client.prepareSearch(inx2.esInxNames : _*)
      .setTypes(inx2.esTypes : _*)
      .setQuery(QueryBuilders.matchAllQuery())
      .execute()
      .map { searchResp2list(_, inx2) }
  }


  /**
   * Удалить документ по id из всех типов и индексов, перечисленных в экземпляре индекса.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteAnyById(id: String, inx2: InxT)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val deleteFuts = for {
      esIndex <- inx2.esInxNames
      esType  <- inx2.esTypes
    } yield {
      client.prepareDelete(esIndex, esType, id)
        .execute()
        .map { _.isFound }
    }
    Future.sequence(deleteFuts).map { results =>
      results.reduce(_ && _)
    }
  }

}


/** Когда индекс является single-индексом, доступны ещё кое-какие функции. */
trait EsModelInx2StaticSingleT[T, InxT <: MSingleInxT] extends EsModelInx2StaticT[T, InxT] {

  /**
   * Выбрать ряд из таблицы по id.
   * @param id Ключ магазина.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String, inx2: InxT)(implicit ec:ExecutionContext, client: Client): Future[Option[T]] = {
    val req = client.prepareGet(inx2.targetEsInxName, inx2.targetEsType, id)
    req.execute()
      .map { getResp =>
        if (getResp.isExists) {
          val result = deserializeOne(getResp.getId, getResp.getSourceAsMap, inx2)
          Some(result)
        } else {
          None
        }
      }
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String, inx2: MMartInx)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    client.prepareDelete(inx2.targetEsInxName, inx2.targetEsType, id)
      .execute()
      .map { _.isFound }
  }

}

