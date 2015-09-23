package io.suggest.model.n2.edge

import io.suggest.model._
import io.suggest.util.MacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map

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
  //with EsDynSearchStatic[MEdgeSearch]
{

  override type T = MEdge

  override val ES_TYPE_NAME = "e"

  val FROM_ID_FN      = "f"
  val PREDICATE_ID_FN = "p"
  val TO_ID_FN        = "t"
  val ORDER_FN        = "o"


  @deprecated("Use deserailizeOne2() instead.", "2015.sep.23")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MEdge = {
    throw new UnsupportedOperationException("Deprecated API not implemented.")
  }

  /** Полусобранный маппер из JSON в экземпляр модели. */
  private val _reads0 = {
    (__ \ FROM_ID_FN).read[String] and
    (__ \ PREDICATE_ID_FN).read[MPredicate] and
    (__ \ TO_ID_FN).read[String] and
    (__ \ ORDER_FN).readNullable[Int]
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[MEdge] = {
    _reads0 { (fromId, predicate, toId, ordering) =>
      MEdge(fromId, predicate, toId, ordering)
    }
  }

  override val esDocWrites: Writes[MEdge] = (
    (__ \ FROM_ID_FN).write[String] and
    (__ \ PREDICATE_ID_FN).write[MPredicate] and
    (__ \ TO_ID_FN).write[String] and
    (__ \ ORDER_FN).writeNullable[Int]
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
