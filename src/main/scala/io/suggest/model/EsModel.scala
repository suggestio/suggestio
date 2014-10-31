package io.suggest.model

import java.util.concurrent.atomic.AtomicInteger

import org.elasticsearch.action.bulk.{BulkResponse, BulkRequest, BulkProcessor}
import org.elasticsearch.action.update.UpdateRequestBuilder
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
import org.elasticsearch.action.get.{GetResponse, MultiGetResponse}
import io.suggest.util.SioEsUtil.IndexMapping
import io.suggest.ym.model.UsernamePw
import java.{util => ju, lang => jl}
import SioConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 * 2014.sep.04: Появились child-модели. Произошло разделение api трейтов: статических и немного динамических.
 */
object EsModel extends MacroLogsImpl {

  import LOGGER._

  /** Список ES-моделей. Нужен для удобства массовых maintance-операций. Расширяется по мере роста числа ES-моделей. */
  def ES_MODELS: Seq[EsModelCommonStaticT] = {
    Seq(MCompany, MWelcomeAd, MShopPriceList, MShopPromoOffer, MYmCategory, MAdStat, MAdnNode, MAd, MAdnNodeGeo)
  }


  implicit def listCmpOrdering[T <: Comparable[T]] = new ListCmpOrdering[T]

  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelCommonStaticT] = ES_MODELS, ignoreExists: Boolean = false)(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
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

  /** Дефолтовый размер скролла, т.е. макс. кол-во получаемых за раз документов. */
  val SCROLL_SIZE_DFLT = 10

  /** number of actions, после которого bulk processor делает flush. */
  val BULK_PROCESSOR_BULK_SIZE_DFLT = 100

  /** Тип аккамулятора, который используется во [[EsModelPlayJsonT]].writeJsonFields(). */
  type FieldsJsonAcc = List[(String, JsValue)]

  def urlParser = stringParser
  def authInfoParser = stringParser andThen {
    case null => None
    case s =>
      val Array(username, pw) = s.split("::", 2)
      Some(UsernamePw(username, pw))
  }

  /** Отрендерить экземпляр модели в JSON, обёрнутый в некоторое подобие метаданных ES (без _index и без _type). */
  def toEsJsonDoc(e: EsModelCommonT): String = {
     var kvs = List[String] (s""" "$FIELD_SOURCE": ${e.toJson}""")
    if (e.versionOpt.isDefined)
      kvs ::= s""" "$FIELD_VERSION": ${e.versionOpt.get}"""
    if (e.id.isDefined)
      kvs ::= s""" "$FIELD_ID": "${e.id.get}" """
    kvs.mkString("{",  ",",  "}")
  }

  /** Отрендерить экземпляры моделей в JSON. */
  def toEsJsonDocs(e: Traversable[EsModelCommonT]): String = {
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

  implicit def date2str(dateTime: ReadableInstant): String = dateFormatterDflt.print(dateTime)
  implicit def date2JsStr(dateTime: ReadableInstant): JsString = JsString(dateTime)


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


  /** Общий код моделей, которые занимаются resave'ом. */
  def resaveBase(getFut: Future[Option[EsModelCommonT]])(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Option[String]] = {
    getFut flatMap {
      case Some(e) =>
        e.save map { Some.apply }
      case None =>
        Future successful None
    }
  }

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
  * Однажды был вынесен из [[EsModelStaticMutAkvT]]. */
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


/** Общий код для обычный и child-моделей. Был вынесен из-за разделения в логике работы обычный и child-моделей. */
trait EsModelCommonStaticT extends EsModelStaticMapping {

  type T <: EsModelCommonT

  // Кое-какие константы, которые можно переопределить в рамках конкретных моделей.
  def MAX_RESULTS_DFLT = EsModel.MAX_RESULTS_DFLT
  def OFFSET_DFLT = EsModel.OFFSET_DFLT
  def SCROLL_KEEPALIVE_MS_DFLT = EsModel.SCROLL_KEEPALIVE_MS_DFLT
  def SCROLL_KEEPALIVE_DFLT = new TimeValue(SCROLL_KEEPALIVE_MS_DFLT)
  def SCROLL_SIZE_DFLT = EsModel.SCROLL_SIZE_DFLT
  def BULK_PROCESSOR_BULK_SIZE_DFLT = EsModel.BULK_PROCESSOR_BULK_SIZE_DFLT

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
   * Метод для краткого запуска скроллинга над моделью.
   * @param query Поисковый запрос, по которому скроллим.
   * @param resultsPerScroll Кол-во результатов за каждую итерацию скролла.
   * @param keepAliveMs TTL scroll-курсора на стороне ES.
   * @return Фьючерс, подлежащий дальнейшей обработке.
   */
  def startScroll(query: QueryBuilder = QueryBuilders.matchAllQuery(), resultsPerScroll: Int = SCROLL_SIZE_DFLT,
                  keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[SearchResponse] = {
    prepareScroll(new TimeValue(keepAliveMs))
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(10)
      .setFetchSource(true)
      .execute()
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
  def foldLeft[A](acc0: A, resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                 (f: (A, T) => A)(implicit ec: ExecutionContext, client: Client): Future[A] = {
    startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs)
      .flatMap { searchResp =>
        foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
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
   * @param resultsPerScroll Кол-во результатов за scroll-итерацию [10].
   * @param keepAliveMs TTL scroll-курсора на стороне ES.
   * @param f Функция асинхронной сверстки.
   * @tparam A Тип значения аккамулятора (без Future[]).
   * @return Фьючерс с результирующим аккамулятором.
   */
  def foldLeftAsync[A](acc0: A, resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                      (f: (Future[A], T) => Future[A])(implicit ec: ExecutionContext, client: Client): Future[A] = {
    startScroll(resultsPerScroll = resultsPerScroll, keepAliveMs = keepAliveMs)
      .flatMap { searchResp =>
        foldSearchScroll(searchResp, acc0, firstReq = true, keepAliveMs) {
          (acc01, hits) =>
            LOGGER.trace(s"foldLeftAsync(): Starting next scroll with ${hits.getHits.length} new hits...")
            hits
              .iterator()
              .map { deserializeSearchHit }
              .foldLeft(Future successful acc01)(f)
        }
      }
  }


  /**
   * foreach для асинхронного обхода всех документов модели.
   * @param resultsPerScroll По сколько документов скроллить?
   * @param keepAliveMs Время жизни scroll-курсора на стороне es.
   * @param f Функция обработки одного результата.
   * @return
   */
  def foreach[U](resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                (f: T => U)(implicit ec: ExecutionContext, client: Client): Future[_] = {
    // Оборачиваем foldLeft(), просто фиксируя аккамулятор.
    val acc0 = None
    foldLeft(acc0, resultsPerScroll, keepAliveMs) {
      (acc, inst) =>
        f(inst)
        acc0
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
  def updateAll(resultsPerScroll: Int = SCROLL_SIZE_DFLT, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT, bulkActions: Int = BULK_PROCESSOR_BULK_SIZE_DFLT)
               (f: T => Future[T])(implicit ec: ExecutionContext, client: Client): Future[Int] = {
    val logPrefix = s"update(${System.currentTimeMillis}): "
    val bp = BulkProcessor.builder(client, new BulkProcessor.Listener {
      /** Перед отправкой каждого bulk-реквеста. */
      override def beforeBulk(executionId: Long, request: BulkRequest): Unit = {
        LOGGER.trace(s"${logPrefix}Going to execute bulk req with ${request.numberOfActions()} actions.")
      }

      /** Данные успешно отправлены в индекс. */
      override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {
        LOGGER.trace(s"$logPrefix")
      }

      /** Ошибка индексации. */
      override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
        LOGGER.error(s"${logPrefix}Failed to execute bulk req with ${request.numberOfActions} actions!", failure)
      }
    })
      .setName(logPrefix)
      .setBulkActions(100)
      .build()
    // Создаём атомный счетчик, который будет инкрементится из разных потоков одновременно.
    // Можно счетчик гнать через аккамулятор, но это будет порождать много бессмысленного мусора.
    val counter = new AtomicInteger(0)
    // Аккамулятор фиксирован (не используется).
    foldLeftAsync(None, resultsPerScroll, keepAliveMs) {
      (accFut, v) =>
        f(v) flatMap { v1 =>
          bp add v1.prepareIndex.request
          counter.incrementAndGet()
          accFut
        }
    } map { _ =>
      bp.close()
      counter.get
    }
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
      .setFetchSource(false)
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
    deserializeOne(Option(hit.getId), hit.getSource, rawVersion2versionOpt(hit.getVersion))
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
      .setFetchSource(false)
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


  def deserializeGetRespFull(getResp: GetResponse): Option[T] = {
    if (getResp.isExists) {
      val result = deserializeOne(Option(getResp.getId), getResp.getSourceAsMap, rawVersion2versionOpt(getResp.getVersion))
      Some(result)
    } else {
      None
    }
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


  /** Рефреш всего индекса, в котором живёт эта модель. */
  def refreshIndex(implicit client: Client): Future[_] = {
    client.admin().indices()
      .prepareRefresh(ES_INDEX_NAME)
      .execute()
  }


  def UPDATE_RETRIES_MAX: Int = EsModel.UPDATE_RETRIES_MAX_DFLT


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
            reget(inst0) flatMap {
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

}


/** Базовый шаблон для статических частей ES-моделей, НЕ имеющих _parent'ов. Применяется в связке с [[EsModelT]].
  * Здесь десериализация полностью выделена в отдельную функцию. */
trait EsModelStaticT extends EsModelCommonStaticT {

  override type T <: EsModelT

  def prepareGet(id: String)(implicit client: Client) = {
    prepareGetBase(id)
  }

  def prepareTermVector(id: String)(implicit client: Client) = prepareTermVectorBase(id)

  def prepareUpdate(id: String)(implicit client: Client) = prepareUpdateBase(id)
  def prepareDelete(id: String)(implicit client: Client) = prepareDeleteBase(id)

  /**
   * Существует ли указанный документ в хранилище?
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
   * Выбрать ряд из таблицы по id.
   * @param id Ключ документа.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[T]] = {
    prepareGet(id)
      .execute()
      .map { deserializeGetRespFull }
  }

  /**
   * Выбрать документ из хранилища без парсинга. Вернуть сырое тело документа (его контент).
   * @param id id документа.
   * @return Строка json с содержимым документа или None.
   */
  def getRawContentById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id)
      .execute()
      .map { deserializeGetRespBodyRawStr }
  }

  /**
   * Прочитать документ как бы всырую.
   * @param id id документа.
   * @return Строка json с документом полностью или None.
   */
  def getRawById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id)
      .execute()
      .map { deserializeGetRespFullRawStr }
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


  def resave(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Option[String]] = {
    resaveBase( getById(id) )
  }

  def reget(inst0: T)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    getById(inst0.id.get)
  }

}


trait EsChildModelStaticT extends EsModelCommonStaticT {

  override type T <: EsChildModelT

  def prepareGet(id: String, parentId: String)(implicit client: Client) = prepareGetBase(id).setParent(parentId)
  def prepareTermVector(id: String, parentId: String)(implicit client: Client) = prepareTermVectorBase(id).setParent(parentId)
  def prepareUpdate(id: String, parentId: String)(implicit client: Client) = prepareUpdateBase(id).setParent(parentId)
  def prepareDelete(id: String, parentId: String)(implicit client: Client) = prepareDeleteBase(id).setParent(parentId)

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id элемента.
   * @param parentId id родительского элемента.
   * @return true/false
   */
  def isExist(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    prepareGet(id, parentId)
      .setFields()
      .setFetchSource(false)
      .execute()
      .map { _.isExists }
  }

  def get(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[T]] = {
    prepareGet(id, parentId)
      .execute()
      .map { deserializeGetRespFull }
  }


  /**
   * Выбрать документ из хранилища без парсинга. Вернуть сырое тело документа (его контент).
   * @param id id документа.
   * @return Строка json с содержимым документа или None.
   */
  def getRawContent(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id, parentId)
      .execute()
      .map { deserializeGetRespBodyRawStr }
  }

  /**
   * Прочитать документ как бы всырую.
   * @param id id документа.
   * @return Строка json с документом полностью или None.
   */
  def getRaw(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id, parentId)
      .execute()
      .map { deserializeGetRespFullRawStr }
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def delete(id: String, parentId: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    prepareDelete(id, parentId)
      .execute()
      .map { _.isFound }
  }

  def resave(id: String, parentId: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Option[String]] = {
    resaveBase( get(id, parentId) )
  }

  def reget(inst0: T)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    get(inst0.id.get, inst0.parentId)
  }

}


/**
 * Результаты работы метода Model.copyContent() возвращаются в этом контейнере.
 * @param success Кол-во успешных документов, т.е. для которых выполнена чтене и запись.
 * @param failed Кол-во обломов.
 */
case class CopyContentResult(success: Long, failed: Long)


/** Шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelPlayJsonT]]. */
trait EsModelStaticMutAkvT extends EsModelCommonStaticT {

  protected def dummy(id: Option[String], version: Option[Long]): T

  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit]

  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T = {
    val acc = dummy(id, version)
    m foreach applyKeyValue(acc)
    acc
  }

}


/** Добавить поле id: Option[String] */
trait OptStrId {
  @JsonIgnore
  def id: Option[String]
}


/** Интерфейс для стирания данных, относящихся только к текущему экземпляру модели, но хранящимися в других моделях. */
trait EraseResources {

  /** Стирание ресурсов, относящихся к этой модели. Например, картинок, на которые ссылкаются поля этой модели. */
  def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    Future successful None
  }

}


/** Общий код динамических частей модели, независимо от child-модели или обычной. */
trait EsModelCommonT extends OptStrId with EraseResources {

  type T <: EsModelCommonT

  /** Модели, желающие версионизации, должны перезаписать это поле. */
  def versionOpt: Option[Long]

  def companion: EsModelCommonStaticT

  def esTypeName = companion.ES_TYPE_NAME
  def esIndexName = companion.ES_INDEX_NAME

  def toJson: String
  def toJsonPretty: String = toJson

  @JsonIgnore def idOrNull = {
    if (id.isDefined)
      id.get
    else
      null
  }

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

  def companionDelete(_id: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean]

  /** Удалить текущий ряд из таблицы. Если ключ не выставлен, то сразу будет экзепшен.
    * @return true - всё ок, false - документ не найден.
    */
  def delete(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = id match {
    case Some(_id)  => companionDelete(_id)
    case None       => Future failed new IllegalStateException("id is not set")
  }

  def prepareDelete(implicit client: Client): DeleteRequestBuilder

  def prepareUpdate(implicit client: Client) = {
    val req = client.prepareUpdate(esIndexName, esTypeName, id.get)
    if (versionOpt.isDefined)
      req.setVersion(versionOpt.get)
    req
  }

}


/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelT extends EsModelCommonT {

  override type T <: EsModelT

  override def companion: EsModelStaticT

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  override def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    val irb = super.indexRequestBuilder
    val rkOpt = getRoutingKey
    if (rkOpt.isDefined)
      irb.setRouting(rkOpt.get)
    irb
  }

  /** Узнать routing key для текущего экземпляра. */
  def getRoutingKey = companion.getRoutingKey(idOrNull)

  override def companionDelete(_id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    companion.deleteById(_id)
  }

  override def prepareDelete(implicit client: Client) = companion.prepareDelete(id.get)
}


/** Динамическая сторона child-модели. */
trait EsChildModelT extends EsModelCommonT {

  override type T <: EsChildModelT

  def parentId: String
  override def companion: EsChildModelStaticT

  override def prepareUpdate(implicit client: Client): UpdateRequestBuilder = {
    super.prepareUpdate.setParent(parentId)
  }

  override def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    super.indexRequestBuilder.setParent(parentId)
  }

  override def companionDelete(_id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    companion.delete(_id, parentId)
  }

  override def prepareDelete(implicit client: Client) = companion.prepareDelete(id.get, parentId)
}


/** Интерфейс с методом сериализации в play.Json экземпляра модели данных. */
trait ToPlayJsonObj {
  def toPlayJsonAcc: FieldsJsonAcc
  /** Сериализовать экземпляр модели данных в промежуточное представление play.Json. */
  def toPlayJson = JsObject(toPlayJsonAcc)
  def toPlayJsonWithId: JsObject
}


/** Шаблон для динамических частей ES-моделей, которые очень хорошо реализуют toJson() через play.json. */
trait EsModelPlayJsonT extends EsModelCommonT with ToPlayJsonObj {
  override type T <: EsModelPlayJsonT

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

trait EsModelJMXMBeanCommonI {

  /** Асинхронно вызвать переиндексацию всех данных в модели. */
  @ManagedOperation(impact=ACTION)
  def resaveMany(maxResults: Int): String

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

  def esIndexName: String
  def esTypeName: String

  /**
   * Выдать сколько-то id'шников в алфавитном порядке.
   * @param maxResults Макс.кол-во выдачи.
   * @return Текст, в каждой строчке новый id.
   */
  def getAllIds(maxResults: Int): String
  def getAll(maxResults: Int): String

  /** Подсчитать кол-во элементов. */
  def countAll(): String
}

trait EsModelJMXMBeanI extends EsModelJMXMBeanCommonI {

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

  def resave(id: String): String

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

}


trait EsModelCommonJMXBase extends JMXBase with EsModelJMXMBeanCommonI with MacroLogsImplLazy {

  import LOGGER._

  def companion: EsModelCommonStaticT

  override def jmxName = "io.suggest.model:type=elasticsearch,name=" + getClass.getSimpleName.replace("Jmx", "")

  // Контексты, зависимые от конкретного проекта.
  implicit def ec: ExecutionContext
  implicit def client: Client
  implicit def sn: SioNotifierStaticClientI

  /** Ругнутся в логи и вернуть строку для возврата клиенту. */
  protected def _formatEx(logPrefix: String, data: String, ex: Throwable): String = {
    error(s"${logPrefix}Failed to make JMX Action:\n$data", ex)
    ex.getClass.getName + ": " + ex.getMessage + "\n\n" + ex.getStackTraceString
  }

  override def resaveMany(maxResults: Int): String = {
    warn(s"resaveMany(maxResults = $maxResults)")
    val resavedIds = companion.resaveMany(maxResults).toSeq
    s"Total: ${resavedIds.size}\n---------\n" + resavedIds.mkString("\n")
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
    val fut = companion.deleteMapping
      .map { _ => "Deleted." }
    awaitString(fut)
  }

  override def generateMapping(): String = {
    trace("generateMapping()")
    val mappingText = companion.generateMapping.string()
    JacksonWrapper.prettify(mappingText)
  }

  override def readCurrentMapping(): String = {
    trace("readCurrentMapping()")
    val fut = companion.getCurrentMapping.map {
      _.fold("Mapping not found.") { JacksonWrapper.prettify }
    }
    awaitString(fut)
  }

  override def getRoutingKey(idOrNull: String): String = {
    trace(s"getRoutingKey($idOrNull)")
    companion.getRoutingKey(idOrNull).toString
  }

  override def getAllIds(maxResults: Int): String = {
    trace(s"getAllIds(maxResults = $maxResults)")
    val fut = companion.getAllIds(maxResults)
      .map { _.sorted.mkString("\n") }
    awaitString(fut)
  }

  override def getAll(maxResults: Int): String = {
    trace(s"getAll(maxResults = $maxResults)")
    val fut = companion.getAll(maxResults, withVsn = true)
      .map { r =>
        val resultNonPretty = toEsJsonDocs(r)
        JacksonWrapper.prettify(resultNonPretty)
      }
    awaitString(fut)
  }

  override def esTypeName: String = companion.ES_TYPE_NAME
  override def esIndexName: String = companion.ES_INDEX_NAME

  override def countAll(): String = {
    val fut = companion.countAll
      .map { _.toString }
    awaitString(fut)
  }
}


trait EsModelJMXBase extends EsModelCommonJMXBase with EsModelJMXMBeanI {

  import LOGGER._

  override def companion: EsModelStaticT

  override def resave(id: String): String = {
    trace(s"resave($id): jmx")
    val fut = companion.resave(id) map {
      case Some(_id) => "Resaved " + _id
      case None      => "Not found id: " + id
    }
    awaitString(fut)
  }

  override def deleteById(id: String): Boolean = {
    warn(s"deleteById($id)")
    companion.deleteById(id)
  }

  override def getById(id: String): String = {
    trace(s"getById($id)")
    val fut = companion.getById(id) map {
      _.fold("not found")(_.toJsonPretty)
    }
    awaitString(fut)
  }

  override def getRawById(id: String): String = {
    trace(s"getRawById($id)")
    val fut = companion.getRawById(id)
      .map { _.fold("not found")(JacksonWrapper.prettify) }
    awaitString(fut)
  }

  override def getRawContentById(id: String): String = {
    trace(s"getRawContentById($id)")
    val fut = companion.getRawContentById(id)
      .map { _.fold("not found")(JacksonWrapper.prettify) }
    awaitString(fut)
  }

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
trait EsModelStaticMutAkvEmptyT extends EsModelStaticMutAkvT {
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    PartialFunction.empty
  }

  def generateMappingProps: List[DocField] = Nil
}


/** Дополнение к [[EsModelStaticMutAkvEmptyT]], но в applyKeyValue() не происходит MatchError. Втыкается в последнем with. */
trait EsModelStaticMutAkvIgnoreT extends EsModelStaticMutAkvT {
  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case other => // Do nothing
    }
  }
}


/** Трейт базовой реализации экземпляра модели. Вынесен из неё из-за особенностей stackable trait pattern.
  * Он содержит stackable-методы, реализованные пустышками. */
trait EsModelEmpty extends EsModelPlayJsonT {
  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    acc
  }
}

