package io.suggest.model.n2.edge.search

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.16 18:47
  * Description: Модель критерия поиска тегов в эджах.
  */
trait ITagCriteria {

  /** Значение искомого тега. */
  def face: String

  /** Последний терм лица тега расценивать как префикс?
    * Используется для поиска по мере набора. */
  def isPrefix: Boolean


  override def toString: String = {
    getClass.getSimpleName + "(" + face + "," + isPrefix + ")"
  }

}


/** Дефолтовая реализация модели [[ITagCriteria]]. */
case class TagCriteria(
  override val face         : String,
  override val isPrefix     : Boolean
)
  extends ITagCriteria
