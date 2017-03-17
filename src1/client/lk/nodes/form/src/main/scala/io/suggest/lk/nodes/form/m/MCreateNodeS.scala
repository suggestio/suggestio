package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.17 18:05
  * Description: Модель состояния данных добавляемого узла.
  *
  */
object MCreateNodeS {

  def empty = MCreateNodeS()

  implicit object MCreateNodeSFastEq extends FastEq[MCreateNodeS] {
    override def eqv(a: MCreateNodeS, b: MCreateNodeS): Boolean = {
      (a.name eq b.name) &&
        (a.nameValid == b.nameValid) &&
        (a.id eq b.id) &&
        (a.idValid == b.idValid) &&
        (a.saving eq b.saving)
    }
  }

}


/** Состояние формочки создания нового узла.
  *
  * @param name Название узла, задаётся юзером.
  * @param id Заданный id-узла. Для маячков, в первую очередь.
  * @param saving Сейчас происходит сохранение узла?
  * @param nameValid флаг текущей валидности названия.
  */
case class MCreateNodeS(
                             override val name       : String          = "",
                             override val nameValid  : Boolean         = false,
                             id                      : Option[String]  = None,
                             idValid                 : Boolean         = false,
                             override val saving     : Pot[_]          = Pot.empty
                           )
  extends IEditNodeState[MCreateNodeS]
{

  override def withName(name2: String) = copy(name = name2)
  override def withName(name2: String, nameValid2: Boolean) = copy(name = name2, nameValid = nameValid2)
  override def withNameValid(nameValid2: Boolean) = copy(nameValid = nameValid2)
  def withId(id2: Option[String]) = copy(id = id2)
  def withId(id2: Option[String], idValid2: Boolean) = copy(id = id2, idValid = idValid2)
  def withIdValid(idValid2: Boolean) = copy(idValid = idValid2)
  override def withSaving(saving2: Pot[_]) = copy(saving = saving2)


  override def isValid = super.isValid && idValid

}
