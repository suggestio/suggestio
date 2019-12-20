package io.suggest.es.model

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.common.empty.{EmptyUtil, OptionUtil}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.scripts.IAggScripts
import io.suggest.es.search.{DynSearchArgs, EsDynSearchStatic}
import io.suggest.es.util.{SioEsUtil, IEsClient}
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.es.MappingDsl
import javax.inject.{Inject, Singleton}
import org.elasticsearch.action.DocWriteResponse.Result
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get.MultiGetRequest.Item
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.client.Client
import play.api.cache.AsyncCacheApi
import japgolly.univeq._
import org.elasticsearch.ResourceNotFoundException
import org.elasticsearch.action.{ActionListener, ActionRequestBuilder, ActionResponse, DocWriteRequest}
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.cluster.metadata.{IndexMetaData, MappingMetaData}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.{SearchHit, SearchHits}
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.scripted.ScriptedMetric
import org.elasticsearch.search.sort.SortBuilders
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
import scala.jdk.CollectionConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:32
 * Description: Файл содержит трейты для базовой сборки типичных ES-моделей, без parent-child и прочего.
 */
@Singleton
final class EsModel @Inject()(
                               esScrollPublisherFactory   : EsScrollPublisherFactory,
                               cache                      : AsyncCacheApi,
                             )(implicit
                               ec           : ExecutionContext,
                               esClientP    : IEsClient,
                               mat          : Materializer,
                             )
  extends MacroLogsImpl
{ esModel =>

  import esClientP.esClient


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

    /** Класс для ActionRequestBuilder'ов, который явно возвращает Future.
      * Для ES >= 6.x, возможно для младших версий.
      */
    implicit class EsActionBuilderOpsExt[AResp <: ActionResponse](val esActionBuilder: ActionRequestBuilder[_, AResp, _]) {

      /** Запуск экшена с возвращением Future[AResp]. */
      def executeFut(): Future[AResp] = {
        val p = Promise[AResp]()
        val startedAtMs = System.currentTimeMillis()

        val l = new ActionListener[AResp] {
          override def onResponse(response: AResp): Unit = {
            val doneAtMs = System.currentTimeMillis()
            p.success(response)
            val tookMs = doneAtMs - startedAtMs
            if (tookMs > SioEsUtil.ES_EXECUTE_WARN_IF_TAKES_TOO_LONG_MS) {
              val logPrefix = s"executeFut()#$startedAtMs:"
              LOGGER.info(s"$logPrefix ES-request took ${tookMs}ms")
              // Бывают ложные срабатывания, например при запуске, когда большая параллельная нагрузка.
              // Чтобы не заваливать логи мусором, логгируем что-то серьёзное только при TRACE.
              LOGGER.trace(s"$logPrefix $esActionBuilder")
            }
          }

          override def onFailure(e: Exception): Unit =
            p.failure(e)
        }
        esActionBuilder.execute(l)

        p.future
      }

    }

    /** Поддержка API для StaticMapping. */
    trait EsModelStaticMappingOpsT {

      val model: EsModelStaticMapping

      /** Отправить маппинг в elasticsearch. */
      def putMapping()(implicit dsl: MappingDsl): Future[Boolean] = {
        lazy val logPrefix = s"$model.putMapping[${System.currentTimeMillis()}]:"
        val indexName = model.ES_INDEX_NAME
        val typeName = model.ES_TYPE_NAME
        LOGGER.trace(s"$logPrefix $indexName/$typeName")

        val fut = esClient.admin().indices()
          .preparePutMapping(indexName)
          .setType(typeName)
          .setSource( model.generateMapping().toString(), XContentType.JSON )
          .executeFut()
          .map( _.isAcknowledged )

        fut.onComplete {
          case Success(res) => LOGGER.trace(s"$logPrefix Done, ack=$res")
          case Failure(ex)  => LOGGER.error(s"$logPrefix Failed", ex)
        }

        fut
      }

      /** Рефреш всего индекса, в котором живёт эта модель. */
      def refreshIndex(): Future[_] = {
        val indexName = model.ES_INDEX_NAME
        LOGGER.trace(s"$model.refreshIndex(): Will refresh $indexName")
        esClient.admin().indices()
          .prepareRefresh(indexName)
          .executeFut()
      }

      def generateMapping()(implicit dsl: MappingDsl): JsObject = {
        Json.obj(
          model.ES_TYPE_NAME -> model.indexMapping
        )
      }

    }
    implicit final class EsModelStaticMappingOps( override val model: EsModelStaticMapping )
      extends EsModelStaticMappingOpsT


    /** Базовый доступ в модель без учёта типа элемента модели.
      * Появилась, т.к. часть кода проекта работает со списками моделей, без определения type-T вообще.
      */
    trait EsModelCommonStaticUntypedOpsT {

      val model: EsModelCommonStaticT

      def prepareSearchViaClient(client: Client): SearchRequestBuilder = {
        client
          .prepareSearch(model.ES_INDEX_NAME)
          .setTypes(model.ES_TYPE_NAME)
      }

      def prepareSearch(): SearchRequestBuilder =
        prepareSearchViaClient(esClient)

      def prepareDeleteBase(id: String) = {
        val req = esClient.prepareDelete(model.ES_INDEX_NAME, model.ES_TYPE_NAME, id)
        val rk = model.getRoutingKey(id)
        if (rk.isDefined)
          req.setRouting(rk.get)
        req
      }

      def prepareCount(): SearchRequestBuilder = {
        prepareSearch()
          .setSize(0)
      }

      def prepareGet(id: String) = {
        val req = esClient.prepareGet(model.ES_INDEX_NAME, model.ES_TYPE_NAME, id)
        val rk = model.getRoutingKey(id)
        if (rk.isDefined)
          req.setRouting(rk.get)
        req
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

      /**
        * Примитив для рассчета кол-ва документов, удовлетворяющих указанному запросу.
        *
        * @param query Произвольный поисковый запрос.
        * @return Кол-во найденных документов.
        */
      def countByQuery(query: QueryBuilder): Future[Long] = {
        prepareCount()
          .setQuery(query)
          .executeFut()
          .map { _.getHits.getTotalHits }
      }

      def prepareScroll(keepAlive: TimeValue = model.SCROLL_KEEPALIVE_DFLT, srb: SearchRequestBuilder = prepareSearch()): SearchRequestBuilder = {
        srb
          .setScroll(keepAlive)
          // Elasticsearch-2.1+: вместо search_type=SCAN желательно юзать сортировку по полю _doc.
          .addSort( SortBuilders.fieldSort( SioEsUtil.StdFns.FIELD_DOC ) )
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
      def countAll(): Future[Long] =
        countByQuery( QueryBuilders.matchAllQuery() )


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
        val bp = bulkProcessor(listener, model.BULK_DELETE_QUEUE_LEN)

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
        * @return Фьючерс с Int'ом внутри.
        */
      def docsHashSum(scripts: IAggScripts, q: QueryBuilder = QueryBuilders.matchAllQuery()): Future[Int] = {
        val aggName = "dcrc"

        for {
          resp <- prepareSearch()
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


      def bulkProcessor(listener: BulkProcessor.Listener, queueLen: Int = 100): BulkProcessor = {
        BulkProcessor.builder(esClient, listener)
          .setBulkActions( queueLen )
          .build()
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


      /** Удалить всё документы из модели. */
      def truncate(areYouSure: Boolean = false): Future[Int] = {
        assert(areYouSure, "Are you sure? No.")
        deleteByQuery( startScroll() )
      }

    }

    implicit final class EsModelCommonStaticUntypedOps(override val model: EsModelCommonStaticT)
      extends EsModelCommonStaticUntypedOpsT


    /** Типизированный API для EsModelCommonStaticT. */
    trait EsModelCommonStaticTypedOpsT[T1 <: EsModelCommonT] {

      val model: EsModelCommonStaticT { type T = T1 }

      def prepareIndex(m: T1): IndexRequestBuilder = {
        val irb = prepareIndexNoVsn(m)
        if (m.versionOpt.isDefined)
          irb.setVersion(m.versionOpt.get)
        irb
      }

      def prepareIndexNoVsn(m: T1): IndexRequestBuilder = {
        val irb = prepareIndexNoVsnUsingClient(m, esClient)
        val rkOpt = model.getRoutingKey(m.idOrNull)
        if (rkOpt.isDefined)
          irb.setRouting(rkOpt.get)
        irb
      }

      def prepareIndexNoVsnUsingClient(m: T1, client: Client): IndexRequestBuilder = {
        val idOrNull = m.idOrNull
        val json = model.toJson(m)
        //LOGGER.trace(s"prepareIndexNoVsn($indexName/$typeName/$idOrNull): $json")
        client
          .prepareIndex(model.ES_INDEX_NAME, model.ES_TYPE_NAME, idOrNull)
          .setSource(json, XContentType.JSON)
      }

      /**
        * Сохранить экземпляр в хранилище ES.
        *
        * @return Фьючерс с новым/текущим id
        *         VersionConflictException если транзакция в текущем состоянии невозможна.
        */
      def save(m: T1): Future[String] =
        save(m, EsSaveOpts.empty)
      def save(m: T1, opts: EsSaveOpts): Future[String] = {
        model._save(m) { () =>
          val reqB = prepareIndex(m)

          for (refreshPolicy <- opts.refreshPolicy)
            reqB.setRefreshPolicy( refreshPolicy )
          for (opType <- opts.opType)
            reqB.setOpType( opType )

          reqB
            .executeFut()
            .map { _.getId }
        }
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
          .map( searchResp2stream )
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

        val bpListener = new BulkProcessorListener(logPrefix)
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
        import SioEsUtil.StdFns._

        var kvs = List[String] (s""" "$FIELD_SOURCE": ${model.toJson(e)}""")
        if (e.versionOpt.isDefined)
          kvs ::= s""" "$FIELD_VERSION": ${e.versionOpt.get}"""
        if (e.id.isDefined)
          kvs ::= s""" "$FIELD_ID": "${e.id.get}" """
        kvs.mkString("{",  ",",  "}")
      }

      /** Отрендерить экземпляры моделей в JSON. */
      def toEsJsonDocs(e: IterableOnce[T1]): String = {
        e.iterator
          .map { toEsJsonDoc }
          .mkString("[",  ",\n",  "]")
      }

      /**
        * Пересохранение всех данных модели. По сути getAll + all.map(_.save). Нужно при ломании схемы.
        *
        * @return
        */
      def resaveAll(): Future[Int] = {
        val I = model.Implicits
        import I._

        val src = source[T1]( QueryBuilders.matchAllQuery() )

        val logPrefix = s"resaveMany()#${System.currentTimeMillis()}:"
        val listener = new BulkProcessorListener(logPrefix)
        val bp = model.bulkProcessor(listener)

        val counter = new AtomicInteger(0)

        val sourceFut = src.runForeach { elT =>
          try {
            bp.add( prepareIndex(elT).request() )
          } catch {
            case ex: Throwable =>
              LOGGER.error(s"$logPrefix Failing to add element#${elT.idOrNull} counter=${counter.get}", ex)
          }
          counter.addAndGet(1)
        }

        sourceFut
          .recover { case ex =>
            LOGGER.error(s"$logPrefix Failure occured during execution, counter=${counter.get()} elements.", ex)
          }
          .map { _ =>
            LOGGER.info(s"$logPrefix finished, total processed ${counter.get()} elements.")
            bp.close()
            counter.get()
          }
      }


      /** Поточно читаем выхлоп elasticsearch.
        *
        * @param searchQuery Поисковый запрос.
        * @tparam To тип одного элемента.
        * @return Source[T, NotUsed].
        */
      def source[To: IEsSourcingHelper](searchQuery: QueryBuilder, maxResults: Option[Long] = None): Source[To, NotUsed] = {
        val helper = implicitly[IEsSourcingHelper[To]]
        // Нужно помнить, что SearchDefinition -- это mutable-инстанс и всегда возвращает this.
        val scrollArgs = MScrollArgs(
          query           = searchQuery,
          model           = model,
          sourcingHelper  = helper,
          keepAlive       = model.SCROLL_KEEPALIVE_DFLT,
          maxResults      = maxResults,
          resultsPerScroll = model.MAX_RESULTS_DFLT
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
                    bulk.add( prepareIndexNoVsnUsingClient(el, toClient) )
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


      /** Лениво распарсить выхлоп multi-GET. */
      final def mgetResp2Stream(mgetResp: MultiGetResponse): LazyList[T1] = {
        mgetResp
          .getResponses
          .iterator
          .flatMap { mgetItem =>
            // Поиск может содержать элементы, которые были только что удалены. Нужно их отсеивать.
            if (mgetItem.isFailed || !mgetItem.getResponse.isExists) {
              Nil
            } else {
              model.deserializeOne2(mgetItem.getResponse) :: Nil
            }
          }
          .to( LazyList )
      }


      /** Внутренний метод для укорачивания кода парсеров ES SearchResponse. */
      def searchRespMap[A](searchResp: SearchResponse)(f: SearchHit => A): Iterator[A] = {
        searchResp
          .getHits
          .iterator()
          .asScala
          .map(f)
      }

      // На ленивость LazyList (Stream) завязана работа akka-stream Source, который имитируется поверх этого метода.
      def searchResp2stream(searchResp: SearchResponse): LazyList[T1] = {
        searchRespMap(searchResp)( model.deserializeSearchHit )
          // Безопасно ли тут делать ленивый Stream? Обычно да, но java-код elasticsearch с mutable внутри может в будущем посчитать иначе.
          .to( LazyList )
      }

    }

    implicit final class EsModelCommonStaticTypedOps[T1 <: EsModelCommonT]( override val model: EsModelCommonStaticT { type T = T1 } )
      extends EsModelCommonStaticTypedOpsT[T1]


    /** Статические методы для hi-level API: */
    trait EsModelStaticOpsT[T1 <: EsModelT] {

      val model: EsModelStaticT { type T = T1 }

      def prepareDelete(id: String) =
        model.prepareDeleteBase(id)

      /**
        * Генератор delete-реквеста. Используется при bulk-request'ах.
        *
        * @param id adId
        * @return Новый экземпляр DeleteRequestBuilder.
        */
      def deleteRequestBuilder(id: String): DeleteRequestBuilder = {
        val req = prepareDelete(id)
        val rk = model.getRoutingKey(id)
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
        val fut = deleteRequestBuilder(id)
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
            val delReq = prepareDelete(id)
            bulk.add( delReq )
          }
          bulk
            .executeFut()
            .map( EmptyUtil.someF )
        }
        model._deleteByIds(ids)(resFut)
      }


      /** Реализация контейнера для вызова [[EsModelUtil]].tryUpdate() для es-моделей. */
      class TryUpdateData(override val _saveable: T1) extends ITryUpdateData[T1, TryUpdateData] {
        override def _instance(m: T1) = new TryUpdateData(m)
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


      /** Вернуть id если он задан. Часто бывает, что idOpt, а не id. */
      def maybeGetById(idOpt: Option[String], options: GetOpts = model._getArgsDflt): Future[Option[T1]] = {
        FutureUtil.optFut2futOpt(idOpt) {
          getById(_, options)
        }
      }


      /**
        * Выбрать ряд из таблицы по id.
        *
        * @param id Ключ документа.
        * @return Экземпляр сабжа, если такой существует.
        */
      def getById(id: String, options: GetOpts = model._getArgsDflt): Future[Option[T1]] = {
        for {
          getResp <- {
            val rq = model.prepareGet(id)
            for (sf <- options.sourceFiltering)
              rq.setFetchSource(sf.includes.toArray, sf.excludes.toArray)
            rq.executeFut()
          }
        } yield {
          OptionUtil.maybe( getResp.isExists ) {
            model.deserializeOne2(getResp)
          }
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
       * @return Список результатов в порядке ответа.
       */
      def multiGet(ids: Iterable[String],
                   options: GetOpts = model._getArgsDflt): Future[LazyList[T1]] = {
        if (ids.isEmpty) {
          Future.successful( LazyList.empty )
        } else {
          multiGetRaw(ids, options)
            .map( model.mgetResp2Stream )
        }
      }
      def multiGetRaw(ids: Iterable[String], options: GetOpts = model._getArgsDflt): Future[MultiGetResponse] = {
        val req = esClient
          .prepareMultiGet()
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
        * @return Фьючерс с картой результатов.
        */
      def multiGetMap(ids: Iterable[String],
                      options: GetOpts = model._getArgsDflt): Future[Map[String, T1]] = {
        multiGet(ids, options = options)
          // Конвертим список результатов в карту, где ключ -- это id. Если id нет, то выкидываем.
          .map( resultsToMap )
      }


      /** Тоже самое, что и multiget, но этап разбора ответа сервера поточный: элементы возвращаются по мере парсинга. */
      def multiGetSrc(ids: Iterable[String],
                      options: GetOpts = model._getArgsDflt): Source[T1, _] = {
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
              Source( items.toSeq )
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
        model.resaveBase( getById(id) )
      }


      /** Обойти с помощью функции, которая выдаёт узлы для следующего шага.
        * Можно применить Пригодно для поиска
        *
        * @param ids id узлов, начиная от которых надо плясать.
        * @param acc0 Начальный аккамулятор.
        * @param f Функция, определяющая новый акк и список узлов, которые тоже надо тоже отработать.
        * @tparam A Тип аккамулятора.
        * @return Итоговый аккамулятор функции.
        */
      def walk[A](acc0: A, ids: Set[String])(f: (A, T1) => (A, Set[String])): Future[A] =
        walkUsing(acc0, ids, multiGetSrc(_))(f)

      def reget(inst0: T1): Future[Option[T1]] =
        getById(inst0.id.get)


      /** Сохранение нового инстанса с проверкой уникальности по указанной функции.
        * Т.к. ES не предоставляет транзакции, то можно эмулировать через некрасивую цепочку:
        * count + save + refresh + count.
        * Конечно, это можно явно обойти через проброс операций в bulkRequest/bulkProcessor,
        * но это уже на совести программиста.
        *
        * @param countQuery Функция подсчёта кол-ва элементов с требованием уникальности.
        * @param newInstance Сохраняемый инстанс, в котором содержаться данные, требующие уникальности.
        * @return Фьючерс с сохранённым id.
        *         Фьючерс с [[EsUniqCondBrokenException]], если требование уникальности было нарушено.
        */
      def saveUniq(countQuery: QueryBuilder)(newInstance: T1): Future[String] = {
        lazy val logPrefix = s"$model.saveUniq()${newInstance.id.fold("")("#" + _)}#${System.currentTimeMillis()}:"

        for {
          // Предварительный рефреш отсутствует для снижения нагрузки.
          // подсчёт текущего кол-ва элементов, удовлетворяющих условию уникальности:
          countExisting <- model.countByQuery( countQuery )
          if countExisting ==* 0L

          // Сохраняем.
          savedId <- model.save( newInstance, EsSaveOpts(
            // Вместо ручного рефреша - дожидаемся общего рефреша по таймеру, чтобы нельзя было искусственно/случайно cпровоцировать refresh-флуд:
            refreshPolicy = Some( RefreshPolicy.WAIT_UNTIL ),
          ))

          // И снова подсчёт текущего кол-ва элементов, удовлетворяющих условию уникальности:
          countExisting2 <- {
            val fut = model.countByQuery( countQuery )
            LOGGER.debug(s"$logPrefix Saved #$savedId . Re-checking uniq.condition...")
            fut
          }

          _ <- {
            if (countExisting2 ==* 1L) {
              Future.successful( None )

            } else {
              val deleteByIdFut = deleteById( savedId )

              if (countExisting2 >= 2L)
                LOGGER.debug(s"$logPrefix Found $countExisting2 existing docs, treated as uniq. Will erase previosly saved doc#$savedId")
              else
                // Функция подсчёта вернула 0 или меньше - это ошибка в подсчёте:
                LOGGER.error(s"$logPrefix uniq-cond is invalid: returned invalid value $countExisting2 after saving #$savedId. Will erase it...")

              deleteByIdFut.transform { tryIsDeleted =>
                val msg = s"$logPrefix #$savedId breaks uniq condition and was deleted?${tryIsDeleted getOrElse false}"
                LOGGER.warn(msg)
                val ex = EsUniqCondBrokenException(
                  model         = model,
                  savedIdOpt    = Some(savedId),
                  isDeletedOpt  = tryIsDeleted.toOption,
                  exOpt         = tryIsDeleted.failed.toOption,
                )
                Failure(ex)
              }
            }
          }

        } yield {
          LOGGER.trace(s"$logPrefix Success. Saved as #$savedId")
          savedId
        }
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
              .to( LazyList )

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
            Source( els.toSeq )
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
      def cacheThese1(results: Iterable[T1]): Unit =
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


    /** Поддержка dynSearch (v1) и search (v2). */
    implicit final class EsDynSearchOps[T1 <: EsModelT, A <: DynSearchArgs]( val model: EsDynSearchStatic[A] { type T = T1 }) {

      // TODO С prepareSearch() пока какой-то говнокод. args.prepareSearchRequest следовало бы вынести за пределы модели DynSearchArgs куда-то сюда.
      /** Сборка билдера поискового запроса. */
      def prepareSearch1(args: A): SearchRequestBuilder =
        prepareSearch2(args, model.prepareSearch())
      def prepareSearch2(args: A, srb0: SearchRequestBuilder): SearchRequestBuilder =
        args.prepareSearchRequest(srb0)

      /** Трейт для сборки DynSearchHelper, возвращающего Future-результат. */
      // abstract class для оптимизации, но можно завернуть назад в трейт.
      abstract class EsSearchFutHelper[R] extends IEsSearchHelper[A, R] {

        /** Подготовка исходного реквеста к поиску. */
        def prepareSearchRequest(args: A): SearchRequestBuilder =
          model.prepareSearch1(args)

        /** Парсинг и обработка сырого результата в некий результат. */
        def mapSearchResp(searchResp: SearchResponse): Future[R]

        override def run(args: A): Future[R] = {
          // Запускаем собранный запрос.
          val srb = prepareSearchRequest(args)

          val fut = srb
            .executeFut()
            .flatMap( mapSearchResp )

          // Логгируем всё вместе с es-индексом и типом, чтобы облегчить curl-отладку на основе залоггированного.
          LOGGER.trace(s"dynSearch2.run($args): Will search on ${model.ES_INDEX_NAME}/${model.ES_TYPE_NAME}\n Compiled request = \n${srb.toString}")

          fut
        }
      }

      /** Если поисковый запрос подразумевает только получение id'шников, то использовать этот трейт. */
      abstract class EsSearchIdsFutHelper[R] extends EsSearchFutHelper[R] {
        override def prepareSearchRequest(args: A): SearchRequestBuilder = {
          super.prepareSearchRequest(args)
            .setFetchSource(false)
            //.setNoFields()
        }
      }


      // Реализации typeclass'ов поиска. Без object, т.к. в этих одноразовых implicit class'ах нет смысла что-то хранить/кэшировать.

      /** search-маппер, просто возвращающий сырой ответ. */
      implicit def RawSearchRespMapper: EsSearchFutHelper[SearchResponse] = {
        new EsSearchFutHelper[SearchResponse] {
          /** Парсинг и обработка сырого результата в некий результат. */
          override def mapSearchResp(searchResp: SearchResponse): Future[SearchResponse] =
            Future.successful( searchResp )
        }
      }

      /** typeclass: возвращает результаты в виде инстансом моделей. */
      implicit def LazyStreamMapper: EsSearchFutHelper[LazyList[T1]] = {
        new EsSearchFutHelper[LazyList[T1]] {
          override def mapSearchResp(searchResp: SearchResponse): Future[LazyList[T1]] = {
            val result = model.searchResp2stream(searchResp)
            Future.successful(result)
          }
        }
      }

      /** typeclass: Маппер ответа в id'шники ответов. */
      implicit def IdsMapper: EsSearchIdsFutHelper[ISearchResp[String]] = {
        new EsSearchIdsFutHelper[ISearchResp[String]] {
          override def mapSearchResp(searchResp: SearchResponse): Future[ISearchResp[String]] = {
            val result = esModel.searchResp2idsList(searchResp)
            Future.successful(result)
          }
        }
      }

      /** typeclass: подсчёт кол-ва результатов без самих результатов. */
      implicit def CountMapper: IEsSearchHelper[A, Long] = {
        new IEsSearchHelper[A, Long] {
          override def run(args: A): Future[Long] =
            model.countByQuery( args.toEsQuery )
        }
      }

      /** typeclass для возврата максимум одного результата и в виде Option'а. */
      implicit def OptionTMapper: EsSearchFutHelper[Option[T1]] = {
        new EsSearchFutHelper[Option[T1]] {
          /** Парсинг и обработка сырого результата в некий результат. */
          override def mapSearchResp(searchResp: SearchResponse): Future[Option[T1]] = {
            val r = model.searchResp2stream(searchResp)
              .headOption
            Future.successful(r)
          }
        }
      }

      /** typeclass маппинга в карту по id. */
      implicit def MapByIdMapper: EsSearchFutHelper[Map[String, T1]] = {
        new EsSearchFutHelper[Map[String, T1]] {
          override def mapSearchResp(searchResp: SearchResponse): Future[Map[String, T1]] = {
            val r = OptId.els2idMap[String, T1] {
              model.searchRespMap(searchResp)( model.deserializeSearchHit )
            }
            Future.successful(r)
          }
        }
      }

      /** Вызываемая вручную сборка multigetter'а для найденных результатов.
        * Это typeclass, передаваемый вручную в dynSearch2() при редкой необходимости такого действия.
        */
      def SeqRtMapper: EsSearchIdsFutHelper[Seq[T1]] = {
        new EsSearchIdsFutHelper[Seq[T1]] {
          /** Для ряда задач бывает необходимо задействовать multiGet вместо обычного поиска, который не успевает за refresh.
            * Этот метод позволяет сконвертить поисковые результаты в результаты multiget.
            *
            * @return Результат - что-то неопределённом порядке.
            */
          override def mapSearchResp(searchResp: SearchResponse): Future[Seq[T1]] = {
            val searchHits = searchResp.getHits.getHits
            if (searchHits.isEmpty) {
              Future successful Nil
            } else {
              val mgetReq = esClient
                .prepareMultiGet()
                .setRealtime(true)
              for (hit <- searchHits)
                mgetReq.add(hit.getIndex, hit.getType, hit.getId)
              mgetReq
                .executeFut()
                .map { model.mgetResp2Stream }
            }
          }
        }
      }


      // DynSearch v2: Для всего (кроме Rt) используется ровно один метод с параметризованным типом результата.

      /** Отрефакторенный вариант dynSearch, где логика, касающаяся возвращаемого типа, вынесена в helper typeclass.
        *
        * @param args Аргументы поиска.
        * @param helper typeclass dyn-search хелпер'а. Занимается финальным допиливание search-реквеста и маппингом результатов.
        * @tparam R Тип результата.
        * @return Фьючерс с результатом типа R.
        */
      def search[R](args: A)(implicit helper: IEsSearchHelper[A, R]): Future[R] =
        helper.run(args)


      // DynSearch v1 API реализован поверх v2 API ( search[T]() ).

      /**
        * Поиск карточек в ТЦ по критериям.
        *
        * @return Список рекламных карточек, подходящих под требования.
        */
      def dynSearch(dsa: A): Future[LazyList[T1]] =
        search[LazyList[T1]] (dsa)

      /** Поиск с возвратом akka-streams.
        * Source эмулируется поверх dynSearch, никакого scroll'а тут нет, т.к. scroll не умеет сортировку, скоринг, лимиты.
        * Просто для удобства сделано или на.
        */
      def dynSearchSource(dsa: A): Source[T1, _] = {
        Source.fromFutureSource {
          dynSearch(dsa)
            .map { Source.apply }
        }
      }

      /**
        * Поиск и сборка карты результатов в id в качестве ключа.
        *
        * @param dsa Поисковые критерии.
        * @return Карта с найденными элементами в неопределённом порядке.
        */
      def dynSearchMap(dsa: A): Future[Map[String, T1]] =
        search [Map[String,T1]] (dsa)

      /**
        * Разновидность dynSearch для максимум одного результата. Вместо коллекции возвращается Option[T].
        *
        * @param dsa Аргументы поиска.
        * @return Фьючерс с Option[T] внутри.
        */
      def dynSearchOne(dsa: A): Future[Option[T1]] =
        search [Option[T1]] (dsa)

      /**
        * Аналог dynSearch, но возвращаются только id документов.
        *
        * @param dsa Поисковый запрос.
        * @return Список id, подходящих под запрос, в неопределённом порядке.
        */
      def dynSearchIds(dsa: A): Future[ISearchResp[String]] =
        search [ISearchResp[String]] (dsa)

      /**
        * Посчитать кол-во рекламных карточек, подходящих под запрос.
        *
        * @param dsa Экземпляр, описывающий критерии поискового запроса.
        * @return Фьючерс с кол-вом совпадений.
        */
      def dynCount(dsa: A): Future[Long] =
        search[Long](dsa)

      /** Поиск id, подходящих под запрос и последующий multiget. Используется для реалтаймого получения
        * изменчивых результатов, например поиск сразу после сохранения. */
      def dynSearchRt(dsa: A): Future[Seq[T1]] =
        search(dsa)( SeqRtMapper )

      /** API забронировано для exists-запроса в будущем. */
      def dynExists(dsa: A): Future[Boolean] =
        dynCount(dsa)
          .map(_ > 0L)

    }

  }
  val api = new Api
  import api._

  /** Сконвертить распарсенные результаты в карту. */
  private def resultsToMap[T <: OptId[String]](results: IterableOnce[T]): Map[String, T] =
    OptId.els2idMap[String, T](results)

  /** Список результатов в список id. */
  def searchResp2idsList(searchResp: SearchResponse): ISearchResp[String] = {
    val hitsArr = searchResp.getHits.getHits
    new AbstractSearchResp[String] {
      override def total: Long =
        searchResp.getHits.getTotalHits
      override def length: Int =
        hitsArr.length
      override def apply(idx: Int): String =
        hitsArr(idx).getId
    }
  }

  /** Собрать новый индекс для заливки туда моделей ipgeobase. */
  def createIndex(newIndexName: String, settings: Settings): Future[_] = {
    val fut = esClient.admin().indices()
      .prepareCreate(newIndexName)
      // Надо сразу отключить index refresh в целях оптимизации bulk-заливки в индекс.
      .setSettings( settings )
      .executeFut()
      .map(_.isShardsAcked)

    lazy val logPrefix = s"createIndex($newIndexName):"
    fut.onComplete {
      case Success(res) => LOGGER.info(s"$logPrefix Ok, $res")
      case Failure(ex)  => LOGGER.error(s"$logPrefix failed", ex)
    }

    fut
  }

  /** Логика удаления старого ненужного индекса. */
  def deleteIndex(oldIndexName: String): Future[_] = {
    val fut: Future[_] = esClient.admin().indices()
      .prepareDelete(oldIndexName)
      .executeFut()
      .map { _.isAcknowledged }

    val logPrefix = s"deleteIndex($oldIndexName):"

    // Отрабатывать ситуацию, когда индекс не найден.
    val fut1 = fut.recover { case ex: ResourceNotFoundException =>
      LOGGER.debug(s"$logPrefix Looks like, index not exist, already deleted?", ex)
      false
    }

    // Логгировать завершение команды.
    fut1.onComplete {
      case Success(res) => LOGGER.debug(s"$logPrefix Index deleted ok: $res")
      case Failure(ex)  => LOGGER.error(s"$logPrefix Failed to delete index", ex)
    }

    fut1
  }

  /**
    * Убедиться, что индекс существует.
    *
    * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
    *         Если индекс уже существует, то false.
    */
  def ensureIndex(indexName: String, shards: Int = 5, replicas: Int = 1)(implicit dsl: MappingDsl): Future[Boolean] = {
    for {
      existsResp <- esClient.admin().indices()
        .prepareExists(indexName)
        .executeFut()

      _ <- if (existsResp.isExists) {
        Future.successful(false)
      } else {
        val indexSettings = SioEsUtil.getIndexSettingsV2(shards=shards, replicas=replicas)
        val settingsJson = Json
          .toJson( indexSettings )
          .toString()
        esClient.admin().indices()
          .prepareCreate( indexName )
          .setSettings( settingsJson, XContentType.JSON )
          .executeFut()
          .map { _ => true }
      }
    } yield {
      true
    }
  }


  /** Выставить алиас на текущий индекс, забыв о предыдущих данных алиаса. */
  def resetAliasToIndex(indexName: String, aliasName: String): Future[_] = {
    lazy val logPrefix = s"resetIndexAliasTo(index[$indexName] alias[$aliasName])[${System.currentTimeMillis()}]:"
    LOGGER.info(s"$logPrefix Starting, alias = $aliasName")

    val fut = esClient.admin().indices()
      .prepareAliases()
      // Удалить все алиасы с необходимым именем.
      .removeAlias("*", aliasName)
      // Добавить алиас на новый индекс.
      .addAlias(indexName, aliasName)
      .executeFut()
      .map( _.isAcknowledged )

    // Подключить логгирование к работе...
    fut.onComplete {
      case Success(r)  => LOGGER.debug(s"$logPrefix OK, ack=$r")
      case Failure(ex) => LOGGER.error(s"$logPrefix Failed to update index alias $aliasName", ex)
    }

    fut
  }


  /** Узнать имя индекса, сокрытого за алиасом. */
  def getAliasedIndexName(aliasName: String): Future[Set[String]] = {
    def logPrefix = "getAliasesIndexName():"
    esClient.admin().indices()
      .prepareGetAliases( aliasName )
      .executeFut()
      .map { resp =>
        LOGGER.trace(s"$logPrefix Ok, found ${resp.getAliases.size()} indexes.")
        resp.getAliases
          .keysIt()
          .asScala
          .toSet
      }
      .recover { case ex: ResourceNotFoundException =>
        // Если алиасов не найдено, ES обычно возвращает ошибку 404. Это тоже отработать надо бы.
        LOGGER.warn(s"$logPrefix 404, suppressing error to empty result.", ex)
        Set.empty
      }
  }


  /** Когда заливка данных закончена, выполнить подготовку индекса к эсплуатации.
    * elasticsearch 2.0+: переименовали операцию optimize в force merge. */
  def optimizeAfterBulk(indexName: String, indexSettingsAfterBulk: Option[Settings] = None): Future[_] = {
    val startedAt = System.currentTimeMillis()

    // Запустить оптимизацию всего ES-индекса.
    val inxOptFut: Future[_] = {
      esClient.admin().indices()
        .prepareForceMerge(indexName)
        .setMaxNumSegments(1)
        .setFlush(true)
        .executeFut()
    }

    lazy val logPrefix = s"optimizeAfterBulk($indexName):"

    // Потом нужно выставить не-bulk настройки для готового к работе индекса.
    val inxSettingsFut = inxOptFut.flatMap { _ =>
      val updInxSettingsAt = System.currentTimeMillis()

      val fut2: Future[_] = indexSettingsAfterBulk.fold[Future[_]] {
        Future.successful(None)
      } { settings2 =>
        esClient.admin().indices()
          .prepareUpdateSettings(indexName)
          .setSettings( settings2 )
          .executeFut()
      }

      LOGGER.trace(s"$logPrefix Optimize took ${updInxSettingsAt - startedAt} ms")
      for (_ <- fut2)
        LOGGER.trace(s"$logPrefix Update index settings took ${System.currentTimeMillis() - updInxSettingsAt} ms.")

      fut2
    }

    inxSettingsFut
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
    for (imdOpt <- getIndexMeta(indexName)) yield {
      for {
        imd <- imdOpt
        mappingOrNull = imd.mapping(typeName)
        r   <- Option( mappingOrNull )
      } yield {
        r
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
      val errModelsStr = (for {
        v <- allModels
          .map { m =>
            esModelId(m) -> m.getClass.getName
          }
          .groupBy(_._1)
          .valuesIterator
        if v.sizeIs > 1
      } yield {
        v.mkString(", ")
      })
        .mkString("\n")

      throw new InternalError("Two or more es models using same index+type for data store:\n" + errModelsStr)
    }
  }


  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelCommonStaticT], ignoreExists: Boolean = false)(implicit dsl: MappingDsl): Future[Boolean] = {
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
              LOGGER.error(s"$logPrefix FAILed to put mapping to ${esModelStatic.ES_INDEX_NAME}/${esModelStatic.ES_TYPE_NAME}:\n-------------\n${esModelStatic.generateMapping().toString()}\n-------------\n", ex)
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
  def ensureEsModelsIndices(models: Seq[EsModelCommonStaticT])(implicit dsl: MappingDsl): Future[_] = {
    val indices = models
      .map { esModel =>
        esModel.ES_INDEX_NAME -> (esModel.SHARDS_COUNT, esModel.REPLICAS_COUNT)
      }
      .toMap
    Future.traverse(indices.toSeq) {
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
  def tryUpdateM[X <: EsModelT, D <: ITryUpdateData[X, D]]
  (companion: EsModelStaticT { type T = X }, data0: D, maxRetries: Int = 5)
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
                  companion
                    .reget( data1._saveable )
                    .flatMap { opt =>
                      val data2  = data1._instance(opt.get)
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


  /** Дефолтовое значение GetArgs, когда GET-опции не указаны. */
  def _getArgsDflt: GetOpts = GetOptsDflt


  /** Дополнение логики удаления одного элемента, когда необходимо. */
  def _deleteById(id: String)(fut: Future[Boolean]): Future[Boolean] =
    fut

  /** Дополнение логики удаления нескольких элементов, когда необходимо. */
  def _deleteByIds(ids: Iterable[String])(fut: Future[Option[BulkResponse]]): Future[Option[BulkResponse]] =
    fut


  def MAX_WALK_STEPS = 50

}

/** Реализация трейта [[EsModelStaticT]] для уменьшения работы компилятору. */
abstract class EsModelStatic extends EsModelStaticT


/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelT extends EsModelCommonT
// TODO Объеденить EsModelT и EsModelCommonT?


/**
  * Самый абстрактный интерфейс для всех хелперов DynSearch.
  * Вынести за пределы модели нельзя, т.к. трейт зависит от [A].
  */
trait IEsSearchHelper[A <: DynSearchArgs, R] {
  def run(args: A): Future[R]
}


/** Модель ошибки нарушения уникальности. */
final case class EsUniqCondBrokenException(
                                            model        : EsModelCommonStaticT,
                                            savedIdOpt   : Option[String],
                                            isDeletedOpt : Option[Boolean],
                                            exOpt        : Option[Throwable],
                                          )
  extends RuntimeException
{

  override def getMessage: String =
    s"$model${savedIdOpt.fold("")("#" + _ + " ")}breaks uniq condition and was deleted?${isDeletedOpt.getOrElseFalse}"

  override def getCause: Throwable =
    exOpt getOrElse super.getCause

}


/** Модель необязательных опций для save()-метода.
  *
  * @param refreshPolicy Политика рефреша шард после сохранения.
  * @param opType Тип операции записи документа.
  */
case class EsSaveOpts(
                       refreshPolicy    : Option[RefreshPolicy]             = None,
                       opType           : Option[DocWriteRequest.OpType]    = None,
                     )
case object EsSaveOpts {
  val empty = apply()
}
