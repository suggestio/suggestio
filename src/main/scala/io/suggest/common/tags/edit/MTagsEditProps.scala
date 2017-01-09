package io.suggest.common.tags.edit

import io.suggest.i18n.MMessage

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 17:37
  * Description: Клиент-серверная модель js-состояния формы редактирования/выставления тегов.
  * Props -- потому что это состояние, которое ближе по смыслу и использованию к react props,
  * а так же сериализуются на сервер.
  */
case class MTagsEditProps(
                           query       : MTagsEditQueryProps    = MTagsEditQueryProps(),
                           tagsExists  : Set[String]            = Set.empty
) {

  def withQuery(q: MTagsEditQueryProps) = copy( query = q )
  def withTagsExists(te: Set[String]) = copy( tagsExists = te )

}


/** Состояние поиска. */
case class MTagsEditQueryProps(
  text    : String = "",
  errors  : Seq[MMessage] = Nil
) {

  def withText(t: String) = copy(text = t)
  def withErrors(errs: Seq[MMessage]) = copy(errors = errs)

}
