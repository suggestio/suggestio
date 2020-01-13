package io.suggest.n2.edge.search

import io.suggest.es.model.{IMust, Must_t}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.16 18:47
  */


/** Модель критерия поиска тегов в эджах.
  *
  * @param face Значение искомого тега.
  * @param isPrefix Последний терм лица тега расценивать как префикс?
  * Используется для поиска по мере набора.
  * @param exact Искать по точному совпадению, т.е. по raw-полю.
  * @param must
  */
case class TagCriteria(
  face                      : String,
  isPrefix                  : Boolean,
  exact                     : Boolean         = false,
  override val must         : Must_t          = IMust.SHOULD
)
  extends IMust
