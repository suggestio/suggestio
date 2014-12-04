package io.suggest.util

import io.suggest.model.JsonDfsBackend
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import scala.concurrent.{Promise, Future}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.admin.indices.optimize.{OptimizeResponse, OptimizeRequestBuilder}
import scala.concurrent.ExecutionContext
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import scala.util.{Failure, Success}
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS
import org.elasticsearch.index.shard.service.InternalIndexShard.INDEX_REFRESH_INTERVAL

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 17:58
 * Description:
 */

object SioEsIndexUtil extends MacroLogsImpl with Serializable {

  import LOGGER._

  // Описание идентификатор типов
  type IITYPE_t = String
  type IIMap_t  = JsonDfsBackend.JsonMap_t
  type AnyJsonMap = scala.collection.Map[String, Any]

  // Дефолтовые настройки скролла.
  val SCROLL_TIMEOUT_DFLT      = TimeValue.timeValueMinutes(2)
  val SCROLL_TIMEOUT_INIT_DFLT = TimeValue.timeValueSeconds(30)
  val SCROLL_PER_SHARD_DFLT = 25

  val iterableOnlyTrue = { l: Iterable[Boolean] => l.forall(_ == true) }

  val IS_TOLERANT_DFLT = true


  /**
   * Удалить только указанные типы из указанного индекса. Функция последовательно удаляет маппинги
   * парралельно во всех перечисленных индексах.
   * @param indices Имена индексов
   * @param types Удаляемые типы (маппинги).
   * @return true, если всё нормально.
   */
  def deleteMappingsSeqFrom(indices:Seq[String], types:Seq[String])(implicit client:Client, executor:ExecutionContext): Future[_] = {
    val adm = client.admin().indices()
    val p = Promise[Any]()
    // Рекурсивный последовательный фьючерс.
    def deleteMapping(restTypes:Seq[String]) {
      if (restTypes.nonEmpty) {
        val h :: t = restTypes
        adm.prepareDeleteMapping(indices: _*)
          .setType(h)
          .execute()
          .onComplete { result =>
            lazy val indicesStr = indices.mkString(",")
            result match {
              case Success(_)  => debug(s"Mapping $h sucessfully erased from indices $indicesStr")
              case Failure(ex) => error(s"Failed to delete mapping '$h' from indices $indicesStr")
            }
            // Рекурсивно запустить следующую итерацию независимо от результатов.
            deleteMapping(t)
          }
      } else {
        p success None
      }
    }
    deleteMapping(types)
    p.future
  }


  /**
   * Удалить указанные шарды (индексы) целиком.
   * @param indicies имя шарды
   * @return Фьючерс с ответом о завершении удаления индекса.
   */
  def deleteShard(indicies:Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    info("Deleting empty index %s..." format indicies)
    val adm = client.admin().indices()
    val fut: Future[DeleteIndexResponse] = adm.prepareDelete(indicies: _*).execute()
    // Если включен дебаг, то доложить в лог о завершении.
    fut.onComplete {
      case Success(result) => debug("Indices %s deletion result: %s" format(indicies, result))
      case Failure(ex)     => error("Failed to delete indices %s" format indicies, ex)
    }
    fut.map(_.isAcknowledged)
  }


  /**
   * Прооптимизировать индекс просто выкинув из него удаленные документы.
   * @param indices список индексов для оптимизации
   * @return true, если всё ок.
   */
  def optimizeExpunge(indices: Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    debug("Expunge deletes on indices %s..." format indices)
    val adm = client.admin().indices()
    val fut: Future[OptimizeResponse] = {
      new OptimizeRequestBuilder(adm)
        .setIndices(indices : _*)
        .setOnlyExpungeDeletes(true)
        .execute()
    }
    fut onComplete {
      case Success(resp) => debug("Optimize expunge indices %s completed: %s" format (indices, resp.toString))
      case Failure(ex)   => error("Failed to optimize-expunge %s" format indices, ex)
    }
    fut map { _ => true }
  }


  /**
   * Выставить маппинг в es для всех индексов.
   * @param indices Список индексов. По дефолту запрашивается у виртуального индекса.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return фьючерс с isAcknowledged.
   */
  def setMappingsFor(indices:Seq[String], typename:String, failOnError:Boolean = true)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    val mappingData = SioEsUtil.getPageMapping(typename)
    // Запустить параллельную загрузку маппинга во все индексы.
    client.admin().indices()
      .preparePutMapping(indices: _*)
      .setType(typename)
      .setSource(mappingData)
      .execute()
      .flatMap { resp =>
        val isAck = resp.isAcknowledged
        if (isAck || !failOnError) {
          Future.successful(isAck)

        } else {
          val msg = "Failed to set mappings for subshard %s. indices=%s" format (typename, indices)
          error(msg)
          Future.failed(new RuntimeException(msg))
        }
      }
  }


  /**
   * Выставить кол-во реплик для указанных индексов.
   * @param indices индексы.
   * @param replicasCount новое кол-во реплик.
   * @return Удачный фьючерс, когда всё ок.
   */
  def setReplicasCountFor(indices:Seq[String], replicasCount:Int)(implicit client:Client, executor:ExecutionContext): Future[_] = {
    debug(s"setReplicasCountFor($replicasCount for ${indices.mkString(",")}) called...")
    val settings = ImmutableSettings.settingsBuilder()
      .put(SETTING_NUMBER_OF_REPLICAS, replicasCount)
      .build()
    client.admin().indices()
      .prepareUpdateSettings()
      .setIndices(indices: _*)
      .setSettings(settings)
      .execute()
      .map(_ => Unit)
  }


  /** Обновить интервал авто-рефреша. Интервал нужен для near-realtime-индексации.
    * @param indices Индексы.
    * @param newIntervalSec Новый интервал в секундах.
    * @return Фьючерс для синхронизации.
    */
  def setIndexRefreshInterval(indices: Seq[String], newIntervalSec: Int)(implicit client:Client, executor:ExecutionContext): Future[_] = {
    trace(s"setIndexRefreshInterval($newIntervalSec, ${indices.mkString(", ")}): Starting...")
    val settings = ImmutableSettings.settingsBuilder()
      .put(INDEX_REFRESH_INTERVAL, newIntervalSec)
      .build()
    client.admin().indices()
      .prepareUpdateSettings(indices : _*)
      .setSettings(settings)
      .execute()
  }

}
