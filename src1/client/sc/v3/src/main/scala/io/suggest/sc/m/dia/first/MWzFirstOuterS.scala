package io.suggest.sc.m.dia.first

import diode.FastEq
import diode.data.Pot
import io.suggest.perm.IPermissionState
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.19 12:45
  * Description: Внешний контейнер данных first-диалога.
  * Неявно пустая модель, что необходимо для хранения постоянного состояния за пределами данных UI.
  * Здесь лежит контейнер данных для view'а и controller-only поля.
  */
object MWzFirstOuterS {

  def empty = apply()

  implicit object MWzFirstOuterSFastEq extends FastEq[MWzFirstOuterS] {
    override def eqv(a: MWzFirstOuterS, b: MWzFirstOuterS): Boolean = {
      (a.view ===* b.view) &&
      (a.perms ===* b.perms)
    }
  }

  @inline implicit def univEq: UnivEq[MWzFirstOuterS] = UnivEq.derive

  def view    = GenLens[MWzFirstOuterS](_.view)
  def perms   = GenLens[MWzFirstOuterS](_.perms)

}


/** Контейнер данных верхнего уровня для мастера первого запуска.
  *
  * @param view Контейнер данных для view'а.
  * @param perms Список текущих вопросов по пермишшенам.
  */
case class MWzFirstOuterS(
                           view       : Option[MWzFirstS]                       = None,
                           perms      : Map[MWzPhase, Pot[IPermissionState]]    = Map.empty,
                         ) {

  def isVisible = view.isDefined

  lazy val cssOpt = view.map(_.css)

}
