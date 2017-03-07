package io.suggest.lk.nodes.form.m

import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.17 18:05
  * Description: Модель состояния данных добавляемого узла.
 *
  * @param name Название узла, задаётся юзером.
  * @param id Заданный id-узла. Для маячков, в первую очередь.
  * @param saving Сейчас происходит сохранение узла?
  */
case class MAddSubNodeState(
                            name      : String          = "",
                            nameValid : Boolean         = false,
                            id        : Option[String]  = None,
                            idValid   : Boolean         = false,
                            saving    : Pot[_]          = Pot.empty
                           ) {

  def withName(name2: String) = copy(name = name2)
  def withNameValid(nameValid2: Boolean) = copy(nameValid = nameValid2)
  def withId(id2: Option[String]) = copy(id = id2)
  def withIdValid(idValid2: Boolean) = copy(idValid = idValid2)
  def withSaving(saving2: Pot[_]) = copy(saving = saving2)


  def isValid = nameValid && idValid

}
