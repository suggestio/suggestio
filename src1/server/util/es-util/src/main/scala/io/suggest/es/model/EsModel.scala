package io.suggest.es.model

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.es.scripts.IAggScripts
import io.suggest.es.util.SioEsUtil
import io.suggest.es.util.SioEsUtil._
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.primo.id.OptId
import javax.inject.{Inject, Singleton}
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get.MultiGetRequest.Item
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.client.Client
import play.api.cache.AsyncCacheApi
import japgolly.univeq._
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.cluster.metadata.{IndexMetaData, MappingMetaData}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.scripted.ScriptedMetric
import org.elasticsearch.search.sort.SortBuilders

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:32
 * Description: Файл содержит трейты для базовой сборки типичных ES-моделей, без parent-child и прочего.
 */
@Singleton
final class EsModel @Inject()(
                               cache        : AsyncCacheApi,
                             )(
                               implicit ec  : ExecutionContext,
                               esClient     : Client,
                               mat          : Materializer,
                               sn           : SioNotifierStaticClientI,
                             ) { esModel =>


  /** Сконвертить распарсенные результаты в карту. */
  private def resultsToMap[T <: OptId[String]](results: TraversableOnce[T]): Map[String, T] =
    OptId.els2idMap[String, T](results)


  // TODO Код ниже уровня моделей унести в classs EsIndexUtil.
  /**
    * Убедиться, что индекс существует.
    *
    * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
    *         Если индекс уже существует, то false.
    */
  def ensureIndex(indexName: String, shards: Int = 5, replicas: Int = 1): Future[Boolean] = {
    for {
      existsResp <- esClient.admin().indices()
        .prepareExists(indexName)
        .executeFut()

      _ <- if (existsResp.isExists) {
        Future.successful(false)
      } else {
        val indexSettings = SioEsUtil
          .getIndexSettingsV2(shards=shards, replicas=replicas)
        esClient.admin().indices()
          .prepareCreate(indexName)
          .setSettings(indexSettings)
          .executeFut()
          .map { _ => true }
      }
    } yield {
      true
    }
  }


  /**
   * Собрать указанные значения id'шников в аккамулятор-множество.
   *
   * @param searchResp Экземпляр searchResponse.
   * @param acc0 Начальный акк.
   * @param keepAliveMs keepAlive для курсоров на стороне сервера ES в миллисекундах.
   * @return Фьчерс с результирующим аккамулятором-множеством.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html#scrolling]]
   */
  def searchScrollResp2ids(searchResp: SearchResponse, maxAccLen: Int, firstReq: Boolean, currAccLen: Int = 0,
                           acc0: List[String] = Nil, keepAliveMs: Long = 60000L): Future[List[String]] = {
    val hits = searchResp.getHits.getHits
    if (!firstReq && hits.isEmpty) {
      Future successful acc0
    } else {
      val nextAccLen = currAccLen + hits.length
      val canContinue = maxAccLen <= 0 || nextAccLen < maxAccLen
      val nextScrollRespFut = if (canContinue) {
        // Лимит длины акк-ра ещё не пробит. Запустить в фоне получение следующей порции результатов...
        esClient
          .prepareSearchScroll(searchResp.getScrollId)
          .setScroll(new TimeValue(keepAliveMs))
          .executeFut()
      } else {
        null
      }
      // Если акк заполнен, то надо запустить очистку курсора на стороне ES.
      if (!canContinue) {
        esClient
          .prepareClearScroll()
          .addScrollId( searchResp.getScrollId )
          .executeFut()
      }
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val accNew = hits.foldLeft[List[String]] (acc0) { (acc1, hit) =>
        hit.getId :: acc1
      }
      if (canContinue) {
        // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
        nextScrollRespFut flatMap { searchResp2 =>
          searchScrollResp2ids(searchResp2, maxAccLen, firstReq = false, currAccLen = nextAccLen, acc0 = accNew, keepAliveMs = keepAliveMs)
        }
      } else {
        // Пробит лимит аккамулятора по maxAccLen - вернуть акк не продолжая обход.
        Future successful accNew
      }
    }
  }


  /**
   * Узнать метаданные индекса.
   *
   * @param indexName Название индекса.
   * @return Фьючерс с опциональными метаданными индекса.
   */
  def getIndexMeta(indexName: String): Future[Option[IndexMetaData]] = {
    esClient.admin().cluster()
      .prepareState()
      .setIndices(indexName)
      .executeFut()
      .map { cs =>
        val maybeResult = cs.getState
          .getMetaData
          .index(indexName)
        Option(maybeResult)
      }
  }

  /**
   * Прочитать метаданные маппинга.
   *
   * @param indexName Название индекса.
   * @param typeName Название типа.
   * @return Фьючерс с опциональными метаданными маппинга.
   */
  def getIndexTypeMeta(indexName: String, typeName: String): Future[Option[MappingMetaData]] = {
    getIndexMeta(indexName) map { imdOpt =>
      imdOpt.flatMap { imd =>
        Option(imd.mapping(typeName))
      }
    }
  }

  /**
   * Существует ли указанный маппинг в хранилище? Используется, когда модель хочет проверить наличие маппинга
   * внутри общего индекса.
   *
   * @param typeName Имя типа.
   * @return Да/нет.
   */
  def isMappingExists(indexName: String, typeName: String): Future[Boolean] = {
    for {
      metaOpt <- getIndexTypeMeta(indexName, typeName = typeName)
    } yield {
      metaOpt.isDefined
    }
  }

  /** Прочитать текст маппинга из хранилища. */
  def getCurrentMapping(indexName: String, typeName: String): Future[Option[String]] = {
    for {
      metaOpt <- getIndexTypeMeta(indexName, typeName = typeName)
    } yield {
      for (meta <- metaOpt) yield
        meta.source().string()
    }
  }


  /** Сгенерить InternalError, если хотя бы две es-модели испрользуют одно и тоже хранилище для данных.
    * В сообщении экзепшена будут перечислены конфликтующие модели. */
  def errorIfIncorrectModels(allModels: Iterable[EsModelCommonStaticT]): Unit = {
    // Запускаем проверку, что в моделях не используются одинаковые типы в одинаковых индексах.
    def esModelId(esModel: EsModelCommonStaticT): String =
      s"${esModel.ES_INDEX_NAME}/${esModel.ES_TYPE_NAME}"

    val uniqModelsCnt = allModels.iterator
      .map(esModelId)
      .toSet
      .size
    if (uniqModelsCnt < allModels.size) {
      // Найдены модели, которые испрользуют один и тот же индекс+тип. Нужно вычислить их и вернуть в экзепшене.
      val errModelsStr = allModels
        .map { m => esModelId(m) -> m.getClass.getName }
        .groupBy(_._1)
        .valuesIterator
        .filter { _.size > 1 }
        .map { _.mkString(", ") }
        .mkString("\n")
      throw new InternalError("Two or more es models using same index+type for data store:\n" + errModelsStr)
    }
  }


  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelCommonStaticT], ignoreExists: Boolean = false): Future[Boolean] = {
    import api._

    Future.traverse(models) { esModelStatic =>
      val logPrefix = s"${esModelStatic.getClass.getSimpleName}.putMapping():"
      val imeFut = if (ignoreExists) {
        Future.successful(false)
      } else {
        esModelStatic.isMappingExists()
      }
      imeFut.flatMap {
        case false =>
          LOGGER.trace(s"$logPrefix Trying to push mapping for model...")
          val fut = esModelStatic.putMapping()
          fut.onComplete {
            case Success(isOk)  =>
              if (isOk) LOGGER.trace(s"$logPrefix -> OK" )
              else LOGGER.warn(s"$logPrefix NOT ACK!!! Possibly out-of-sync.")
            case Failure(ex)    =>
              LOGGER.error(s"$logPrefix FAILed to put mapping to ${esModelStatic.ES_INDEX_NAME}/${esModelStatic.ES_TYPE_NAME}:\n-------------\n${esModelStatic.generateMapping.string()}\n-------------\n", ex)
          }
          fut

        case true =>
          LOGGER.trace(s"$logPrefix Mapping already exists in index. Skipping...")
          Future successful true
      }
    }.map {
      _.reduceLeft { _ && _ }
    }
  }


  /** Пройтись по всем ES_MODELS и проверить, что всех ихние индексы существуют. */
  def ensureEsModelsIndices(models: Seq[EsModelCommonStaticT]): Future[_] = {
    val indices = models.map { esModel =>
      esModel.ES_INDEX_NAME -> (esModel.SHARDS_COUNT, esModel.REPLICAS_COUNT)
    }.toMap
    Future.traverse(indices) {
      case (inxName, (shards, replicas)) =>
        esModel.ensureIndex(inxName, shards=shards, replicas=replicas)
    }
  }


  /** Рекурсивная асинхронная сверстка скролл-поиска в ES.
    * Перед вызовом функции надо выпонить начальный поисковый запрос, вызвав с setScroll() и,
    * по возможности, включив SCAN.
    *
    * @param searchResp Результат выполненного поиского запроса с активным scroll'ом.
    * @param acc0 Исходное значение аккамулятора.
    * @param firstReq Флаг первого запроса. По умолчанию = true.
    *                 В первом и последнем запросах не приходит никаких результатов, и их нужно различать.
    * @param keepAliveMs Значение keep-alive для курсора на стороне ES.
    * @param f fold-функция, генереящая на основе результатов поиска и старого аккамулятора новый аккамулятор типа A.
    * @tparam A Тип аккамулятора.
    * @return Данные по результатам операции, включающие кол-во удач и ошибок.
    */
  def foldSearchScroll[A](searchResp: SearchResponse, acc0: A, firstReq: Boolean = true,
                          keepAliveMs: Long = EsModelUtil.SCROLL_KEEPALIVE_MS_DFLT, fromClient: Client = esClient)
                         (f: (A, SearchHits) => Future[A]): Future[A] = {
    val hits = searchResp.getHits
    val scrollId = searchResp.getScrollId
    lazy val logPrefix = s"foldSearchScroll($scrollId, 1st=$firstReq):"
    if (!firstReq  &&  hits.getHits.isEmpty) {
      LOGGER.trace(s"$logPrefix no more hits.")
      Future.successful(acc0)
    } else {
      // Запустить в фоне получение следующей порции результатов
      LOGGER.trace(s"$logPrefix has ${hits.getHits.length} hits, total = ${hits.getTotalHits}")
      // Убеждаемся, что scroll выставлен. Имеет смысл проверять это только на первом запросе.
      if (firstReq)
        assert(scrollId != null && !scrollId.isEmpty, "Scrolling looks like disabled. Cannot continue.")
      val nextScrollRespFut: Future[SearchResponse] = {
        fromClient
          .prepareSearchScroll(scrollId)
          .setScroll(new TimeValue(keepAliveMs))
          .executeFut()
      }
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val acc1Fut = f(acc0, hits)
      // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
      nextScrollRespFut.flatMap { searchResp2 =>
        acc1Fut.flatMap { acc1 =>
          foldSearchScroll(searchResp2, acc1, firstReq = false, keepAliveMs, fromClient)(f)
        }
      }
    }
  }


  /**
    * Обновление какого-то элемента с использованием es save и es optimistic locking.
    * В отличии от оригинального [[EsModelStaticT]].tryUpdate(), здесь обновляемые данные не обязательно
    * являются элементами той же модели, а являются контейнером для них..
    *
    * @param data0 Обновляемые данные.
    * @param maxRetries Максимальное кол-во попыток [5].
    * @param updateF Обновление
    * @tparam D Тип обновляемых данных.
    * @return Удачно-сохраненный экземпляр data: T.
    */
  def tryUpdateM[X <: EsModelCommonT, D <: ITryUpdateData[X, D]]
                (companion: EsModelCommonStaticT { type T = X }, data0: D, maxRetries: Int = 5)
                (updateF: D => Future[D]): Future[D] = {
    import api._
    lazy val logPrefix = s"tryUpdateM(${System.currentTimeMillis}):"

    val data1Fut = updateF(data0)

    if (data1Fut == null) {
      LOGGER.debug(logPrefix + " updateF() returned `null`, leaving update")
      Future.successful(data0)

    } else {
      data1Fut.flatMap { data1 =>
        val m2 = data1._saveable
        if (m2 == null) {
          LOGGER.debug(logPrefix + " updateF() data with `null`-saveable, leaving update")
          Future.successful(data1)
        } else {
          // TODO Спилить обращение к companion, принимать статическую модель в аргументах
          companion
            .save(m2)
            .map { _ => data1 }
            .recoverWith {
              case exVsn: VersionConflictEngineException =>
                if (maxRetries > 0) {
                  val n1 = maxRetries - 1
                  LOGGER.warn(s"$logPrefix Version conflict while tryUpdate(). Retry ($n1)...")
                  data1._reget.flatMap { data2 =>
                    tryUpdateM[X, D](companion, data2, n1)(updateF)
                  }
                } else {
                  val ex2 = new RuntimeException(s"$logPrefix Too many save-update retries failed", exVsn)
                  Future.failed(ex2)
                }
            }
        }
      }
    }
  }


  /** По аналогии со slick, тут собраны методы для es-моделей.
    *
    * Позволяет использовать статические методы для унифицированного управления ES-моделями.
    * {{{
    *   class SomeController @Inject()(esModel: EsModel) {
    *     import esModel.api._
    *
    *     mNodes.doSomething(...)
    *   }
    * }}}
    */
  class Api {

    trait EsModelCommonStaticUntypedOpsT {

      val model: EsModelCommonStaticT

      def prepareScroll(keepAlive: TimeValue = model.SCROLL_KEEPALIVE_DFLT, srb: SearchRequestBuilder = model.prepareSearch()): SearchRequestBuilder = {
        srb
          .setScroll(keepAlive)
          // Elasticsearch-2.1+: вместо search_type=SCAN желательно юзать сортировку по полю _doc.
          .addSort( SortBuilders.fieldSort( StdFns.FIELD_DOC ) )
      }

      /** Прочитать маппинг текущей ES-модели из ES. */
      def getCurrentMapping(): Future[Option[String]] = {
        esModel.getCurrentMapping(
          indexName = model.ES_INDEX_NAME,
          typeName  = model.ES_TYPE_NAME
        )
      }


      // TODO Нужно проверять, что текущий маппинг не устарел, и обновлять его.
      def isMappingExists(): Future[Boolean] = {
        esModel.isMappingExists(
          indexName = model.ES_INDEX_NAME,
          typeName  = model.ES_TYPE_NAME
        )
      }

      /**
        * Посчитать кол-во документов в текущей модели.
        *
        * @return Неотрицательное целое.
        */
      def countAll(): Future[Long] = {
        model.countByQuery( QueryBuilders.matchAllQuery() )
      }


      /**
        * Сервисная функция для получения списка всех id.
        *
        * @return Список всех id в неопределённом порядке.
        */
      def getAllIds(maxResults: Int, maxPerStep: Int = model.MAX_RESULTS_DFLT): Future[List[String]] = {
        model
          .prepareScroll()
          .setQuery( QueryBuilders.matchAllQuery() )
          .setSize(maxPerStep)
          .setFetchSource(false)
          //.setNoFields()
          .executeFut()
          .flatMap { searchResp =>
            esModel.searchScrollResp2ids(
              searchResp,
              firstReq    = true,
              maxAccLen   = maxResults,
              keepAliveMs = model.SCROLL_KEEPALIVE_MS_DFLT
            )
          }
      }


      /**
       * Запустить пакетное копирование данных модели из одного ES-клиента в другой.
       *
       * @param fromClient Откуда брать данные?
       * @param toClient Куда записывать данные?
       * @param reqSize Размер реквеста. По умолчанию 50.
       * @param keepAliveMs Время жизни scroll-курсора на стороне from-сервера.
       * @return Фьючерс для синхронизации.
       */
      def copyContent(fromClient: Client, toClient: Client, reqSize: Int = 50, keepAliveMs: Long = model.SCROLL_KEEPALIVE_MS_DFLT): Future[CopyContentResult] = {
        model
          .prepareScroll( new TimeValue(keepAliveMs), srb = model.prepareSearchViaClient(fromClient))
          .setSize(reqSize)
          .executeFut()
          .flatMap { searchResp =>
            // для различания сообщений в логах, дополнительно генерим id текущей операции на базе первого скролла.
            val logPrefix = s"copyContent(${searchResp.getScrollId.hashCode / 1000L}): "
            foldSearchScroll(searchResp, CopyContentResult(0L, 0L), keepAliveMs = keepAliveMs, fromClient = fromClient) {
              (acc0, hits) =>
                LOGGER.trace(s"$logPrefix${hits.getHits.length} hits read from source")
                // Нужно запустить bulk request, который зальёт все хиты в toClient
                val iter = hits.iterator().asScala
                if (iter.nonEmpty) {
                  val bulk = toClient.prepareBulk()
                  for (hit <- iter) {
                    val el = model.deserializeSearchHit(hit)
                    bulk.add( model.prepareIndexNoVsnUsingClient(el, toClient) )
                  }
                  for {
                    bulkResult <- bulk.executeFut()
                  } yield {
                    if (bulkResult.hasFailures)
                      LOGGER.error("copyContent(): Failed to write bulk into target:\n " + bulkResult.buildFailureMessage())
                    val failedCount = bulkResult.iterator()
                      .asScala
                      .count(_.isFailed)
                    val acc1 = acc0.copy(
                      success = acc0.success + bulkResult.getItems.length - failedCount,
                      failed  = acc0.failed + failedCount
                    )
                    LOGGER.trace(s"${logPrefix}bulk write finished. acc.success = ${acc1.success} acc.failed = ${acc1.failed}")
                    acc1
                  }
                } else {
                  Future.successful(acc0)
                }
            }
          }
      }


      /**
        * В es 2.0 удалили поддержку delete by Query.
        * Тут реализация этого недостающего функционала.
        *
        * @param scroller выхлоп startScroll.
        * @return Фьючерс с результатами.
        */
      def deleteByQuery(scroller: SearchRequestBuilder): Future[Int] = {
        lazy val logPrefix = s"deleteByQuery(${System.currentTimeMillis}):"
        LOGGER.trace(s"$logPrefix Starting...")

        val counter = new AtomicInteger(0)

        val listener = new BulkProcessor.Listener {
          /** Перед отправкой каждого bulk-реквеста... */
          override def beforeBulk(executionId: Long, request: BulkRequest): Unit = {
            LOGGER.trace(s"$logPrefix $executionId Before bulk delete ${request.numberOfActions()} documents...")
          }

          /** Документы в очереди успешно удалены. */
          override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
            val countDeleted = response.getItems.length
            LOGGER.trace(s"$logPrefix $executionId Successfully deleted $countDeleted, ${response.buildFailureMessage()}")
            counter.addAndGet(countDeleted)
          }

          /** Ошибка bulk-удаления. */
          override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
            LOGGER.error(s"$logPrefix Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
          }
        }

        // Собираем асинхронный bulk-процессор, т.к. элементов может быть ну очень много.
        val bp = model.bulkProcessor(listener, model.BULK_DELETE_QUEUE_LEN)

        // Интересуют только id документов
        val totalFut = scroller
          .setFetchSource(false)
          .executeFut()
          .flatMap { searchResp =>
            foldSearchScroll(searchResp, acc0 = 0, firstReq = true, keepAliveMs = model.SCROLL_KEEPALIVE_MS_DFLT) {
              (acc01, hits) =>
                for (hit <- hits.iterator().asScala) {
                  val req = esClient.prepareDelete(hit.getIndex, hit.getType, hit.getId)
                    .request()
                  bp.add(req)
                }
                val docsDeletedNow = hits.getHits.length
                val acc02 = acc01 + docsDeletedNow
                LOGGER.trace(s"$logPrefix $docsDeletedNow docs queued for deletion, total queued now: $acc02 docs.")
                Future.successful(acc02)
            }
          }

        for (total <- totalFut) yield {
          bp.close()
          LOGGER.debug(s"$logPrefix $total DEL reqs sent, now deleted ${counter.get()} docs.")
          total
        }
      }


      /** Быстрый рассчёт контрольной суммы для всех найденных документов.
        *
        * @param q Query.
        * @param sourceFields Полные названия полей документа, которые участвую в рассчёте хэша.
        * @return Фьючерс с Int'ом внутри.
        */
      def docsHashSum(scripts: IAggScripts, q: QueryBuilder = QueryBuilders.matchAllQuery()): Future[Int] = {
        val aggName = "dcrc"

        for {
          resp <- model
            .prepareSearch()
            .setQuery( q )
            .setSize(0)
            .setFetchSource(false)
            .addAggregation(
              AggregationBuilders
                .scriptedMetric( aggName )
                .initScript( scripts.initScript )
                .mapScript( scripts.mapScript )
                .combineScript( scripts.combineScript )
                .reduceScript( scripts.reduceScript )
            )
            .executeFut()
        } yield {
          // Извлечь результат из ES-ответа:
          val agg = resp.getAggregations.get[ScriptedMetric](aggName)
          LOGGER.trace(s"docsHashSum(): r=${Option(agg).map(_.aggregation()).orNull} totalHits=${resp.getHits.totalHits}")
          agg.aggregation().asInstanceOf[Integer].intValue()
        }
      }

    }

    implicit final class EsModelCommonStaticUntypedOps(override val model: EsModelCommonStaticT)
      extends EsModelCommonStaticUntypedOpsT


    /** Типизированный API для EsModelCommonStaticT. */
    trait EsModelCommonStaticTypedOpsT[T1 <: EsModelCommonT] {

      val model: EsModelCommonStaticT { type T = T1 }

      /**
        * Сохранить экземпляр в хранилище ES.
        *
        * @return Фьючерс с новым/текущим id
        *         VersionConflictException если транзакция в текущем состоянии невозможна.
        */
      def save(m: T1): Future[String] = {
        model._save(m) { () =>
          model
            .prepareIndex(m)
            .executeFut()
            .map { _.getId }
        }
      }

      /**
       * Метод для краткого запуска скроллинга над моделью.
       *
       * @param queryOpt Поисковый запрос, по которому скроллим. Если None, то будет matchAll().
       * @param resultsPerScroll Кол-во результатов за каждую итерацию скролла.
       * @param keepAliveMs TTL scroll-курсора на стороне ES.
       * @return Фьючерс, подлежащий дальнейшей обработке.
       */
      def startScroll(queryOpt: Option[QueryBuilder] = None, resultsPerScroll: Int = model.SCROLL_SIZE_DFLT,
                            keepAliveMs: Long = model.SCROLL_KEEPALIVE_MS_DFLT): SearchRequestBuilder = {
        val query = queryOpt.getOrElse {
          QueryBuilders.matchAllQuery()
        }
        val req = model
          .prepareScroll(new TimeValue(keepAliveMs))
          .setQuery(query)
          .setSize(resultsPerScroll)
          .setFetchSource(true)
        LOGGER.trace(s"startScroll($queryOpt, rps=$resultsPerScroll, kaMs=$keepAliveMs): query = $query")
        req
      }


      /** Генератор реквеста для генерации запроса для getAll(). */
      def getAllReq(maxResults: Int = model.MAX_RESULTS_DFLT, offset: Int = model.OFFSET_DFLT, withVsn: Boolean = false): SearchRequestBuilder = {
        model
          .prepareSearch()
          .setQuery(QueryBuilders.matchAllQuery())
          .setSize(maxResults)
          .setFrom(offset)
          .setVersion(withVsn)
      }

      /** Запуск поискового запроса и парсинг результатов в представление этой модели. */
      def runSearch(srb: SearchRequestBuilder): Future[Seq[T1]] = {
        srb
          .executeFut()
          .map { model.searchResp2stream }
      }

      /**
       * Выдать все магазины. Метод подходит только для административных задач.
       *
       * @param maxResults Макс. размер выдачи.
       * @param offset Абсолютный сдвиг в выдаче.
       * @param withVsn Возвращать ли версии?
       * @return Список магазинов в порядке их создания.
       */
      def getAll(maxResults: Int = model.MAX_RESULTS_DFLT, offset: Int = model.OFFSET_DFLT, withVsn: Boolean = false): Future[Seq[T1]] = {
        runSearch(
          getAllReq(
            maxResults = maxResults,
            offset = offset,
            withVsn = withVsn
          )
        )
      }


      /**
       * Реактивное обновление всех документов модели.
       * Документы читаются пачками через scroll и сохраняются пачками через bulk по мере готовности оных.
       * Функция обработчик может быть асинхронной, т.е. может затрагивать другие модели или производить другие
       * асинхронные сайд-эффекты. Функция обработчки никогда НЕ должна вызывать save(), а лишь порождать новый
       * экземпляр модели, пригодный для сохранения.
       * Внутри метода используется BulkProcessor, который асинхронно, по мере наполнения очереди индексации,
       * отправляет реквесты на индексацию.
       * Метод полезен для обновления модели, которое затрагивает внутреннюю структуру данных.
       *
       * @param bulkActions Макс.кол-во запросов в очереди на bulk-индексацию. После пробоя этого значения,
       *                    вся очередь реквестов будет отправлена на индексацию.
       * @param f Функция-маппер, которая порождает фьючерс с новым обновлённым экземпляром модели.
       * @return Фьючес с кол-вом обработанных экземпляров модели.
       */
      def updateAll(scroller: SearchRequestBuilder, bulkActions: Int = model.BULK_PROCESSOR_BULK_SIZE_DFLT)
                   (f: T1 => Future[T1]): Future[Int] = {

        val logPrefix = s"update(${System.currentTimeMillis}):"

        val bpListener = new model.BulkProcessorListener(logPrefix)
        val bp = model.bulkProcessor(bpListener)

        // Создаём атомный счетчик, который будет инкрементится из разных потоков одновременно.
        // Можно счетчик гнать через аккамулятор, но это будет порождать много бессмысленного мусора.
        val counter = new AtomicInteger(0)

        // Выполнить обход модели. Аккамулятор фиксирован (не используется).
        val foldFut = foldLeftAsync1(None, scroller) {
          (accFut, v) =>
            f(v).flatMap {
              case null =>
                LOGGER.trace(s"$logPrefix Skipped update of [${v.idOrNull}], f() returned null")
                accFut
              case v1 =>
                bp.add( model.prepareIndex(v1).request )
                counter.incrementAndGet()
                accFut
            }
        }

        // Вернуть результат
        for (_ <- foldFut) yield {
          bp.close()
          counter.get
        }
      }


      /**
        * Пройтись асинхронно по всем документам модели.
        *
        * @param acc0 Начальный аккамулятор.
        * @param keepAliveMs Таймаут курсора на стороне ES.
        * @param f Асинхронная функция обхода.
        * @tparam A Тип аккамулятора.
        * @return Финальный аккамулятор.
        */
      def foldLeft[A](acc0: A, scroller: SearchRequestBuilder, keepAliveMs: Long = model.SCROLL_KEEPALIVE_MS_DFLT)
                           (f: (A, T1) => A): Future[A] = {
        scroller
          .executeFut()
          .flatMap { searchResp =>
            foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
              (acc01, hits) =>
                val acc02 = hits
                  .iterator()
                  .asScala
                  .map { model.deserializeSearchHit }
                  .foldLeft(acc01)(f)
                Future.successful( acc02 )
            }
          }
      }


      /**
       * Аналог foldLeft, но с асинхронным аккамулированием. Полезно, если функция совершает какие-то сайд-эффекты.
       *
       * @param acc0 Начальный акк.
       * @param resultsPerScroll Кол-во результатов с каждой шарды за одну scroll-итерацию [10].
       * @param keepAliveMs TTL scroll-курсора на стороне ES.
       * @param f Функция асинхронной сверстки.
       * @tparam A Тип значения аккамулятора (без Future[]).
       * @return Фьючерс с результирующим аккамулятором.
       */
      // TODO Удалить эту прослойку.
      def foldLeftAsync[A](acc0: A, resultsPerScroll: Int = model.SCROLL_SIZE_DFLT,
                           keepAliveMs: Long = model.SCROLL_KEEPALIVE_MS_DFLT, queryOpt: Option[QueryBuilder] = None)
                          (f: (Future[A], T1) => Future[A]): Future[A] = {
        val scroller = model.startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs, queryOpt = queryOpt)
        foldLeftAsync1(acc0, scroller, keepAliveMs)(f)
      }

      def foldLeftAsync1[A](acc0: A, scroller: SearchRequestBuilder, keepAliveMs: Long = model.SCROLL_KEEPALIVE_MS_DFLT)
                           (f: (Future[A], T1) => Future[A]): Future[A] = {
        scroller
          .executeFut()
          .flatMap { searchResp =>
            foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
              (acc01, hits) =>
                hits.iterator()
                  .asScala
                  .map { model.deserializeSearchHit }
                  .foldLeft(Future.successful(acc01))( f )
            }
          }
      }

      /** Общий код моделей, которые занимаются resave'ом. */
      def resaveBase( getFut: Future[Option[T1]] ): Future[Option[String]] = {
        getFut.flatMap { getResOpt =>
          FutureUtil.optFut2futOpt(getResOpt) { e =>
            model.save(e)
              .map { EmptyUtil.someF }
          }
        }
      }

      /** Отрендерить экземпляр модели в JSON, обёрнутый в некоторое подобие метаданных ES (без _index и без _type). */
      def toEsJsonDoc(e: T1): String = {
        import StdFns._

        var kvs = List[String] (s""" "$FIELD_SOURCE": ${model.toJson(e)}""")
        if (e.versionOpt.isDefined)
          kvs ::= s""" "$FIELD_VERSION": ${e.versionOpt.get}"""
        if (e.id.isDefined)
          kvs ::= s""" "$FIELD_ID": "${e.id.get}" """
        kvs.mkString("{",  ",",  "}")
      }

      /** Отрендерить экземпляры моделей в JSON. */
      def toEsJsonDocs(e: TraversableOnce[T1]): String = {
        e.toIterator
          .map { toEsJsonDoc }
          .mkString("[",  ",\n",  "]")
      }


      /** Реализация контейнера для вызова [[EsModelUtil]].tryUpdate() для es-моделей. */
      class TryUpdateData(override val _saveable: T1)
        extends model.TryUpdateDataAbstract[TryUpdateData]
      {
        override protected def _instance(m: T1) = new TryUpdateData(m)
      }

      /** Вместо TryUpdateData.apply(). */
      def tryUpdateData(inst: T1) = {
        new TryUpdateData(inst)
      }

      /**
       * Попытаться обновить экземпляр модели с помощью указанной функции.
       * Метод является надстройкой над save, чтобы отрабатывать VersionConflict.
       *
       * @param inst0 Исходный инстанс, который необходимо обновить.
       * @param retry Счетчик попыток.
       * @param updateF Функция для апдейта. Может возвращать null для внезапного отказа от апдейта.
       * @return Тоже самое, что и save().
       *         Если updateF запретила апдейт (вернула null), то будет Future.successfull(inst0).
       */
      def tryUpdate(inst0: T1, retry: Int = 0)(updateF: T1 => T1): Future[T1] = {
        // 2015.feb.20: Код переехал в EsModelUtil, а тут остались только wrapper для вызова этого кода.
        val data0 = tryUpdateData(inst0)
        val data2Fut = tryUpdateM[T1, TryUpdateData](model, data0, model.UPDATE_RETRIES_MAX) { data =>
          val data1 = tryUpdateData(
            updateF(data._saveable)
          )
          Future.successful(data1)
        }
        for (data2 <- data2Fut) yield {
          data2._saveable
        }
      }

    }

    implicit final class EsModelCommonStaticTypedOps[T1 <: EsModelCommonT]( override val model: EsModelCommonStaticT { type T = T1 } )
      extends EsModelCommonStaticTypedOpsT[T1]


    /** Статические методы для hi-level API: */
    trait EsModelStaticOpsT[T1 <: EsModelT] {

      val model: EsModelStaticT { type T = T1 }

      /**
        * Удалить документ по id.
        *
        * @param id id документа.
        * @return true, если документ найден и удалён. Если не найден, то false
        */
      def deleteById(id: String): Future[Boolean] = {
        val fut = model.deleteRequestBuilder(id)
          .executeFut()
          .map { EsModelStaticT.delResp2isDeleted }
        model._deleteById(id)(fut)
      }


      /** Удаляем сразу много элементов.
        * @return Обычно Some(BulkResponse), но если нет id'шников в запросе, то будет None.
        */
      def deleteByIds(ids: Iterable[String]): Future[Option[BulkResponse]] = {
        val resFut = if (ids.isEmpty) {
          Future.successful(None)
        } else {
          val bulk = esClient.prepareBulk()
          for (id <- ids) {
            val delReq = model.prepareDelete(id)
            bulk.add( delReq )
          }
          bulk
            .executeFut()
            .map( EmptyUtil.someF )
        }
        model._deleteByIds(ids)(resFut)
      }


      /**
        * Существует ли указанный документ в текущем типе?
        *
        * @param id id документа.
        * @return true/false
        */
      def isExist(id: String): Future[Boolean] = {
        model
          .prepareGet(id)
          .setFetchSource(false)
          .executeFut()
          .map { _.isExists }
      }

      /** Вернуть id если он задан. Часто бывает, что idOpt, а не id. */
      def maybeGetById(idOpt: Option[String], options: GetOpts = model._getArgsDflt): Future[Option[T1]] = {
        FutureUtil.optFut2futOpt(idOpt) {
          model.getById(_, options)
        }
      }


      /**
        * Выбрать документ из хранилища без парсинга. Вернуть сырое тело документа (его контент).
        *
        * @param id id документа.
        * @return Строка json с содержимым документа или None.
        */
      def getRawContentById(id: String): Future[Option[String]] = {
        model
          .prepareGet(id)
          .executeFut()
          .map { EsModelUtil.deserializeGetRespBodyRawStr }
      }

      /**
        * Прочитать документ как бы всырую.
        *
        * @param id id документа.
        * @return Строка json с документом полностью или None.
        */
      def getRawById(id: String): Future[Option[String]] = {
        model
          .prepareGet(id)
          .executeFut()
          .map { EsModelUtil.deserializeGetRespFullRawStr }
      }


      /**
       * Прочитать из базы все перечисленные id разом.
       *
       * @param ids id документов этой модели. Можно передавать как коллекцию, так и свеженький итератор оной.
       * @param acc0 Начальный аккамулятор.
       * @return Список результатов в порядке ответа.
       */
      def multiGet(ids: TraversableOnce[String], options: GetOpts = model._getArgsDflt): Future[Stream[T1]] = {
        if (ids.isEmpty) {
          Future.successful( Stream.empty )
        } else {
          multiGetRaw(ids, options)
            .map( model.mgetResp2Stream )
        }
      }
      def multiGetRaw(ids: TraversableOnce[String], options: GetOpts = model._getArgsDflt): Future[MultiGetResponse] = {
        val req = esClient.prepareMultiGet()
          .setRealtime(true)
        val indexName = model.ES_INDEX_NAME
        val typeName = model.ES_TYPE_NAME
        for (id <- ids) {
          val item = new Item(indexName, typeName, id)
          for (sf <- options.sourceFiltering)
            item.fetchSourceContext( sf.toFetchSourceCtx )
          req.add(item)
        }
        req.executeFut()
      }

      /**
        * Пакетно вернуть инстансы модели с указанными id'шниками, но в виде карты (id -> T).
        * Враппер над multiget, но ещё вызывает resultsToMap над результатами.
        *
        * @param ids Коллекция или итератор необходимых id'шников.
        * @param acc0 Необязательный начальный акк. полезен, когда некоторые инстансы уже есть на руках.
        * @return Фьючерс с картой результатов.
        */
      def multiGetMap(ids: TraversableOnce[String], options: GetOpts = model._getArgsDflt): Future[Map[String, T1]] = {
        multiGet(ids, options = options)
          // Конвертим список результатов в карту, где ключ -- это id. Если id нет, то выкидываем.
          .map { resultsToMap }
      }


      /** Тоже самое, что и multiget, но этап разбора ответа сервера поточный: элементы возвращаются по мере парсинга. */
      def multiGetSrc(ids: Traversable[String], options: GetOpts = model._getArgsDflt): Source[T1, _] = {
        if (ids.isEmpty) {
          Source.empty
        } else {
          lazy val idsCount = ids.size
          lazy val logPrefix = s"multiGetSrc($idsCount)#${System.currentTimeMillis()}:"
          LOGGER.trace(s"$logPrefix Will source $idsCount items: ${ids.mkString(", ")}")

          val srcFut = for {
            resp <- multiGetRaw(ids, options = options)
          } yield {
            LOGGER.trace(s"$logPrefix Fetched ${resp.getResponses.length} of ${ids.size}")
            val items = resp.getResponses
            if (items.isEmpty) {
              Source.empty
            } else {
              Source( items.toStream )
            }
          }
          Source
            .fromFutureSource( srcFut )
            .filterNot { i =>
              val r = i.isFailed || !i.getResponse.isExists
              if (r)
                LOGGER.trace(s"$logPrefix Dropping ${i.getId} as failed or invalid: ${Option(i.getFailure).flatMap(f => Option(f.getMessage))}")
              r
            }
            .map { i =>
              model.deserializeOne2( i.getResponse )
            }
        }
      }


      /** Обойти с помощью функции, которая выдаёт узлы для следующего шага.
        *
        * @param ids id элементов.
        * @param acc0 Аккамулятор.
        * @param multiGetSrcF Функция для сорсинга. Для кэша модели можно переопределить.
        * @param f Функция обработки одного элемента.
        * @tparam A Тип аккамулятора.
        * @return Фьючерс с финальным аккамулятором.
        */
      def walkUsing[A](acc0: A,  ids: Set[String],  multiGetSrcF: Set[String] => Source[T1, _], counter: Int = 0)
                      (f: (A, T1) => (A, Set[String])): Future[A] = {
        lazy val logPrefix = s"walkUsing(${ids.size})[$counter]:"

        if (counter >= model.MAX_WALK_STEPS) {
          LOGGER.error(s"$logPrefix Stop, too many walk steps.\n ids[${ids.size}] = ${ids.mkString(", ")}")
          Future.failed( new IllegalArgumentException(s"$logPrefix Too many iterations: $counter") )

        } else if (ids.isEmpty) {
          Future.successful(acc0)

        } else {
          // Сорсим элементы по id из хранилища:
          LOGGER.trace(s"$logPrefix Step#${counter}")
          multiGetSrc(ids)
            .toMat(
              Sink.fold (acc0 -> Set.empty[String]) {
                case ((xacc0, idsAcc0), el) =>
                  // TODO Opt Для теоретического ускорения работы можно запускать чтение НОВЫХ id сразу после каждого f() и заброс фьючерсов в акк.
                  val (xacc2, ids2) = f(xacc0, el)
                  xacc2 -> (idsAcc0 ++ ids2)
              }
            )( Keep.right )
            .run()
            .flatMap { case (acc2, needIds2) =>
              walkUsing(acc2, needIds2, multiGetSrcF)(f)
            }
        }
      }


      def resave(id: String): Future[Option[String]] = {
        model.resaveBase( model.getById(id) )
      }


      /** Обойти с помощью функции, которая выдаёт узлы для следующего шага.
        * Можно применить Пригодно для поиска
        *
        * @param els Узлы, начиная от которых надо плясать.
        * @param acc0 Начальный аккамулятор.
        * @param f Функция, определяющая новый акк и список узлов, которые тоже надо тоже отработать.
        * @tparam A Тип аккамулятора.
        * @return Итоговый аккамулятор функции.
        */
      def walk[A](acc0: A, ids: Set[String])(f: (A, T1) => (A, Set[String])): Future[A] = {
        walkUsing(acc0, ids, multiGetSrc(_))(f)
      }

    }

    implicit final class EsModelStaticOps[T1 <: EsModelT] ( override val model: EsModelStaticT { type T = T1 } )
      extends EsModelStaticOpsT[T1]


    /** Поддержка кэширования для моделей, помеченных трейтом cacheable. */
    implicit final class EsModelCacheOps[T1 <: EsModelT]( model: EsModelStaticCacheableT { type T = T1 } ) {

      def getByIdFromCache(id: String)(implicit classTag: ClassTag[T1]): Future[Option[T1]] = {
        val ck = model.cacheKey(id)
        cache.get[T1](ck)
      }

      /**
       * Вернуть закешированный результат либо прочитать его из хранилища.
       * @param id id исходного документа.
       * @return Тоже самое, что и исходный getById().
       */
      def getByIdCache(id: String)(implicit classTag: ClassTag[T1]): Future[Option[T1]] = {
        // 2014.nov.24: Форсируем полный асинхрон при работе с кешем.
        val ck = model.cacheKey(id)
        cache.get[T1](ck)
          .filter { _.isDefined }
          .recoverWith { case _: NoSuchElementException =>
            LOGGER.trace(s"getById($id): Not found $id in cache")
            getByIdAndCache(id, ck)
          }
      }


      /**
       * Аналог getByIdCached, но для multiget().
       * @param ids id'шники, которые надо бы получить. Ожидается, что будут без дубликатов.
       * @return Результаты в неопределённом порядке.
       */
      def multiGetCache(ids: Iterable[String])(implicit classTag: ClassTag[T1]): Future[Seq[T1]] = {
        if (ids.isEmpty) {
          Future.successful(Nil)

        } else {
          // Сначала ищем элементы в кэше. Можно искать в несколько потоков, что и делаем:
          val cachedFoundEithersFut = Future.traverse( ids ) { id =>
            for (resOpt <- getByIdFromCache(id)) yield
              resOpt.toRight( id )
          }

          cachedFoundEithersFut.flatMap { cachedFoundEiths =>

            // Как можно скорее запустить multiGET к базе за недостающими элементами:
            val nonCachedIds = cachedFoundEiths
              .iterator
              .collect {
                case Left(id) => id
              }
              .toSet

            // Ленивая коллекция уже закэшированных результатов:
            val cachedResults = cachedFoundEiths
              .iterator
              .collect {
                case Right(r) => r
              }
              // Максимально лениво, чтобы отложить это всё напотом без lazy val или def:
              .toStream

            // Если есть отсутствующие в кэше элементы, то поискать их в хранилищах:
            if (nonCachedIds.nonEmpty) {
              // Строим трубу для чтения и параллельного кэширования элементов. Не очень ясно, будет ли это быстрее, но вполне модно и реактивно:
              val nonCachedResultsFut = model
                .multiGetSrc( nonCachedIds )
                .alsoTo(
                  Sink.foreach[T1]( cacheThat )
                )
                // TODO Opt Дописывать сразу в аккамулятор из cachedResults вместо начально-пустого акка? Начальный seq builder инициализировать с size?
                .toMat( Sink.seq )( Keep.right )
                .run()

              // Объеденить результаты из кэша и результаты из хранилища:
              val allResFut = if (cachedResults.isEmpty) {
                nonCachedResultsFut
              } else {
                for (nonCachedResults <- nonCachedResultsFut) yield
                  // Vector ++ Stream.
                  nonCachedResults ++ cachedResults
              }

              // Вернуть итоговый фьючерс с объединёнными результатами (в неочень неопределённом порядке):
              allResFut

            } else {
              Future.successful( cachedResults )
            }
          }
        }
      }


      /** Вернуть Source-источник для
        *
        * @param ids id искомых элементов.
        * @return Source, для которого в фоне уже начали собираться возвращаемые элементы.
        */
      def multiGetCacheSrc(ids: Iterable[String])(implicit classTag: ClassTag[T1]): Source[T1, _] = {
        // TODO Реализовать нормальную выкачку, более оптимальную по сравнению с multiGet()
        Source.fromFutureSource {
          for (els <- multiGetCache(ids)) yield
            Source( els.toStream )
        }
      }


      def multiGetMapCache(ids: Set[String])(implicit classTag: ClassTag[T1]): Future[Map[String, T1]] = {
        multiGetCache(ids)
          .map { resultsToMap }
      }


      def cacheThat(result: T1): Unit = {
        val id = result.id.get
        val ck = model.cacheKey(id)
        cache.set(ck, result, model.EXPIRE)
      }

      def cacheThese(results: T1*): Unit =
        cacheThese1(results)

      /** Принудительное кэширование для всех указанных item'ов. */
      def cacheThese1(results: TraversableOnce[T1]): Unit =
        results.foreach( cacheThat )


      /**
       * Если id задан, то прочитать из кеша или из хранилища. Иначе вернуть None.
       * @param idOpt Опциональный id.
       * @return Тоже самое, что и [[getByIdCache]].
       */
      def maybeGetByIdCached(idOpt: Option[String])(implicit classTag: ClassTag[T1]): Future[Option[T1]] = {
        FutureUtil.optFut2futOpt(idOpt)(getByIdCache)
      }
      def maybeGetByEsIdCached(esIdOpt: Option[MEsUuId])(implicit classTag: ClassTag[T1]): Future[Option[T1]] = {
        maybeGetByIdCached(
          esIdOpt.map(_.id)
        )
      }

      /**
       * Прочитать из хранилища документ, и если всё нормально, то отправить его в кеш.
       * @param id id документа.
       * @param ck0 Ключ в кеше.
       * @return Тоже самое, что и исходный getById().
       */
      def getByIdAndCache(id: String): Future[Option[T1]] = {
        val ck = model.cacheKey(id)
        getByIdAndCache(id, ck)
      }
      def getByIdAndCache(id: String, ck: String): Future[Option[T1]] = {
        val resultFut = model.getById(id)
        for (adnnOpt <- resultFut) {
          // TODO Кэш для None надо держать? Можно короткий EXPIRE организовать, просто для защиты от атак.
          for (adnn <- adnnOpt)
            cache.set(ck, adnn, model.EXPIRE)
        }
        resultFut
      }


      def putToCache(value: T1): Future[_] = {
        val ck = model.cacheKey( value.id.get )
        cache.set(ck, value, model.EXPIRE)
      }


      /** Гуляние по графу узлов/элементов через кэш с помощью функции. */
      def walkCache[A](acc0: A, ids: Set[String])
                      (f: (A, T1) => (A, Set[String]))
                      (implicit classTag: ClassTag[T1]): Future[A] = {
        model.walkUsing(acc0, ids, multiGetCacheSrc(_))(f)
      }

    }

  }
  val api = new Api

}

trait EsModelDi {
  val esModel: EsModel
}

object EsModelStaticT {

  def delResp2isDeleted(dr: DeleteResponse): Boolean = {
    dr.getResult ==* Result.DELETED
  }

}

/** Базовый шаблон для статических частей ES-моделей, НЕ имеющих _parent'ов. Применяется в связке с [[EsModelT]].
  * Здесь десериализация полностью выделена в отдельную функцию. */
trait EsModelStaticT extends EsModelCommonStaticT {

  override type T <: EsModelT

  import mCommonDi._

  final def prepareGet(id: String) =
    prepareGetBase(id)

  final def prepareDelete(id: String) =
    prepareDeleteBase(id)


  /** Дефолтовое значение GetArgs, когда GET-опции не указаны. */
  def _getArgsDflt: GetOpts = GetOptsDflt

  /**
   * Выбрать ряд из таблицы по id.
   *
   * @param id Ключ документа.
   * @return Экземпляр сабжа, если такой существует.
   */
  final def getById(id: String, options: GetOpts = _getArgsDflt): Future[Option[T]] = {
    val rq = prepareGet(id)
    for (sf <- options.sourceFiltering) {
      rq.setFetchSource(sf.includes.toArray, sf.excludes.toArray)
    }
    rq.executeFut()
      .map { deserializeGetRespFull }
  }


  /**
   * Генератор delete-реквеста. Используется при bulk-request'ах.
   *
   * @param id adId
   * @return Новый экземпляр DeleteRequestBuilder.
   */
  final def deleteRequestBuilder(id: String): DeleteRequestBuilder = {
    val req = prepareDelete(id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  /** Дополнение логики удаления одного элемента, когда необходимо. */
  def _deleteById(id: String)(fut: Future[Boolean]): Future[Boolean] =
    fut

  /** Дополнение логики удаления нескольких элементов, когда необходимо. */
  def _deleteByIds(ids: Iterable[String])(fut: Future[Option[BulkResponse]]): Future[Option[BulkResponse]] =
    fut

  override final def reget(inst0: T): Future[Option[T]] = {
    getById(inst0.id.get)
  }


  def MAX_WALK_STEPS = 50

}

/** Реализация трейта [[EsModelStaticT]] для уменьшения работы компилятору. */
abstract class EsModelStatic extends EsModelStaticT


/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelT extends EsModelCommonT {
}

/** Доп.API для инстансов ES-моделей с явной поддержкой версий. */
trait EsModelVsnedT[T <: EsModelVsnedT[T]] extends EsModelCommonT {

  def withVersion(versionOpt: Option[Long]): T // = copy(versionOpt = versionOpt)

  // После самого первого сохранения выставляется вот эта вот версия:
  def withFirstVersion = withVersion( Some(1L) )

}
