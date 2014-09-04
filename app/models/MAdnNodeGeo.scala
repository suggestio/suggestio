package models

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.event.{AdnNodeDeletedEvent, SNStaticSubscriber, SioNotifierStaticClientI}
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import io.suggest.model.geo.{CircleGs, GeoShapeQuerable, GeoShape}
import io.suggest.model._
import play.api.libs.json._
import java.{util => ju}
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
object MAdnNodeGeo extends EsChildModelStaticT with PlayMacroLogsImpl with SNStaticSubscriber {

  import LOGGER._

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
  def findByNode(adnId: String, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT, withVersions: Boolean = false)
                (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( adnIdQuery(adnId) )
      .setSize( maxResults )
      .setFrom( offset )
      .setVersion(withVersions)
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

  /** Удалить все документы, относящиеся к указанному adnId. */
  def deleteByAdnId(adnId: String)(implicit ec: ExecutionContext, client: Client): Future[Int] = {
    prepareDeleteByQuery
      .setQuery( adnIdQuery(adnId) )
      .execute()
      .map { _.iterator().size }
  }

  /** При удалении узла нужно производить чистку в этой модели. */
  override def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import _root_.util.SiowebEsUtil.client
    List(
      AdnNodeDeletedEvent.getClassifier() -> Seq(SnFunSubscriber {
        case nde: AdnNodeDeletedEvent =>
          trace(s"Node ${nde.adnId} deletion signal received. Let's clean-up node's geos...")
          deleteByAdnId(nde.adnId) onComplete {
            case Success(r)  => info(s"Successfully deleted $r geos related to node ${nde.adnId}.")
            case Failure(ex) => error("Failed to cleanup geos, related to node " + nde.adnId, ex)
          }
      })
    )
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
) extends EsModelPlayJsonT with EsChildModelT {

  override type T = MAdnNodeGeo

  @JsonIgnore
  override def companion = MAdnNodeGeo
  @JsonIgnore
  override def parentId = adnId

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



trait MAdnNodeGeoJmxMBean extends EsModelJMXMBeanCommonI
final class MAdnNodeGeoJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelCommonJMXBase
  with MAdnNodeGeoJmxMBean
{
  override def companion = MAdnNodeGeo
}

