package io.suggest.model.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty, OptionUtil}
import io.suggest.model.PrefixedFn
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.geo.MNodeGeoLevel
import io.suggest.util.logs.MacroLogsImpl
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import japgolly.univeq._

import scala.collection.MapView

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

  implicit val nodeEdgesJson: Format[MNodeEdges] = {
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
  def edgesToMap1(edges: IterableOnce[MEdge]): Seq[MEdge] = {
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
  def nextOrderId(edges: IterableOnce[MEdge]): Int = {
    val iter = edges
      .iterator
      .flatMap(_.order)
    if (iter.isEmpty) 0
    else iter.max + 1
  }


  object Filters {

    def nodePredF(nodeId: String, predicate: MPredicate)(medge: MEdge): Boolean = {
      medge.nodeIds.contains(nodeId) &&
        medge.predicate ==>> predicate
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

  lazy val edgesByPred: MapView[MPredicate, Seq[MEdge]] = {
    if (out.isEmpty) {
      MapView.empty
    } else {
      (for {
        e <- out.iterator
        // Т.к. предикаты имеют иерархию, на каждый эдж может быть сразу несколько элементов в карте.
        pred <- e.predicate.meAndParentsIterator
      } yield {
        pred -> e
      })
        .to( LazyList )
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2))
    }
  }


  lazy val edgesByUid: Map[EdgeUid_t, MEdge] = {
    if (out.isEmpty) {
      Map.empty
    } else {
      (for {
        e   <- out
        uid <- e.doc.uid
      } yield {
        (uid, e)
      })
        .toMap
    }
  }


  /** Отковырять первый элемент из 2-кортежа с эджем.  */
  private def _first(e: (MEdge,_)) = e._1

  /** Найти эдж с указанным порядковым номером. */
  def withIndex(i: Int): Option[MEdge] = {
    out
      .iterator
      .zipWithIndex
      .find(_._2 == i)
      .map(_first)
  }


  def withIndexUpdated(i: Int)(f: MEdge => IterableOnce[MEdge]): Iterator[MEdge] = {
    out
      .iterator
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
    MNodeEdges.out.set(
      withPredicateIter(preds: _*)
        .to(LazyList)
    )(this)
  }
  def withoutPredicate(preds: MPredicate*): MNodeEdges = {
    MNodeEdges.out.set(
      withoutPredicateIter(preds: _*)
        .to(LazyList)
    )(this)
  }

  def withPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    if (preds.isEmpty) {
      throw new IllegalArgumentException("preds must be non-empty")
    } else {
      preds
        .iterator
        .flatMap( edgesByPred.get )
        .flatten
    }
  }


  def withoutPredicateIter(preds: MPredicate*): Iterator[MEdge] = {
    if (preds.isEmpty) {
      MNodeEdges.LOGGER.warn(s"withoutPredicateIter() called with zero args from\n${Thread.currentThread().getStackTrace.iterator.take(3).mkString("\n")}")
      out.iterator
    } else {
      //out
      //  .iterator
      //  .filterNot( MNodeEdges.Filters.predsF(preds) )
      // Оптимизировано через edgesByPred на случай больших списков эджей: так эджи фильтруются сразу группами.
      for {
        (groupPred, edgesGroup) <- edgesByPred
          .view
          .filterKeys { k =>
            val matches = preds.exists { p =>
              k ==>> p
            }
            !matches
          }
          .iterator
        edge <- edgesGroup
        // Оптимизация через карту: у предикатов есть parent-предикаты в карте, которые дублируют ряды эджей,
        // и values надо дофильтровывать, отсеивая искусственные parent-значения.
        if edge.predicate ==* groupPred
      } yield {
        edge
      }
    }
  }

  def withPredicateIterIds(pred: MPredicate*): Iterator[String] = {
    withPredicateIter(pred: _*)
      .flatMap( _.nodeIds )
  }

  def withNodeId(nodeIds: String*): Iterator[MEdge] = {
    out
      .iterator
      .filter { medge =>
        medge.nodeIds.exists(nodeIds.contains)
      }
  }

  def withNodePred(nodeId: String, predicate: MPredicate): Iterator[MEdge] = {
    edgesByPred
      .get( predicate )
      .iterator
      .flatten
      .filter( _.nodeIds contains nodeId )
  }

  def withoutNodePred(nodeId: String, predicate: MPredicate): Iterator[MEdge] = {
    out
      .iterator
      .filterNot( MNodeEdges.Filters.nodePredF(nodeId, predicate) )
  }


  /** Фильтрация по edge uid. */
  def withUid(edgeUids: EdgeUid_t*) = withUid1( edgeUids )
  def withUid1(edgeUids: Iterable[EdgeUid_t]): MNodeEdges = {
    if (edgeUids.isEmpty) {
      throw new IllegalArgumentException( "edgeUids must be non-empty" )
    } else {
      MNodeEdges.out.set(
        edgeUids
          .iterator
          .flatMap( edgesByUid.get )
          .to( LazyList )
      )(this)
    }
  }

  /** Фильтрация по отсутствую указанных edge uid. */
  def withoutUid(edgeUids: EdgeUid_t*): MNodeEdges = {
    MNodeEdges.out.set(
      MNodeEdges.edgesToMap1(
        edgeUids
          .foldLeft( edgesByUid )( _ - _ )
          .values
      )
    )(this)
  }

  /**
    * Найти и обновить с помощью функции эдж, который соответствует предикату.
    *
    * @param findF Поиск производить этим предикатом.
    * @param updateF Обновлять эдж этой фунцией.
    * @return Обновлённый экземпляр [[MNodeEdges]].
    */
  def updateAll(findF: MEdge => Boolean)(updateF: MEdge => Option[MEdge]): MNodeEdges = {
    MNodeEdges.out.modify(
      _.flatMap { e =>
        if (findF(e)) {
          updateF(e)
        } else {
          e :: Nil
        }
      }
    )(this)
  }

}
