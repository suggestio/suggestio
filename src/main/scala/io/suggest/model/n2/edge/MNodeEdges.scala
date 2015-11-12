package io.suggest.model.n2.edge

import io.suggest.common.EmptyProduct
import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 11:00
 * Description: Подмодель для [[io.suggest.model.n2.node.MNode]] для хранения эджей прямо
 * внутри узла. В изначальной задумке N2-архитектуры модель MEdge была полностью отдельной,
 * а до N2 эджи по суди описывались отдельными конкретными полями.
 * MEdge создало проблемы с транзакционным обновлением, когда индексы эджей не успевали обновится
 * при рендере ответа формы (POST-RDR-GET), ответ отображал наполовину старые данные.
 * Так же, это затрудняет поиск.
 *
 * Обновлённый вариант N2-архитектуры денормализует MEdge в MNode, т.е. по сути исходящие
 * эджи возвращаются внутрь моделей, из которых они исходят. Это как бы золотая середина
 * для исходной архитектуры и новой.
 */
object MNodeEdges extends IGenEsMappingProps {

  object Fields {

    val OUT_FN = "out"

    object Out extends PrefixedFn {
      override protected def _PARENT_FN = OUT_FN

      import MEdge.Fields._

      // Префиксируем поля в out-объектах.
      def OUT_PREDICATE_FN  = _fullFn( PREDICATE_FN )
      def OUT_NODE_ID_FN    = _fullFn( NODE_ID_FN )
      def OUT_ORDER_FN      = _fullFn( ORDER_FN )
      def OUT_INFO_SLS_FN   = _fullFn( Info.INFO_SLS_FN )
      def OUT_INFO_FLAG_FN  = _fullFn( Info.FLAG_FN )
    }

  }

  /** Статический пустой экземпляр модели. */
  val empty: MNodeEdges = {
    new MNodeEdges() {
      override def nonEmpty = false
    }
  }

  val EMAP_FORMAT: Format[NodeEdgesMap_t] = Format(
    Reads.of[Iterable[MEdge]]
      .map[NodeEdgesMap_t] { edges =>
        edges
          .iterator
          .map { e => e.toEmapKey -> e }
          .toMap
      },
    Writes[NodeEdgesMap_t] { emap =>
      Json.toJson( emap.values )
    }
  )

  implicit val FORMAT: Format[MNodeEdges] = {
    (__ \ Fields.OUT_FN).formatNullable[NodeEdgesMap_t]
      // Приведение опциональной карты к неопциональной.
      .inmap [NodeEdgesMap_t] (
        _ getOrElse Map.empty,
        {mnes => if (mnes.isEmpty) None else Some(mnes) }
      )
      // Вместо apply используем inmap, т.к. только одно поле тут.
      .inmap [MNodeEdges](
        MNodeEdges.apply,
        _.out
      )
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNestedObject(Fields.OUT_FN, enabled = true, properties = MEdge.generateMappingProps)
    )
  }

  def edgesToMapIter(edges: MEdge*): Iterator[(NodeEdgesMapKey_t, MEdge)] = {
    edgesToMapIter1(edges)
  }
  def edgesToMapIter1(edges: TraversableOnce[MEdge]): Iterator[(NodeEdgesMapKey_t, MEdge)] = {
    edges.toIterator
      .map { edge => edge.toEmapKey -> edge }
  }
  def edgesToMap1(edges: TraversableOnce[MEdge]): NodeEdgesMap_t = {
    edgesToMapIter1(edges)
      .toMap
  }
  def edgesToMap(edges: MEdge*): NodeEdgesMap_t = {
    edgesToMap1(edges)
  }

}


case class MNodeEdges(
  out   : NodeEdgesMap_t    = Map.empty
)
  extends EmptyProduct
{

  def withPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    out.valuesIterator
      .filter { medge =>
        preds.exists { p =>
          medge.predicate ==>> p
        }
      }
  }

  def withoutPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    out.valuesIterator
      .filter { e =>
        !preds.exists { p =>
          e.predicate ==>> p
        }
      }
  }

  def withPredicateIterIds(pred: MPredicate*): Iterator[String] = {
    withPredicateIter(pred: _*)
      .map { _.nodeId }
  }

  def withNodeId(nodeIds: String*): Iterator[MEdge] = {
    out.valuesIterator
      .filter { medge =>
        nodeIds contains medge.nodeId
      }
  }

}
