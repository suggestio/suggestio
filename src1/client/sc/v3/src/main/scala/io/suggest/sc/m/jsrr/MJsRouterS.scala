package io.suggest.sc.m.jsrr

import diode.FastEq
import diode.data.Pot
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.m.IScRootAction
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.19 14:25
  */
object MJsRouterS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MJsRouterS] = UnivEq.derive

  implicit object MJsRouterSFastEq extends FastEq[MJsRouterS] {
    override def eqv(a: MJsRouterS, b: MJsRouterS): Boolean = {
      (a.jsRouter ===* b.jsRouter) &&
      (a.actionsAccRev ===* b.actionsAccRev)
    }
  }

  val jsRouter = GenLens[MJsRouterS](_.jsRouter)
  val actionsAccRev = GenLens[MJsRouterS](_.actionsAccRev)

}


/** Контейнер состояния js-роутера.
  *
  * @param jsRouter Состояние js-роутера.
  * @param actionsAccRev Акк экшенов, которые нужно подождать до получения js-роутера.
  */
case class MJsRouterS(
                       jsRouter         : Pot[ScJsRoutes.type]    = Pot.empty,
                       actionsAccRev    : List[IScRootAction]     = List.empty
                     )
