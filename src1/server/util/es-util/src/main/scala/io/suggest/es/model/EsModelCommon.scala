package io.suggest.es.model

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.primo.TypeT
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.OptStrId
import io.suggest.util.JacksonWrapper
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.get.{GetResponse, MultiGetResponse}
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortBuilders

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:24
 * Description:
 */

/** Общий код для обычный и child-моделей. Был вынесен из-за разделения в логике работы обычный и child-моделей. */
trait EsModelCommonStaticT extends EsModelStaticMapping with TypeT { outer =>

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

  def prepareSearch(): SearchRequestBuilder = prepareSearch(esClient)
  def prepareSearch(client: Client): SearchRequestBuilder = {
    client
      .prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
  }

  def prepareCount(): SearchRequestBuilder = {
    prepareSearch()
      .setSize(0)
  }

  def prepareGetBase(id: String) = {
    val req = esClient.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  def prepareUpdateBase(id: String) = {
    val req = esClient.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  def prepareDeleteBase(id: String) = {
    val req = esClient.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
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
    val bp = BulkProcessor
      .builder(esClient, listener)
      .setName(logPrefix)
      .setBulkActions(BULK_DELETE_QUEUE_LEN)
      .build()

    // Интересуют только id документов
    val totalFut = scroller
      .setFetchSource(false)
      .execute()
      .flatMap { searchResp =>
        EsModelUtil.foldSearchScroll(searchResp, acc0 = 0, firstReq = true, keepAliveMs = SCROLL_KEEPALIVE_MS_DFLT) {
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

  /** Кол-во item'ов в очереди на удаление. */
  def BULK_DELETE_QUEUE_LEN = 200


  def prepareScroll(keepAlive: TimeValue = SCROLL_KEEPALIVE_DFLT, srb: SearchRequestBuilder = prepareSearch()): SearchRequestBuilder = {
    srb
      .setScroll(keepAlive)
      // Elasticsearch-2.1+: вместо search_type=SCAN желательно юзать сортировку по полю _doc.
      .addSort( SortBuilders.fieldSort( StdFns.FIELD_DOC ) )
  }

  /** Запуск поискового запроса и парсинг результатов в представление этой модели. */
  def runSearch(srb: SearchRequestBuilder): Future[Seq[T]] = {
    srb.execute().map { searchResp2list }
  }

  /** Прочитать маппинг текущей ES-модели из ES. */
  def getCurrentMapping(): Future[Option[String]] = {
    EsModelUtil.getCurrentMapping(ES_INDEX_NAME, typeName = ES_TYPE_NAME)
  }

  /**
   * Метод для краткого запуска скроллинга над моделью.
   *
   * @param queryOpt Поисковый запрос, по которому скроллим. Если None, то будет matchAll().
   * @param resultsPerScroll Кол-во результатов за каждую итерацию скролла.
   * @param keepAliveMs TTL scroll-курсора на стороне ES.
   * @return Фьючерс, подлежащий дальнейшей обработке.
   */
  def startScroll(queryOpt: Option[QueryBuilder] = None, resultsPerScroll: Int = SCROLL_SIZE_DFLT,
                  keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT): SearchRequestBuilder = {
    val query = queryOpt.getOrElse {
      QueryBuilders.matchAllQuery()
    }
    val req = prepareScroll(new TimeValue(keepAliveMs))
      .setQuery(query)
      .setSize(resultsPerScroll)
      .setFetchSource(true)
    LOGGER.trace(s"startScroll($queryOpt, rps=$resultsPerScroll, kaMs=$keepAliveMs): query = $query")
    req
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
  def foldLeft[A](acc0: A, scroller: SearchRequestBuilder, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                 (f: (A, T) => A): Future[A] = {
    scroller
      .execute()
      .flatMap { searchResp =>
        EsModelUtil.foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
          (acc01, hits) =>
            val acc02 = hits
              .iterator()
              .asScala
              .map { deserializeSearchHit }
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
  def foldLeftAsync[A](acc0: A, resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT,
                       queryOpt: Option[QueryBuilder] = None)
                      (f: (Future[A], T) => Future[A]): Future[A] = {
    val scroller = startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs, queryOpt = queryOpt)
    foldLeftAsync1(acc0, scroller, keepAliveMs)(f)
  }

  def foldLeftAsync1[A](acc0: A, scroller: SearchRequestBuilder, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                       (f: (Future[A], T) => Future[A]): Future[A] = {
    scroller
      .execute()
      .flatMap { searchResp =>
        EsModelUtil.foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
          (acc01, hits) =>
            hits.iterator()
              .asScala
              .map { deserializeSearchHit }
              .foldLeft(Future.successful(acc01))( f )
        }
      }
  }

  // !!! map() с сохранением реализован в методе updateAll() !!!

  /**
   * foreach для асинхронного обхода всех документов модели.
   *
   * @param resultsPerScroll По сколько документов скроллить?
   * @param keepAliveMs Время жизни scroll-курсора на стороне es.
   * @param f Функция обработки одного результата.
   * @return Future синхронизации завершения обхода или ошибки.
   *         Цифра внутри содержит кол-во пройденных результатов.
   */
  def foreach[U](resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                (f: T => U): Future[Long] = {
    // Оборачиваем foldLeft(), просто фиксируя аккамулятор.
    val scroller = startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs)
    foldLeft(0L, scroller, keepAliveMs = keepAliveMs) {
      (acc, inst) =>
        f(inst)
        acc + 1L
    }
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
  def updateAll(scroller: SearchRequestBuilder, bulkActions: Int = BULK_PROCESSOR_BULK_SIZE_DFLT)
               (f: T => Future[T]): Future[Int] = {

    val logPrefix = s"update(${System.currentTimeMillis}):"

    val listener = new BulkProcessor.Listener {
      /** Перед отправкой каждого bulk-реквеста. */
      override def beforeBulk(executionId: Long, request: BulkRequest): Unit = {
        LOGGER.trace(s"$logPrefix Going to execute bulk req with ${request.numberOfActions()} actions.")
      }

      /** Данные успешно отправлены в индекс. */
      override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
        LOGGER.trace(s"$logPrefix afterBulk OK with resp $response")
      }

      /** Ошибка индексации. */
      override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
        LOGGER.error(s"$logPrefix Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
      }
    }

    val bp = BulkProcessor.builder(esClient, listener)
      .setName(logPrefix)
      .setBulkActions(100)
      .build()

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
            bp.add( prepareIndex(v1).request )
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
   * Сервисная функция для получения списка всех id.
   *
   * @return Список всех id в неопределённом порядке.
   */
  def getAllIds(maxResults: Int, maxPerStep: Int = MAX_RESULTS_DFLT): Future[List[String]] = {
    prepareScroll()
      .setQuery( QueryBuilders.matchAllQuery() )
      .setSize(maxPerStep)
      .setFetchSource(false)
      //.setNoFields()
      .execute()
      .flatMap { searchResp =>
        EsModelUtil.searchScrollResp2ids(
          searchResp,
          firstReq    = true,
          maxAccLen   = maxResults,
          keepAliveMs = SCROLL_KEEPALIVE_MS_DFLT
        )
      }
  }

  /**
   * Примитив для рассчета кол-ва документов, удовлетворяющих указанному запросу.
   *
   * @param query Произвольный поисковый запрос.
   * @return Кол-во найденных документов.
   */
  def countByQuery(query: QueryBuilder): Future[Long] = {
    prepareCount()
      .setQuery(query)
      .execute()
      .map { _.getHits.getTotalHits }
  }

  /**
   * Посчитать кол-во документов в текущей модели.
   *
   * @return Неотрицательное целое.
   */
  def countAll(): Future[Long] = {
    countByQuery(QueryBuilders.matchAllQuery())
  }

  // TODO Нужно проверять, что текущий маппинг не устарел, и обновлять его.
  def isMappingExists(): Future[Boolean] = {
    EsModelUtil.isMappingExists(
      indexName = ES_INDEX_NAME,
      typeName  = ES_TYPE_NAME
    )
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
  def searchRespMap[A](searchResp: SearchResponse)(f: SearchHit => A): Iterator[A] = {
    searchResp.getHits
      .iterator()
      .asScala
      .map(f)
  }

  /** Список результатов с source внутри перегнать в распарсенный список. */
  def searchResp2list(searchResp: SearchResponse): Seq[T] = {
    searchRespMap(searchResp)(deserializeSearchHit)
      .toSeq
  }

  def deserializeSearchHit(hit: SearchHit): T = {
    deserializeOne2(hit)
  }

  /** Список результатов в список id. */
  def searchResp2idsList(searchResp: SearchResponse): ISearchResp[String] = {
    val hitsArr = searchResp.getHits.getHits
    new AbstractSearchResp[String] {
      override def total: Long = {
        searchResp.getHits.getTotalHits
      }
      override def length: Int = {
        hitsArr.length
      }
      override def apply(idx: Int): String = {
        hitsArr(idx).getId
      }
    }
  }

  def searchResp2fnList[T](searchResp: SearchResponse, fn: String): Seq[T] = {
    searchRespMap(searchResp) { hit =>
      hit.getField(fn).getValue[T]
    }
      .toSeq
  }


  /** Для ряда задач бывает необходимо задействовать multiGet вместо обычного поиска, который не успевает за refresh.
    * Этот метод позволяет сконвертить поисковые результаты в результаты multiget.
    *
    * @return Результат - что-то неопределённом порядке.
    */
  def searchResp2RtMultiget(searchResp: SearchResponse, acc0: List[T] = Nil): Future[List[T]] = {
    val searchHits = searchResp.getHits.getHits
    if (searchHits.isEmpty) {
      Future successful acc0
    } else {
      val mgetReq = esClient.prepareMultiGet()
        .setRealtime(true)
      searchHits.foreach { hit =>
        mgetReq.add(hit.getIndex, hit.getType, hit.getId)
      }
      mgetReq
        .execute()
        .map { mgetResp2list(_, acc0) }
    }
  }


  /** Распарсить выхлоп мультигета. */
  def mgetResp2list(mgetResp: MultiGetResponse, acc0: List[T] = Nil): List[T] = {
    mgetResp.getResponses.foldLeft (acc0) { (acc, mgetItem) =>
      // Поиск может содержать элементы, которые были только что удалены. Нужно их отсеивать.
      if (mgetItem.isFailed || !mgetItem.getResponse.isExists) {
        acc
      } else {
        deserializeOne2(mgetItem.getResponse) :: acc
      }
    }
  }


  /** Генератор реквеста для генерации запроса для getAll(). */
  def getAllReq(maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVsn: Boolean = false): SearchRequestBuilder = {
    prepareSearch()
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(maxResults)
      .setFrom(offset)
      .setVersion(withVsn)
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   *
   * @param maxResults Макс. размер выдачи.
   * @param offset Абсолютный сдвиг в выдаче.
   * @param withVsn Возвращать ли версии?
   * @return Список магазинов в порядке их создания.
   */
  def getAll(maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVsn: Boolean = false): Future[Seq[T]] = {
    runSearch(
      getAllReq(
        maxResults = maxResults,
        offset = offset,
        withVsn = withVsn
      )
    )
  }


  def deserializeGetRespFull(getResp: GetResponse): Option[T] = {
    if (getResp.isExists) {
      val result = deserializeOne2(getResp)
      Some(result)
    } else {
      None
    }
  }


  /**
   * Пересохранение всех данных модели. По сути getAll + all.map(_.save). Нужно при ломании схемы.
   *
   * @return
   */
  def resaveMany(maxResults: Int = MAX_RESULTS_DFLT): Future[BulkResponse] = {
    val allFut = getAll(maxResults, withVsn = true)
    val br = esClient.prepareBulk()
    allFut.flatMap { results =>
      for (r <- results) {
        br.add( prepareIndexNoVsn(r) )
      }
      br.execute()
    }
  }


  /** Рефреш всего индекса, в котором живёт эта модель. */
  def refreshIndex(): Future[_] = {
    esClient.admin().indices()
      .prepareRefresh(ES_INDEX_NAME)
      .execute()
  }


  def UPDATE_RETRIES_MAX: Int = EsModelUtil.UPDATE_RETRIES_MAX_DFLT


  /**
   * Запустить пакетное копирование данных модели из одного ES-клиента в другой.
   *
   * @param fromClient Откуда брать данные?
   * @param toClient Куда записывать данные?
   * @param reqSize Размер реквеста. По умолчанию 50.
   * @param keepAliveMs Время жизни scroll-курсора на стороне from-сервера.
   * @return Фьючерс для синхронизации.
   */
  def copyContent(fromClient: Client, toClient: Client, reqSize: Int = 50, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT): Future[CopyContentResult] = {
    prepareScroll( new TimeValue(keepAliveMs), srb = prepareSearch(fromClient))
      .setSize(reqSize)
      .execute()
      .flatMap { searchResp =>
        // для различания сообщений в логах, дополнительно генерим id текущей операции на базе первого скролла.
        val logPrefix = s"copyContent(${searchResp.getScrollId.hashCode / 1000L}): "
        EsModelUtil.foldSearchScroll(searchResp, CopyContentResult(0L, 0L), keepAliveMs = keepAliveMs) {
          (acc0, hits) =>
            LOGGER.trace(s"$logPrefix${hits.getHits.length} hits read from source")
            // Нужно запустить bulk request, который зальёт все хиты в toClient
            val iter = hits.iterator().asScala
            if (iter.nonEmpty) {
              val bulk = toClient.prepareBulk()
              for (hit <- iter) {
                val model = deserializeSearchHit(hit)
                bulk.add( prepareIndexNoVsn(model, toClient) )
              }
              for (bulkResult <- bulk.execute()) yield {
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
        }(ec, fromClient) // implicit'ы передаём вручную, т.к. несколько es-клиентов
      }
  }


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

  /** Реализация контейнера для вызова [[EsModelUtil]].tryUpdate() для es-моделей. */
  class TryUpdateData(override val _saveable: T)
    extends TryUpdateDataAbstract[TryUpdateData]
  {
    override protected def _instance(m: T) = new TryUpdateData(m)
  }

  /** Вместо TryUpdateData.apply(). */
  def tryUpdateData(inst: T) = {
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
  def tryUpdate(inst0: T, retry: Int = 0)(updateF: T => T): Future[T] = {
    // 2015.feb.20: Код переехал в EsModelUtil, а тут остались только wrapper для вызова этого кода.
    val data0 = tryUpdateData(inst0)
    val data2Fut = EsModelUtil.tryUpdate[T, TryUpdateData](this, data0, UPDATE_RETRIES_MAX) { data =>
      val data1 = tryUpdateData(
        updateF(data._saveable)
      )
      Future.successful(data1)
    }
    for (data2 <- data2Fut) yield {
      data2._saveable
    }
  }

  def esTypeName(m: T) = ES_TYPE_NAME
  def esIndexName(m: T) = ES_INDEX_NAME

  def prepareIndexNoVsn(m: T): IndexRequestBuilder = prepareIndexNoVsn(m, esClient)
  def prepareIndexNoVsn(m: T, client: Client): IndexRequestBuilder = {
    val indexName = esIndexName(m)
    val typeName = esTypeName(m)
    val idOrNull = m.idOrNull
    val json = toJson(m)
    //LOGGER.trace(s"prepareIndexNoVsn($indexName/$typeName/$idOrNull): $json")
    client
      .prepareIndex(indexName, typeName, idOrNull)
      .setSource(json, XContentType.JSON)
  }


  def prepareIndex(m: T): IndexRequestBuilder = {
    val irb = prepareIndexNoVsn(m)
    if (m.versionOpt.isDefined)
      irb.setVersion(m.versionOpt.get)
    irb
  }

  /**
   * Сохранить экземпляр в хранилище ES.
   *
   * @return Фьючерс с новым/текущим id
   *         VersionConflictException если транзакция в текущем состоянии невозможна.
   */
  def save(m: T): Future[String] = {
    prepareIndex(m)
      .execute()
      .map { _.getId }
  }

  def toJsonPretty(m: T): String = toJson(m)
  def toJson(m: T): String

  /** Общий код моделей, которые занимаются resave'ом. */
  def resaveBase( getFut: Future[Option[T]] ): Future[Option[String]] = {
    getFut.flatMap { getResOpt =>
      FutureUtil.optFut2futOpt(getResOpt) { e =>
        save(e)
          .map { EmptyUtil.someF }
      }
    }
  }


  /** Отрендерить экземпляр модели в JSON, обёрнутый в некоторое подобие метаданных ES (без _index и без _type). */
  def toEsJsonDoc(e: T): String = {
    import StdFns._

    var kvs = List[String] (s""" "$FIELD_SOURCE": ${toJson(e)}""")
    if (e.versionOpt.isDefined)
      kvs ::= s""" "$FIELD_VERSION": ${e.versionOpt.get}"""
    if (e.id.isDefined)
      kvs ::= s""" "$FIELD_ID": "${e.id.get}" """
    kvs.mkString("{",  ",",  "}")
  }

  /** Отрендерить экземпляры моделей в JSON. */
  def toEsJsonDocs(e: TraversableOnce[T]): String = {
    e.toIterator
      .map { toEsJsonDoc }
      .mkString("[",  ",\n",  "]")
  }


  /** Поточно читаем выхлоп elasticsearch.
    *
    * @param searchQuery Поисковый запрос.
    * @tparam To тип одного элемента.
    * @return Source[T, NotUsed].
    */
  def source[To](searchQuery: QueryBuilder)(implicit helper: IEsSourcingHelper[To]): Source[To, NotUsed] = {
    // Нужно помнить, что SearchDefinition -- это mutable-инстанс и всегда возвращает this.
    val scrollArgs = MScrollArgs(
      query           = searchQuery,
      model           = this,
      sourcingHelper  = helper,
      keepAlive       = SCROLL_KEEPALIVE_DFLT,
      maxResults      = None,
      resultsPerScroll = MAX_RESULTS_DFLT
    )

    // Собираем безлимитный publisher. Явно указываем maxElements для гарантированной защиты от ломания API es4s в будущем.
    val pub = esScrollPublisherFactory.publisher(scrollArgs)

    lazy val logPrefix = s"source()[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Starting using $helper; searchDef = $searchQuery")

    Source
      .fromPublisher( pub )
      .mapConcat { searchHit =>
        // Логгируем любые ошибки, т.к. есть основания подозревать akka-streams в молчаливости.
        // https://github.com/akka/akka/issues/19950
        try {
          helper.mapSearchHit(searchHit) :: Nil
        } catch {
          case ex: Throwable =>
            LOGGER.error(s"$logPrefix Failed to helper.mapSearchHit() for hit#${searchHit.getId} = $searchHit", ex)
            Nil
        }
      }
  }


  /** typeclass для source() для простой десериализации ответов в обычные элементы модели. */
  class ElSourcingHelper extends IEsSourcingHelper[T] {

    override def mapSearchHit(from: SearchHit): T = {
      deserializeSearchHit( from )
    }

    /** Подготовка search definition'а к будущему запросу. */
    override def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
      super.prepareSrb(srb)
        .setFetchSource(true)
    }

    override def toString: String = {
      s"${outer.getClass.getSimpleName}.${super.toString}"
    }
  }


  /** Implicit API модели завёрнуто в этот класс, который можно экстендить. */
  class Implicits {

    /** Mock-адаптер для тестирования сериализации-десериализации моделей на базе play.json.
      * На вход он получает просто экземпляры классов моделей. */
    implicit def mockPlayDocRespEv = new IEsDoc[T] {
      override def id(v: T): Option[String] = {
        v.id
      }
      override def version(v: T): Option[Long] = {
        v.versionOpt
      }
      override def rawVersion(v: T): Long = {
        v.versionOpt.getOrElse(-1)
      }
      override def bodyAsScalaMap(v: T): collection.Map[String, AnyRef] = {
        JacksonWrapper.convert[collection.Map[String, AnyRef]]( toJson(v) )
      }
      override def bodyAsString(v: T): String = {
        toJson(v)
      }
      override def idOrNull(v: T): String = {
        v.idOrNull
      }
    }

    /** stream-сорсинг для обычных случаев. */
    implicit def elSourcingHelper: IEsSourcingHelper[T] = {
      new ElSourcingHelper
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


