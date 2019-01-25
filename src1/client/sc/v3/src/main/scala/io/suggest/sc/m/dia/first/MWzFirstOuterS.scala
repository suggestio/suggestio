package io.suggest.sc.m.dia.first

import io.suggest.ueq.UnivEqUtil._
import diode.FastEq
import diode.data.Pot
import io.suggest.perm.IPermissionState
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.19 12:45
  * Description: Внешний контейнер данных first-диалога.
  * Здесь лежит контейнер данных для view'а и controller-only поля.
  */
object MWzFirstOuterS {

  implicit object MWzFirstOuterSFastEq extends FastEq[MWzFirstOuterS] {
    override def eqv(a: MWzFirstOuterS, b: MWzFirstOuterS): Boolean = {
      (a.view ===* b.view) &&
      (a.perms ===* b.perms)
    }
  }

  implicit def univEq: UnivEq[MWzFirstOuterS] = UnivEq.derive

}


/** Контейнер данных верхнего уровня для мастера первого запуска.
  *
  * @param view Контейнер данных для view'а.
  * @param perms Список текущих вопросов по пермишшенам.
  */
case class MWzFirstOuterS(
                           view       : MWzFirstS,
                           perms      : Map[MWzPhase, Pot[IPermissionState]]   = Map.empty,
                         ) {

  def withView(view: MWzFirstS) = copy(view = view)
  def withPerms(perms: Map[MWzPhase, Pot[IPermissionState]]) = copy(perms = perms)

}
