package io.suggest.es.model

import japgolly.univeq._
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.index.mapper.{IdFieldMapper, RoutingFieldMapper, SourceFieldMapper, VersionFieldMapper}
import org.elasticsearch.xcontent.{ToXContent, XContentFactory}

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 * 2014.sep.04: Появились child-модели. Произошло разделение api трейтов: статических и немного динамических.
 */
object EsModelUtil {

  /** Сколько раз по дефолту повторять попытку update при конфликте версий. */
  def UPDATE_RETRIES_MAX_DFLT = 5

  /** Special use_field_mapping format tells Elasticsearch to use the format from the mapping.
    *
    * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/6.8/search-request-docvalue-fields.html]]
    */
  final def USE_FIELD_MAPPING = "use_field_mapping"


  def MAX_RESULTS_DFLT = 100
  def OFFSET_DFLT = 0
  def SCROLL_KEEPALIVE_MS_DFLT = 60000L

  /** Дефолтовый размер скролла, т.е. макс. кол-во получаемых за раз документов. */
  def SCROLL_SIZE_DFLT = 10

  /** number of actions, после которого bulk processor делает flush. */
  def BULK_PROCESSOR_BULK_SIZE_DFLT = 100

  def ES_EXECUTE_WARN_IF_TAKES_TOO_LONG_MS = 1000


  /** Стандартные имена полей ES. */
  object StandardFieldNames {
    def SOURCE        = SourceFieldMapper.NAME
    def ROUTING       = RoutingFieldMapper.NAME
    def ID            = IdFieldMapper.NAME
    def VERSION       = VersionFieldMapper.NAME
    def DOC           = "_doc"
  }


  object Settings {

    object Index {

      private def INDEX_PREFIX = "index."

      def REFRESH_INTERVAL = INDEX_PREFIX + "refresh_interval"
      def NUMBER_OF_REPLICAS = INDEX_PREFIX + "number_of_replicas"
      def NUMBER_OF_SHARDS = INDEX_PREFIX + "number_of_shards"

    }

  }


  /** Десериализовать тело документа внутри GetResponse в строку. */
  def deserializeGetRespBodyRawStr(getResp: GetResponse): Option[String] = {
    if (getResp.isExists) {
      val result = getResp.getSourceAsString
      Some(result)
    } else {
      None
    }
  }


  /** Десериализовать документ всырую, вместе с _id, _type и т.д. */
  def deserializeGetRespFullRawStr(getResp: GetResponse): Option[String] = {
    val baos = new ByteArrayOutputStream( 2048 )

    val result: String = try {
      val xc = XContentFactory
        .jsonBuilder(baos)
        .prettyPrint()

      try {
        getResp.toXContent( xc, ToXContent.EMPTY_PARAMS )
      } finally {
        xc.close()
      }

      new String( baos.toByteArray, StandardCharsets.UTF_8 )
    } finally {
      baos.close()
    }

    Some(result)
  }

}


/** Интерфейс контейнера данных для вызова [[EsModelUtil]].tryUpdate(). */
trait ITryUpdateData[T <: EsModelCommonT, TU <: ITryUpdateData[T, TU]] {

  def _instance(m: T): TU

  /** Экземпляр, пригодный для сохранения. */
  def _saveable: T

}

/** Реализация контейнера для вызова [[EsModelUtil]].tryUpdate() для es-моделей. */
final class TryUpdateData[T <: EsModelCommonT](override val _saveable: T) extends ITryUpdateData[T, TryUpdateData[T]] {
  override def _instance(m: T) = new TryUpdateData(m)
}


/**
 * Результаты работы метода Model.copyContent() возвращаются в этом контейнере.
 *
 * @param success Кол-во успешных документов, т.е. для которых выполнена чтене и запись.
 * @param failed Кол-во обломов.
 */
final case class CopyContentResult(success: Long, failed: Long)
