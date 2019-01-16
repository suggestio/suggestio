package io.suggest.es.model

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.common.xcontent.{ToXContent, XContentFactory}

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

  /** Имя индекса, который будет использоваться для хранения данных для большинства остальных моделей. */
  // es-5.0+ запрещает создание индексов, начинающихся с -/+ или прочего непотребства.
  // Однако, индекс "-sio" уже давно с нами, со всеми данными надо промигрировать.
  val DFLT_INDEX        = "sio.main.v5"


  // Имена полей в разных хранилищах. НЕЛЬЗЯ менять их значения.
  val PERSON_ID_ESFN    = "personId"
  val KEY_ESFN          = "key"
  val VALUE_ESFN        = "value"
  val IS_VERIFIED_ESFN  = "isVerified"

  def MAX_RESULTS_DFLT = 100
  def OFFSET_DFLT = 0
  def SCROLL_KEEPALIVE_MS_DFLT = 60000L

  /** Дефолтовый размер скролла, т.е. макс. кол-во получаемых за раз документов. */
  def SCROLL_SIZE_DFLT = 10

  /** number of actions, после которого bulk processor делает flush. */
  def BULK_PROCESSOR_BULK_SIZE_DFLT = 100


  def SHARDS_COUNT_DFLT   = 5
  def REPLICAS_COUNT_DFLT = 1


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
    if (getResp.isExists) {
      val xc = getResp.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS)
      val result = xc.string()
      Some(result)
    } else {
      None
    }
  }

}


/** Интерфейс контейнера данных для вызова [[EsModelUtil]].tryUpdate(). */
trait ITryUpdateData[T <: EsModelCommonT, TU <: ITryUpdateData[T, TU]] {

  def _instance(m: T): TU

  /** Экземпляр, пригодный для сохранения. */
  def _saveable: T

}


/**
 * Результаты работы метода Model.copyContent() возвращаются в этом контейнере.
 *
 * @param success Кол-во успешных документов, т.е. для которых выполнена чтене и запись.
 * @param failed Кол-во обломов.
 */
case class CopyContentResult(success: Long, failed: Long)
