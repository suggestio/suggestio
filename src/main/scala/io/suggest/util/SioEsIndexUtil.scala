package io.suggest.util

import org.joda.time.LocalDate
import io.suggest.model.JsonDfsBackend
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import scala.concurrent.{Promise, Future}
import org.elasticsearch.action.search.{SearchType, SearchResponse}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.admin.indices.optimize.{OptimizeResponse, OptimizeRequestBuilder}
import scala.concurrent.ExecutionContext
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import scala.util.{Failure, Success}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.action.index.IndexRequest
import io.suggest.index_info.{MDVIUnitAlterable, MDVIUnit}
import SioConstants.FIELD_DATE
import java.util.Date
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS
import org.apache.hadoop.fs.FileSystem
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
  def deleteMappingsSeqFrom(indices:Seq[String], types:Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    val adm = client.admin().indices()
    val p = Promise[Unit]()
    // Рекурсивный последовательный фьючерс.
    def deleteMapping(restTypes:Seq[String]) {
      if (!restTypes.isEmpty) {
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
        p success ()
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
   * Высокоуровневая функция копирования данных между индексами. При проблемах фунция чистит конечный индекс.
   * Возврат значения фьючерса происходит только когда выполнены все операции копирования или отката копирования.
   * @param fromIndex Исходный индекс.
   * @param toIndex Целевой индекс.
   * @param isTolerant Гасить одиночные ошибки импорта?
   * @return Фьючерс, по которому можно оценивать окончание импрота.
   */
  def copy(fromIndex:MDVIUnit, toIndex:MDVIUnitAlterable, isTolerant:Boolean = IS_TOLERANT_DFLT)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    val logPrefix = "copy(%s -> %s tolerant=%s): " format (fromIndex.id, toIndex.id, isTolerant)
    debug(logPrefix + "Start scrolling...")
    fromIndex.startFullScroll().flatMap { resp =>
      val scrollId = resp.getScrollId
      if (scrollId == null) {
        // Если id скроллера не пришел, то значит что-то идёт не так. Нужно остановить выполнение.
        val msg = logPrefix + "scroll_id is NULL. Something gone wrong."
        error(msg)
        Future.failed(new RuntimeException(msg))

      } else {
        // Старт дан, и пока всё нормально.
        debug(logPrefix + "Scroller ready. Lets start import...")
        val fut = scrollImport(scrollId, toIndex, isTolerant=isTolerant)
        // Цепочка обработки ошибок импорта: нужно вычистить неконсистентный индекс.
        fut recoverWith { case ex =>
          warn(logPrefix + "scrollImport() failed. Rollback...", ex)
          val futRecover = toIndex.deleteMappings
          futRecover onFailure { case ex1 =>
            error("Cannot rollback: failed to delete inconsistent index %s" format toIndex.id, ex1)
          }
          futRecover
        }
      }
    }
  }


  /**
   * Враппер над copy для перемещения данных между индексами. Тоже самое, что и copy, но если всё нормально, то исходные данные удаляются по завершению.
   * @param fromIndex Исходный индекс.
   * @param toIndex Целевой индекс.
   * @param isTolerant Гасить одиночные ошибки импорта?
   * @return true, когда всё нормально.
   */
  def move(fromIndex:MDVIUnitAlterable, toIndex:MDVIUnitAlterable, isTolerant:Boolean = IS_TOLERANT_DFLT)(implicit client:Client, fs:FileSystem, executor:ExecutionContext): Future[_] = {
    lazy val logPrefix = "move(%s -> %s tolerant=%s): " format (fromIndex.getVin, toIndex.getVin, isTolerant)
    debug(logPrefix + "Starting copy()...")
    copy(fromIndex, toIndex, isTolerant) flatMap { _ =>
      debug(logPrefix + "copy() finished. Let's delete old index %s..." format fromIndex.getVin)
      fromIndex.eraseIndexOrMappings
    }
  }


  /**
   * Цикл импорта из одного индекса в другой. У исходного индекса запущен скроллер, у другого индекса будут выполнятся bulk insert.
   * Тут быдлокод, ибо ряд вещей написан через задницу, чтобы избежать утечки памяти на рекурсивных фьючерсах.
   * В случае серьезной/любой ошибки, функция останавливается и возвращает ошибку, и ничего больше не делает.
   * @param scrollId id курсора на стороне ES.
   * @param timeout Таймаут создаваемых scroll-курсоров.
   * @param isTolerant Если true, то ошибки при обработке документов будут подавляться. По дефолту true, ибо допускаются небольшие потери.
   * @return Фьючерс, который висит пока не наступает успех.
   */
  def scrollImport(scrollId:String, toIndex:MDVIUnitAlterable, timeout:TimeValue = SCROLL_TIMEOUT_DFLT, isTolerant:Boolean = IS_TOLERANT_DFLT)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    val logPrefix = "scrollImport(-> %s): " format toIndex.id
    info(logPrefix + "starting...")
    val p = Promise[Unit]()
    def scrollImportIteration(_scrollId: String) {
      val fut: Future[SearchResponse] = client
        .prepareSearchScroll(_scrollId)
        .setScroll(timeout)
        .execute()
      // Используем голые callback'и вместо всяких flatMap и andThen, т.к. каллабки не порождают жирных хвостов из promise'ов.
      fut onComplete {
        // Пришли результаты скроллинга, как и ожидалось.
        case Success(searchResp) =>
          // Внешний try позволяет перехватить любые ошибки в теле всей итерации импорта.
          try {
            val hits0 = searchResp.getHits
            val hits  = hits0.getHits
            debug(logPrefix + "SearchResp: count=%s total=%s" format(hits.length, hits0.getTotalHits))
            // Произвести некоторые действия
            if (hits.length > 0) {
              // Поступила порция результатов скролла. Нужно запустить bulkInsert в целевой индекс.
              val bulkRequest = client.prepareBulk()

              // Отмаппить все полученные hit'ы на bulkRequest.
              hits.foreach { hit =>
              // Внутренний try позволяет толерантно обходить проблемы внутри цикла.
                try {
                  val date: LocalDate = hit.field(FIELD_DATE) match {
                    case null =>
                      warn(logPrefix + "No `date` field found in %s/%s/%s ; create random" format(hit.getIndex, hit.getType, hit.getId))
                      DateParseUtil.randomDate

                    case r => new LocalDate(r.getValue[Date])
                  }
                  val (inx, typ) = toIndex.getInxTypeForDate(date)
                  val inxReq = new IndexRequest()
                    .index(inx)
                    .`type`(typ)
                    .id(hit.getId)
                    .source(hit.getSourceRef, false)
                  bulkRequest.add(inxReq)

                } catch {
                  case ex:Throwable =>
                    error(logPrefix + "Failed to process doc from %s/%s/%s" format(hit.getIndex, hit.getType, hit.getId), ex)
                    // Если толерантность к ошибкам отключена, то прервать текущий поток.
                    if (!isTolerant) throw ex
                }
              } // hits.foreach

              // При ошибке всё-таки попытаться перейти на следующую итерацию
              // Извлекаем скроллер заранее, чтобы жирный searchResp по-быстрее отправился под резак GC.
              val _scrollId1  = searchResp.getScrollId
              // Отправить в фон сохранение результатов. Затем запустить следующую итерацию.
              bulkRequest.execute() foreach { resp =>
                debug(logPrefix + "Save took %s sec; hasFailures=%s" format(resp.getTookInMillis/1000, resp.hasFailures))
                if(resp.hasFailures)
                  error(logPrefix + resp.buildFailureMessage())
                // Запустить следующую итерацию
                scrollImportIteration(_scrollId1)
              }

              // Скроллинг завершен. Завершить исходный promise.
            } else {
              info(logPrefix + "No more hits received. Finishing...")
              p success ()
            }

            // Проблема импорта целой пачки документов или isTolerant отключен. Надо прерываться в любом случае.
          } catch {
            case ex:Throwable =>
              p failure ex
              error(logPrefix + "error during hits bulk import", ex)
          }


        // Возникла проблема при выполнении Search-запроса. Прерываемся, ибо новый scroll_id отсутствует, и продолжение невозможно.
        case Failure(ex) =>
          error("Cannot import data into %s" format toIndex.id, ex)
          p failure ex
      }
    }

    // Запустить скроллинг. Функция почти сразу уйдет в фон.
    scrollImportIteration(scrollId)
    // Вернуть overall-фьючерс вопрошающему.
    p.future
  }


  /**
   * Подготовиться к скроллингу по индексу (индексам). Скроллинг позволяет эффективно проходить по огромным объемам данных server-side курсором.
   * @return scroll_id, который нужно передавать аргументом в сlient.prepareSearchScroll()
   */
  def startFullScrollIn(allShards:Seq[String], allTypes:Seq[String], timeout:TimeValue = SCROLL_TIMEOUT_INIT_DFLT, sizePerShard:Int = SCROLL_PER_SHARD_DFLT)(implicit client:Client): Future[SearchResponse] = {
    debug("startFullScroll() starting: indices=%s types=%s timeout=%ss perShard=%s" format(allShards, allTypes, timeout, sizePerShard))
    client
      .prepareSearch(allShards: _*)
      .setSearchType(SearchType.SCAN)
      .setTypes(allTypes: _*)
      .setSize(sizePerShard)
      .setQuery(QueryBuilders.matchAllQuery())
      .setScroll(timeout)
      .execute()
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
  def setReplicasCountFor(indices:Seq[String], replicasCount:Int)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
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
