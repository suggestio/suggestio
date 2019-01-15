package io.suggest.es.model

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.es.util.SioEsUtil._
import io.suggest.primo.id.OptId
import javax.inject.{Inject, Singleton}
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get.MultiGetRequest.Item
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.Client
import play.api.cache.AsyncCacheApi
import japgolly.univeq._
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.sort.SortBuilders

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:32
 * Description: Файл содержит трейты для базовой сборки типичных ES-моделей, без parent-child и прочего.
 */

@Singleton
final class EsModel @Inject()(
                               esIndexUtil  : EsIndexUtil,
                               cache        : AsyncCacheApi,
                             )(
                               implicit ec  : ExecutionContext,
                               esClient     : Client,
                               mat          : Materializer,
                             ) {

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
        esIndexUtil.getCurrentMapping(
          indexName = model.ES_INDEX_NAME,
          typeName  = model.ES_TYPE_NAME
        )
      }


      // TODO Нужно проверять, что текущий маппинг не устарел, и обновлять его.
      def isMappingExists(): Future[Boolean] = {
        esIndexUtil.isMappingExists(
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
      final def getAllIds(maxResults: Int, maxPerStep: Int = model.MAX_RESULTS_DFLT): Future[List[String]] = {
        model
          .prepareScroll()
          .setQuery( QueryBuilders.matchAllQuery() )
          .setSize(maxPerStep)
          .setFetchSource(false)
          //.setNoFields()
          .executeFut()
          .flatMap { searchResp =>
            esIndexUtil.searchScrollResp2ids(
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
            EsModelUtil.foldSearchScroll(searchResp, CopyContentResult(0L, 0L), keepAliveMs = keepAliveMs) {
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
            }(ec, fromClient) // implicit'ы передаём вручную, т.к. несколько es-клиентов
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
            EsModelUtil.foldSearchScroll(searchResp, acc0 = 0, firstReq = true, keepAliveMs = model.SCROLL_KEEPALIVE_MS_DFLT) {
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

    }

    implicit final class EsModelCommonStaticOps(override val model: EsModelCommonStaticT)
      extends EsModelCommonStaticUntypedOpsT


    /** Типизированный API для EsModelCommonStaticT. */
    trait EsModelCommonStaticTypedOpsT[T1 <: EsModelCommonT] {

      val model: EsModelCommonStaticT { type T = T1 }

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
            EsModelUtil.foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
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
            EsModelUtil.foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
              (acc01, hits) =>
                hits.iterator()
                  .asScala
                  .map { model.deserializeSearchHit }
                  .foldLeft(Future.successful(acc01))( f )
            }
          }
      }

    }

    implicit final class EsModelCommonStaticTypedOps[T1 <: EsModelCommonT]( override val model: EsModelCommonStaticT { type T = T1 } )
      extends EsModelCommonStaticTypedOpsT[T1]


    /** Статические методы для hi-level API: */
    trait EsModelStaticOpsT[T1 <: EsModelT] {

      val model: EsModelStaticT { type T = T1 }

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
        * Пакетно вернуть инстансы модели с указанными id'шниками, но в виде карты (id -> T).
        * Враппер над multiget, но ещё вызывает resultsToMap над результатами.
        *
        * @param ids Коллекция или итератор необходимых id'шников.
        * @param acc0 Необязательный начальный акк. полезен, когда некоторые инстансы уже есть на руках.
        * @return Фьючерс с картой результатов.
        */
      def multiGetMap(ids: TraversableOnce[String], options: GetOpts = model._getArgsDflt): Future[Map[String, T1]] = {
        model
          .multiGet(ids, options = options)
          // Конвертим список результатов в карту, где ключ -- это id. Если id нет, то выкидываем.
          .map { model.resultsToMap }
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
            resp <- model.multiGetRaw(ids, options = options)
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
          .map { model.resultsToMap }
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

  final def prepareUpdate(id: String) =
    prepareUpdateBase(id)
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
   * Прочитать из базы все перечисленные id разом.
   *
   * @param ids id документов этой модели. Можно передавать как коллекцию, так и свеженький итератор оной.
   * @param acc0 Начальный аккамулятор.
   * @return Список результатов в порядке ответа.
   */
  final def multiGet(ids: TraversableOnce[String], options: GetOpts = _getArgsDflt): Future[Stream[T]] = {
    if (ids.isEmpty) {
      Future.successful( Stream.empty )
    } else {
      multiGetRaw(ids, options)
        .map( mgetResp2Stream )
    }
  }
  def multiGetRaw(ids: TraversableOnce[String], options: GetOpts = _getArgsDflt): Future[MultiGetResponse] = {
    val req = esClient.prepareMultiGet()
      .setRealtime(true)
    for (id <- ids) {
      val item = new Item(ES_INDEX_NAME, ES_TYPE_NAME, id)
      for (sf <- options.sourceFiltering)
        item.fetchSourceContext( sf.toFetchSourceCtx )
      req.add(item)
    }
    req.executeFut()
  }



  /** Сконвертить распарсенные результаты в карту. */
  final def resultsToMap(results: TraversableOnce[T]): Map[String, T] = {
    OptId.els2idMap[String, T](results)
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

  /**
   * Удалить документ по id.
   *
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String): Future[Boolean] = {
    deleteRequestBuilder(id)
      .executeFut()
      .map { EsModelStaticT.delResp2isDeleted }
  }

  /** Удаляем сразу много элементов.
    * @return Обычно Some(BulkResponse), но если нет id'шников в запросе, то будет None.
    */
  def deleteByIds(ids: TraversableOnce[String]): Future[Option[BulkResponse]] = {
    if (ids.isEmpty) {
      Future.successful(null)
    } else {
      val bulk = esClient.prepareBulk()
      for (id <- ids) {
        bulk.add(
          prepareDelete(id)
        )
      }
      bulk
        .executeFut()
        .map( EmptyUtil.someF )
    }
  }

  override final def reget(inst0: T): Future[Option[T]] = {
    getById(inst0.id.get)
  }

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  override def prepareIndexNoVsn(m: T): IndexRequestBuilder = {
    val irb = super.prepareIndexNoVsn(m)

    val rkOpt = getRoutingKey(m.idOrNull)
    if (rkOpt.isDefined)
      irb.setRouting(rkOpt.get)

    irb
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
