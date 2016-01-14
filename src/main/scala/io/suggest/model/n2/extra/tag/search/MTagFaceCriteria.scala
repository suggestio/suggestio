package io.suggest.model.n2.extra.tag.search

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 19:14
  * Description: Модель критериев поиска тегов по лицам.
  */
trait ITagFaceCriteria {

  /** Значение искомого тега. */
  def face: String

  /** Последний терм лица тега расценивать как префикс?
    * Используется для поиска по мере набора. */
  def isPrefix: Boolean

  /**
   * Отработка объединения через bool query:
   * @return None => should,
   *         Some(true) => must,
   *         Some(false) => mustNot
   */
  def must: Option[Boolean]

}


/** Дефолтовая реализация модели критерия поиска тега [[ITagFaceCriteria]]. */
case class MTagFaceCriteria(
  override val face         : String,
  override val isPrefix     : Boolean,
  override val must         : Option[Boolean] = None
)
  extends ITagFaceCriteria
