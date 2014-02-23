package models

import scala.concurrent.Future
import util.SiowebEsUtil.client
import io.suggest.util.SioEsUtil.laFuture2sFuture
import play.api.libs.concurrent.Execution.Implicits._
import io.suggest.util.SioEsUtil
import util.PlayMacroLogsImpl
import org.joda.time.{ReadableInstant, DateTime}
import org.elasticsearch.action.search.SearchResponse
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 */
object EsModel extends PlayMacroLogsImpl {

  import LOGGER._

  /** Имя индекса, который будет использоваться для хранения данных остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val ES_INDEX_NAME = "-sio"

  // Имена полей в разных хранилищах
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
  val SITE_URL_ESFN       = "siteUrl"

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
  val intParser: PartialFunction[AnyRef, Int] = {
    case null       => ???
    case i: Integer => i.intValue()
  }
  val floatParser: PartialFunction[AnyRef, Float] = {
    case null               => ???
    case f: java.lang.Float => f.floatValue()
  }
  val stringParser: PartialFunction[AnyRef, String] = {
    case null       => null
    case s: String  => s
  }
  val booleanParser: PartialFunction[AnyRef, Boolean] = {
    case null                 => ???
    case b: java.lang.Boolean => b.booleanValue()
  }
  val dateTimeParser: PartialFunction[AnyRef, DateTime] = {
    case null                => null
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
  def ensureIndex: Future[Boolean] = {
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
  def isMappingExists(typename: String): Future[Boolean] = {
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

  def putMapping: Future[Boolean] = {
    client.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
      .execute()
      .map { _.isAcknowledged }
  }

  def isMappingExists = EsModel.isMappingExists(ES_TYPE_NAME)

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
  def getById(id: String): Future[Option[T]] = {
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
  def getAll: Future[Seq[T]] = {
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
  def deleteById(id: String): Future[Boolean] = {
    client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .execute()
      .map { !_.isNotFound }
  }

}

/** Шаблон для статических частей ES-моделей. Применяется в связке с [[EsModelT]]. */
trait EsModelStaticT[T <: EsModelT[T]] extends EsModelMinimalStaticT[T] {

  protected def dummy(id: String): T
  def applyMap(m: collection.Map[String, AnyRef], acc: T): T

  /**
   * Существует ли указанный магазин в хранилище?
   * @param id id магазина.
   * @return true/false
   */
  def isExist(id: String): Future[Boolean] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .setFields()
      .execute()
      .map { _.isExists }
  }

  def deserializeOne(id: String, m: collection.Map[String, AnyRef]): T = {
    val acc = dummy(id)
    applyMap(m, acc)
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
  def save: Future[String] = {
    client.prepareIndex(ES_INDEX_NAME, companion.ES_TYPE_NAME, idOrNull)
      .setSource(toJson)
      .execute()
      .map { _.getId }
  }


  /** Удалить текущий ряд из таблицы. Если ключ не выставлен, то сразу будет экзепшен.
    * @return true - всё ок, false - документ не найден.
    */
  def delete: Future[Boolean] = id match {
    case Some(_id)  => companion.deleteById(_id)
    case None       => Future failed new IllegalStateException("id is not set")
  }
}

/** Шаблон для динамических частей ES-моделей. */
trait EsModelT[E <: EsModelT[E]] extends EsModelMinimalT[E] {
  override def companion: EsModelStaticT[E]

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

