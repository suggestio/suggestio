package io.suggest.es.model

import io.suggest.primo.TypeT
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.OptStrId
import io.suggest.util.JacksonWrapper
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.get.{GetResponse, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:24
 * Description: Общий код для обычный и child-моделей.
 * Был вынесен из-за разделения в логике работы обычный и child-моделей.
 */
trait EsModelCommonStaticT extends EsModelStaticMapping with TypeT with IEsModelDi { outer =>

  import mCommonDi._

  override type T <: EsModelCommonT

  // Кое-какие константы, которые можно переопределить в рамках конкретных моделей.
  def MAX_RESULTS_DFLT = EsModelUtil.MAX_RESULTS_DFLT
  def OFFSET_DFLT = EsModelUtil.OFFSET_DFLT
  def SCROLL_KEEPALIVE_MS_DFLT = EsModelUtil.SCROLL_KEEPALIVE_MS_DFLT
  def SCROLL_KEEPALIVE_DFLT = new TimeValue(SCROLL_KEEPALIVE_MS_DFLT)
  def SCROLL_SIZE_DFLT = EsModelUtil.SCROLL_SIZE_DFLT
  def BULK_PROCESSOR_BULK_SIZE_DFLT = EsModelUtil.BULK_PROCESSOR_BULK_SIZE_DFLT

  def HAS_RESOURCES: Boolean = false

  /** Если модели требуется выставлять routing для ключа, то можно делать это через эту функцию.
    *
    * @param idOrNull id или null, если id отсутствует.
    * @return None если routing не требуется, иначе Some(String).
    */
  def getRoutingKey(idOrNull: String): Option[String] = None

  // Короткие враппер для типичных операций в рамках статической модели.

  final def prepareSearch(): SearchRequestBuilder =
    prepareSearchViaClient(esClient)
  final def prepareSearchViaClient(client: Client): SearchRequestBuilder = {
    client
      .prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
  }

  final def prepareCount(): SearchRequestBuilder = {
    prepareSearch()
      .setSize(0)
  }

  final def prepareDeleteBase(id: String) = {
    val req = esClient.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }
  /** Кол-во item'ов в очереди на удаление. */
  def BULK_DELETE_QUEUE_LEN = 200

  /**
   * Примитив для рассчета кол-ва документов, удовлетворяющих указанному запросу.
   *
   * @param query Произвольный поисковый запрос.
   * @return Кол-во найденных документов.
   */
  final def countByQuery(query: QueryBuilder): Future[Long] = {
    prepareCount()
      .setQuery(query)
      .executeFut()
      .map { _.getHits.getTotalHits }
  }


  /**
   * Десериализация одного элементам модели.
   *
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T

  /** Десериализация по новому API: документ передается напрямую, а данные извлекаются через статический typeclass.
    *
    * @param doc Документ, т.е. GetResponse или SearchHit или же ещё что-то...
    * @param ev Неявный typeclass, обеспечивающий унифицированный интерфейс к doc.
    * @tparam D Класс переданного документа.
    * @return Экземпляр модели.
    */
  def deserializeOne2[D](doc: D)(implicit ev: IEsDoc[D]): T = {
    // Здесь код для совместимости. Когда новая архитектура будет заимплеменчена во всех моделях, этот код нужно удалить,
    // (метод станет abstract), а deserializeOne() удалить вместе с реализациями.
    deserializeOne(ev.id(doc), ev.bodyAsScalaMap(doc), ev.version(doc))
  }


  /** Внутренний метод для укорачивания кода парсеров ES SearchResponse. */
  final def searchRespMap[A](searchResp: SearchResponse)(f: SearchHit => A): Iterator[A] = {
    searchResp.getHits
      .iterator()
      .asScala
      .map(f)
  }

  /** Список результатов с source внутри перегнать в распарсенный список. */
  final def searchResp2iter(searchResp: SearchResponse): Iterator[T] = {
    searchRespMap(searchResp)(deserializeSearchHit)
  }
  // Stream! Нельзя менять тип. На ленивость завязана работа akka-stream Source, который имитируется поверх этого метода.
  final def searchResp2stream(searchResp: SearchResponse): Stream[T] = {
    searchResp2iter(searchResp)
      // Безопасно ли тут делать ленивый Stream? Обычно да, но java-код elasticsearch с mutable внутри может в будущем посчитать иначе.
      .toStream
  }

  final def deserializeSearchHit(hit: SearchHit): T = {
    deserializeOne2(hit)
  }

  /** Список результатов в список id. */
  final def searchResp2idsList(searchResp: SearchResponse): ISearchResp[String] = {
    val hitsArr = searchResp.getHits.getHits
    new AbstractSearchResp[String] {
      override def total: Long =
        searchResp.getHits.getTotalHits
      override def length: Int =
        hitsArr.length
      override def apply(idx: Int): String =
        hitsArr(idx).getId
    }
  }


  /** Лениво распарсить выхлоп multi-GET. */
  final def mgetResp2Stream(mgetResp: MultiGetResponse): Stream[T] = {
    mgetResp
      .getResponses
      .iterator
      .flatMap { mgetItem =>
        // Поиск может содержать элементы, которые были только что удалены. Нужно их отсеивать.
        if (mgetItem.isFailed || !mgetItem.getResponse.isExists) {
          Nil
        } else {
          deserializeOne2(mgetItem.getResponse) :: Nil
        }
      }
      .toStream
  }


  final def deserializeGetRespFull(getResp: GetResponse): Option[T] = {
    if (getResp.isExists) {
      val result = deserializeOne2(getResp)
      Some(result)
    } else {
      None
    }
  }


  def UPDATE_RETRIES_MAX: Int = EsModelUtil.UPDATE_RETRIES_MAX_DFLT


  /**
   * Перечитывание из хранилища указанного документа, используя реквизиты текущего документа.
   * Нужно для parent-child случаев, когда одного _id уже мало.
   *
   * @param inst0 Исходный (устаревший) инстанс.
   * @return тоже самое, что и getById()
   */
  def reget(inst0: T): Future[Option[T]]

  // TODO Ужаснейший говнокод ниже: распиливание tryUpdate и последующая дедубликация породили ещё больший объем кода.
  // Это из-за того, что исторически есть два типа T: в static и в инстансе модели.
  // TODO Надо залить ITryUpdateData в EsModelCommonT или сделать что-то, чтобы не было так страшно здесь.

  /** Абстрактный класс контейнера для вызова [[EsModelUtil]].tryUpdate(). */
  abstract class TryUpdateDataAbstract[TU <: TryUpdateDataAbstract[TU]] extends ITryUpdateData[T, TU] {
    protected def _instance(m: T): TU
    /** Данные для сохранения потеряли актуальность, собрать новый аккамулятор. */
    override def _reget: Future[TU] = {
      for (opt <- reget(_saveable)) yield {
        _instance(opt.get)
      }
    }
  }


  def _save(m: T)(f: () => Future[String]): Future[String] =
    f()


  def toJsonPretty(m: T): String = toJson(m)
  def toJson(m: T): String

  /** Implicit API модели завёрнуто в этот класс, который можно экстендить. */
  class Implicits {

    /** Mock-адаптер для тестирования сериализации-десериализации моделей на базе play.json.
      * На вход он получает просто экземпляры классов моделей. */
    implicit def mockPlayDocRespEv: IEsDoc[T] = new IEsDoc[T] {
      override def id(v: T): Option[String] =
        v.id
      override def version(v: T): Option[Long] =
        v.versionOpt
      override def rawVersion(v: T): Long =
        v.versionOpt.getOrElse(-1)
      override def bodyAsScalaMap(v: T): collection.Map[String, AnyRef] =
        JacksonWrapper.convert[collection.Map[String, AnyRef]]( toJson(v) )
      override def bodyAsString(v: T): String =
        toJson(v)
      override def idOrNull(v: T): String =
        v.idOrNull
    }

    /** stream-сорсинг для обычных случаев. */
    implicit def elSourcingHelper: IEsSourcingHelper[T] = {
      // typeclass для source() для простой десериализации ответов в обычные элементы модели.
      new IEsSourcingHelper[T] {
        override def mapSearchHit(from: SearchHit): T =
          deserializeSearchHit( from )
        override def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
          super.prepareSrb(srb)
            .setFetchSource(true)
        }
        override def toString: String =
          s"${outer.getClass.getSimpleName}.${super.toString}"
      }
    }

    override def toString: String = {
      s"${outer.getClass.getSimpleName}.${getClass.getSimpleName}"
    }

  }

  // Вызываемый конструктор для класса Implicits. Должен быть перезаписан как val в итоге.
  def Implicits = new Implicits

}


/** Общий код динамических частей модели, независимо от child-модели или обычной. */
trait EsModelCommonT extends OptStrId {

  /** Модели, желающие версионизации, должны перезаписать это поле. */
  def versionOpt: Option[Long]

  def idOrNull: String = id.orNull

}


trait BulkProcessorListenerT extends BulkProcessor.Listener {
  def _logPrefix: String

  /** Перед отправкой каждого bulk-реквеста. */
  override def beforeBulk(executionId: Long, request: BulkRequest): Unit =
    LOGGER.trace(s"${_logPrefix} Going to execute bulk req with ${request.numberOfActions()} actions.")
  /** Данные успешно отправлены в индекс. */
  override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit =
    LOGGER.trace(s"${_logPrefix} afterBulk OK, took ${response.getTook}ms${if (response.hasFailures) "\n " + response.buildFailureMessage() else ""}")
  /** Ошибка индексации. */
  override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit =
    LOGGER.error(s"${_logPrefix} Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
}
class BulkProcessorListener(override val _logPrefix: String) extends BulkProcessorListenerT

