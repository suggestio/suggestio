package io.suggest.model.n2.edge

import io.suggest.common.empty.{IEmpty, EmptyProduct}
import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.common.empty.EmptyUtil._
import io.suggest.ym.model.NodeGeoLevel
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
object MNodeEdges extends IGenEsMappingProps with IEmpty {

  override type T = MNodeEdges

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

      // Теги
      def OUT_INFO_TAGS_FN        = _fullFn( Info.TAGS_FN )
      def OUT_INFO_TAGS_RAW_FN    = _fullFn( Info.TAGS_RAW_FN )

      // Гео-шейпы
      def OUT_INFO_GS_FN                = _fullFn( Info.INFO_GS_FN )
      def OUT_INFO_GS_GLEVEL_FN         = _fullFn( Info.INFO_GS_GLEVEL_FN )
      def OUT_INFO_GS_GJSON_COMPAT_FN   = _fullFn( Info.INFO_GS_GJSON_COMPAT_FN )
      def OUT_INFO_GS_SHAPE_FN(ngl: NodeGeoLevel) = _fullFn( Info.INFO_GS_SHAPE_FN(ngl) )

      // Гео-точки
      def OUT_INFO_GEO_POINTS_FN        = _fullFn( Info.INFO_GEO_POINTS_FN )

    }

  }

  /** Статический пустой экземпляр модели. */
  override val empty: MNodeEdges = {
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
        opt2ImplEmpty1F( Map.empty ),
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
    for (edge <- edges.toIterator) yield {
      edge.toEmapKey -> edge
    }
  }
  def edgesToMap1(edges: TraversableOnce[MEdge]): NodeEdgesMap_t = {
    edgesToMapIter1(edges)
      .toMap
  }
  def edgesToMap(edges: MEdge*): NodeEdgesMap_t = {
    edgesToMap1(edges)
  }

  /**
    * Узнать следующий номер для order id.
    *
    * @param edges Эджи.
    * @return Значение Order id, пригодное для сборки нового [[IEdge]].
    */
  def nextOrderId(edges: TraversableOnce[IEdge]): Int = {
    val iter = edges.toIterator.flatMap(_.order)
    if (iter.isEmpty) 0 else iter.max + 1
  }
  def nextOrderId(edgeMap: NodeEdgesMap_t): Int = {
    nextOrderId( edgeMap.valuesIterator )
  }

}


case class MNodeEdges(
  out   : NodeEdgesMap_t    = Map.empty
)
  extends EmptyProduct
{

  def iterator = out.valuesIterator

  def withPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    iterator
      .filter { medge =>
        preds.exists { p =>
          medge.predicate ==>> p
        }
      }
  }

  def withoutPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    iterator
      .filter { e =>
        !preds.exists { p =>
          e.predicate ==>> p
        }
      }
  }

  def withPredicateIterIds(pred: MPredicate*): Iterator[String] = {
    withPredicateIter(pred: _*)
      .flatMap { _.nodeIds}
  }

  def withNodeId(nodeIds: String*): Iterator[MEdge] = {
    iterator
      .filter { medge =>
        medge.nodeIds.exists(nodeIds.contains)
      }
  }

  def withNodePred(nodeId: String, predicate: MPredicate): Iterator[MEdge] = {
    iterator
      .filter { medge =>
        medge.nodeIds.contains(nodeId)  &&  medge.predicate ==>> predicate
      }
  }

  def withoutNodePred(nodeId: String, predicate: MPredicate): Iterator[MEdge] = {
    iterator
      .filterNot { medge =>
        medge.nodeIds.contains(nodeId)  &&  medge.predicate ==>> predicate
      }
  }

  /**
    * Найти и обновить с помощью функции эдж, который соответствует предикату.
    *
    * @param findF Поиск производить этим предикатом.
    * @param updateF Обновлять эдж этой фунцией.
    * @return Обновлённый экземпляр [[MNodeEdges]].
    */
  def updateFirst(findF: MEdge => Boolean)(updateF: MEdge => Option[MEdge]): MNodeEdges = {
    val (k0, v0) = out
      .iterator
      .find { case (k, v) =>
        findF(v)
      }
      .get
    val v1Opt = updateF(v0)

    val out1 = v1Opt.fold [NodeEdgesMap_t] {
      out - k0
    } { v1 =>
      out.updated(k0, v1)
    }

    copy(
      out = out1
    )
  }

}
