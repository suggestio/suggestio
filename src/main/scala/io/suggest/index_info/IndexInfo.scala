package io.suggest.index_info

import org.joda.time.LocalDate
import io.suggest.model.{SioSearchContext, JsonDfsBackend}
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import scala.concurrent.{Promise, Future}
import org.elasticsearch.action.search.{SearchType, SearchResponse}
import io.suggest.util.SioEsUtil._
import io.suggest.util.{DateParseUtil, Logs}
import org.elasticsearch.action.admin.indices.optimize.{OptimizeResponse, OptimizeRequestBuilder}
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import scala.util.{Failure, Success}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.action.index.IndexRequest
import SioEsConstants.FIELD_DATE
import java.util.Date

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.04.13 16:09
 * Description: Интерфейсы и константы для классов *IndexInfo.
 */

trait IndexInfo extends Logs with Serializable {

  import IndexInfoStatic._

  val dkey: String

  // Вернуть алиас типа
  val iitype : IITYPE_t

  /**
   * Строка, которая дает идентификатор этому индексу в целом, безотносительно числа шард/типов и т.д.
   * @return ASCII-строка без пробелов, включающая в себя имя используемой шарды и, вероятно, dkey.
   */
  def name: String

  // имя индекса, в которой лежат страницы для указанной даты
  def indexTypeForDate(d:LocalDate) : (String, String)

  def export : IIMap_t

  // тип, используемый для хранения страниц.
  def type_page : String

  /**
   * Является ли индекс шардовым или нет? Генератор реквестов может учитывать это при построении
   * запроса к ElasticSearch.
   */
  def isSharded : Boolean

  /**
   * Вебморда собирается сделать запрос, и ей нужно узнать то, какие индесы необходимо опрашивать
   * и какие типы в них фильтровать.
   * TODO тут должен быть некий экземпляр request context, который будет описывать ход поиска.
   * @param sc Контекст поискового запроса.
   * @return Список названий индексов-шард и список имен типов в этих индексах.
   */
  def indexesTypesForRequest(sc:SioSearchContext) : (List[String], List[String])

  def allShards: List[String]
  def allTypes:  List[String]
  def shardTypesMap : Map[String, Seq[String]]

  /**
   * Убедиться, что все шарды ES созданы.
   * @return true, если всё ок.
   */
  def ensureShards(implicit client:Client): Future[Boolean] = {
    Future.traverse(allShards) { shardName =>
      client
        .admin()
        .indices()
        .prepareCreate(shardName)
        .setSettings(getNewIndexSettings(shards=1))
        .execute()
        .map { _.isAcknowledged }

    } map iterableOnlyTrue
  }


  /**
   * Залить все маппинги в базу. Пока что тут у всех одинаковый маппинг, однако потом это всё будет усложняться и
   * выноситься на уровень имплементаций.
   * @return true, если всё ок.
   */
  def ensureMappings(implicit client:Client) : Future[Boolean] = {
    val adm = client.admin().indices()
    Future.traverse(shardTypesMap) { case (inx, types) =>
      Future.traverse(types) { typ =>
        adm.preparePutMapping(inx)
          .setSource(getPageMapping(typ))
          .execute()
          .map { resp =>
            val result = resp.isAcknowledged
            // При ошибке написать в лог
            if(!result) {
              error("Cannot ensure inx/typ %s/%s: %s" format(inx, typ, resp.getHeaders))
            }
            result
          }
      } map iterableOnlyTrue
    } map iterableOnlyTrue
  }

  def delete(implicit client: Client): Future[Boolean]


  /**
   * Удалить только данные из индексов, не трогая сами индексы. Это ресурсоемкая операция, используется для удаления
   * части данных из индекса.
   * @return true, если всё нормально.
   */
  def deleteMappings(implicit client: Client): Future[Boolean] = {
    Future.traverse(shardTypesMap) {
      case (inx, types) => deleteMappingsFrom(inx, types)
    } map iterableOnlyTrue
  }


  /**
   * Подготовиться к скроллингу по индексу. Скроллинг позволяет эффективно проходить по огромным объемам данных курсором.
   * @return scroll_id, который нужно передавать аргументом в сlient.prepareSearchScroll()
   */
  def startFullScroll(timeout:TimeValue = SCROLL_TIMEOUT_INIT_DFLT, sizePerShard:Int = SCROLL_PER_SHARD_DFLT)(implicit client:Client): Future[SearchResponse] = {
    val _allShards = allShards
    val _allTypes  = allTypes
    debug("startFullScroll() starting: indices=%s types=%s timeout=%ss perShard=%s" format(_allShards, _allTypes, timeout, sizePerShard))
    client
      .prepareSearch(_allShards: _*)
      .setSearchType(SearchType.SCAN)
      .setTypes(_allTypes: _*)
      .setSize(sizePerShard)
      .setQuery(QueryBuilders.matchAllQuery())
      .setScroll(timeout)
      .execute()
  }

}


// Статическая часть IndexInfo.
object IndexInfoStatic extends Logs with Serializable {

  // Описание идентификатор типов
  type IITYPE_t = String
  type IIMap_t  = JsonDfsBackend.JsonMap_t
  type AnyJsonMap = scala.collection.Map[String, Any]

  val IITYPE_SMALL_MULTI  : IITYPE_t = "smi"
  val IITYPE_BIG_SHARDED  : IITYPE_t = "bsi"

  // Дефолтовые настройки скролла.
  val SCROLL_TIMEOUT_DFLT      = TimeValue.timeValueMinutes(2)
  val SCROLL_TIMEOUT_INIT_DFLT = TimeValue.timeValueSeconds(30)
  val SCROLL_PER_SHARD_DFLT = 25

  /**
   * Импорт данных из экспортированной карты и типа.
   * @param iitype тип (выхлоп iitype)
   * @param iimap карта (результат IndexInfo.export)
   * @return Инстанс IndexInfo.
   */
  def apply(dkey:String, iitype: IITYPE_t, iimap: AnyJsonMap): IndexInfo = {
    iitype match {
      case IITYPE_SMALL_MULTI => SmallMultiIndex(dkey, iimap)
      case IITYPE_BIG_SHARDED => BigShardedIndex(dkey, iimap)
    }
  }

  val iterableOnlyTrue = {l: Iterable[Boolean] => l.forall(_ == true)}

  val futureTrue  = Future.successful(true)
  val futureFalse = Future.successful(false)
  val futureNone  = Future.successful(None)

  val IS_TOLERANT_DFLT = true


  /**
   * Удалить только указанные типы из указанного индекса.
   * @param inx имя шарды.
   * @param types Удаляемые типы.
   * @return true, если всё нормально.
   */
  def deleteMappingsFrom(inx:String, types:Seq[String], optimize:Boolean = true)(implicit client: Client): Future[Boolean] = {
    val adm = client.admin().indices()
    // TODO тут лучше бы последовательное, а не парралельное удаление...
    val fut = Future.traverse(types) { typ =>
      adm.prepareDeleteMapping(inx)
        .setType(typ)
        .execute()
        .map(_ => true)
    } .map(iterableOnlyTrue)
    // Заказать оптимизацию индекса, если удаление маппингов прошло ок.
    if (optimize) {
      fut onSuccess { case true => optimizeExpunge(inx) }
    }
    // Вернуть фьючерс
    fut
  }


  /**
   * Удалить указанные шарды (индексы) целиком.
   * @param indicies имя шарды
   * @return Фьючерс с ответом о завершении удаления индекса.
   */
  def deleteShard(indicies:String *)(implicit client:Client): Future[Boolean] = {
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
  def optimizeExpunge(indices: String *)(implicit client:Client): Future[Boolean] = {
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
  def copy(fromIndex:IndexInfo, toIndex:IndexInfo, isTolerant:Boolean = IS_TOLERANT_DFLT)(implicit client:Client): Future[Boolean] = {
    val logPrefix = "copy(%s -> %s tolerant=%s): " format(fromIndex.name, toIndex.name, isTolerant)
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
          toIndex.delete andThen {
            case Success(_)   => Future.failed(ex)

            // Не удалось почистить/удалить кривой индекс. Возможно, индекса не существует или работа кластера сильно нарушена.
            case Failure(ex1) =>
              error("Cannot rollback: failed to delete inconsistent index %s" format toIndex.name, ex1)
              Future.failed(ex)
          }
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
  def move(fromIndex:IndexInfo, toIndex:IndexInfo, isTolerant:Boolean = IS_TOLERANT_DFLT)(implicit client:Client): Future[Boolean] = {
    lazy val logPrefix = "move(%s -> %s tolerant=%s): " format(fromIndex.name, toIndex.name, isTolerant)
    debug(logPrefix + "Starting copy()...")
    copy(fromIndex, toIndex, isTolerant) flatMap { _ =>
      debug(logPrefix + "copy() finished. Let's delete old index %s..." format fromIndex.name)
      fromIndex.delete
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
  def scrollImport(scrollId: String, toIndex: IndexInfo, timeout:TimeValue = SCROLL_TIMEOUT_DFLT, isTolerant:Boolean = IS_TOLERANT_DFLT)(implicit client: Client) = {
    val logPrefix = "scrollImport(-> %s): " format toIndex.name
    info(logPrefix + "starting...")
    val p = Promise[Boolean]()
    def scrollImportIteration(_scrollId: String) {
      val fut: Future[SearchResponse] = client.prepareSearchScroll(_scrollId).setScroll(timeout).execute()
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
                  val (inx, typ) = toIndex.indexTypeForDate(date)
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
              p success true
            }

          // Проблема импорта целой пачки документов или isTolerant отключен. Надо прерываться в любом случае.
          } catch {
            case ex:Throwable =>
              p failure ex
              error(logPrefix + "error during hits bulk import", ex)
          }


        // Возникла проблема при выполнении Search-запроса. Прерываемся, ибо новый scroll_id отсутствует, и продолжение невозможно.
        case Failure(ex) =>
          error("Cannot import data into %s" format toIndex, ex)
          p failure ex
      }
    }

    // Запустить скроллинг. Функция почти сразу уйдет в фон.
    scrollImportIteration(scrollId)
    // Вернуть overall-фьючерс вопрошающему.
    p.future
  }

}