package io.suggest.common.tags.edit

import io.suggest.i18n.MMessage

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 17:37
  * Description: Клиент-серверная модель js-состояния формы редактирования/выставления тегов.
  */
case class MTagsEditS(
  query       : MTagsSearchS    = MTagsSearchS(),
  tagsExists  : Set[String]     = Set.empty
) {

  def withQuery(q: MTagsSearchS) = copy( query = q )
  def withTagsExists(te: Set[String]) = copy( tagsExists = te )

}


/** Состояние поиска. */
case class MTagsSearchS(
  text    : String = "",
  errors  : Seq[MMessage] = Nil
) {

  def withText(t: String) = copy(text = t)
  def withErrors(errs: Seq[MMessage]) = copy(errors = errs)

}
