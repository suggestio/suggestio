package io.suggest.model

import scala.concurrent.{Awaitable, Await, ExecutionContext, Future}
import io.suggest.util.{JMXBase, JacksonWrapper, MacroLogsImpl, SioEsUtil}
import SioEsUtil._
import org.joda.time.{DateTimeZone, ReadableInstant, DateTime}
import org.elasticsearch.action.search.SearchResponse
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}
import org.elasticsearch.client.Client
import scala.util.{Failure, Success}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model._
import java.lang.{Iterable => jlIterable}
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.action.index.IndexRequestBuilder
import scala.annotation.tailrec
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.action.delete.DeleteRequestBuilder
import io.suggest.ym.model.stat._
import com.sun.org.glassfish.gmbal.{Impact, ManagedOperation}
import play.api.libs.json.{JsArray, JsString, JsValue, JsObject}
import org.joda.time.format.ISODateTimeFormat
import org.elasticsearch.action.get.MultiGetResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 */
object EsModel extends MacroLogsImpl {

  import LOGGER._

  /** Список ES-моделей. Нужен для удобства массовых maintance-операций. Расширяется по мере роста числа ES-моделей. */
  def ES_MODELS: Seq[EsModelMinimalStaticT[_]] = {
    Seq(MShopPriceList, MShopPromoOffer, MYmCategory, MAdStat, MAdnNode, MAd)
  }


  implicit def listCmpOrdering[T <: Comparable[T]] = new ListCmpOrdering[T]

  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelMinimalStaticT[_]] = ES_MODELS, ignoreExists: Boolean = false)(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
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
          trace(logPrefix + "Mapping already exists in index. Skipping...")
          Future successful true
      }
    } map {
      _.reduceLeft { _ && _ }
    }
  }

  /** Сконвертить флаг reversed-сортировки в параметр для ES типа SortOrder. */
  val isReversed2sortOrder: PartialFunction[Boolean, SortOrder] = {
    case false => SortOrder.ASC
    case true  => SortOrder.DESC
  }

  /** Имя индекса, который будет использоваться для хранения данных для большинства остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val SIO_ES_INDEX_NAME = "-sio"

  // Имена полей в разных хранилищах. НЕЛЬЗЯ менять их значения.
  val COMPANY_ID_ESFN   = "companyId"
  val MART_ID_ESFN      = "martId"
  val NAME_ESFN         = "name"
  val DATE_CREATED_ESFN = "dateCreated"
  val DESCRIPTION_ESFN  = "description"
  val SHOP_ID_ESFN      = "shopId"
  @deprecated("mart+shop arch is deprecated. Use EMAdnMMetadata instead.", "2014.apr.10")
  val MART_FLOOR_ESFN   = "martFloor"
  @deprecated("mart+shop arch is deprecated. Use EMAdnMMetadata instead.", "2014.apr.10")
  val MART_SECTION_ESFN = "martSection"
  val AUTH_INFO_ESFN    = "authInfo"
  val URL_ESFN          = "url"
  val ADDRESS_ESFN      = "address"
  val SITE_URL_ESFN     = "siteUrl"
  val PARENT_ID_ESFN    = "parentId"
  val PERSON_ID_ESFN    = "personId"
  val KEY_ESFN          = "key"
  val VALUE_ESFN        = "value"
  val IS_VERIFIED_ESFN  = "isVerified"
  val TOWN_ESFN         = "town"
  val COUNTRY_ESFN      = "country"
  val CATEGORY_ESFN     = "cat"
  val CATEGORY_ID_ESFN  = "catId"
  val PRIO_ESFN         = "prio"
  val PHONE_ESFN        = "phone"
  val LOGO_IMG_ESFN     = "logoImg"
  val LOGO_IMG_ID_ESFN  = "logoImgId"
  /** Настройки. Это под-объект, чьё содержимое никогда не анализируется никем. */
  val SETTINGS_ESFN     = "settings"
  val META_ESFN         = "meta"

  /** Тип аккамулятора, который используется во [[EsModelT.writeJsonFields()]]. */
  type FieldsJsonAcc = List[(String, JsValue)]

  def companyIdParser = stringParser
  def martIdParser = stringParser
  def nameParser = stringParser
  def dateCreatedParser = dateTimeParser
  def descriptionParser = stringParser
  def martFloorParser = intParser
  def martSectionParser = intParser
  def shopIdParser = stringParser
  def urlParser = stringParser
  def addressParser = stringParser
  def siteUrlParser = stringParser
  def authInfoParser = stringParser andThen {
    case null => None
    case s =>
      val Array(username, pw) = s.split("::", 2)
      Some(UsernamePw(username, pw))
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
    case is: jlIterable[_] =>
      intParser(is.head.asInstanceOf[AnyRef])
    case i: Integer => i.intValue()
  }
  val floatParser: PartialFunction[Any, Float] = {
    case null               => ???
    case fs: jlIterable[_] =>
      floatParser(fs.head.asInstanceOf[Any])
    case f: java.lang.Number => f.floatValue()
  }
  val stringParser: PartialFunction[Any, String] = {
    case null => null
    case strings: jlIterable[_] =>
      stringParser(strings.head.asInstanceOf[AnyRef])
    case s: String  => s
  }
  val booleanParser: PartialFunction[Any, Boolean] = {
    case null => ???
    case bs: jlIterable[_] =>
      booleanParser(bs.head.asInstanceOf[AnyRef])
    case b: java.lang.Boolean => b.booleanValue()
  }
  val dateTimeParser: PartialFunction[Any, DateTime] = {
    case null => null
    case dates: jlIterable[_] =>
      dateTimeParser(dates.head.asInstanceOf[AnyRef])
    case s: String           => new DateTime(s)
    case d: java.util.Date   => new DateTime(d)
    case d: DateTime         => d
    case ri: ReadableInstant => new DateTime(ri)
  }

  def generateIndexSettings = SioEsUtil.getIndexSettingsV2(shards=1, replicas=1)

  /**
   * Убедиться, что индекс существует.
   * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
   *         Если индекс уже существует, то false.
   */
  def ensureSioIndex(indexName: String = SIO_ES_INDEX_NAME)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    val adm = client.admin().indices()
    adm.prepareExists(indexName).execute().flatMap { existsResp =>
      if (existsResp.isExists) {
        Future.successful(false)
      } else {
        adm.prepareCreate(indexName)
          .setSettings(generateIndexSettings)
          .execute()
          .map { _ => true }
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
    client.admin().cluster()
      .prepareState()
      .setIndices(indexName)
      .execute()
      .map { cmd =>
        val imd = cmd.getState
          .getMetaData
          .index(indexName)
          .mapping(typeName)
        trace("mapping exists resp: " + imd)
        imd != null
      }
  }

  /** Сериализация набора строк. */
  def asJsonStrArray(strings : Iterable[String]): JsArray = {
    val strSeq = strings.foldLeft [List[JsString]] (Nil) {(acc, e) =>
      JsString(e) :: acc
    }
    JsArray(strSeq)
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
trait EsModelStaticMapping extends EsModelStaticMappingGenerators {

  def ES_INDEX_NAME = SIO_ES_INDEX_NAME
  def ES_TYPE_NAME: String

  def generateMapping: XContentBuilder = generateMappingFor(ES_TYPE_NAME)

  /** Флаг, который можно перезаписать в реализации static-модели чтобы проигнорить конфликты при апдейте маппинга. */
  protected def mappingIgnoreConflicts: Boolean = false

  /** Отправить маппинг в elasticsearch. */
  def putMapping(ignoreConflicts: Boolean = mappingIgnoreConflicts)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
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
    client.admin().indices()
      .prepareDeleteMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .execute()
  }

}


/** Базовый шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelMinimalT]].
  * Здесь десериализация полностью выделена в отдельную функцию. */
trait EsModelMinimalStaticT[T <: EsModelMinimalT[T]] extends EsModelStaticMapping {

  // Короткие враппер для типичных операций в рамках статической модели.
  def prepareSearch(implicit client: Client) = client.prepareSearch(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)
  def prepareCount(implicit client: Client)  = client.prepareCount(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)
  def prepareGet(id: String)(implicit client: Client) = client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
  def prepareUpdate(id: String)(implicit client: Client) = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, id)
  def prepareDelete(id: String)(implicit client: Client) = client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
  def prepareDeleteByQuery(implicit client: Client) = client.prepareDeleteByQuery(ES_INDEX_NAME).setTypes(ES_TYPE_NAME)

  val MAX_RESULTS_DFLT = 100
  val OFFSET_DFLT = 0

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id магазина.
   * @return true/false
   */
  def isExist(id: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    val req = prepareGet(id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req.setFields()
      .execute()
      .map { _.isExists }
  }


  /**
   * Сервисная функция для получения списка всех id.
   * @return Список всех id в алфавитном порядке.
   */
  def getAllIds(maxResults: Int = 500)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    prepareSearch
      .setNoFields()
      .setSize(maxResults)
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.foldLeft(List.empty[String]) {
          (acc, hit) => hit.getId :: acc
        }.sorted
      }
  }

  /**
   * Примитив для рассчета кол-ва документов, удовлетворяющих указанному запросу.
   * @param query Произвольный поисковый запрос.
   * @return Кол-во найденных документов.
   */
  def count(query: QueryBuilder)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    prepareCount
      .setQuery(query)
      .execute()
      .map { _.getCount }
  }


  /** Если модели требуется выставлять routing для ключа, то можно делать это через эту функцию.
    * @param idOrNull id или null, если id отсутствует.
    * @return None если routing не требуется, иначе Some(String).
    */
  def getRoutingKey(idOrNull: String): Option[String] = None

  /** Пересоздать маппинг удаляется и создаётся заново. */
  def resetMapping(implicit ec: ExecutionContext, client: Client): Future[_] = {
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
  def deserializeOne(id: String, m: collection.Map[String, AnyRef]): T


  /**
   * Выбрать ряд из таблицы по id.
   * @param id Ключ магазина.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String)(implicit ec:ExecutionContext, client: Client): Future[Option[T]] = {
    val maybeRk = getRoutingKey(id)
    val req = prepareGet(id)
    if (maybeRk.isDefined)
      req.setRouting(maybeRk.get)
    req.execute()
      .map { getResp =>
        if (getResp.isExists) {
          val result = deserializeOne(getResp.getId, getResp.getSourceAsMap)
          Some(result)
        } else {
          None
        }
      }
  }

  /** Список результатов с source внутри перегнать в распарсенный список. */
  def searchResp2list(searchResp: SearchResponse): Seq[T] = {
    searchResp.getHits.getHits.toSeq.map { hit =>
      deserializeOne(hit.getId, hit.getSource)
    }
  }

  /**
   * Прочитать из базы все перечисленные id разом.
   * @param ids id документов этой модели.
   * @return Список результатов в неопределённом порядке.
   */
  def multiGet(ids: Seq[String])(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    if (ids.isEmpty) {
      Future successful Nil
    } else {
      val req = client.prepareMultiGet()
        .setRealtime(true)
      ids.foreach {
        id =>
          req.add(ES_INDEX_NAME, ES_TYPE_NAME, id)
      }
      req.execute()
        .map {
        mgetResp2list
      }
    }
  }

  /** Для ряда задач бывает необходимо задействовать multiGet вместо обычного поиска, который не успевает за refresh.
    * Этот метод позволяет сконвертить поисковые результаты в результаты multiget.
    * @return Результат - что-то неопределённом порядке. */
  def searchResp2RtMultiget(searchResp: SearchResponse)(implicit ex: ExecutionContext, client: Client): Future[List[T]] = {
    val searchHits = searchResp.getHits.getHits
    if (searchHits.length == 0) {
      Future successful Nil
    } else {
      val mgetReq = client.prepareMultiGet()
        .setRealtime(true)
      searchHits.foreach { hit =>
        mgetReq.add(hit.getIndex, hit.getType, hit.getId)
      }
      mgetReq
        .execute()
        .map { mgetResp2list }
    }
  }


  /** Распарсить выхлоп мультигета. */
  def mgetResp2list(mgetResp: MultiGetResponse): List[T] = {
    mgetResp.getResponses.foldLeft[List[T]] (Nil) { (acc, mgetItem) =>
      // Поиск может содержать элементы, которые были только что удалены. Нужно их отсеивать.
      if (mgetItem.isFailed || !mgetItem.getResponse.isExists)
        acc
      else
        deserializeOne(mgetItem.getId, mgetItem.getResponse.getSourceAsMap) :: acc
    }
  }


  /** С помощью query найти результаты, но сами результаты прочитать с помощью realtime multi-get. */
  def findQueryRt(query: QueryBuilder, maxResults: Int = 100)(implicit ec: ExecutionContext, client: Client): Future[List[T]] = {
    prepareSearch
      .setQuery(query)
      .setNoFields()
      .setSize(maxResults)
      .execute()
      .flatMap { searchResp2RtMultiget }
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @return Список магазинов в порядке их создания.
   */
  def getAll(maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(maxResults)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
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
  def reindexAll(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Seq[String]] = {
    getAll().flatMap { results =>
      Future.traverse(results) { el =>
        el.save
      }
    }
  }

}

/** Шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelT]]. */
trait EsModelStaticT[T <: EsModelT[T]] extends EsModelMinimalStaticT[T] {

  protected def dummy(id: String): T

  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit]

  def deserializeOne(id: String, m: collection.Map[String, AnyRef]): T = {
    val acc = dummy(id)
    m foreach applyKeyValue(acc)
    acc.postDeserialize()
    acc
  }

}



/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelMinimalT[E <: EsModelMinimalT[E]] {

  @JsonIgnore def companion: EsModelMinimalStaticT[E]

  @JsonIgnore protected def esTypeName = companion.ES_TYPE_NAME
  @JsonIgnore protected def esIndexName = companion.ES_INDEX_NAME

  /** Можно делать какие-то действия после десериализации. Например, можно исправлять значения после эволюции схемы. */
  @JsonIgnore def postDeserialize() {}

  @JsonIgnore def toJson: String

  @JsonIgnore
  def id: Option[String]

  @JsonIgnore def idOrNull = {
    if (id.isDefined)
      id.get
    else
      null
  }

  def prepareUpdate(implicit client: Client) = client.prepareUpdate(esIndexName, esTypeName, id.get)

  /** Загрузка новых значений *пользовательских* полей из указанного экземпляра такого же класса.
    * Полезно при edit form sumbit после накатывания маппинга формы на реквест. */
  def loadUserFieldsFrom(other: E) {}

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
   */
  def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    if (isFieldsValid) {
      indexRequestBuilder
        .execute()
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

/** Шаблон для динамических частей ES-моделей. */
trait EsModelT[E <: EsModelT[E]] extends EsModelMinimalT[E] {
  def toJson = JsObject(writeJsonFields(Nil)).toString()
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
  def reindexAll()

  /**
   * Существует ли указанный маппинг сейчас?
   * @return true, если маппинг сейчас существует.
   */
  @ManagedOperation(impact=INFO)
  def isMappingExists: Boolean

  /** Асинхронно сбросить маппинг. */
  @ManagedOperation(impact=ACTION)
  def resetMapping()

  /** Асинхронно отправить маппинг в ES. */
  @ManagedOperation(impact=ACTION)
  def putMapping: String

  /** Асинхронно удалить маппинг вместе со всеми данными. */
  @ManagedOperation(impact=ACTION)
  def deleteMapping()

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

}

trait EsModelJMXBase extends JMXBase with EsModelJMXMBeanCommon {

  def companion: EsModelMinimalStaticT[_]

  override def jmxName = "io.suggest:type=model,name=" + getClass.getSimpleName.replace("Jmx", "")

  // Контексты, зависимые от конкретного проекта.
  implicit def ec: ExecutionContext
  implicit def client: Client
  implicit def sn: SioNotifierStaticClientI

  def reindexAll() {
    companion.reindexAll
  }

  def isMappingExists: Boolean = {
    companion.isMappingExists
  }

  def resetMapping() {
    companion.resetMapping
  }

  def putMapping = {
    companion.putMapping()
      .map(_.toString)
      .recover { case ex: Exception => s"${ex.getClass.getName}: ${ex.getMessage}\n${ex.getStackTraceString}" }
  }

  def deleteMapping() {
    companion.deleteMapping
  }

  def getRoutingKey(idOrNull: String): String = {
    companion.getRoutingKey(idOrNull).toString
  }

  def deleteById(id: String): Boolean = {
    companion.deleteById(id)
  }

  def getById(id: String): String = {
    val docOpt: Option[_] = companion.getById(id)
    docOpt match {
      case None => null
      case Some(obj) => JacksonWrapper.serializePretty(obj)
    }
  }

  def getAllIds(maxResults: Int): String = {
    companion.getAllIds(maxResults).mkString("\n")
  }

  def esTypeName: String = companion.ES_TYPE_NAME
  def esIndexName: String = companion.ES_INDEX_NAME
}




/**
 * Трейт для статической части модели, построенной через Stackable trait pattern.
 * Для нормального stackable trait без подсветки красным цветом везде, надо чтобы была базовая реализация отдельно
 * от целевой реализации и stackable-реализаций (abstract override).
 * Тут реализованы методы-заглушки для хвоста стэка декораторов. */
trait EsModelStaticEmpty[T <: EsModelEmpty[T]] extends EsModelStaticT[T] {

  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    PartialFunction.empty
  }

  def generateMappingProps: List[DocField] = {
    Nil
  }

}

/** Трейт базовой реализации экземпляра модели. Вынесен из неё из-за особенностей stackable trait pattern.
  * Он содержит stackable-методы, реализованные пустышками. */
trait EsModelEmpty[T <: EsModelEmpty[T]] extends EsModelT[T] {
  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    acc
  }
}


