package io.suggest.ad.edit.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:30
  * Description: Корневая модель состояния формы редактора рекламной карточки.
  */
case class MAdEditRoot(
                        conf      : MAdEditFormConf,
                        doc       : MDocS
                      ) {

  /** Экспорт данных формы. */
  def toForm: MAdEditForm = ???

  def withDoc(doc: MDocS) = copy(doc = doc)

}
