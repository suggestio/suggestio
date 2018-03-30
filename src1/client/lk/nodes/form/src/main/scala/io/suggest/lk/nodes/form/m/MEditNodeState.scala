package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.17 15:01
  * Description: Модель состояния редактирования узла.
  */

trait IEditNodeState[T <: IEditNodeState[T]] {

  /** Название узла, задаётся юзером. */
  def name      : String
  /** Флаг текущей валидности названия. */
  def nameValid : Boolean
  /** Сейчас происходит сохранение узла? */
  def saving    : Pot[_]


  def withName(name2: String): T
  def withName(name2: String, nameValid: Boolean): T
  def withNameValid(nameValid2: Boolean): T
  def withSaving(saving2: Pot[_]): T
  def withSavingPending(): T = {
    withSaving( saving.pending() )
  }

  /** Флаг общей итоговой валидности. */
  def isValid: Boolean = nameValid

}

object MEditNodeState {
  implicit def univEq: UnivEq[MEditNodeState] = UnivEq.force
}


case class MEditNodeState(
                           override val name      : String,
                           override val nameValid : Boolean,
                           override val saving    : Pot[_]          = Pot.empty
                         )
  extends IEditNodeState[MEditNodeState]
{

  override def withName(name2: String) = copy(name = name2)
  override def withName(name2: String, nameValid2: Boolean) = copy(name = name2, nameValid = nameValid2)
  override def withNameValid(nameValid2: Boolean) = copy(nameValid = nameValid2)
  override def withSaving(saving2: Pot[_]) = copy(saving = saving2)

}
