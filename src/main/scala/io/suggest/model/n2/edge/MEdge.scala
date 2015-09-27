package io.suggest.model.n2.edge

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.model.n2.edge.search.EdgeSearch
import io.suggest.model.search.EsDynSearchStatic
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.client.Client
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 10:27
 * Description: Модель ребер графа N2. Создана по мотивам модели zotonic m_edge.
 * @see [[MPredicates]] модель предикатов, описывающих тип рёбер.
 */
object MEdge
  extends EsModelStaticT
  with EsmV2Deserializer
  with MacroLogsImpl
  with IEsDocJsonWrites
  with EsDynSearchStatic[EdgeSearch]
{

  override type T = MEdge

  override val ES_TYPE_NAME = "e"

  val FROM_ID_FN      = "f"
  private val FROM_ID_PATH = __ \ FROM_ID_FN

  val PREDICATE_ID_FN = "p"
  private val PREDICATE_ID_PATH = __ \ PREDICATE_ID_FN

  val TO_ID_FN        = "t"
  private val TO_ID_PATH = __ \ TO_ID_FN

  val ORDER_FN        = "o"
  private val ORDER_PATH = __ \ ORDER_FN


  @deprecated("Use deserailizeOne2() instead.", "2015.sep.23")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MEdge = {
    throw new UnsupportedOperationException("Deprecated API not implemented.")
  }

  /** Полусобранный маппер из JSON в экземпляр модели. */
  private val _reads0 = {
    FROM_ID_PATH.read[String] and
    PREDICATE_ID_PATH.read[MPredicate] and
    TO_ID_PATH.read[String] and
    ORDER_PATH.readNullable[Int]
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[MEdge] = {
    _reads0 { (fromId, predicate, toId, ordering) =>
      MEdge(fromId, predicate, toId, ordering, versionOpt = meta.version)
    }
  }

  override val esDocWrites: Writes[MEdge] = (
    FROM_ID_PATH.write[String] and
    PREDICATE_ID_PATH.write[MPredicate] and
    TO_ID_PATH.write[String] and
    ORDER_PATH.writeNullable[Int]
  ) { medge =>
    (medge.fromId, medge.predicate, medge.toId, medge.ordering)
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    def fsNa(id: String) = FieldString(id, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    List(
      fsNa(FROM_ID_FN),
      fsNa(PREDICATE_ID_FN),
      fsNa(TO_ID_FN),
      FieldNumber(ORDER_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/** Реализация модели ребер графа. */
case class MEdge(
  fromId      : String,
  predicate   : MPredicate,
  toId        : String,
  ordering    : Option[Int]   = None,
  versionOpt  : Option[Long]  = None
)
  extends EsModelT
  with EsModelJsonWrites
{

  override type T = MEdge
  override def companion = MEdge

  override def id: Option[String] = {
    Some(fromId + "." + predicate.strId + "." + toId)
  }

}


// Поддержка JMX
trait MEdgeJmxMBean extends EsModelJMXMBeanI
final class MEdgeJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MEdgeJmxMBean
{
  override def companion = MEdge
}
