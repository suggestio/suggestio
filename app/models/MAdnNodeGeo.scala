package models

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import io.suggest.model.geo.{CircleGs, GeoShapeQuerable, GeoShape}
import io.suggest.model.{EsModel, EsModelT, EsModelMinimalStaticT}
import play.api.libs.json._
import java.{util => ju}
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

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
object MAdnNodeGeo extends EsModelMinimalStaticT with PlayMacroLogsImpl {

  override type T = MAdnNodeGeo

  /** Название поля с id узла. */
  val ADN_ID_ESFN = "adnId"

  /** Название поля, хранящего смежное гео-барахло. В модели оно неявное. Внутри -- nested-object. */
  val GEO_ESFN    = "geo"
  val URL_ESFN    = "url"
  val LAST_MODIFIED_ESFN = "lm"

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
      .foldLeft(List[DocField]()) {
        (acc, nglv)  =>
          val ngl: NodeGeoLevel = nglv
          FieldGeoShape(ngl.esfn, precision = ngl.precision)  ::  acc
      }
    List(
      FieldString(ADN_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(URL_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldDate(LAST_MODIFIED_ESFN, index = null, include_in_all = false),
      FieldNestedObject(GEO_ESFN, enabled = true, properties = nglFields)
    )
  }

  /** Десериализация данных, сохранённых в nested гео-контейнере. */
  val deserializeGeoContainer: PartialFunction[Any, (NodeGeoLevel, GeoShape)] = {
    case jmap: ju.Map[_, _] =>
      _deserializeGeoTuple(jmap.head)
    case smap: collection.Map[_, _] =>
      _deserializeGeoTuple(smap.head)
  }

  private def _deserializeGeoTuple(raw: (Any,Any)): (NodeGeoLevel, GeoShape) = {
    val (esfnRaw, shapeRaw) = raw
    val esfn = NodeGeoLevels.withName( EsModel.stringParser(esfnRaw) )
    val shape = GeoShape.deserialize(shapeRaw).get
    (esfn, shape)
  }

  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], versionOpt: Option[Long]): T = {
    val geo = deserializeGeoContainer( m(GEO_ESFN) )
    MAdnNodeGeo(
      adnId       = EsModel.stringParser( m(ADN_ID_ESFN) ),
      glevel      = geo._1,
      shape       = geo._2,
      url         = m.get(URL_ESFN).map(EsModel.stringParser),
      lastModified = m.get(LAST_MODIFIED_ESFN).fold(DateTime.now)(EsModel.dateTimeParser),
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
  def findByNode(adnId: String, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( adnIdQuery(adnId) )
      .setSize( maxResults )
      .setFrom( offset )
      .execute()
      .map { searchResp2list }
  }

  /** Сгенерить запрос для поиска совпадений в гео-полях указанного уровня. */
  def geoQuery(glevel: NodeGeoLevel, shape: GeoShapeQuerable): QueryBuilder = {
    val shapeQuery = QueryBuilders.geoShapeQuery(glevel.fullEsfn, shape.toEsShapeBuilder)
    QueryBuilders.nestedQuery(GEO_ESFN, shapeQuery)
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
      .setNoFields()
      .addField(ADN_ID_ESFN)
      .execute()
      .map { searchResp2fnList[String](_, ADN_ID_ESFN) }
  }

}


import MAdnNodeGeo._


final case class MAdnNodeGeo(
  adnId       : String,
  glevel      : NodeGeoLevel,
  shape       : GeoShape,
  url         : Option[String] = None,
  lastModified: DateTime = DateTime.now(),
  id          : Option[String] = None,
  versionOpt  : Option[Long] = None
) extends EsModelT {

  override type T = MAdnNodeGeo

  @JsonIgnore
  override def companion = MAdnNodeGeo

  override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    var acc: FieldsJsonAcc =
      ADN_ID_ESFN -> JsString(adnId) ::
      LAST_MODIFIED_ESFN -> EsModel.date2JsStr(lastModified) ::
      GEO_ESFN -> JsObject(Seq(
        glevel.esfn -> shape.toPlayJson
      ))  ::  acc0
    if (url.isDefined)
      acc ::= URL_ESFN -> JsString(url.get)
    acc
  }

  /** Дополнительные параметры сохранения (parent, ttl, etc) можно выставить через эту функцию. */
  override def saveBuilder(irb: IndexRequestBuilder): Unit = {
    super.saveBuilder(irb)
    irb.setParent(adnId)
  }

  /** Является ли текущий геошейп - кругом? */
  def isCircle: Boolean = shape.isInstanceOf[CircleGs]
}


/** Гео-уровни, т.е. отражают используемые поля и влияют на их индексацию. */
object NodeGeoLevels extends Enumeration {

  protected case class Val(esfn: String, precision: String) extends super.Val(esfn) {
    def fullEsfn = GEO_ESFN + "." + esfn
  }

  type NodeGeoLevel = Val

  val NGL_BUILDING: NodeGeoLevel        = Val("bu", "50m")
  val NGL_TOWN_DISTRICT: NodeGeoLevel   = Val("td", "800m")
  val NGL_TOWN: NodeGeoLevel            = Val("to", "5km")

  def default = NGL_BUILDING

  implicit def value2val(x: Value): NodeGeoLevel = x.asInstanceOf[NodeGeoLevel]

  def maybeWithName(x: String): Option[NodeGeoLevel] = {
    try {
      Some(withName(x))
    } catch {
      case ex: NoSuchElementException => None
    }
  }
}
