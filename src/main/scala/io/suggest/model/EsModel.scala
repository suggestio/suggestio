package io.suggest.model

import org.elasticsearch.cluster.metadata.{MappingMetaData, IndexMetaData}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.search.{SearchHit, SearchHits}
import org.elasticsearch.search.lookup.SourceLookup
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util._
import SioEsUtil._
import org.joda.time.{DateTimeZone, ReadableInstant, DateTime}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchType, SearchResponse}
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.common.xcontent.{ToXContent, XContentFactory, XContentBuilder}
import org.elasticsearch.client.Client
import scala.util.{Failure, Success}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model._
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.action.index.IndexRequestBuilder
import scala.annotation.tailrec
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.action.delete.DeleteRequestBuilder
import io.suggest.ym.model.stat._
import com.sun.org.glassfish.gmbal.{Impact, ManagedOperation}
import play.api.libs.json._
import org.joda.time.format.ISODateTimeFormat
import org.elasticsearch.action.get.MultiGetResponse
import io.suggest.util.SioEsUtil.IndexMapping
import io.suggest.ym.model.UsernamePw
import com.typesafe.scalalogging.slf4j.Logger
import java.{util => ju, lang => jl}
import SioConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 */
object EsModel extends MacroLogsImpl {

  import LOGGER._

  /** Список ES-моделей. Нужен для удобства массовых maintance-операций. Расширяется по мере роста числа ES-моделей. */
  def ES_MODELS: Seq[EsModelMinimalStaticT] = {
    Seq(MShopPriceList, MShopPromoOffer, MYmCategory, MAdStat, MAdnNode, MAd)
  }


  implicit def listCmpOrdering[T <: Comparable[T]] = new ListCmpOrdering[T]

  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelMinimalStaticT] = ES_MODELS, ignoreExists: Boolean = false)(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    Future.traverse(models) { esModelStatic =>
      val logPrefix = esModelStatic.getClass.getSimpleName + ".putMapping(): "
      val imeFut = if (ignoreExists) {
        Future successful false
      } else {
        esModelStatic.isMappingExists
      }
      imeFut flatMap {
        case false =>
          info(logPrefix + "Trying to push mapping for model...")
          val fut = esModelStatic.putMapping(ignoreExists)
          fut onComplete {
            case Success(true)  => debug(logPrefix + "-> OK" )
            case Success(false) => warn(logPrefix  + "NOT ACK!!! Possibly out-of-sync.")
            case Failure(ex)    => error(logPrefix + "FAILed", ex)
          }
          fut

        case true =>
          info(logPrefix + "Mapping already exists in index. Skipping...")
          Future successful true
      }
    } map {
      _.reduceLeft { _ && _ }
    }
  }

  /** Сколько раз по дефолту повторять попытку update при конфликте версий. */
  val UPDATE_RETRIES_MAX_DFLT = MyConfig.CONFIG.getInt("es.model.update.retries.max.dflt") getOrElse 5

  /** Сконвертить флаг reversed-сортировки в параметр для ES типа SortOrder. */
  def isReversed2sortOrder(is: Boolean): SortOrder = {
    if (is) {
      SortOrder.DESC
    } else {
      SortOrder.ASC
    }
  }

  /** Имя индекса, который будет использоваться для хранения данных для большинства остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val DFLT_INDEX        = "-sio"
  /** Имя индекса, куда сваливается всякий частоменяющийся/часторастущий хлам. */
  val GARBAGE_INDEX     = "-siostat"


  // Имена полей в разных хранилищах. НЕЛЬЗЯ менять их значения.
  val MART_ID_ESFN      = "martId"
  val NAME_ESFN         = "name"
  val DESCRIPTION_ESFN  = "description"
  val SHOP_ID_ESFN      = "shopId"
  val AUTH_INFO_ESFN    = "authInfo"
  val URL_ESFN          = "url"
  val PARENT_ID_ESFN    = "parentId"
  val PERSON_ID_ESFN    = "personId"
  val KEY_ESFN          = "key"
  val VALUE_ESFN        = "value"
  val IS_VERIFIED_ESFN  = "isVerified"
  val LOGO_IMG_ID_ESFN  = "logoImgId"
  /** Настройки. Это под-объект, чьё содержимое никогда не анализируется никем. */
  val META_ESFN         = "meta"

  val MAX_RESULTS_DFLT = 100
  val OFFSET_DFLT = 0
  val SCROLL_KEEPALIVE_MS_DFLT = 60000L

  /** Тип аккамулятора, который используется во [[EsModelT]].writeJsonFields(). */
  type FieldsJsonAcc = List[(String, JsValue)]

  def urlParser = stringParser
  def authInfoParser = stringParser andThen {
    case null => None
    case s =>
      val Array(username, pw) = s.split("::", 2)
      Some(UsernamePw(username, pw))
  }

  /** Отрендерить экземпляр модели в JSON, обёрнутый в некоторое подобие метаданных ES (без _index и без _type). */
  def toEsJsonDoc(e: EsModelMinimalT): String = {
     var kvs = List[String] (s""" "$FIELD_SOURCE": ${e.toJson}""")
    if (e.versionOpt.isDefined)
      kvs ::= s""" "$FIELD_VERSION": ${e.versionOpt.get}"""
    if (e.id.isDefined)
      kvs ::= s""" "$FIELD_ID": "${e.id.get}" """
    kvs.mkString("{",  ",",  "}")
  }

  /** Отрендерить экземпляры моделей в JSON. */
  def toEsJsonDocs(e: Traversable[EsModelMinimalT]): String = {
    e.map { toEsJsonDoc }
      .mkString("[",  ",\n",  "]")
  }


  /**
   * Сортировка последовательности экземпляров ES-модели с учетом древовидности через parent-child связи.
   * Это сравнительно ресурсоёмкая функция, поэтому использовать её следует только для административных задач,
   * например отображения полного списка категорий.
   * @param elems Исходные элементы в неопределённом порядке.
   * @tparam T Тип элементов.
   * @return Новая последовательности элементов и их уровней в дереве (level, T) в порядке для отображения дерева.
   */
  def seqTreeSort[T <: TreeSortable](elems: Seq[T]): Seq[(Int, T)] = {
    // Самый простой метод для сортировки: сгенерить List[String]-ключи на основе цепочек id и всех parent_id, затем отсортировать.
    val srcMap = elems.iterator.map { e => e.idOrNull -> e }.toMap
    def collectParentsRev(eOpt: Option[T], acc:List[String]): List[String] = {
      eOpt match {
        case Some(e) =>
          val acc1 = e.name :: acc
          if (e.parentId.isDefined) {
            val parentEOpt = srcMap get e.parentId.get
            collectParentsRev(parentEOpt, acc1)
          } else {
            acc1
          }

        case None => acc
      }
    }
    elems
      .map { e =>
        val eKey = collectParentsRev(Some(e), Nil)
        eKey -> e
      }
      .sortBy(_._1)
      .map { case (k, v) => k.size -> v }
  }

  // ES-выхлопы страдают динамической типизацией, поэтому нужна коллекция парсеров для примитивных типов.
  // Следует помнить, что любое поле может быть списком значений.
  val intParser: PartialFunction[Any, Int] = {
    case null => ???
    case is: jl.Iterable[_] =>
      intParser(is.head.asInstanceOf[AnyRef])
    case i: Integer => i.intValue()
  }
  val longParser: PartialFunction[Any, Long] = {
    case null => ???
    case ls: jl.Iterable[_] =>
      longParser(ls.head.asInstanceOf[AnyRef])
    case l: jl.Number => l.longValue()
  }
  val floatParser: PartialFunction[Any, Float] = {
    case null               => ???
    case fs: jl.Iterable[_] =>
      floatParser(fs.head.asInstanceOf[AnyRef])
    case f: jl.Number => f.floatValue()
  }
  val doubleParser: PartialFunction[Any, Double] = {
    case null               => ???
    case fs: jl.Iterable[_] =>
      doubleParser(fs.head.asInstanceOf[AnyRef])
    case f: jl.Number => f.doubleValue()
  }
  val stringParser: PartialFunction[Any, String] = {
    case null => null
    case strings: jl.Iterable[_] =>
      stringParser(strings.head.asInstanceOf[AnyRef])
    case s: String  => s
  }
  val booleanParser: PartialFunction[Any, Boolean] = {
    case null => ???
    case bs: jl.Iterable[_] =>
      booleanParser(bs.head.asInstanceOf[AnyRef])
    case b: jl.Boolean => b.booleanValue()
  }
  val dateTimeParser: PartialFunction[Any, DateTime] = {
    case null => null
    case dates: jl.Iterable[_] =>
      dateTimeParser(dates.head.asInstanceOf[AnyRef])
    case s: String           => new DateTime(s)
    case d: ju.Date          => new DateTime(d)
    case d: DateTime         => d
    case ri: ReadableInstant => new DateTime(ri)
  }

  val strListParser: PartialFunction[Any, List[String]] = {
    case null => Nil
    case l: jl.Iterable[_] =>
      l.foldLeft (List.empty[String]) {
        (acc,e) => EsModel.stringParser(e) :: acc
      }.reverse
  }
  
  
  def SHARDS_COUNT_DFLT   = MyConfig.CONFIG.getInt("es.model.shards.count.dflt") getOrElse 5
  def REPLICAS_COUNT_DFLT = MyConfig.CONFIG.getInt("es.model.replicas.count.dflt") getOrElse 1


  /**
   * Убедиться, что индекс существует.
   * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
   *         Если индекс уже существует, то false.
   */
  def ensureIndex(indexName: String, shards: Int = 5, replicas: Int = 1)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    val adm = client.admin().indices()
    adm.prepareExists(indexName).execute().flatMap { existsResp =>
      if (existsResp.isExists) {
        Future.successful(false)
      } else {
        val indexSettings = SioEsUtil.getIndexSettingsV2(shards=shards, replicas=replicas)
        adm.prepareCreate(indexName)
          .setSettings(indexSettings)
          .execute()
          .map { _ => true }
      }
    }
  }

  /** Пройтись по всем ES_MODELS и проверить, что всех ихние индексы существуют. */
  def ensureEsModelsIndices(implicit ec:ExecutionContext, client: Client): Future[_] = {
    val indices = ES_MODELS.map { esModel =>
      esModel.ES_INDEX_NAME -> (esModel.SHARDS_COUNT, esModel.REPLICAS_COUNT)
    }.toMap
    Future.traverse(indices) {
      case (inxName, (shards, replicas)) =>
        ensureIndex(inxName, shards=shards, replicas=replicas)
    }
  }

  /** Сконвертить выхлоп getVersion() в Option[Long]. */
  def rawVersion2versionOpt(version: Long): Option[Long] = {
    if (version < 0L)  None  else  Some(version)
  }

  /**
   * Узнать метаданные индекса.
   * @param indexName Название индекса.
   * @return Фьючерс с опциональными метаданными индекса.
   */
  def getIndexMeta(indexName: String)(implicit ec: ExecutionContext, client: Client): Future[Option[IndexMetaData]] = {
    client.admin().cluster()
      .prepareState()
      .setIndices(indexName)
      .execute()
      .map { cs =>
        val maybeResult = cs.getState
          .getMetaData
          .index(indexName)
        Option(maybeResult)
      }
  }

  /**
   * Прочитать метаданные маппинга.
   * @param indexName Название индекса.
   * @param typeName Название типа.
   * @return Фьючерс с опциональными метаданными маппинга.
   */
  def getIndexTypeMeta(indexName: String, typeName: String)(implicit ec: ExecutionContext, client: Client): Future[Option[MappingMetaData]] = {
    getIndexMeta(indexName) map { imdOpt =>
      imdOpt.flatMap { imd =>
        Option(imd.mapping(typeName))
      }
    }
  }

  /**
   * Существует ли указанный маппинг в хранилище? Используется, когда модель хочет проверить наличие маппинга
   * внутри общего индекса.
   * @param typeName Имя типа.
   * @return Да/нет.
   */
  def isMappingExists(indexName: String, typeName: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    getIndexTypeMeta(indexName, typeName = typeName)
      .map { _.isDefined }
  }

  /** Прочитать текст маппинга из хранилища. */
  def getCurrentMapping(indexName: String, typeName: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    getIndexTypeMeta(indexName, typeName = typeName) map {
      _.map { _.source().string() }
    }
  }

  /** Сериализация набора строк. */
  def asJsonStrArray(strings : Iterable[String]): JsArray = {
    val strSeq = strings.foldLeft [List[JsString]] (Nil) {(acc, e) =>
      JsString(e) :: acc
    }
    JsArray(strSeq)
  }


  /**
   * Собрать указанные значения id'шников в аккамулятор-множество.
   * @param searchResp Экземпляр searchResponse.
   * @param acc0 Начальный акк.
   * @param keepAliveMs keepAlive для курсоров на стороне сервера ES в миллисекундах.
   * @return Фьчерс с результирующим аккамулятором-множеством.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html#scrolling]]
   */
  def searchScrollResp2ids(searchResp: SearchResponse, maxAccLen: Int, firstReq: Boolean, currAccLen: Int = 0, acc0: List[String] = Nil, keepAliveMs: Long = 60000L)
                          (implicit ec: ExecutionContext, client: Client): Future[List[String]] = {
    val hits = searchResp.getHits.getHits
    if (!firstReq && hits.length == 0) {
      Future successful acc0
    } else {
      val nextAccLen = currAccLen + hits.length
      val canContinue = maxAccLen <= 0 || nextAccLen < maxAccLen
      val nextScrollRespFut = if (canContinue) {
        // Лимит длины акк-ра ещё не пробит. Запустить в фоне получение следующей порции результатов...
        client.prepareSearchScroll(searchResp.getScrollId)
          .setScroll(new TimeValue(keepAliveMs))
          .execute()
      } else {
        null
      }
      // Если акк заполнен, то надо запустить очистку курсора на стороне ES.
      if (!canContinue) {
        client.prepareClearScroll().addScrollId(searchResp.getScrollId).execute()
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


  /** Рекурсивная асинхронная сверстка скролл-поиска в ES.
    * Перед вызовом функции надо выпонить начальный поисковый запрос, вызвав с setScroll() и,
    * по возможности, включив SCAN.
    * @param searchResp Результат выполненного поиского запроса с активным scroll'ом.
    * @param acc0 Исходное значение аккамулятора.
    * @param firstReq Флаг первого запроса. По умолчанию = true.
    *                 В первом и последнем запросах не приходит никаких результатов, и их нужно различать.
    * @param keepAliveMs Значение keep-alive для курсора на стороне ES.
    * @param f fold-функция, генереящая на основе результатов поиска и старого аккамулятора новый аккамулятор типа A.
    * @tparam A Тип аккамулятора.
    * @return Данные по результатам операции, включающие кол-во удач и ошибок.
    */
  def foldSearchScroll[A](searchResp: SearchResponse, acc0: A, firstReq: Boolean = true, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                         (f: (A, SearchHits) => Future[A])
                         (implicit ec: ExecutionContext, client: Client): Future[A] = {
    if (!firstReq  &&  searchResp.getHits.getHits.length == 0) {
      Future successful acc0
    } else {
      // Запустить в фоне получение следующей порции результатов
      val scrollId = searchResp.getScrollId
      // Убеждаемся, что scroll выставлен. Имеет смысл проверять это только на первом запросе.
      if (firstReq)
        assert(scrollId != null && !scrollId.isEmpty, "Scrolling looks like disabled. Cannot continue.")
      val nextScrollRespFut = client.prepareSearchScroll(scrollId)
        .setScroll(new TimeValue(keepAliveMs))
        .execute()
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val acc1Fut = f(acc0, searchResp.getHits)
      // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
      nextScrollRespFut flatMap { searchResp2 =>
        acc1Fut flatMap { acc1 =>
          foldSearchScroll(searchResp2, acc1, firstReq = false, keepAliveMs)(f)
        }
      }
    }
  }

  // Сериализация дат
  val dateFormatterDflt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)

  implicit def date2str(dateTime: ReadableInstant) = dateFormatterDflt.print(dateTime)
  implicit def date2JsStr(dateTime: ReadableInstant) = JsString(dateTime)
}


/** Самые базовые функции генерации маппингов. */
trait EsModelStaticMappingGenerators {

  def generateMappingStaticFields: List[Field]
  def generateMappingProps: List[DocField]

  def generateMappingFor(typeName: String): XContentBuilder = jsonGenerator { implicit b =>
    // Собираем маппинг индекса.
    IndexMapping(
      typ = typeName,
      staticFields = generateMappingStaticFields,
      properties = generateMappingProps
    )
  }

}


import EsModel._

/** Трейт содержит статические хелперы для работы с маппингами.
  * Однажды был вынесен из [[EsModelStaticT]]. */
trait EsModelStaticMapping extends EsModelStaticMappingGenerators with MacroLogsI {

  def ES_INDEX_NAME = DFLT_INDEX
  def ES_TYPE_NAME: String
  def SHARDS_COUNT = SHARDS_COUNT_DFLT
  def REPLICAS_COUNT = REPLICAS_COUNT_DFLT

  def generateMapping: XContentBuilder = generateMappingFor(ES_TYPE_NAME)

  /** Флаг, который можно перезаписать в реализации static-модели чтобы проигнорить конфликты при апдейте маппинга. */
  protected def mappingIgnoreConflicts: Boolean = false

  /** Отправить маппинг в elasticsearch. */
  def putMapping(ignoreConflicts: Boolean = mappingIgnoreConflicts)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    LOGGER.debug(s"putMapping(): $ES_INDEX_NAME/$ES_TYPE_NAME")
    client.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
      .setIgnoreConflicts(ignoreConflicts)
      .execute()
      .map { _.isAcknowledged }
  }

  /** Удалить маппинг из elasticsearch. */
  def deleteMapping(implicit client: Client): Future[_] = {
    LOGGER.warn(s"deleteMapping(): $ES_INDEX_NAME/$ES_TYPE_NAME")
    client.admin().indices()
      .prepareDeleteMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .execute()
  }

  def ensureIndex(implicit ec:ExecutionContext, client: Client) = {
    EsModel.ensureIndex(ES_INDEX_NAME, shards = SHARDS_COUNT, replicas = REPLICAS_COUNT)
  }
}


/** Базовый шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelMinimalT]].
  * Здесь десериализация полностью выделена в отдельную функцию. */
trait EsModelMinimalStaticT extends EsModelStaticMapping {

  type T <: EsModelMinimalT

  // Кое-какие константы, которые можно переопределить в рамках конкретных моделей.
  def MAX_RESULTS_DFLT = EsModel.MAX_RESULTS_DFLT
  def OFFSET_DFLT = EsModel.OFFSET_DFLT
  def SCROLL_KEEPALIVE_MS_DFLT = EsModel.SCROLL_KEEPALIVE_MS_DFLT
  def SCROLL_KEEPALIVE_DFLT = new TimeValue(SCROLL_KEEPALIVE_MS_DFLT)

  // Короткие враппер для типичных операций в рамках статической модели.
  def prepareSearch(implicit client: Client) = client.prepareSearch(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)
  def prepareCount(implicit client: Client)  = client.prepareCount(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)
  def prepareGet(id: String)(implicit client: Client) = {
    val req = client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }
  def prepareUpdate(id: String)(implicit client: Client) = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, id)
  def prepareDelete(id: String)(implicit client: Client) = client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
  def prepareDeleteByQuery(implicit client: Client) = client.prepareDeleteByQuery(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)
  def prepareScroll(keepAlive: TimeValue = SCROLL_KEEPALIVE_DFLT)(implicit client: Client) = {
    prepareSearch.setSearchType(SearchType.SCAN).setScroll(keepAlive)
  }

  /** Запуск поискового запроса и парсинг результатов в представление этой модели. */
  def runSearch(srb: SearchRequestBuilder)(implicit ec: ExecutionContext): Future[Seq[T]] = {
    srb.execute().map { searchResp2list }
  }

  /** Прочитать маппинг текущей ES-модели из ES. */
  def getCurrentMapping(implicit ec: ExecutionContext, client: Client) = {
    EsModel.getCurrentMapping(ES_INDEX_NAME, typeName = ES_TYPE_NAME)
  }

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id магазина.
   * @return true/false
   */
  def isExist(id: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    prepareGet(id)
      .setFields()
      .execute()
      .map { _.isExists }
  }


  /**
   * Сервисная функция для получения списка всех id.
   * @return Список всех id в неопределённом порядке.
   */
  def getAllIds(maxResults: Int, maxPerStep: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[List[String]] = {
    prepareSearch
      .setSearchType(SearchType.SCAN)
      .setScroll(SCROLL_KEEPALIVE_DFLT)
      .setQuery( QueryBuilders.matchAllQuery() )
      .setSize(maxPerStep)
      .setNoFields()
      .execute()
      .flatMap { searchResp =>
        searchScrollResp2ids(searchResp, firstReq = true, maxAccLen = maxResults, keepAliveMs = SCROLL_KEEPALIVE_MS_DFLT)
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

  /** Если модели требуется выставлять routing для ключа, то можно делать это через эту функцию.
    * @param idOrNull id или null, если id отсутствует.
    * @return None если routing не требуется, иначе Some(String).
    */
  def getRoutingKey(idOrNull: String): Option[String] = None

  /** Пересоздать маппинг удаляется и создаётся заново. */
  def resetMapping(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    deleteMapping flatMap { _ => putMapping() }
  }

  // TODO Нужно проверять, что текущий маппинг не устарел, и обновлять его.
  def isMappingExists(implicit ec:ExecutionContext, client: Client) = {
    EsModel.isMappingExists(indexName=ES_INDEX_NAME, typeName=ES_TYPE_NAME)
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T


  /**
   * Выбрать ряд из таблицы по id.
   * @param id Ключ магазина.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[T]] = {
    prepareGet(id)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          val result = deserializeOne(Option(getResp.getId), getResp.getSourceAsMap, rawVersion2versionOpt(getResp.getVersion))
          Some(result)
        } else {
          None
        }
      }
  }

  /**
   * Выбрать документ из хранилища без парсинга. Вернуть сырое тело документа (его контент).
   * @param id id документа.
   * @return Строка json с содержимым документа или None.
   */
  def getRawContentById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          val result = getResp.getSourceAsString
          Some(result)
        } else {
          None
        }
      }
  }

  /**
   * Прочитать документ как бы всырую.
   * @param id id документа.
   * @return Строка json с документом полностью или None.
   */
  def getRawById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          val xc = getResp.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS)
          val result = xc.string()
          Some(result)
        } else {
          None
        }
      }
  }
  
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
    deserializeOne(Option(hit.getId), hit.getSource, rawVersion2versionOpt(hit.getVersion))
  }

  /** Список результатов в список id. */
  def searchResp2idsList(searchResp: SearchResponse): Seq[String] = {
    searchRespMap(searchResp)(_.getId)
  }

  /**
   * Прочитать из базы все перечисленные id разом.
   * @param ids id документов этой модели. Можно передавать как коллекцию, так и свеженький итератор оной.
   * @return Список результатов в неопределённом порядке.
   */
  def multiGet(ids: TraversableOnce[String], acc0: List[T] = Nil)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    if (ids.isEmpty) {
      Future successful acc0
    } else {
      val req = client.prepareMultiGet()
        .setRealtime(true)
      ids.foreach {
        id =>
          req.add(ES_INDEX_NAME, ES_TYPE_NAME, id)
      }
      req.execute()
        .map { mgetResp2list(_, acc0) }
    }
  }

  /** Для ряда задач бывает необходимо задействовать multiGet вместо обычного поиска, который не успевает за refresh.
    * Этот метод позволяет сконвертить поисковые результаты в результаты multiget.
    * @return Результат - что-то неопределённом порядке. */
  def searchResp2RtMultiget(searchResp: SearchResponse, acc0: List[T] = Nil)(implicit ex: ExecutionContext, client: Client): Future[List[T]] = {
    val searchHits = searchResp.getHits.getHits
    if (searchHits.length == 0) {
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
        val resp = mgetItem.getResponse
        deserializeOne(Option(mgetItem.getId), resp.getSourceAsMap, rawVersion2versionOpt(resp.getVersion)) :: acc
      }
    }
  }


  /** С помощью query найти результаты, но сами результаты прочитать с помощью realtime multi-get. */
  def findQueryRt(query: QueryBuilder, maxResults: Int = 100, acc0: List[T] = Nil)(implicit ec: ExecutionContext, client: Client): Future[List[T]] = {
    prepareSearch
      .setQuery(query)
      .setNoFields()
      .setSize(maxResults)
      .execute()
      .flatMap { searchResp2RtMultiget(_, acc0) }
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @param maxResults Макс. размер выдачи.
   * @param offset Абсолютный сдвиг в выдаче.
   * @param withVsn Возвращать ли версии?
   * @return Список магазинов в порядке их создания.
   */
  def getAll(maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVsn: Boolean = false)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    val req = prepareSearch
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(maxResults)
      .setFrom(offset)
      .setVersion(withVsn)
    runSearch(req)
  }


  /**
   * Генератор delete-реквеста. Используется при bulk-request'ах.
   * @param id adId
   * @return Новый экземпляр DeleteRequestBuilder.
   */
  def deleteRequestBuilder(id: String)(implicit client: Client): DeleteRequestBuilder = {
    val req = prepareDelete(id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    deleteRequestBuilder(id)
      .execute()
      .map { _.isFound }
  }


  /**
   * Пересохранение всех данных модели. По сути getAll + all.map(_.save). Нужно при ломании схемы.
   * @return
   */
  def resaveMany(maxResults: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Seq[String]] = {
    getAll(maxResults, withVsn = true).flatMap { results =>
      Future.traverse(results) { el =>
        tryUpdate(el)(identity)
      }
    }
  }

  def resave(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Option[String]] = {
    getById(id) flatMap {
      case Some(e) =>
        e.save map { Some.apply }
      case None =>
        Future successful None
    }
  }

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
          LOGGER.error(logPrefix + "Failed to make remap. Lost data is:\n" + toEsJsonDocs(results))
        }
        resultFut
      }
    }
  }

  /** Рефреш всего индекса, в котором живёт эта модель. */
  def refreshIndex(implicit client: Client): Future[_] = {
    client.admin().indices()
      .prepareRefresh(ES_INDEX_NAME)
      .execute()
  }


  def UPDATE_RETRIES_MAX: Int = EsModel.UPDATE_RETRIES_MAX_DFLT

  /**
   * Попытаться обновить экземпляр модели с помощью указанной функции.
   * Метод является надстройкой над save, чтобы отрабатывать VersionConflict.
   * @param retry Попытка
   * @param updateF Функция для апдейта.
   * @return Тоже самое, что и save().
   */
  def tryUpdate(inst0: T, retry: Int = 0)(updateF: T => T)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    updateF(inst0)
      .save
      .recoverWith {
        case ex: VersionConflictEngineException =>
          lazy val logPrefix = s"tryUpdate(${inst0.id}, try=$retry): "
          if (retry < UPDATE_RETRIES_MAX) {
            val n1 = retry + 1
            LOGGER.warn(s"${logPrefix}Version conflict while trying to save. Retrying ($n1/$UPDATE_RETRIES_MAX)...")
            getById(inst0.id.get) flatMap {
              case Some(inst) =>
                tryUpdate(inst, n1)(updateF)
              case None =>
                throw new IllegalStateException(s"${logPrefix}Looks like instance has been deleted during update. last try was $retry", ex)
            }
          } else {
            throw new RuntimeException(logPrefix + "Too many save-update retries failed: " + retry, ex)
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
        foldSearchScroll(searchResp, CopyContentResult(0L, 0L), keepAliveMs = keepAliveMs) {
          (acc0, hits) =>
            LOGGER.trace(s"${logPrefix}${hits.getHits.length} hits read from source")
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
}

/**
 * Результаты работы метода Model.copyContent() возвращаются в этом контейнере.
 * @param success Кол-во успешных документов, т.е. для которых выполнена чтене и запись.
 * @param failed Кол-во обломов.
 */
case class CopyContentResult(success: Long, failed: Long)


/** Шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelT]]. */
trait EsModelStaticT extends EsModelMinimalStaticT {

  override type T <: EsModelT

  protected def dummy(id: Option[String], version: Option[Long]): T

  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit]

  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T = {
    val acc = dummy(id, version)
    m foreach applyKeyValue(acc)
    acc.postDeserialize()
    acc
  }

}


/** Добавить поле id: Option[String] */
trait OptStrId {
  @JsonIgnore
  def id: Option[String]
}

trait EraseResources {
  /** Стирание ресурсов, относящихся к этой модели. */
  @JsonIgnore
  def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    Future successful None
  }
}

/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelMinimalT extends OptStrId with EraseResources {

  type T <: EsModelMinimalT

  /** Модели, желающие версионизации, должны перезаписать это поле. */
  @JsonIgnore def versionOpt: Option[Long]

  @JsonIgnore def companion: EsModelMinimalStaticT

  @JsonIgnore protected def esTypeName = companion.ES_TYPE_NAME
  @JsonIgnore protected def esIndexName = companion.ES_INDEX_NAME

  /** Можно делать какие-то действия после десериализации. Например, можно исправлять значения после эволюции схемы. */
  @JsonIgnore def postDeserialize() {}

  @JsonIgnore def toJson: String
  @JsonIgnore def toJsonPretty: String = toJson

  @JsonIgnore def idOrNull = {
    if (id.isDefined)
      id.get
    else
      null
  }

  def prepareUpdate(implicit client: Client) = {
    val req = client.prepareUpdate(esIndexName, esTypeName, id.get)
    if (versionOpt.isDefined)
      req.setVersion(versionOpt.get)
    req
  }

  /** Перед сохранением можно проверять состояние экземпляра. */
  @JsonIgnore
  def isFieldsValid: Boolean = true

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    val irb = client.prepareIndex(esIndexName, esTypeName, idOrNull)
      .setSource(toJson)
    saveBuilder(irb)
    val rkOpt = getRoutingKey
    if (rkOpt.isDefined)
      irb.setRouting(rkOpt.get)
    irb
  }

  /** Узнать routing key для текущего экземпляра. */
  def getRoutingKey = companion.getRoutingKey(idOrNull)

  /** Генератор delete-реквеста. Полезно при построении bulk-реквестов. */
  def deleteRequestBuilder(implicit client: Client) = companion.deleteRequestBuilder(id.get)

  /**
   * Сохранить экземпляр в хранилище ES.
   * @return Фьючерс с новым/текущим id
   *         VersionConflictException если транзакция в текущем состоянии невозможна.
   */
  def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    if (isFieldsValid) {
      val irb = indexRequestBuilder
      if (versionOpt.isDefined)
        irb.setVersion(versionOpt.get)
      irb.execute()
        .map { _.getId }
    } else {
      throw new IllegalStateException("Some or all important fields have invalid values: " + this)
    }
  }

  /** Дополнительные параметры сохранения (parent, ttl, etc) можно выставить через эту функцию. */
  def saveBuilder(irb: IndexRequestBuilder) {}

  /** Удалить текущий ряд из таблицы. Если ключ не выставлен, то сразу будет экзепшен.
    * @return true - всё ок, false - документ не найден.
    */
  def delete(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = id match {
    case Some(_id)  => companion.deleteById(_id)
    case None       => Future failed new IllegalStateException("id is not set")
  }
}


/** Интерфейс с методом сериализации в play.Json экземпляра модели данных. */
trait ToPlayJsonObj {
  def toPlayJsonAcc: FieldsJsonAcc
  /** Сериализовать экземпляр модели данных в промежуточное представление play.Json. */
  def toPlayJson = JsObject(toPlayJsonAcc)
  def toPlayJsonWithId: JsObject
}

/** Шаблон для динамических частей ES-моделей. */
trait EsModelT extends EsModelMinimalT with ToPlayJsonObj {
  override type T <: EsModelT

  override def toPlayJsonAcc = writeJsonFields(Nil)
  override def toPlayJsonWithId: JsObject = {
    var acc = toPlayJsonAcc
    if (id.isDefined)
      acc ::= "id" -> JsString(id.get)
    JsObject(acc)
  }

  override def toJson = toPlayJson.toString()
  override def toJsonPretty: String = Json.prettyPrint(toPlayJson)

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc
}


class ForeignKeyException(msg: String) extends RuntimeException {
  override def getMessage: String = msg
}


trait TreeSortable {
  def name: String
  def idOrNull: String
  def parentId: Option[String]
}


class ListCmpOrdering[T <: Comparable[T]] extends Ordering[List[T]] {
  @tailrec final def compare(x: List[T], y: List[T]): Int = {
    if (x.isEmpty) {
      -1
    } else if (y.isEmpty) {
      1
    } else {
      val headCmpResult = x.head compareTo y.head
      if (headCmpResult == 0) {
        compare(x.tail, y.tail)
      } else {
        headCmpResult
      }
    }
  }
}


// JMX-подсистема для ES-моделей. Включает общие для моделей MBean-интерфейсы и реализации.

import Impact._

trait EsModelJMXMBeanCommon {
  /** Асинхронно вызвать переиндексацию всех данных в модели. */
  @ManagedOperation(impact=ACTION)
  def resaveMany(maxResults: Int): String
  def resave(id: String): String
  
  def remapMany(maxResults: Int): String

  /**
   * Существует ли указанный маппинг сейчас?
   * @return true, если маппинг сейчас существует.
   */
  @ManagedOperation(impact=INFO)
  def isMappingExists: Boolean

  /** Асинхронно сбросить маппинг. */
  @ManagedOperation(impact=ACTION)
  def resetMapping(): String

  /** Асинхронно отправить маппинг в ES. */
  @ManagedOperation(impact=ACTION)
  def putMapping(ignoreConflicts: Boolean): String

  /** Асинхронно удалить маппинг вместе со всеми данными. */
  @ManagedOperation(impact=ACTION)
  def deleteMapping(): String

  def generateMapping(): String
  def readCurrentMapping(): String

  /** Запросить routingKey у модели.
    * @param idOrNull исходный id
    * @return Будет распечатана строка вида "Some(..)" или "None".
    */
  @ManagedOperation(impact=INFO)
  def getRoutingKey(idOrNull: String): String

  /**
   * Выполнить удаление документа.
   * @param id id удаляемого докуметна.
   * @return true, если элемент был удалён. Иначе false.
   */
  @ManagedOperation(impact=ACTION)
  def deleteById(id: String): Boolean

  /**
   * Прочитать из хранилища и вернуть данные по документу.
   * @param id id документа
   * @return pretty JSON или null.
   */
  def getById(id: String): String

  def esIndexName: String
  def esTypeName: String

  /**
   * Выдать сколько-то id'шников в алфавитном порядке.
   * @param maxResults Макс.кол-во выдачи.
   * @return Текст, в каждой строчке новый id.
   */
  def getAllIds(maxResults: Int): String
  def getAll(maxResults: Int): String

  /**
   * Выдать документ "в сырую".
   * @param id id документа.
   * @return Сырая строка json pretty.
   */
  def getRawById(id: String): String

  /**
   * Выдать содержимое документа без парсинга.
   * @param id id документа.
   * @return Сырая строка json pretty.
   */
  def getRawContentById(id: String): String

  /**
   * Отправить в хранилище один экземпляр модели, представленный в виде JSON.
   * @param data Сериализованный в JSON экземпляр модели.
   * @return id сохраненного документа.
   */
  def putOne(id: String, data: String): String

  /** Выхлоп getAll() отправить на сохранение в хранилище.
    * @param all Выхлоп getAll().
    */
  def putAll(all: String): String

  /** Подсчитать кол-во элементов. */
  def countAll(): Long
}


trait EsModelJMXBase extends JMXBase with EsModelJMXMBeanCommon with MacroLogsImplLazy {

  import LOGGER._

  def companion: EsModelMinimalStaticT

  override def jmxName = "io.suggest:type=model,name=" + getClass.getSimpleName.replace("Jmx", "")

  // Контексты, зависимые от конкретного проекта.
  implicit def ec: ExecutionContext
  implicit def client: Client
  implicit def sn: SioNotifierStaticClientI

  override def resaveMany(maxResults: Int): String = {
    warn(s"resaveMany(maxResults = $maxResults)")
    val resavedIds = companion.resaveMany(maxResults).toSeq
    s"Total: ${resavedIds.size}\n---------\n" + resavedIds.mkString("\n")
  }

  override def resave(id: String): String = {
    trace(s"resave($id): jmx")
    (companion.resave(id) : Option[String]) match {
      case Some(_id) => "Resaved " + _id
      case None      => "Not found id: " + id
    }
  }

  override def remapMany(maxResults: Int): String = {
    warn(s"remapMany(maxResults = $maxResults)")
    companion.remapMany(maxResults)
      .map { total => s"Remapped ok $total items." }
      .recover { case ex: Throwable => _formatEx(s"remapMany($maxResults)", "...", ex) }
  }

  override def isMappingExists: Boolean = {
    trace(s"isMappingExists()")
    companion.isMappingExists
  }

  override def resetMapping(): String = {
    warn("resetMapping()")
    companion.resetMapping
      .map { _.toString }
      .recover { case ex: Throwable =>  _formatEx("resetMapping()", "", ex) }
  }

  override def putMapping(ignoreConflicts: Boolean): String = {
    warn(s"putMapping($ignoreConflicts)")
    companion.putMapping(ignoreConflicts = ignoreConflicts)
      .map(_.toString)
      .recover { case ex: Throwable => _formatEx(s"putMapping($ignoreConflicts)", "", ex) }
  }

  override def deleteMapping(): String = {
    warn("deleteMapping()")
    companion.deleteMapping
      .map { _ => "Deleted." }
  }

  override def generateMapping(): String = {
    trace("generateMapping()")
    val mappingText = companion.generateMapping.string()
    JacksonWrapper.prettify(mappingText)
  }

  override def readCurrentMapping(): String = {
    trace("readCurrentMapping()")
    companion.getCurrentMapping.fold("Mapping not found.") { JacksonWrapper.prettify }
  }

  override def getRoutingKey(idOrNull: String): String = {
    trace(s"getRoutingKey($idOrNull)")
    companion.getRoutingKey(idOrNull).toString
  }

  override def deleteById(id: String): Boolean = {
    warn(s"deleteById($id)")
    companion.deleteById(id)
  }

  override def getById(id: String): String = {
    trace(s"getById($id)")
    companion.getById(id)
      .fold("not found")(_.toJsonPretty)
  }

  override def getAllIds(maxResults: Int): String = {
    trace(s"getAllIds(maxResults = $maxResults)")
    companion.getAllIds(maxResults).sorted.mkString("\n")
  }

  override def getAll(maxResults: Int): String = {
    trace(s"getAll(maxResults = $maxResults)")
    val resultNonPretty = toEsJsonDocs( companion.getAll(maxResults, withVsn = true) )
    JacksonWrapper.prettify(resultNonPretty)
  }

  override def getRawById(id: String): String = {
    trace(s"getRawById($id)")
    companion.getRawById(id)
      .map { _.fold("not found")(JacksonWrapper.prettify) }
  }

  override def getRawContentById(id: String): String = {
    trace(s"getRawContentById($id)")
    companion.getRawContentById(id)
      .map { _.fold("not found")(JacksonWrapper.prettify) }
  }

  override def esTypeName: String = companion.ES_TYPE_NAME
  override def esIndexName: String = companion.ES_INDEX_NAME

  override def putOne(id: String, data: String): String = {
    info(s"putOne(id=$id): $data")
    val idOpt = Option(id.trim).filter(!_.isEmpty)
    val b = data.getBytes
    try {
      val dataMap = SourceLookup.sourceAsMap(b, 0, b.length)
      _saveOne(idOpt, dataMap)
    } catch {
      case ex: Throwable =>
        _formatEx(s"putOne($id): ", data, ex)
    }
  }

  override def putAll(all: String): String = {
    info("putAll(): " + all)
    try {
      val raws = JacksonWrapper.deserialize[List[Map[String, AnyRef]]](all)
      val ids = Future.traverse(raws) { tmap =>
        val idOpt = tmap.get(FIELD_ID).map(_.toString.trim)
        val sourceStr = JacksonWrapper.serialize(tmap get FIELD_SOURCE)
        val b = sourceStr.getBytes
        val dataMap = SourceLookup.sourceAsMap(b, 0, b.length)
        _saveOne(idOpt, dataMap)
      }
      (("Total saved: " + ids.size) :: "----" :: ids)
        .mkString("\n")
    } catch {
      case ex: Throwable =>
        _formatEx(s"putAll(${all.size}): ", all, ex)
    }
  }

  override def countAll(): Long = {
    companion.countAll
  }

  /** Ругнутся в логи и вернуть строку для возврата клиенту. */
  private def _formatEx(logPrefix: String, data: String, ex: Throwable): String = {
    error(s"${logPrefix}Failed to make JMX Action:\n$data", ex)
    ex.getClass.getName + ": " + ex.getMessage + "\n\n" + ex.getStackTraceString
  }

  /** Общий код парсинга и добавления элементов в хранилище вынесен сюда. */
  private def _saveOne(idOpt: Option[String], dataMap: ju.Map[String, AnyRef], versionOpt: Option[Long] = None): Future[String] = {
    companion
      .deserializeOne(idOpt, dataMap, version = None)
      .save
  }

}



/**
 * Трейт для статической части модели, построенной через Stackable trait pattern.
 * Для нормального stackable trait без подсветки красным цветом везде, надо чтобы была базовая реализация отдельно
 * от целевой реализации и stackable-реализаций (abstract override).
 * Тут реализованы методы-заглушки для хвоста стэка декораторов. */
trait EsModelStaticEmpty extends EsModelStaticT {
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    PartialFunction.empty
  }

  def generateMappingProps: List[DocField] = Nil
}


/** Дополнение к [[EsModelStaticEmpty]], но в applyKeyValue() не происходит MatchError. Втыкается в последнем with. */
trait EsModelStaticIgnore extends EsModelStaticT {
  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case other => // Do nothing
    }
  }
}


/** Трейт базовой реализации экземпляра модели. Вынесен из неё из-за особенностей stackable trait pattern.
  * Он содержит stackable-методы, реализованные пустышками. */
trait EsModelEmpty extends EsModelT {
  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    acc
  }
}

