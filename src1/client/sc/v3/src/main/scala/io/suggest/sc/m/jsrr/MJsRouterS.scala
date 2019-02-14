package io.suggest.sc.m.jsrr

import diode.FastEq
import diode.data.Pot
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.m.RouteTo
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
      (a.delayedRouteTo ===* b.delayedRouteTo)
    }
  }

  val jsRouter = GenLens[MJsRouterS](_.jsRouter)
  val delayedRouteTo = GenLens[MJsRouterS](_.delayedRouteTo)

}


/** Контейнер состояния js-роутера.
  *
  * @param jsRouter Состояние js-роутера.
  * @param delayedRouteTo Возможно, что RouteTo пришла до готовности js-роутера к обработке запросов,
  *                       поэтому TailAh пришлось частично отложить экшен до готовности js-роутера.
  */
case class MJsRouterS(
                       jsRouter         : Pot[ScJsRoutes.type]    = Pot.empty,
                       delayedRouteTo   : Option[RouteTo]         = None,
                     )
