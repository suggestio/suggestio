package io.suggest.n2.edge

import io.suggest.common.empty.{EmptyProduct, IEmpty, OptionUtil}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
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
 * Description: Подмодель для [[io.suggest.n2.node.MNode]] для хранения эджей прямо
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
object MNodeEdges
  extends IEsMappingProps
  with IEmpty
  with MacroLogsImpl
{

  override type T = MNodeEdges

  object Fields {

    val OUT_FN = "out"

    object Out extends PrefixedFn {
      override protected def _PARENT_FN = OUT_FN

      import MEdge.Fields._

      // Префиксируем поля в out-объектах.
      def O_PREDICATE_FN  = _fullFn( PREDICATE_FN )
      def O_NODE_ID_FN    = _fullFn( NODE_ID_FN )
      def O_ORDER_FN      = _fullFn( ORDER_FN )


      // Info
      def O_INFO_FLAG_FN  = _fullFn( Info.FLAG_FN )

      // Теги
      def O_INFO_TAGS_FN        = _fullFn( Info.TAGS_FN )
      def O_INFO_TAGS_RAW_FN    = _fullFn( Info.TAGS_RAW_FN )

      // Гео-шейпы
      def O_INFO_GS_FN                = _fullFn( Info.INFO_GS_FN )
      def O_INFO_GS_GLEVEL_FN         = _fullFn( Info.INFO_GS_GLEVEL_FN )
      def O_INFO_GS_GJSON_COMPAT_FN   = _fullFn( Info.INFO_GS_GJSON_COMPAT_FN )
      def O_INFO_GS_SHAPE_FN(ngl: MNodeGeoLevel) = _fullFn( Info.INFO_GS_SHAPE_FN(ngl) )

      // Гео-точки
      def O_INFO_GEO_POINTS_FN        = _fullFn( Info.INFO_GEO_POINTS_FN )

      // Внешние сервисы
      def O_INFO_EXT_SERVICE_FN       = _fullFn( Info.INFO_EXT_SERVICE_FN )
      def O_INFO_OS_FAMILY_FN         = _fullFn( Info.INFO_OS_FAMILY_FN )



      // Edge media
      import MEdge.Fields.{Media => EM}

      def O_MEDIA_FM_MIME_FN = _fullFn( EM.MEDIA_FM_MIME_FN )
      def O_MEDIA_FM_MIME_AS_TEXT_FN = _fullFn( EM.MEDIA_FM_MIME_AS_TEXT_FN )

      /** Full FN nested-поля с хешами. */
      def O_MEDIA_FM_HASHES_FN = _fullFn( EM.MEDIA_FM_HASHES_FN )

      def O_MEDIA_FM_HASHES_TYPE_FN = _fullFn( EM.MEDIA_FM_HASHES_TYPE_FN )
      def O_MEDIA_FM_HASHES_VALUE_FN = _fullFn( EM.MEDIA_FM_HASHES_VALUE_FN )

      def O_MEDIA_FM_SIZE_B_FN = _fullFn( EM.MEDIA_FM_SIZE_B_FN )

      def O_MEDIA_FM_IS_ORIGINAL_FN = _fullFn( EM.MEDIA_FM_IS_ORIGINAL_FN )

      def O_MEDIA_S_TYPE_FN = _fullFn( EM.MEDIA_S_TYPE_FN )
      def O_MEDIA_S_DATA_META_FN = _fullFn( EM.MEDIA_S_DATA_META_FN )

    }

  }

  /** Статический пустой экземпляр модели. */
  override def empty = apply()

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


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.OUT_FN -> FObject.nested( MEdge.esMappingProps ),
    )
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
      (medge.nodeIds contains nodeId) &&
      medge.predicate ==>> predicate
    }

  }

  val out = GenLens[MNodeEdges](_.out)

}

// TODO В модели исторически сформировалось какое-то упоротое API.
//      Оно какое-то топорное, наверное можно придумать что-то по-лучше.

final case class MNodeEdges(
                             out   : Seq[MEdge]    = Nil
                           )
  extends EmptyProduct
{

  /** Поиск по предикату - частая операция. Тут - карта для оптимизации данного действа.
    * Следует помнить, что эта карта трудно-обратима назад в эджи без шаманства: она содержит в себе дублирующиеся значения, т.к. предикаты имеют наследственность. */
  lazy val edgesByPred: MapView[MPredicate, Seq[MEdge]] = {
    if (out.isEmpty) {
      MapView.empty
    } else {
      (for {
        e <- out.iterator
        // Т.к. предикаты имеют иерархию, на каждый эдж может быть сразу несколько элементов в карте.
        pred <- e.predicate.meAndParents
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
        uid <- e.doc.id
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

  def withPredicateIter(preds: MPredicate*): Iterator[MEdge] =
    withPredicateIter1( preds )
  def withPredicateIter1(preds: IterableOnce[MPredicate]): Iterator[MEdge] = {
    if (preds.isEmpty) {
      throw new IllegalArgumentException("preds must be non-empty")
    } else {
      // В значениях edgesByPred содержатся эджи-дубликаты. Если в preds ошибочно содержатся и родительские,
      // и дочерние элементы по одной линии, то дубликаты будут на выходе withPredicates*().
      // Поэтому дедублицируем предикаты в пользу child-элементов через mostChildest():
      preds
        .mostChildest
        .iterator
        .flatMap( edgesByPred.get )
        // TODO Если preds содержит
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
      // Если пришёл пустой список, то вероятно, что что-то не так: в лучшем случае неоптимально, в худшем - логическая ошибка.
      MNodeEdges.LOGGER.warn(
        "withUid1([]): Edge UIDs are empty from:\n " +
          Thread
            .currentThread()
            .getStackTrace
            .iterator
            .drop(1)
            .take(4)
            .mkString("\n ")
      )
      MNodeEdges.empty
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
