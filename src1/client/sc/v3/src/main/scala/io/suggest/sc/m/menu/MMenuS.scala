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

  def default = MMenuS()

  implicit object MMenuSFastEq extends FastEq[MMenuS] {
    override def eqv(a: MMenuS, b: MMenuS): Boolean = {
      (a.opened ==* b.opened) &&
      (a.nativeApp ===* b.nativeApp)
    }
  }

  @inline implicit def univEq: UnivEq[MMenuS] = UnivEq.derive

  val opened        = GenLens[MMenuS](_.opened)
  val nativeApp     = GenLens[MMenuS](_.nativeApp)

}


/** Состояние меню.
  *
  * @param opened Открыта ли панель меню?
  * @param nativeApp Состояние пункта нативного приложения.
  */
case class MMenuS(
                   nativeApp      : Option[MMenuNativeApp]    = None,
                   opened         : Boolean                     = false,
                 )
