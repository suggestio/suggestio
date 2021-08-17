package io.suggest.n2.edge.search

import io.suggest.common.empty.EmptyProduct
import io.suggest.dev.MOsFamily
import io.suggest.es.model.{IMust, Must_t}
import io.suggest.ext.svc.MExtService
import io.suggest.geo.MGeoPoint
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicate, MPredicates}
import io.suggest.n2.media.storage.MStorage
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._

import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.15 14:42
  * Description: Модель критерия для поиска по node-edges.
  * Заданные здесь множества id-узлов и их предикатов проверяются в рамках каждого nested-объкета.
  * Для задания нескольких критериев поиска нужно несколько критериев перечислить.
  *
  * @param nodeIds id искомых узлов.
  * @param predicates искомые предикаты.
  * @param must
  * @param flag Состояние дополнительного флага в контейнера info. (v1)
  * @param flags Флаги: критерии флагов.
  *              Несколько критериев объединяются через ИЛИ.
  * @param date Поиск по индексируемой дате-времени.
  *             Несколько интервалов - объединяются через ИЛИ.
  * @param tags Критерии для поиска по тегам.
  * @param gsIntersect Данные для поиска по гео-шейпам.
  * @param nodeIdsMatchAll Каким образом трактовать nodeIds, если их >= 2?
  *                        true значит объединять запрошенные nodeId через AND.
  *                        false - OR.
  * @param extService Искомые внешние сервисы.
  * @param osFamilies Искомые операционки.
  * @param fileHashesHex Поиск по файловым хэшам.
  * @param fileMimes Поиск по mime-типам файлов.
  * @param fileSizeB Поиск по размеру (размерам) хранимых файлов.
  * @param pictureHeightPx Высота картинки.
  *                        List(x) - точное совпадение размера.
  *                        List(x1, x2) - интервал от и до.
  * @param pictureWidthPx Ширина картинки. Аналогично высоте.
  */
final case class Criteria(
                           nodeIds           : Seq[String]              = Nil,
                           predicates        : Seq[MPredicate]          = Nil,
                           must              : Must_t                   = IMust.SHOULD,
                           // info
                           flag              : Option[Boolean]          = None,
                           flags             : Seq[EdgeFlagCriteria]    = Nil,
                           date              : Seq[EsRange]             = Nil,
                           tags              : Seq[TagCriteria]         = Nil,
                           gsIntersect       : Option[IGsCriteria]      = None,
                           nodeIdsMatchAll   : Boolean                  = false,
                           geoDistanceSort   : Option[MGeoPoint]        = None,
                           extService        : Option[Seq[MExtService]] = None,
                           osFamilies        : Option[Seq[MOsFamily]]   = None,
                           // media
                           fileHashesHex     : Iterable[MHashCriteria]  = Nil,
                           fileMimes         : Iterable[String]         = Nil,
                           fileSizeB         : Iterable[Long]           = Nil,
                           fileIsOriginal    : Option[Boolean]          = None,
                           fileStorType      : Set[MStorage]            = Set.empty,
                           fileStorMetaData  : Set[String]              = Set.empty,
                           fileStorShards : Option[Set[String]]      = None,
                           // media.picture
                           pictureHeightPx   : List[Int]                = Nil,
                           pictureWidthPx    : List[Int]                = Nil,
                         )
  extends EmptyProduct
  with IMust
{

  def isContainsSort: Boolean =
    geoDistanceSort.nonEmpty

  override def toString: String = {
    val sb = new StringBuilder(128, productPrefix)
    sb.append('(')

    sb.append(
      IMust.toString(must)
    )

    val _preds = predicates
    if (_preds.nonEmpty) {
      sb.append( ",p=[" )
        .append( _preds.mkString(",") )
        .append( ']')
    }

    val _nodeIds = nodeIds
    if (_nodeIds.nonEmpty) {
      val delim = s" ${if (nodeIdsMatchAll) "&" else "|"} "
      sb.append(",nodes=[")
      for (nodeId <- _nodeIds) {
        sb.append(nodeId)
          .append(delim)
      }
      sb.append(']')
    }

    for (_flag <- flag) {
      sb.append(",flag=")
        .append( _flag )
    }

    for (_tag <- tags) {
      sb.append(",tag=")
        .append(_tag)
    }

    for (_gsCr <- gsIntersect) {
      sb.append(",gs=")
        .append(_gsCr)
    }

    for (mgp <- geoDistanceSort) {
      sb.append(",geoDistanceSort=")
        .append(mgp.toHumanFriendlyString)
    }

    sb.append(')')
      .toString()
  }

}


object Criteria extends MacroLogsImpl {

  /** Application-side edges matching against criteria.
    * Usually, everything is matched on elasticsearch-side. Here we have application-side utils.
    */
  class EdgeMatcher {

    def nodeIdsIsMatch( cr: Criteria, edge: MEdge ): Boolean = {
      cr.nodeIds.exists( edge.nodeIds.contains )
    }

    def single( cr: Criteria, nodeEdges: MNodeEdges ): Seq[MEdge] = {
      // Filtering by predicate (using MNodeEdges.edgesByPred map for speed):
      var nodeEdges0 = (if (cr.predicates.nonEmpty) {
        nodeEdges
          .withPredicate( cr.predicates: _* )
      } else {
        nodeEdges
      })
        .out

      // Filter by value:
      if (cr.nodeIds.nonEmpty) {
        nodeEdges0 = nodeEdges0.filter {
          nodeIdsIsMatch( cr, _ )
        }
      }

      // Filter by flag value:
      for (flagValue <- cr.flag) {
        nodeEdges0 = nodeEdges0.filter { e =>
          e.info.flag contains[Boolean] flagValue
        }
      }

      // Filter by ext service value:
      for (crExtServices <- cr.extService) {
        nodeEdges0 = if (crExtServices.isEmpty) {
          nodeEdges0.filter { e =>
            e.info.extService.nonEmpty
          }
        } else {
          nodeEdges0.filter { e =>
            e.info.extService.exists { extSvc =>
              crExtServices contains[MExtService] extSvc
            }
          }
        }
      }

      // TODO implement all other fields matchers?

      nodeEdges0
    }


    /** Match all edges. Obey value of .must field.
      *
      * @param crs Criterias.
      * @param nodeEdges Edges.
      * @return None, if group match failed (merge using .must failed).
      */
    def all(crs: List[Criteria], nodeEdges: MNodeEdges): Option[Set[MEdge]] = {
      __foldCriteriaStep( nodeEdges, crs, Set.empty, seenAtLeastOneShould = false, atLeastOneShouldIsTrue = false )
    }


    @tailrec
    private def __foldCriteriaStep(nodeEdges: MNodeEdges,
                                   restCrs: List[Criteria],
                                   resultEdgesAcc0: Set[MEdge],
                                   seenAtLeastOneShould: Boolean,
                                   atLeastOneShouldIsTrue: Boolean,
                                  ): Option[Set[MEdge]] = {
      restCrs match {
        case cr :: crsTail =>
          val crEdges = single( cr, nodeEdges )

          if (cr.must ==* IMust.SHOULD) {
            __foldCriteriaStep(
              nodeEdges,
              crsTail,
              resultEdgesAcc0 = resultEdgesAcc0 ++ crEdges,
              seenAtLeastOneShould = true,
              atLeastOneShouldIsTrue = atLeastOneShouldIsTrue || crEdges.nonEmpty,
            )

          } else if (cr.must ==* IMust.MUST) {
            if (crEdges.isEmpty)
              None
            else
              __foldCriteriaStep(
                nodeEdges,
                crsTail,
                resultEdgesAcc0 = resultEdgesAcc0 ++ crEdges,
                seenAtLeastOneShould = seenAtLeastOneShould,
                atLeastOneShouldIsTrue = atLeastOneShouldIsTrue,
              )

          } else {
            // MustNot
            if (crEdges.isEmpty) {
              __foldCriteriaStep( nodeEdges, crsTail, resultEdgesAcc0, seenAtLeastOneShould, atLeastOneShouldIsTrue = atLeastOneShouldIsTrue )
            } else {
              None
            }
          }

        case _ =>
          if ((seenAtLeastOneShould && atLeastOneShouldIsTrue) || !seenAtLeastOneShould) {
            Some( resultEdgesAcc0 )
          } else {
            None
          }
      }
    }

  }

}
