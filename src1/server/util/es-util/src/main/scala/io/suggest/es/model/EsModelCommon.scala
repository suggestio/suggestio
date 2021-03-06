package io.suggest.es.model

import io.suggest.primo.TypeT
import io.suggest.primo.id.OptStrId
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.core.TimeValue
import org.elasticsearch.search.SearchHit

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:24
 * Description: Общий код для обычный и child-моделей.
 * Был вынесен из-за разделения в логике работы обычный и child-моделей.
 *
 * TODO Надо распилить на чистую статику и всякие _save(), имеющие DI-зависимость.
 *      Затем, статика должна жить в object, а динамика - как-то иначе.
 */
trait EsModelCommonStaticT extends EsModelStaticMapping with TypeT { outer =>

  override type T <: EsModelCommonT

  // Кое-какие константы, которые можно переопределить в рамках конкретных моделей.
  def MAX_RESULTS_DFLT = EsModelUtil.MAX_RESULTS_DFLT
  def OFFSET_DFLT = EsModelUtil.OFFSET_DFLT
  def SCROLL_KEEPALIVE_MS_DFLT = EsModelUtil.SCROLL_KEEPALIVE_MS_DFLT
  def SCROLL_KEEPALIVE_DFLT = TimeValue.timeValueMillis( SCROLL_KEEPALIVE_MS_DFLT )
  def SCROLL_SIZE_DFLT = EsModelUtil.SCROLL_SIZE_DFLT
  def BULK_PROCESSOR_BULK_SIZE_DFLT = EsModelUtil.BULK_PROCESSOR_BULK_SIZE_DFLT

  /** Если модели требуется выставлять routing для ключа, то можно делать это через эту функцию.
    *
    * @param idOrNull String id or null.
    * @return None если routing не требуется, иначе Some(String).
    */
  def getRoutingKey(idOrNull: String): Option[String] = None

  /** Кол-во item'ов в очереди на удаление. */
  def BULK_DELETE_QUEUE_LEN = 200


  /** Десериализация по новому API: документ передается напрямую, а данные извлекаются через статический typeclass.
    *
    * @param doc Документ, т.е. GetResponse или SearchHit или же ещё что-то...
    * @param ev Неявный typeclass, обеспечивающий унифицированный интерфейс к doc.
    * @tparam D Класс переданного документа.
    * @return Экземпляр модели.
    */
  def deserializeOne2[D](doc: D)(implicit ev: IEsDoc[D]): T


  def UPDATE_RETRIES_MAX: Int = EsModelUtil.UPDATE_RETRIES_MAX_DFLT

  /** Update _id, _version and possibly other fields.
    *
    * @param m Instance.
    * @param docMeta New ES document metadata.
    * @return Updated instance.
    */
  def withDocMeta(m: T, docMeta: EsDocMeta): T

  def _save(m: T)(f: () => Future[EsDocMeta]): Future[EsDocMeta] =
    f()


  def toJsonPretty(m: T): String = toJson(m)
  def toJson(m: T): String


  // TODO Надо как-то унести это в EsModel implicits, не ясно только как: внутренний implicit-код сильно зависит от explicit-инстанса модели.
  /** Implicit API модели завёрнуто в этот класс, который можно экстендить. */
  object Implicits {

    /** Mock-адаптер для тестирования сериализации-десериализации моделей на базе play.json.
      * На вход он получает просто экземпляры классов моделей. */
    implicit def mockPlayDocRespEv: IEsDoc[T] = new IEsDoc[T] {
      override def id(v: T): Option[String] =
        v.id
      override def version(v: T): EsDocVersion =
        v.versioning
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
          deserializeOne2( from )
        override def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
          super.prepareSrb(srb)
            .setFetchSource(true)
        }
        override def toString: String =
          s"${outer.getClass.getSimpleName}.${super.toString}"
      }
    }

    override def toString: String =
      s"${outer.getClass.getSimpleName}.${getClass.getSimpleName}"

  }

}


/** Общий код динамических частей модели, независимо от child-модели или обычной. */
trait EsModelCommonT extends OptStrId {

  /** Модели, желающие версионизации, должны перезаписать это поле. */
  def versioning: EsDocVersion

}


object EsModelCommonT {
  implicit final class EsModelCommonOpsExt( private val esModel: EsModelCommonT ) extends AnyVal {
    def idOrNull: String =
      esModel.id.orNull
  }
}


object BulkProcessorListener extends MacroLogsImpl
trait BulkProcessorListenerT
  extends BulkProcessor.Listener
{
  import BulkProcessorListener.LOGGER

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
case class BulkProcessorListener(override val _logPrefix: String) extends BulkProcessorListenerT

