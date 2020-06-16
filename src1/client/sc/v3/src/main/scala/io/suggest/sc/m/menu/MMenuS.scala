package io.suggest.sc.m.menu

import diode.FastEq
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 21:45
  * Description: Модель состояния левой панели меню.
  */
object MMenuS {

  def empty = apply()

  implicit object MMenuSFastEq extends FastEq[MMenuS] {
    override def eqv(a: MMenuS, b: MMenuS): Boolean = {
      (a.opened ==* b.opened) &&
      (a.dlApp ===* b.dlApp)
    }
  }

  @inline implicit def univEq: UnivEq[MMenuS] = UnivEq.derive

  def dlApp         = GenLens[MMenuS](_.dlApp)
  def opened        = GenLens[MMenuS](_.opened)

}


/** Состояние меню.
  *
  * @param opened Открыта ли панель меню?
  * @param dlApp Состояние пункта нативного приложения.
  */
case class MMenuS(
                   dlApp          : MDlAppDia               = MDlAppDia.empty,
                   opened         : Boolean                 = false,
                 )
