package io.suggest.sc.m.in

import diode.FastEq
import diode.data.Pot
import io.suggest.routes.routes
import io.suggest.sc.m.RouteTo
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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

  def jsRouter = GenLens[MJsRouterS](_.jsRouter)
  def delayedRouteTo = GenLens[MJsRouterS](_.delayedRouteTo)

}


/** Контейнер состояния js-роутера.
  *
  * @param jsRouter Состояние js-роутера.
  * @param delayedRouteTo Возможно, что RouteTo пришла до готовности js-роутера к обработке запросов,
  *                       поэтому TailAh пришлось частично отложить экшен до готовности js-роутера.
  */
case class MJsRouterS(
                       jsRouter         : Pot[routes.type]    = Pot.empty,
                       delayedRouteTo   : Option[RouteTo]         = None,
                     ) {

  lazy val jsRouterOpt = jsRouter.toOption

}
