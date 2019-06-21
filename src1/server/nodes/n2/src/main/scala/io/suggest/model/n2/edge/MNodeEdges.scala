package io.suggest.model.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty, OptionUtil}
import io.suggest.model.PrefixedFn
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.geo.MNodeGeoLevel
import io.suggest.util.logs.MacroLogsImpl
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.SeqView

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
object MNodeEdges extends IGenEsMappingProps with IEmpty with MacroLogsImpl {

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
      def OUT_INFO_GS_SHAPE_FN(ngl: MNodeGeoLevel) = _fullFn( Info.INFO_GS_SHAPE_FN(ngl) )

      // Гео-точки
      def OUT_INFO_GEO_POINTS_FN        = _fullFn( Info.INFO_GEO_POINTS_FN )

      // Внешние сервисы
      def OUT_INFO_EXT_SERVICE_FN       = _fullFn( Info.INFO_EXT_SERVICE_FN )

    }

  }

  /** Статический пустой экземпляр модели. */
  override val empty: MNodeEdges = {
    new MNodeEdges() {
      override def nonEmpty = false
    }
  }

  implicit val FORMAT: Format[MNodeEdges] = {
    (__ \ Fields.OUT_FN)
      // Парсинг в два этапа, чтобы можно отсеивать некорректные эджи.
      .formatNullable[Seq[JsObject]]
      .inmap[Seq[MEdge]](
        // Десериализация эджей: подавлять эджи, с парсингом которых возникла проблема.
        // Как правило, это просто устаревшие эджи, которые неактуальны с нарушением совместимости.
        // Например, какой-то предикат выкинули, а эджи пока остались.
        {jsObjectsOpt =>
          jsObjectsOpt
            .getOrElse(Nil)
            .iterator
            .flatMap { jsObj =>
              try {
                jsObj.validate[MEdge].fold(
                  {err =>
                    // Бывает, что эджи содержат удалённые deprecated-предикаты. Они просто дропаются.
                    LOGGER.debug(s"Not parsed edge:\n error = $err\n $jsObj")
                    Nil
                  },
                  {medge =>
                    medge :: Nil
                  }
                )
              } catch {
                case ex: Throwable =>
                  LOGGER.error(s"Edge parsing failure, skipped: $jsObj", ex)
                  Nil
              }
            }
            .toSeq
        },
        // Сериализация эджей проста и понятна:
        {edges =>
          OptionUtil.maybe( edges.nonEmpty ) {
            edges.map( Json.toJsObject(_) )
          }
        }
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
  def edgesToMap1(edges: TraversableOnce[MEdge]): Seq[MEdge] = {
    edges.toSeq
  }
  def edgesToMap(edges: MEdge*): Seq[MEdge] = {
    edgesToMap1(edges)
  }

  /**
    * Узнать следующий номер для order id.
    *
    * @param edges Эджи.
    * @return Значение Order id, пригодное для сборки нового [[MEdge]].
    */
  def nextOrderId(edges: TraversableOnce[MEdge]): Int = {
    val iter = edges.toIterator.flatMap(_.order)
    if (iter.isEmpty) 0
    else iter.max + 1
  }


  object Filters {

    def hasUidF(edgeUids: Traversable[EdgeUid_t])(medge: MEdge): Boolean = {
      edgeUids.exists( medge.doc.uid.contains )
    }

    def nodePredF(nodeId: String, predicate: MPredicate)(medge: MEdge): Boolean = {
      medge.nodeIds.contains(nodeId) &&
        medge.predicate ==>> predicate
    }

    def predsF(preds: Traversable[MPredicate])(medge: MEdge): Boolean = {
      preds.exists { p =>
        medge.predicate ==>> p
      }
    }

  }

  val out = GenLens[MNodeEdges](_.out)

}

// TODO В модели исторически сформировалось какое-то упоротое API.
//      Оно какое-то топорное, наверное можно придумать что-то по-лучше.

case class MNodeEdges(
                       out   : Seq[MEdge]    = Nil
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


  def withIndexUpdated(i: Int)(f: MEdge => TraversableOnce[MEdge]): Iterator[MEdge] = {
    iterator
      .zipWithIndex
      .flatMap { case (e, x) =>
        if (x == i) {
          f(e)
        } else {
          e :: Nil
        }
      }
  }


  def withPredicate(preds: MPredicate*): MNodeEdges = {
    withFilter( MNodeEdges.Filters.predsF(preds) )
  }
  def withoutPredicate(preds: MPredicate*): MNodeEdges = {
    withFilterNot( MNodeEdges.Filters.predsF(preds) )
  }

  // TODO deprecated withPredicate
  def withPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    withPredicate(preds: _*).iterator
  }

  // TODO deprecated withoutPredicate
  def withoutPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    withoutPredicate( preds: _* )
      .iterator
  }

  def withPredicateIterIds(pred: MPredicate*): Iterator[String] = {
    withPredicateIter(pred: _*)
      .flatMap { _.nodeIds }
  }

  def withNodeId(nodeIds: String*): Iterator[MEdge] = {
    iterator
      .filter { medge =>
        medge.nodeIds.exists(nodeIds.contains)
      }
  }

  def withNodePred(nodeId: String, predicate: MPredicate): Iterator[MEdge] = {
    iterator
      .filter( MNodeEdges.Filters.nodePredF(nodeId, predicate) )
  }

  def withoutNodePred(nodeId: String, predicate: MPredicate): Iterator[MEdge] = {
    iterator
      .filterNot( MNodeEdges.Filters.nodePredF(nodeId, predicate) )
  }


  /** Фильтрация по edge uid. */
  def withUid(edgeUids: EdgeUid_t*) = withUid1(edgeUids)
  def withUid1(edgeUids: Traversable[EdgeUid_t]): MNodeEdges = {
    withFilter( MNodeEdges.Filters.hasUidF(edgeUids) )
  }

  /** Фильтрация по отсутствую указанных edge uid. */
  def withoutUid(edgeUids: EdgeUid_t*): MNodeEdges = {
    withFilterNot( MNodeEdges.Filters.hasUidF(edgeUids) )
  }

  def outView = if (out.isInstanceOf[SeqView[_,_]]) {
    out
  } else {
    out.view
  }

  def withFilter(f: MEdge => Boolean): MNodeEdges = {
    withOut(
      out = outView.filter(f)
    )
  }
  def withFilterNot(f: MEdge => Boolean): MNodeEdges = {
    withFilter( f.andThen(!_) )
  }


  /** Докинуть эджей. */
  def withEdge(edges: MEdge*): MNodeEdges = {
    if (edges.isEmpty) {
      this
    } else {
      withOut(
        out = outView ++ edges
      )
    }
  }

  /** Докинуть коллекции эджей. */
  def withEdges(edgess: TraversableOnce[MEdge]*): MNodeEdges = {
    if (edgess.isEmpty) {
      this
    } else {
      withOut(
        out = outView ++ edgess.iterator.flatten
      )
    }
  }

  lazy val edgesByUid: Map[EdgeUid_t, MEdge] = {
    if (out.isEmpty) {
      Map.empty
    } else {
      val iter = for {
        e   <- out
        uid <- e.doc.uid
      } yield {
        (uid, e)
      }
      iter.toMap
    }
  }

  /** Название сбивает с толку, но это метод для перезаписи out на случай возможного появления других полей case-класса. */
  def withOut(out: Seq[MEdge]) = copy(out = out)

  /**
    * Найти и обновить с помощью функции эдж, который соответствует предикату.
    *
    * @param findF Поиск производить этим предикатом.
    * @param updateF Обновлять эдж этой фунцией.
    * @return Обновлённый экземпляр [[MNodeEdges]].
    */
  def updateAll(findF: MEdge => Boolean)(updateF: MEdge => Option[MEdge]): MNodeEdges = {
    withOut(
      out = this.out.flatMap { e =>
        if (findF(e)) {
          updateF(e)
        } else {
          e :: Nil
        }
      }
    )
  }

}
