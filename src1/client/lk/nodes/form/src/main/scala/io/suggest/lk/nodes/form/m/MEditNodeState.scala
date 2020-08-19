package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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
  @inline implicit def univEq: UnivEq[MEditNodeState] = UnivEq.force

  val saving = GenLens[MEditNodeState](_.saving)

  implicit class EnsOptExt( private val ensOpt: Option[MEditNodeState] ) extends AnyVal {
    def name: String = ensOpt.fold("")(_.name)
    def nameValid: Boolean = ensOpt.exists(_.nameValid)
    def isPending: Boolean = ensOpt.exists(_.saving.isPending)
    def exceptionOption = ensOpt.flatMap(_.exceptionOption)
  }

}


case class MEditNodeState(
                           override val name      : String,
                           override val nameValid : Boolean,
                           override val saving    : Pot[_]          = Pot.empty
                         )
  extends IEditNodeState[MEditNodeState]
{

  lazy val exceptionOption = saving.exceptionOption


  override def withName(name2: String) = copy(name = name2)
  override def withName(name2: String, nameValid2: Boolean) = copy(name = name2, nameValid = nameValid2)
  override def withNameValid(nameValid2: Boolean) = copy(nameValid = nameValid2)
  override def withSaving(saving2: Pot[_]) = copy(saving = saving2)

}
