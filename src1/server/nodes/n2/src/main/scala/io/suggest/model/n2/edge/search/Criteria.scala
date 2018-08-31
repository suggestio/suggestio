package io.suggest.model.n2.edge.search

import io.suggest.common.empty.EmptyProduct
import io.suggest.es.model.{IMust, Must_t}
import io.suggest.geo.MGeoPoint
import io.suggest.model.n2.edge.MPredicate

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
  * @param flag Состояние дополнительного флага в контейнера info.
  * @param tags Критерии для поиска по тегам.
  * @param gsIntersect Данные для поиска по гео-шейпам.
  * @param nodeIdsMatchAll Каким образом трактовать nodeIds, если их >= 2?
  *                        true значит объединять запрошенные nodeId через AND.
  *                        false - OR.
  */
final case class Criteria(
                           nodeIds           : Seq[String]          = Nil,
                           predicates        : Seq[MPredicate]      = Nil,
                           must              : Must_t               = IMust.SHOULD,
                           flag              : Option[Boolean]      = None,
                           tags              : Seq[TagCriteria]     = Nil,
                           gsIntersect       : Option[IGsCriteria]  = None,
                           nodeIdsMatchAll   : Boolean              = false,
                           geoDistanceSort   : Option[MGeoPoint]    = None,
                         )
  extends EmptyProduct
  with IMust
{

  def isContainsSort: Boolean = {
    geoDistanceSort.nonEmpty
  }

  /** Тест на наличие предиката или его дочерних предикатов в списке предикатов. */
  def containsPredicate(pred: MPredicate): Boolean = {
    predicates.exists(_.eqOrHasParent(pred))
  }

  override def toString: String = {
    val sb = new StringBuilder(32, productPrefix)
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
        .append(mgp.toQsStr)
    }

    sb.append(')')
      .toString()
  }

}
