package io.suggest.model.n2.edge.search

import io.suggest.common.empty.{EmptyProduct, IIsNonEmpty}
import io.suggest.model.n2.edge.MPredicate
import io.suggest.model.sc.common.SlNameTokenStr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:42
 * Description: Критерий для поиска по node-edges.
 * Заданные здесь множества id-узлов и их предикатов проверяются в рамках каждого nested-объкета.
 * Для задания нескольких критериев поиска нужно несколько критериев перечислить.
 */

trait ICriteria extends IIsNonEmpty {

  /** id искомых узлов. */
  def nodeIds     : Seq[String]

  /** id предикатов. */
  def predicates  : Seq[MPredicate]

  /** Искомые уровни отображения. */
  def sls         : Seq[SlNameTokenStr]

  /** Флаг наличия любого уровня или отсутствия всех уровней отображения. */
  def anySl       : Option[Boolean]

  /** Критерий для поиска по тегу. */
  def tags        : Option[ITagCriteria]

  /**
   * Вместо should clause будет использована must или mustNot для true или false соответственно.
   * Т.е. тут можно управлять семантикой объединения нескольких критериев, как если бы [OR, AND, NAND].
   *
   * @return None для should. Хотя бы один из should-clause всегда должен быть истинным.
   *         Some(true) -- обязательный clause, должна обязательно быть истинной.
   *         Some(false) -- негативный clause, т.е. срабатывания выкидываются из выборки результатов.
   */
  def must        : Option[Boolean]

  /** Состояние дополнительного флага в контейнера info. */
  def flag        : Option[Boolean]


  /** Тест на наличие предиката или его дочерних предикатов в списке предикатов. */
  def containsPredicate(pred: MPredicate): Boolean = {
    predicates.exists(_.eqOrHasParent(pred))
  }

  override def toString: String = {
    val sb = new StringBuilder(32, getClass.getSimpleName)
    sb.append('(')

    sb.append {
      must match {
        case None         => "should"
        case Some(true)   => "must"
        case Some(false)  => "mustNot"
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
      sb.append(",nodes=[")
      for (nodeId <- _nodeIds) {
        sb.append(nodeId).append(',')
      }
      sb.append(']')
    }

    val _sls = sls
    if (_sls.nonEmpty) {
      sb.append( ",sls=[" )
        .append( _sls.mkString(",") )
        .append( ']' )
    }

    for (_anySl <- anySl) {
      sb.append(",anySl=")
        .append(_anySl)
    }

    for (_flag <- flag) {
      sb.append(",flag=")
        .append( _flag )
    }

    for (_tag <- tags) {
      sb.append(",tag=")
        .append(_tag)
    }

    sb.append(')')
      .toString()
  }

}


/** Дефолтовая реализация [[ICriteria]]. */
case class Criteria(
  override val nodeIds     : Seq[String]          = Nil,
  override val predicates  : Seq[MPredicate]      = Nil,
  override val sls         : Seq[SlNameTokenStr]  = Nil,
  override val anySl       : Option[Boolean]      = None,
  override val must        : Option[Boolean]      = None,
  override val flag        : Option[Boolean]      = None,
  override val tags        : Option[ITagCriteria] = None
)
  extends ICriteria
  with EmptyProduct
{
  override def toString: String = super.toString
}
