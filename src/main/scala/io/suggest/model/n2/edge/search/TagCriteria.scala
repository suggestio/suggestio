package io.suggest.model.n2.edge.search

import io.suggest.model.es.IMust

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.16 18:47
  * Description: Модель критерия поиска тегов в эджах.
  */
trait ITagCriteria extends IMust {

  /** Значение искомого тега. */
  def face: String

  /** Последний терм лица тега расценивать как префикс?
    * Используется для поиска по мере набора. */
  def isPrefix: Boolean

  /** Искать по точному совпадению, т.е. по raw-полю. */
  def exact: Boolean

  override def toString: String = {
    s"${getClass.getSimpleName}($face,$isPrefix,$must,$exact)"
  }

}


/** Дефолтовая реализация модели [[ITagCriteria]]. */
case class TagCriteria(
  override val face         : String,
  override val isPrefix     : Boolean,
  override val exact        : Boolean         = false,
  override val must         : Option[Boolean] = None
)
  extends ITagCriteria
