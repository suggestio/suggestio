package io.suggest.model.n2.edge

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.model.n2.edge.search.{EdgeSearchDfltImpl, EdgeSearch}
import io.suggest.model.search.EsDynSearchStatic
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.client.Client
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

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

  /**
   * Комплексное обновление исходящих из одного узла эджей.
   * old-эджи, отсутствующие в new будут удалены.
   * new-эджи, отсутствующие в old будут созданы.
   * @param fromId1 id исходящего узла.
   * @param predicates1 предикаты, для которых эджи ищем/создаем/удаляем.
   * @param oldToIds старый набор toId.
   * @param newToIds новый набор toId.
   * @return Фьючерс.
   */
  def updateEdgesFrom(fromId1: String, predicates1: Seq[MPredicate], oldToIds: Seq[String], newToIds: Seq[String])
                     (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    if (oldToIds.isEmpty && newToIds.isEmpty) {
      Future successful Nil
    } else {

      // Готовим множества старых и новых значений поля toId
      val oldToIdsSet = oldToIds.toSet
      val newToIdsSet = newToIds.toSet

      // Если множества идентичны, то обновлять ничего не надо.
      if (oldToIdsSet == newToIdsSet) {
        Future successful Nil
      } else {

        // Ищем все существующие эджи
        val searchArgs = new EdgeSearchDfltImpl {
          override def fromId     = Seq(fromId1)
          override def predicates = predicates1
          override val toId       = oldToIds ++ newToIds
          override def limit      = predicates.size * toId.size
        }
        val searchFut = dynSearch(searchArgs)

        // Готовим множества для создания и удаления
        val crToIdsSet  = newToIdsSet -- oldToIdsSet
        val rmToIdsSet  = oldToIdsSet -- newToIdsSet

        // исполнить задуманное
        for {
          edges     <- searchFut
          bulkResp  <- {
            val curEdgesMap = edges
              .iterator
              .map { e => (e.predicate, e.toId) -> e }
              .toMap
            val bulk = client.prepareBulk()

            for (pred <- predicates1; toId <- crToIdsSet) {
              if (!(curEdgesMap contains (pred, toId))) {
                val me = MEdge(fromId = fromId1, predicate = pred, toId = toId)
                bulk.add( me.indexRequestBuilder )
              }
            }

            for (pred <- predicates1; toId <- rmToIdsSet; medge <- curEdgesMap.get((pred, toId))) {
              bulk.add( medge.prepareDelete )
            }

            bulk.execute()
          }
        } yield {
          if (bulkResp.hasFailures)
            LOGGER.warn(s"updateEdgesFrom($fromId1, $predicates1, $oldToIds, $newToIds): bulk update had errors:\n ${bulkResp.buildFailureMessage()}")
          bulkResp
        }
      }
    }
  }


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


/** Трейт экземпляра ребра. */
trait IEdge {

  /** Узел-источник. */
  def fromId      : String

  /** Предикат, т.е. что-то типа категории ребра. */
  def predicate   : MPredicate

  /** Узел-адресат. */
  def toId        : String

  /** Очередность ребра. */
  def ordering    : Option[Int]

  /** Сборка id ребра. */
  def edgeId      : String = {
    fromId + "." + predicate.strId + "." + toId
  }

}


/** Реализация модели ребер графа. */
case class MEdge(
  override val fromId      : String,
  override val predicate   : MPredicate,
  override val toId        : String,
  override val ordering    : Option[Int]   = None,
  override val versionOpt  : Option[Long]  = None
)
  extends EsModelT
  with IEdge
  with EsModelJsonWrites
{

  override type T = MEdge
  override def companion = MEdge

  override def id: Option[String] = {
    Some( edgeId )
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
