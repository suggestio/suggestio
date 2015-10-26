package io.suggest.ym.model

import java.{util => ju}

import akka.actor.ActorContext
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.event.SioNotifier.Event
import io.suggest.event.subscriber.SnClassSubscriber
import io.suggest.event.{MNodeDeletedEvent, SNStaticSubscriber, SioNotifierStaticClientI}
import io.suggest.model.es._
import EsModelUtil.FieldsJsonAcc
import io.suggest.model._
import io.suggest.model.geo.{IGeoShapeIndexed, CircleGs, GeoShape, GeoShapeQuerable}
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.index.query.{FilterBuilders, FilterBuilder, QueryBuilder, QueryBuilders}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.14 17:56
 * Description: Хранилище географических объектов (GeoShape'ов) для элементов модели MAdnNode.
 * Географические элементы очень жирные, их сериализация-десириализация довольно тяжела, поэтому
 * эти данные хранятся в этой модели.
 * Элементы имеют parent-поле для хранения рядом с экземпляром MAdnNode и для has_parent.
 *
 * Геообъекты хранятся в поле nested-объекта. Имя поле формируется на основе уровня. Разные поля для разноуровневых
 * геообъектов необходимо, чтобы гибко управлять точностью и ресурсоёмкостью индекса. И просто чтобы регулировать
 * приоритет объектов.
 */
object MAdnNodeGeo extends EsChildModelStaticT with MacroLogsImpl {

  import LOGGER._

  override type T = MAdnNodeGeo

  private[this] val GEO_ESFN = "geo"

  /** Название поля с id узла. */
  val ADN_ID_ESFN = "adnId"
  /** Поле с опциональной ссылкой на исходник. */
  val URL_ESFN    = "url"
  /** Дата последнего обновления документа. */
  val LAST_MODIFIED_ESFN = "lm"
  /** Храним название поля в отдельном поле. Это нужно для получения уровня без получения самого документа. */
  val GLEVEL_ESFN = "gl"
  /** Флаг совместимости фигуры с geo-json. */
  val GEO_JSON_COMPATIBLE_ESFN = "gjsc"

  override val ES_TYPE_NAME = "ang"   // ang = adn node geo

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldParent(MAdnNode.ES_TYPE_NAME),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    val nglFields = NodeGeoLevels.values
      .foldLeft( List.empty[DocField] ) {
        (acc, nglv)  =>
          val ngl: NodeGeoLevel = nglv
          FieldGeoShape(ngl.esfn, precision = ngl.precision)  ::  acc
      }
    // Генерим top level object mapping:
    FieldString(ADN_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = true) ::
      FieldString(URL_ESFN, index = FieldIndexingVariants.no, include_in_all = false) ::
      FieldDate(LAST_MODIFIED_ESFN, index = null, include_in_all = false) ::
      // store = true для возможности простого и быстрого получения названия используемого glevel-поля в обход очень тяжелого _source.
      FieldString(GLEVEL_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = true) ::
      FieldBoolean(GEO_JSON_COMPATIBLE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false, store = false) ::
      nglFields
  }

  /** Десериализация данных, сохранённых в nested гео-контейнере старого формата.
    * Нужно удалить этот код по завершению миграции на новый формат (нужно пересохранение). */
  @deprecated("nested-object geo structure replaced by flat structure.", "2014.09.09")
  private[this] def deserializeGeoContainer(raw: Any): (NodeGeoLevel, GeoShape) = {
    raw match {
      case jmap: ju.Map[_, _] =>
        _deserializeGeoTuple(jmap.head)
      case smap: collection.Map[_, _] =>
        _deserializeGeoTuple(smap.head)
    }
  }

  @deprecated("nested-object geo structure replaced by flat structure.", "2014.09.09")
  private[this] def _deserializeGeoTuple(raw: (Any,Any)): (NodeGeoLevel, GeoShape) = {
    val (esfnRaw, shapeRaw) = raw
    val esfn = NodeGeoLevels.withName( EsModelUtil.stringParser(esfnRaw) )
    val shape = GeoShape.deserialize(shapeRaw).get
    (esfn, shape)
  }

  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], versionOpt: Option[Long]): T = {
    // 2014.09.09: Вместо nested object используется полностью flat-структура полей без всяких object'ов вообще.
    // Название используемого поля хранится в поле GLEVEL_ESFN.
    val glevelOpt: Option[NodeGeoLevel] = m.get(GLEVEL_ESFN)
      .map(EsModelUtil.stringParser)
      .map(NodeGeoLevels.withName)
    val (glevel, shape): (NodeGeoLevel, GeoShape) = glevelOpt match {
      // Современный формат геоданных
      case Some(_glevel) =>
        val gs = m.get(_glevel.esfn)
          .flatMap(GeoShape.deserialize)
          .get
        _glevel -> gs
      // Устаревший формат geo-данных
      case None =>
        deserializeGeoContainer( m(GEO_ESFN) )
    }
    MAdnNodeGeo(
      adnId       = EsModelUtil.stringParser( m(ADN_ID_ESFN) ),
      glevel      = glevel,
      shape       = shape,
      url         = m.get(URL_ESFN).map(EsModelUtil.stringParser),
      lastModified = m.get(LAST_MODIFIED_ESFN).fold(DateTime.now)(EsModelUtil.dateTimeParser),
      id          = id,
      versionOpt  = versionOpt
    )
  }

  /** Собрать запрос для поиска в модели по узлу. */
  def adnIdQuery(adnId: String) = QueryBuilders.termQuery(ADN_ID_ESFN, adnId)

  /**
   * Поиск по id узла.
   * @param adnId id узла
   * @param maxResults Макс.число результатов.
   * @param offset Сдвиг по выдаче.
   * @return Фьючерс со списком результатов.
   */
  def findByNode(adnId: String, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVersions: Boolean = false)
                (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( adnIdQuery(adnId) )
      .setSize( maxResults )
      .setFrom( offset )
      .setVersion(withVersions)
      .setFetchSource(true)
      .setRouting(adnId)  // adnId является parentId, поэтому можно точно указать шарду, в которой надо искать результат.
      .execute()
      .map { searchResp2list }
  }

  /**
   * Посчитать кол-во имеющихся шейпов для указанного узла.
   * @param adnId id узла.
   * @return Целое неотрицательное.
   */
  def countByNode(adnId: String)(implicit ec: ExecutionContext, client: Client): Future[Int] = {
    prepareCount
      .setQuery( adnIdQuery(adnId) )
      .setRouting(adnId)
      .execute()
      .map { _.getCount.toInt }
  }

  /** Сгенерить запрос для поиска совпадений в гео-полях указанного уровня. */
  def geoQuery(glevel: NodeGeoLevel, shape: GeoShapeQuerable): QueryBuilder = {
    QueryBuilders.geoShapeQuery(glevel.esfn, shape.toEsShapeBuilder)
  }

  private def geoFilter(glevel: NodeGeoLevel, shape: GeoShapeQuerable): FilterBuilder = {
    FilterBuilders.geoShapeFilter(glevel.esfn, shape.toEsShapeBuilder, ShapeRelation.INTERSECTS)
  }

  def glevelQuery(glevel: NodeGeoLevel): QueryBuilder = {
    QueryBuilders.termQuery(GLEVEL_ESFN, glevel.esfn)
  }

  def glevelFilter(glevel: NodeGeoLevel): FilterBuilder = {
    FilterBuilders.termFilter(GLEVEL_ESFN, glevel.esfn)
  }

  def glevelsQuery(glevels: Seq[NodeGeoLevel]): QueryBuilder = {
    QueryBuilders.termsQuery(GLEVEL_ESFN, glevels.map(_.esfn) : _*)
  }

  private def searchResp2adnIdsList(searchResp: SearchResponse): Seq[String] = {
    searchResp2fnList[String](searchResp, ADN_ID_ESFN)
  }

  /**
   * Быстрый поиск узлов и быстрая десериализация результатов поиска по пересечению с указанной геолокацией.
   * @param glevel Уровень поиска.
   * @param shape Фигура, по которой ищем.
   * @param maxResults Макс.кол-во результатов.
   * @param offset Сдвиг в результатах.
   * @return Фьючерс со списком adnId, чьи фигуры пересекают переданную фигуру.
   */
  def geoFindAdnIdsOnLevel(glevel: NodeGeoLevel, shape: GeoShapeQuerable, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                          (implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    prepareSearch
      .setQuery( geoQuery(glevel, shape) )
      .setSize( maxResults )
      .setFrom( offset )
      .setFetchSource(false)
      .addField(ADN_ID_ESFN)
      .execute()
      .map { searchResp2adnIdsList }
  }

  /**
   * Сбор adnId, имеющих фигуры на указанных уровнях (слоях). Этот поиск идёт БЕЗ использования геошейпов.
   * @param glevels слои
   * @param maxResults Макс. кол-во результатов.
   * @param offset Сдвиг в выдаче.
   * @return Список adn_id в неопределённом порядке, с возможными дубликатами.
   */
  def findAdnIdsWithLevels(glevels: Seq[NodeGeoLevel], maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                          (implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {

    prepareSearch
      .setQuery(glevelsQuery(glevels))
      .setFetchSource(false)
      .addField(ADN_ID_ESFN)
      .setSize(maxResults)
      .setFrom(offset)
      .execute()
      .map { searchResp2adnIdsList }
  }

  /** Удалить все документы, относящиеся к указанному adnId.
    * @param adnId id узла-родителя.
    * @return Кол-во удалённых рядов.
    */
  def deleteByAdnId(adnId: String)(implicit ec: ExecutionContext, client: Client): Future[Int] = {
    prepareDeleteByQuery
      .setQuery( adnIdQuery(adnId) )
      .execute()
      .map { _.iterator().size }
  }

  /**
   * Быстрое узнавание гео-уровня (гео-слоя), в котором сохранена геоинформация указанного документа.
   * @param id id документа.
   * @param parentId id родительского узла.
   * @return Фьючерс с опциональным значением геоуровня.
   */
  private def getGeoLevelsUsed(id: String, parentId: String)(implicit ec: ExecutionContext, client: Client): Future[Option[NodeGeoLevel]] = {
    // Используем быстрый доступ к сохранённому glevel через stored-значение поля GLEVEL_ESFN.
    // Это позволяет избежать фетчинга и разбора жирного _source.
    prepareGet(id, parentId = parentId)
      // Для доступа через stored-поле нужно realtime=false. Если будет true, то будет ковыряние в тяжелом _source.
      // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-get.html#realtime
      .setRealtime(false)
      .setFields(GLEVEL_ESFN)
      .setFetchSource(false)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          Option(getResp.getField(GLEVEL_ESFN))
            .flatMap { field => NodeGeoLevels.maybeWithName( field.getValue.toString ) }
        } else {
          None
        }
      }
  }


  /**
   * Аналог getGeoLevelUsed(), но с поиском по полю adn_id.
   * @param adnId id узла.
   * @param maxResults макс. кол-во результатов.
   * @return Список указателей в неопределённом порядке.
   */
  def findIndexedPtrsForNode(adnId: String, maxResults: Int = MAX_RESULTS_DFLT)
                            (implicit ec: ExecutionContext, client: Client): Future[List[MAdnNodeGeoIndexed]] = {
    prepareSearch
      .setQuery( adnIdQuery(adnId) )
      .setRouting(adnId)
      .setSize(maxResults)
      .setFetchSource(false)
      .addField(GLEVEL_ESFN)
      .execute()
      .map { searchResp =>
        searchResp.getHits.foldLeft (List.empty[MAdnNodeGeoIndexed]) { (acc, hit) =>
          val res = MAdnNodeGeoIndexed(
            _id = hit.getId,
            glevel = Option(hit.field(GLEVEL_ESFN))
              .flatMap { sf => Option(sf.getValue[String]) }
              .flatMap { NodeGeoLevels.maybeWithName }
              .get
          )
          res :: acc
        }
      }
  }

  /** Подписчик на события удаления узла. Нужно чистить модель при удалении узла. */
  class CleanUpOnAdnNodeDelete(implicit ec: ExecutionContext, client: Client) extends SNStaticSubscriber with SnClassSubscriber {
    override def snMap = List(
      MNodeDeletedEvent.getClassifier() -> Seq(this)
    )

    override def publish(event: Event)(implicit ctx: ActorContext): Unit = {
      event match {
        case nde: MNodeDeletedEvent =>
          trace(s"Node ${nde.nodeId} deletion signal received. Let's clean-up node's geos...")
          deleteByAdnId(nde.nodeId) onComplete {
            case Success(r)  => info(s"Successfully deleted $r geos related to node ${nde.nodeId}.")
            case Failure(ex) => error("Failed to cleanup geos, related to node " + nde.nodeId, ex)
          }

        case _ => warn("Unexpected event received: " + event)
      }
    }
  }

  def renderableFilter = FilterBuilders.termFilter(GEO_JSON_COMPATIBLE_ESFN, true)
  def renderableFiltered(q: QueryBuilder) = QueryBuilders.filteredQuery(q, renderableFilter)

  /**
   * Найти все пригодные для рендера фигуры. Непригодные -- это circle, envelope. Они не имеют true значения в поле
   * совместимости с GeoJSON.
   * @param glevel На каком уровне ищем фигуры.
   * @param maxResults Макс.кол-во результатов в выдаче.
   * @param offset Сдвиг в выдаче.
   * @return Список результатов в неопределённом порядке.
   */
  def findAllRenderable(glevel: NodeGeoLevel, adnIdOpt: Option[String] = None, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                       (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( renderableQuery(glevel, adnIdOpt) )
      .setSize(maxResults)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
  }

  private def deleteAllRenderable(glevel: NodeGeoLevel, adnIdOpt: Option[String])
                                 (implicit ec: ExecutionContext, client: Client): Future[_] = {
    prepareDeleteByQuery
      .setQuery( renderableQuery(glevel, adnIdOpt) )
      .execute()
  }

  private def renderableQuery(glevel: NodeGeoLevel, adnIdOpt: Option[String]): QueryBuilder = {
    val query0 = adnIdOpt match {
      case Some(adnId) =>
        QueryBuilders.filteredQuery(adnIdQuery(adnId), glevelFilter(glevel))
      case None =>
        glevelQuery(glevel)
    }
    renderableFiltered(query0)
  }

}


import io.suggest.ym.model.MAdnNodeGeo._


final case class MAdnNodeGeo(
  adnId       : String,
  glevel      : NodeGeoLevel,
  shape       : GeoShape,
  url         : Option[String] = None,
  lastModified: DateTime = DateTime.now(),
  id          : Option[String] = None,
  versionOpt  : Option[Long] = None
) extends EsModelPlayJsonT with EsChildModelT {

  override type T = MAdnNodeGeo

  @JsonIgnore
  override def companion = MAdnNodeGeo
  @JsonIgnore
  override def parentId = adnId

  override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    var acc: FieldsJsonAcc =
      ADN_ID_ESFN -> JsString(adnId) ::
      GLEVEL_ESFN -> JsString(glevel.esfn) ::
      glevel.esfn -> shape.toPlayJson(geoJsonCompatible = false) ::
      LAST_MODIFIED_ESFN -> EsModelUtil.date2JsStr(lastModified) ::
      GEO_JSON_COMPATIBLE_ESFN -> JsBoolean(shape.shapeType.isGeoJsonCompatible) ::
      acc0
    if (url.isDefined)
      acc ::= URL_ESFN -> JsString(url.get)
    acc
  }

  /** Является ли текущий геошейп - кругом? */
  def isCircle: Boolean = shape.isInstanceOf[CircleGs]

  /** Сконвертить интанс в экземпляр указателя, пригодного для гео-поиска pre-indexed shape. */
  private def toIndexedPtr = MAdnNodeGeoIndexed(id.get, glevel)
}



trait MAdnNodeGeoJmxMBean extends EsModelJMXMBeanCommonI
final class MAdnNodeGeoJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelCommonJMXBase
  with MAdnNodeGeoJmxMBean
{
  override def companion = MAdnNodeGeo
}


/** Враппер для описания указателя на уже проиндексированный шейп. */
case class MAdnNodeGeoIndexed(_id: String, glevel: NodeGeoLevel) extends IGeoShapeIndexed {
  override def _index = MAdnNodeGeo.ES_INDEX_NAME
  override def _type  = MAdnNodeGeo.ES_TYPE_NAME
  override def name   = glevel.esfn
  override def path   = glevel.esfn
}

