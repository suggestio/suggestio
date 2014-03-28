package io.suggest.model

import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil, SioEsUtil._
import org.joda.time.{ReadableInstant, DateTime}
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
    Seq(MMart, MShop, MShopPriceList, MShopPromoOffer, MYmCategory, MMartAd)
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
          val fut = esModelStatic.putMapping
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

  /** Имя индекса, который будет использоваться для хранения данных остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val ES_INDEX_NAME = "-sio"

  // Имена полей в разных хранилищах. НЕЛЬЗЯ менять их значения.
  val COMPANY_ID_ESFN   = "companyId"
  val MART_ID_ESFN      = "martId"
  val NAME_ESFN         = "name"
  val DATE_CREATED_ESFN = "dateCreated"
  val DESCRIPTION_ESFN  = "description"
  val SHOP_ID_ESFN      = "shopId"
  val MART_FLOOR_ESFN   = "martFloor"
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
  val LOGO_IMG_ID       = "logoImgId"
  /** Настройки. Это под-объект, чьё содержимое никогда не анализируется никем. */
  val SETTINGS_ESFN     = "settings"
  val META_ESFN         = "meta"


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
  val intParser: PartialFunction[AnyRef, Int] = {
    case null => ???
    case is: jlIterable[_] =>
      intParser(is.head.asInstanceOf[AnyRef])
    case i: Integer => i.intValue()
  }
  val floatParser: PartialFunction[AnyRef, Float] = {
    case null               => ???
    case fs: jlIterable[_] =>
      floatParser(fs.head.asInstanceOf[AnyRef])
    case f: java.lang.Float => f.floatValue()
  }
  val stringParser: PartialFunction[AnyRef, String] = {
    case null => null
    case strings: jlIterable[_] =>
      stringParser(strings.head.asInstanceOf[AnyRef])
    case s: String  => s
  }
  val booleanParser: PartialFunction[AnyRef, Boolean] = {
    case null => ???
    case bs: jlIterable[_] =>
      booleanParser(bs.head.asInstanceOf[AnyRef])
    case b: java.lang.Boolean => b.booleanValue()
  }
  val dateTimeParser: PartialFunction[AnyRef, DateTime] = {
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
  def ensureSioIndex(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    val adm = client.admin().indices()
    adm.prepareExists(ES_INDEX_NAME).execute().flatMap { existsResp =>
      if (existsResp.isExists) {
        Future.successful(false)
      } else {
        adm.prepareCreate(ES_INDEX_NAME)
          .setSettings(generateIndexSettings)
          .execute()
          .map { _ => true }
      }
    }
  }

  /**
   * Существует ли указанный маппинг в хранилище? Используется, когда модель хочет проверить наличие маппинга
   * внутри общего индекса.
   * @param typename Имя типа.
   * @return Да/нет.
   */
  def isMappingExists(typename: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    client.admin().cluster()
      .prepareState()
      .setIndices(ES_INDEX_NAME)
      .execute()
      .map { cmd =>
        val imd = cmd.getState
          .getMetaData
          .index(ES_INDEX_NAME)
          .mapping(typename)
        trace("mapping exists resp: " + imd)
        imd != null
      }
  }


}

import EsModel._

/** Базовый шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelMinimalT]].
  * Здесь десериализация полностью выделена в отдельную функцию. */
trait EsModelMinimalStaticT[T <: EsModelMinimalT[T]] {
  val ES_TYPE_NAME: String

  def generateMapping: XContentBuilder = generateMappingFor(ES_TYPE_NAME)
  def generateMappingFor(typeName: String): XContentBuilder = jsonGenerator { implicit b =>
    // Собираем маппинг индекса.
    IndexMapping(
      typ = typeName,
      staticFields = generateMappingStaticFields,
      properties = generateMappingProps
    )
  }

  def generateMappingStaticFields: List[Field]
  def generateMappingProps: List[DocField]

  /** Флаг, который можно перезаписать в реализации static-модели чтобы проигнорить конфликты при апдейте маппинга. */
  protected def mappingIgnoreConflicts: Boolean = false

  /** Отправить маппинг в elasticsearch. */
  def putMapping(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    client.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
      .setIgnoreConflicts(mappingIgnoreConflicts)
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

  /**
   * Примитив для рассчета кол-ва документов, удовлетворяющих указанному запросу.
   * @param query Произвольный поисковый запрос.
   * @return Кол-во найденных документов.
   */
  protected def count(query: QueryBuilder)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    client.prepareCount(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
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
    deleteMapping flatMap { _ => putMapping }
  }

  // TODO Нужно проверять, что текущий маппинг не устарел, и обновлять его.
  def isMappingExists(implicit ec:ExecutionContext, client: Client) = EsModel.isMappingExists(ES_TYPE_NAME)

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
    val req = client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
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
  protected def searchResp2list(searchResp: SearchResponse): Seq[T] = {
    searchResp.getHits.getHits.toSeq.map { hit =>
      deserializeOne(hit.getId, hit.getSource)
    }
  }

  /** Для ряда задач бывает необходимо задействовать multiGet вместо обычного поиска, который не успевает за refresh.
    * Этот метод позволяет сконвертить поисковые результаты в результаты multiget.
    * @return Результат - что-то неопределённом порядке. */
  protected def searchResp2RtMultiget(searchResp: SearchResponse)(implicit ex: ExecutionContext, client: Client): Future[List[T]] = {
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
        .map { mgetResp =>
          mgetResp.getResponses.foldLeft[List[T]] (Nil) { (acc, mgetItem) =>
            // Поиск может содержать элементы, которые были только что удалены. Нужно их отсеивать.
            if (mgetItem.isFailed || !mgetItem.getResponse.isExists)
              acc
            else
              deserializeOne(mgetItem.getId, mgetItem.getResponse.getSourceAsMap) :: acc
          }
        }
    }
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @return Список магазинов в порядке их создания.
   */
  def getAll(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(QueryBuilders.matchAllQuery())
      .execute()
      .map { searchResp2list }
  }

  /**
   * Генератор delete-реквеста. Используется при bulk-request'ах.
   * @param id adId
   * @return Новый экземпляр DeleteRequestBuilder.
   */
  def deleteRequestBuilder(id: String)(implicit client: Client): DeleteRequestBuilder = {
    val req = client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
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

}

/** Шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelT]]. */
trait EsModelStaticT[T <: EsModelT[T]] extends EsModelMinimalStaticT[T] {

  protected def dummy(id: String): T
  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit]

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id магазина.
   * @return true/false
   */
  def isExist(id: String)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    val req = client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
    val rk = getRoutingKey(id)
    if (rk.isDefined)
      req.setRouting(rk.get)
    req.setFields()
      .execute()
      .map { _.isExists }
  }

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
  @JsonIgnore protected def esIndexName = ES_INDEX_NAME

  /** Можно делать какие-то действия после десериализации. Например, можно исправлять значения после эволюции схемы. */
  @JsonIgnore def postDeserialize() {}

  @JsonIgnore def toJson: XContentBuilder

  def id: Option[String]

  @JsonIgnore def idOrNull = {
    if (id.isDefined)
      id.get
    else
      null
  }

  /** Перед сохранением можно проверять состояние экземпляра. */
  def isFieldsValid: Boolean = true

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    val _idOrNull = idOrNull
    val irb = client.prepareIndex(esIndexName, esTypeName, _idOrNull)
      .setSource(toJson)
    saveBuilder(irb)
    val rkOpt = companion.getRoutingKey(_idOrNull)
    if (rkOpt.isDefined)
      irb.setRouting(rkOpt.get)
    irb
  }

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
  def toJson: XContentBuilder = {
    val acc = XContentFactory.jsonBuilder()
      .startObject()
    writeJsonFields(acc)
    acc.endObject()
  }
  def writeJsonFields(acc: XContentBuilder)
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

