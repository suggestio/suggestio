package io.suggest.model

import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.util.{MacroLogsImpl, SioEsUtil}
import org.joda.time.{ReadableInstant, DateTime}
import org.elasticsearch.action.search.SearchResponse
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}
import org.elasticsearch.client.Client
import scala.util.{Failure, Success}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model._
import java.lang.{Iterable => jlIterable}
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.action.index.IndexRequestBuilder

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
    Seq(MMart, MShop, MShopPriceList, MShopPromoOffer, MYmCategory)
  }

  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelMinimalStaticT[_]] = ES_MODELS)(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    Future.traverse(models) { esModelStatic =>
      val logPrefix = esModelStatic.getClass.getSimpleName + ".putMapping(): "
      esModelStatic.isMappingExists flatMap {
        case false =>
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

  def generateIndexSettings = SioEsUtil.getNewIndexSettings(shards=1, replicas=1)

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
      .setFilterIndices(ES_INDEX_NAME)
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

  def generateMapping: XContentBuilder

  /** Отправить маппинг в elasticsearch. */
  def putMapping(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    client.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
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
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .execute()
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
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .execute()
      .map { !_.isNotFound }
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
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .setFields()
      .execute()
      .map { _.isExists }
  }

  def deserializeOne(id: String, m: collection.Map[String, AnyRef]): T = {
    val acc = dummy(id)
    m foreach applyKeyValue(acc)
    acc
  }

}

/** Шаблон для динамических частей ES-моделей.
 * В минимальной редакции механизм десериализации полностью абстрактен. */
trait EsModelMinimalT[E <: EsModelMinimalT[E]] {
  def companion: EsModelMinimalStaticT[E]

  def toJson: XContentBuilder

  def id: Option[String]

  def idOrNull = {
    if (id.isDefined)
      id.get
    else
      null
  }

  /**
   * Сохранить экземпляр в хранилище ES.
   * @return Фьючерс с новым/текущим id
   */
  def save(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val irb = client.prepareIndex(ES_INDEX_NAME, companion.ES_TYPE_NAME, idOrNull)
      .setSource(toJson)
    saveBuilder(irb)
    irb.execute()
      .map { _.getId }
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

