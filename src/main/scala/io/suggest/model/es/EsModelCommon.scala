package io.suggest.model.es

import java.util.concurrent.atomic.AtomicInteger

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.common.OptStrId
import io.suggest.primo.TypeT
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.DeleteRequestBuilder
import org.elasticsearch.action.get.{GetResponse, MultiGetResponse}
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:24
 * Description:
 */

/** Общий код для обычный и child-моделей. Был вынесен из-за разделения в логике работы обычный и child-моделей. */
trait EsModelCommonStaticT extends EsModelStaticMapping with TypeT {

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
    * @param idOrNull id или null, если id отсутствует.
    * @return None если routing не требуется, иначе Some(String).
    */
  def getRoutingKey(idOrNull: String): Option[String] = None

  // Короткие враппер для типичных операций в рамках статической модели.
  def prepareSearch(implicit client: Client) = client.prepareSearch(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)
  def prepareCount(implicit client: Client)  = client.prepareCount(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)

  def prepareTermVectorBase(id: String)(implicit client: Client) = {
    val req = client.prepareTermVector(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  def prepareGetBase(id: String)(implicit client: Client) = {
    val req = client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  def prepareUpdateBase(id: String)(implicit client: Client) = {
    val req = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  def prepareDeleteBase(id: String)(implicit client: Client) = {
    val req = client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  def prepareDeleteByQuery(implicit client: Client) = {
    client.prepareDeleteByQuery(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
  }

  def prepareScroll(keepAlive: TimeValue = SCROLL_KEEPALIVE_DFLT)(implicit client: Client): SearchRequestBuilder = {
    prepareScrollFor(prepareSearch, keepAlive)
  }
  /** Включить скролл для указанного собираемого запроса. */
  def prepareScrollFor(srb: SearchRequestBuilder, keepAlive: TimeValue = SCROLL_KEEPALIVE_DFLT): SearchRequestBuilder = {
    srb
      // Setting search_type to scan disables sorting and makes scrolling very efficient.
      .setSearchType(SearchType.SCAN)
      .setScroll(keepAlive)
  }

  /** Запуск поискового запроса и парсинг результатов в представление этой модели. */
  def runSearch(srb: SearchRequestBuilder)(implicit ec: ExecutionContext): Future[Seq[T]] = {
    srb.execute().map { searchResp2list }
  }

  /** Прочитать маппинг текущей ES-модели из ES. */
  def getCurrentMapping(implicit ec: ExecutionContext, client: Client) = {
    EsModelUtil.getCurrentMapping(ES_INDEX_NAME, typeName = ES_TYPE_NAME)
  }

  /**
   * При удаление инстанса модели бывает нужно стирать связанные ресурсы (связанные модели).
   * Тут общий код логики необязательного стирания ресурсов.
   * @param ignoreResources Флаг запрета каких-либо стираний. Полезно, если всё уже стёрто.
   * @param getF Функция получения фьючерса с возможным инстансом модели.
   * @return Фьючерс для синхронизации.
   */
  def maybeEraseResources(ignoreResources: Boolean, getF: => Future[Option[EsModelCommonT]])
                         (implicit client: Client, ec: ExecutionContext, sn: SioNotifierStaticClientI): Future[_] = {
    if (!ignoreResources && HAS_RESOURCES) {
      getF flatMap {
        case Some(mInts) => mInts.eraseResources
        case None        => Future successful None
      }
    } else {
      Future successful None
    }
  }

  /**
   * Метод для краткого запуска скроллинга над моделью.
   * @param queryOpt Поисковый запрос, по которому скроллим. Если None, то будет matchAll().
   * @param resultsPerScroll Кол-во результатов за каждую итерацию скролла.
   * @param keepAliveMs TTL scroll-курсора на стороне ES.
   * @return Фьючерс, подлежащий дальнейшей обработке.
   */
  def startScroll(queryOpt: Option[QueryBuilder] = None, resultsPerScroll: Int = SCROLL_SIZE_DFLT,
                  keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                 (implicit ec: ExecutionContext, client: Client): Future[SearchResponse] = {
    val query = queryOpt getOrElse QueryBuilders.matchAllQuery()
    val req = prepareScroll(new TimeValue(keepAliveMs))
      .setQuery(query)
      .setSize(resultsPerScroll)
      .setFetchSource(true)
    LOGGER.trace(s"startScroll($queryOpt, rps=$resultsPerScroll, kaMs=$keepAliveMs): query = $query")
    req.execute()
  }

  /**
   * Пройтись асинхронно по всем документам модели.
   * @param acc0 Начальный аккамулятор.
   * @param resultsPerScroll Кол-во результатов на итерацию [10].
   * @param keepAliveMs Таймаут курсора на стороне ES.
   * @param f Асинхронная функция обхода.
   * @tparam A Тип аккамулятора.
   * @return Финальный аккамулятор.
   */
  def foldLeft[A](acc0: A, resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT,
                  queryOpt: Option[QueryBuilder] = None)(f: (A, T) => A)
                 (implicit ec: ExecutionContext, client: Client): Future[A] = {
    startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs, queryOpt = queryOpt)
      .flatMap { searchResp =>
        EsModelUtil.foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
          (acc01, hits) =>
            val acc02 = hits
              .iterator()
              .map { deserializeSearchHit }
              .foldLeft(acc01)(f)
            Future successful acc02
        }
      }
  }

  /**
   * Аналог foldLeft, но с асинхронным аккамулированием. Полезно, если функция совершает какие-то сайд-эффекты.
   * @param acc0 Начальный акк.
   * @param resultsPerScroll Кол-во результатов с каждой шарды за одну scroll-итерацию [10].
   * @param keepAliveMs TTL scroll-курсора на стороне ES.
   * @param f Функция асинхронной сверстки.
   * @tparam A Тип значения аккамулятора (без Future[]).
   * @return Фьючерс с результирующим аккамулятором.
   */
  def foldLeftAsync[A](acc0: A, resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT,
                       queryOpt: Option[QueryBuilder] = None)(f: (Future[A], T) => Future[A])
                      (implicit ec: ExecutionContext, client: Client): Future[A] = {
    startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs, queryOpt = queryOpt)
      .flatMap { searchResp =>
        EsModelUtil.foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
          (acc01, hits) =>
            hits.iterator()
              .map { deserializeSearchHit }
              .foldLeft(Future successful acc01)(f)
        }
      }
  }


  // !!! map() с сохранением реализован в методе updateAll() !!!

  /**
   * foreach для асинхронного обхода всех документов модели.
   * @param resultsPerScroll По сколько документов скроллить?
   * @param keepAliveMs Время жизни scroll-курсора на стороне es.
   * @param f Функция обработки одного результата.
   * @return Future синхронизации завершения обхода или ошибки.
   *         Цифра внутри содержит кол-во пройденных результатов.
   */
  def foreach[U](resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                (f: T => U)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    // Оборачиваем foldLeft(), просто фиксируя аккамулятор.
    foldLeft(0L, resultsPerScroll, keepAliveMs) {
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
   * @param resultsPerScroll Кол-во результатов на каждой итерации. [10]
   * @param keepAliveMs Таймаут scroll-курсора на стороне ES.
   * @param bulkActions Макс.кол-во запросов в очереди на bulk-индексацию. После пробоя этого значения,
   *                    вся очередь реквестов будет отправлена на индексацию.
   * @param f Функция-маппер, которая порождает фьючерс с новым обновлённым экземпляром модели.
   * @return Фьючес с кол-вом обработанных экземпляров модели.
   */
  def updateAll(resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT,
                bulkActions: Int = BULK_PROCESSOR_BULK_SIZE_DFLT, queryOpt: Option[QueryBuilder] = None)
               (f: T => Future[T])(implicit ec: ExecutionContext, client: Client): Future[Int] = {

    val logPrefix = s"update(${System.currentTimeMillis}):"

    val listener = new BulkProcessor.Listener {
      /** Перед отправкой каждого bulk-реквеста. */
      override def beforeBulk(executionId: Long, request: BulkRequest): Unit = {
        LOGGER.trace(s"$logPrefix Going to execute bulk req with ${request.numberOfActions()} actions.")
      }

      /** Данные успешно отправлены в индекс. */
      override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
        LOGGER.trace(s"$logPrefix ")
      }

      /** Ошибка индексации. */
      override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
        LOGGER.error(s"$logPrefix Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
      }
    }

    val bp = BulkProcessor.builder(client, listener)
      .setName(logPrefix)
      .setBulkActions(100)
      .build()

    // Создаём атомный счетчик, который будет инкрементится из разных потоков одновременно.
    // Можно счетчик гнать через аккамулятор, но это будет порождать много бессмысленного мусора.
    val counter = new AtomicInteger(0)

    // Выполнить обход модели. Аккамулятор фиксирован (не используется).
    val foldFut = foldLeftAsync(None, resultsPerScroll, keepAliveMs, queryOpt) {
      (accFut, v) =>
        f(v).flatMap {
          case null =>
            LOGGER.trace(s"$logPrefix Skipped update of [${v.idOrNull}], f() returned null")
            accFut
          case v1 =>
            bp.add( v1.prepareIndex.request )
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
   * @return Список всех id в неопределённом порядке.
   */
  def getAllIds(maxResults: Int, maxPerStep: Int = MAX_RESULTS_DFLT)
               (implicit ec: ExecutionContext, client: Client): Future[List[String]] = {
    prepareSearch
      .setSearchType(SearchType.SCAN)
      .setScroll(SCROLL_KEEPALIVE_DFLT)
      .setQuery( QueryBuilders.matchAllQuery() )
      .setSize(maxPerStep)
      .setFetchSource(false)
      .setNoFields()
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
   * @param query Произвольный поисковый запрос.
   * @return Кол-во найденных документов.
   */
  def countByQuery(query: QueryBuilder)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    prepareCount
      .setQuery(query)
      .execute()
      .map { _.getCount }
  }

  /**
   * Посчитать кол-во документов в текущей модели.
   * @return Неотрицательное целое.
   */
  def countAll(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    countByQuery(QueryBuilders.matchAllQuery())
  }

  /** Пересоздать маппинг удаляется и создаётся заново. */
  def resetMapping(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    deleteMapping flatMap { _ => putMapping() }
  }

  // TODO Нужно проверять, что текущий маппинг не устарел, и обновлять его.
  def isMappingExists(implicit ec:ExecutionContext, client: Client) = {
    EsModelUtil.isMappingExists(indexName=ES_INDEX_NAME, typeName=ES_TYPE_NAME)
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T

  /** Десериализация по новому API: документ передается напрямую, а данные извлекаются через статический typeclass.
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
  def searchRespMap[A](searchResp: SearchResponse)(f: SearchHit => A): Seq[A] = {
    searchResp.getHits
      .iterator()
      .map(f)
      .toSeq
  }

  /** Список результатов с source внутри перегнать в распарсенный список. */
  def searchResp2list(searchResp: SearchResponse): Seq[T] = {
    searchRespMap(searchResp)(deserializeSearchHit)
  }

  def deserializeSearchHit(hit: SearchHit): T = {
    deserializeOne2(hit)
  }

  /** Список результатов в список id. */
  def searchResp2idsList(searchResp: SearchResponse): Seq[String] = {
    searchRespMap(searchResp)(_.getId)
  }

  def searchResp2fnList[T](searchResp: SearchResponse, fn: String): Seq[T] = {
    searchRespMap(searchResp) { hit =>
      hit.field(fn).getValue[T]
    }
  }


  /** Для ряда задач бывает необходимо задействовать multiGet вместо обычного поиска, который не успевает за refresh.
    * Этот метод позволяет сконвертить поисковые результаты в результаты multiget.
    * @return Результат - что-то неопределённом порядке. */
  def searchResp2RtMultiget(searchResp: SearchResponse, acc0: List[T] = Nil)
                           (implicit ex: ExecutionContext, client: Client): Future[List[T]] = {
    val searchHits = searchResp.getHits.getHits
    if (searchHits.isEmpty) {
      Future successful acc0
    } else {
      val mgetReq = client.prepareMultiGet()
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


  /** С помощью query найти результаты, но сами результаты прочитать с помощью realtime multi-get. */
  def findQueryRt(query: QueryBuilder, maxResults: Int = 100, acc0: List[T] = Nil)
                 (implicit ec: ExecutionContext, client: Client): Future[List[T]] = {
    prepareSearch
      .setQuery(query)
      .setFetchSource(false)
      .setNoFields()
      .setSize(maxResults)
      .execute()
      .flatMap { searchResp2RtMultiget(_, acc0) }
  }

  /** Генератор реквеста для генерации запроса для getAll(). */
  def getAllReq(maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVsn: Boolean = false)
               (implicit client: Client): SearchRequestBuilder = {
    prepareSearch
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(maxResults)
      .setFrom(offset)
      .setVersion(withVsn)
  }

  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @param maxResults Макс. размер выдачи.
   * @param offset Абсолютный сдвиг в выдаче.
   * @param withVsn Возвращать ли версии?
   * @return Список магазинов в порядке их создания.
   */
  def getAll(maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVsn: Boolean = false)
            (implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
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
   * @return
   */
  def resaveMany(maxResults: Int = MAX_RESULTS_DFLT)
                (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[BulkResponse] = {
    val allFut = getAll(maxResults, withVsn = true)
    val br = client.prepareBulk()
    allFut.flatMap { results =>
      for (r <- results) {
        br.add( r.indexRequestBuilder )
      }
      br.execute()
    }
  }


  /** Рефреш всего индекса, в котором живёт эта модель. */
  def refreshIndex(implicit client: Client): Future[_] = {
    client.admin().indices()
      .prepareRefresh(ES_INDEX_NAME)
      .execute()
  }


  def UPDATE_RETRIES_MAX: Int = EsModelUtil.UPDATE_RETRIES_MAX_DFLT


  /**
   * Прочитать в RAM n документов, пересоздать маппинг, отправить документы назад в индекс.
   * Крайне опасно дергать эту функцию в продакшене, т.к. она скорее всего приведёт к потере данных.
   * Функция не экономит память и сильно грузит кластер при сохранении, т.к. не использует bulk request.
   * @param maxResults Макс. число результатов для прочтения из хранилища модели.
   * @return
   */
  def remapMany(maxResults: Int = -1)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Int] = {
    val logPrefix = s"remapMany($maxResults): "
    LOGGER.warn(logPrefix + "Starting model data remapping...")
    // TODO Надо бы сохранять данные маппинга в файл, считывая их через SCAN и курсоры.
    val startedAt = System.currentTimeMillis()
    val atMostFut: Future[Int] = if (maxResults <= 0) {
      countAll.map(_.toInt + 10)
    } else {
      Future successful maxResults
    }
    // [withVsn = false] из-за проблем с версионизацией на стёртых маппингах VersionConflictEngineException version conflict, current [-1], provided [3]
    atMostFut flatMap { atMost =>
      getAll(atMost, withVsn = false) flatMap { results =>
        val resultFut = for {
          _ <- deleteMapping
          _ <- putMapping(ignoreConflicts = false)
          _ <- Future.traverse(results) { e => tryUpdate(e)(identity) }
          _ <- refreshIndex
        } yield {
          LOGGER.info(s"${logPrefix}Model's data remapping finished after ${System.currentTimeMillis - startedAt} ms.")
          results.size
        }
        resultFut onFailure { case ex =>
          LOGGER.error(logPrefix + "Failed to make remap. Lost data is:\n" + EsModelUtil.toEsJsonDocs(results))
        }
        resultFut
      }
    }
  }


  /**
   * Запустить пакетное копирование данных модели из одного ES-клиента в другой.
   * @param fromClient Откуда брать данные?
   * @param toClient Куда записывать данные?
   * @param reqSize Размер реквеста. По умолчанию 50.
   * @param keepAliveMs Время жизни scroll-курсора на стороне from-сервера.
   * @return Фьючерс для синхронизации.
   */
  def copyContent(fromClient: Client, toClient: Client, reqSize: Int = 50, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                 (implicit ec: ExecutionContext): Future[CopyContentResult] = {
    prepareScroll( new TimeValue(keepAliveMs) )(fromClient)
      .setSize(reqSize)
      .execute()
      .flatMap { searchResp =>
        // для различания сообщений в логах, дополнительно генерим id текущей операции на базе первого скролла.
        val logPrefix = s"copyContent(${searchResp.getScrollId.hashCode / 1000L}): "
        EsModelUtil.foldSearchScroll(searchResp, CopyContentResult(0L, 0L), keepAliveMs = keepAliveMs) {
          (acc0, hits) =>
            LOGGER.trace(s"$logPrefix${hits.getHits.length} hits read from source")
            // Нужно запустить bulk request, который зальёт все хиты в toClient
            val iter = hits.iterator().toIterator
            if (iter.nonEmpty) {
              val bulk = toClient.prepareBulk()
              iter.foreach { hit =>
                val model = deserializeSearchHit(hit)
                bulk.add( model.indexRequestBuilder(toClient) )
              }
              bulk.execute().map { bulkResult =>
                if (bulkResult.hasFailures)
                  LOGGER.error("copyContent(): Failed to write bulk into target:\n " + bulkResult.buildFailureMessage())
                val failedCount = bulkResult.iterator().count(_.isFailed)
                val acc1 = acc0.copy(
                  success = acc0.success + bulkResult.getItems.length - failedCount,
                  failed  = acc0.failed + failedCount
                )
                LOGGER.trace(s"${logPrefix}bulk write finished. acc.success = ${acc1.success} acc.failed = ${acc1.failed}")
                acc1
              }
            } else {
              Future successful acc0
            }
        }(ec, fromClient) // implicit'ы передаём вручную, т.к. несколько es-клиентов
      }
  }


  /**
   * Перечитывание из хранилища указанного документа, используя реквизиты текущего документа.
   * Нужно для parent-child случаев, когда одного _id уже мало.
   * @param inst0 Исходный (устаревший) инстанс.
   * @return тоже самое, что и getById()
   */
  def reget(inst0: T)(implicit ec: ExecutionContext, client: Client): Future[Option[T]]

  /**
   * Попытаться обновить экземпляр модели с помощью указанной функции.
   * Метод является надстройкой над save, чтобы отрабатывать VersionConflict.
   * @param retry Счетчик попыток.
   * @param updateF Функция для апдейта. Может возвращать null для внезапного отказа от апдейта.
   * @return Тоже самое, что и save().
   *         Если updateF запретила апдейт (вернула null), то будет Future.successfull(null).
   */
  def tryUpdate(inst0: T, retry: Int = 0)(updateF: T => T)
               (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    lazy val logPrefix = s"tryUpdate(${Option(inst0).flatMap(_.id).orNull}, $retry):"

    val inst1 = updateF(inst0)

    if (inst1 == null) {
      LOGGER.debug(logPrefix + " updateF() returned `null`, leaving update of inst")
      Future.successful(null)

    } else {
      inst1
        .save
        .recoverWith {
          case ex: VersionConflictEngineException =>
            if (retry < UPDATE_RETRIES_MAX) {
              val n1 = retry + 1
              LOGGER.warn(s"$logPrefix Version conflict while tryUpdate(). Retry ($n1/$UPDATE_RETRIES_MAX)...")
              reget(inst0).flatMap {
                case Some(inst) =>
                  tryUpdate(inst, n1)(updateF)
                case None =>
                  throw new IllegalStateException(s"$logPrefix Looks like instance has been deleted during update. last try was $retry", ex)
              }
            } else {
              throw new RuntimeException(s"$logPrefix Too many save-update retries failed: $retry", ex)
            }
        }
    }
  }

}


/** Общий код динамических частей модели, независимо от child-модели или обычной. */
trait EsModelCommonT extends OptStrId with EraseResources with TypeT {

  /** Тип T это -- this.type конечной реализации, но связать его с this.type компилятор не позволяет. */
  override type T <: EsModelCommonT

  /**
   * Тип T1 -- это алиас для типа T.
   * Бывает нужно произвести сравнение типа с другим типом T в другом классе.
   * {{{
   *   def x : SomeClass { type T = T1 }
   * }}}
   */
  protected[this] type T1 = T

  /** Доступ к this как к реализации типа T. По задумке типа T, это должно быть безопасно. */
  def thisT: T = this.asInstanceOf[T]

  /** Модели, желающие версионизации, должны перезаписать это поле. */
  def versionOpt: Option[Long]

  def companion: EsModelCommonStaticT

  def esTypeName = companion.ES_TYPE_NAME
  def esIndexName = companion.ES_INDEX_NAME

  def toJson: String
  def toJsonPretty: String = toJson

  @JsonIgnore
  def idOrNull: String = id.orNull

  /** Перед сохранением можно проверять состояние экземпляра. */
  @JsonIgnore
  def isFieldsValid: Boolean = true

  def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    client.prepareIndex(esIndexName, esTypeName, idOrNull)
      .setSource(toJson)
  }

  def prepareIndex(implicit client: Client): IndexRequestBuilder = {
    val irb = indexRequestBuilder
    if (versionOpt.isDefined)
      irb.setVersion(versionOpt.get)
    irb
  }

  /**
   * Сохранить экземпляр в хранилище ES.
   * @return Фьючерс с новым/текущим id
   *         VersionConflictException если транзакция в текущем состоянии невозможна.
   */
  def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    if (isFieldsValid) {
      prepareIndex
        .execute()
        .map { _.getId }
    } else {
      throw new IllegalStateException("Some or all important fields have invalid values: " + this)
    }
  }

  def companionDelete(_id: String, ignoreResources: Boolean)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean]

  /**
   * Удалить текущий ряд из таблицы. Если ключ не выставлен, то сразу будет экзепшен.
   * @return true - всё ок, false - документ не найден.
   */
  def delete(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    eraseResources flatMap { _ =>
      id match {
        case Some(_id)  => companionDelete(_id, ignoreResources = true)
        case None       => Future failed new IllegalStateException("id is not set")
      }
    }
  }

  def prepareDelete(implicit client: Client): DeleteRequestBuilder

  def prepareUpdate(implicit client: Client) = {
    val req = client.prepareUpdate(esIndexName, esTypeName, id.get)
    if (versionOpt.isDefined)
      req.setVersion(versionOpt.get)
    req
  }

}


