package io.suggest.model.n2.edge.search

import io.suggest.common.empty.{EmptyProduct, IIsNonEmpty}
import io.suggest.es.model.{IMust, Must_t}
import io.suggest.model.n2.edge.MPredicate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:42
 * Description: Критерий для поиска по node-edges.
 * Заданные здесь множества id-узлов и их предикатов проверяются в рамках каждого nested-объкета.
 * Для задания нескольких критериев поиска нужно несколько критериев перечислить.
 */

trait ICriteria extends IIsNonEmpty with IMust {

  /** id искомых узлов. */
  def nodeIds     : Seq[String]

  /** Каким образом трактовать nodeIds, если их >= 2?
    * @return true значит объединять запрошенные nodeId через AND.
    *         false - OR.
    */
  def nodeIdsMatchAll: Boolean

  /** id предикатов. */
  def predicates  : Seq[MPredicate]

  /** Критерий для поиска по тегу. */
  def tags        : Seq[TagCriteria]

  /** Состояние дополнительного флага в контейнера info. */
  def flag        : Option[Boolean]

  /** Данные для поиска по гео-шейпам. */
  def gsIntersect : Option[IGsCriteria]


  /** Тест на наличие предиката или его дочерних предикатов в списке предикатов. */
  def containsPredicate(pred: MPredicate): Boolean = {
    predicates.exists(_.eqOrHasParent(pred))
  }

  override def toString: String = {
    val sb = new StringBuilder(32, getClass.getSimpleName)
    sb.append('(')

    sb.append {
      must.fold ("should") {
        case true   => "must"
        case false  => "mustNot"
      }
    }

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

    sb.append(')')
      .toString()
  }

}


/** Дефолтовая реализация [[ICriteria]]. */
case class Criteria(
                     override val nodeIds           : Seq[String]          = Nil,
                     override val predicates        : Seq[MPredicate]      = Nil,
                     override val must              : Must_t               = IMust.SHOULD,
                     override val flag              : Option[Boolean]      = None,
                     override val tags              : Seq[TagCriteria]     = Nil,
                     override val gsIntersect       : Option[IGsCriteria]  = None,
                     override val nodeIdsMatchAll   : Boolean              = false,
                   )
  extends ICriteria
  with EmptyProduct
{
  override def toString: String = super.toString
}
