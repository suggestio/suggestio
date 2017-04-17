package io.suggest.model.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.model.PrefixedFn
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model.IGenEsMappingProps
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

  implicit val FORMAT: Format[MNodeEdges] = {
    (__ \ Fields.OUT_FN).formatNullable[NodeEdgesMap_t]
      // Приведение опциональной карты к неопциональной.
      .inmap [NodeEdgesMap_t] (
        opt2ImplEmpty1F( Nil ),
        {mnes => if (mnes.isEmpty) None else Some(mnes) }
      )
      // Вместо apply используем inmap, т.к. только одно поле тут.
      .inmap [MNodeEdges](
        MNodeEdges.apply,
        _.out
      )
  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNestedObject(Fields.OUT_FN, enabled = true, properties = MEdge.generateMappingProps)
    )
  }

  def edgesToMapIter(edges: MEdge*): Seq[MEdge] = {
    edges
  }
  def edgesToMap1(edges: TraversableOnce[MEdge]): NodeEdgesMap_t = {
    edges.toSeq
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

}

// TODO В модели исторически сформировалось какое-то упоротое API.
//      Оно какое-то топорное, наверное можно придумать что-то по-лучше.

case class MNodeEdges(
  out   : NodeEdgesMap_t    = Nil
)
  extends EmptyProduct
{

  def iterator = out.iterator

  /** Отковырять первый элемент из 2-кортежа с эджем.  */
  private def _first(e: (MEdge,_)) = e._1

  /** Найти эдж с указанным порядковым номером. */
  def withIndex(i: Int): Option[MEdge] = {
    iterator
      .zipWithIndex
      .find(_._2 == i)
      .map(_first)
  }

  def withoutIndexRaw(i: Int): Iterator[(MEdge, Int)] = {
    iterator
      .zipWithIndex
      .filter(_._2 != i)
  }

  def withoutIndex(i: Int): Iterator[MEdge] = {
    withoutIndexRaw(i)
      .map(_first)
  }

  def withIndexUpdated(i: Int)(f: MEdge => TraversableOnce[MEdge]): Iterator[MEdge] = {
    iterator
      .zipWithIndex
      .flatMap { case (e, x) =>
        if (x == i) {
          f(e)
        } else {
          Seq(e)
        }
      }
  }


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
    copy(
      out = this.out.flatMap { e =>
        if (findF(e)) {
          updateF(e)
        } else {
          Seq(e)
        }
      }
    )
  }

}
