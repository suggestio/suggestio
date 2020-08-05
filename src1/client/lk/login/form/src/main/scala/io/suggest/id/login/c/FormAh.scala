package io.suggest.id.login.c

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.m.{LoginShowHide, MLoginFormOverallS, SwitсhLoginTab}
import io.suggest.spa.{DoNothing, SioPages}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 18:14
  * Description: Контроллер формы логина в целом, т.е. крупными мазками.
  */
class FormAh[M](
                 modelRW      : ModelRW[M, MLoginFormOverallS],
                 routerCtl    : RouterCtl[SioPages],
               )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: SioPages.Login =>
      val v0 = value
      var updatesAcc = List.empty[MLoginFormOverallS => MLoginFormOverallS]

      if (v0.loginTab !=* m.currTab)
        updatesAcc ::= MLoginFormOverallS.loginTab.set( m.currTab )

      if (v0.returnUrl !=* m.returnUrl)
        updatesAcc ::= MLoginFormOverallS.returnUrl.set( m.returnUrl )

      updatesAcc
        .reduceOption(_ andThen _)
        .fold(noChange) { updateF =>
          val v2 = updateF( v0 )
          updated(v2)
        }


    case m: LoginShowHide =>
      val v0 = value
      if (v0.isVisible ==* m.isShow) {
        noChange
      } else {
        val v2 = MLoginFormOverallS.isVisible.set(m.isShow)(v0)
        updated(v2)
      }


    case m: SwitсhLoginTab =>
      val v0 = value
      if (v0.loginTab ==* m.tab) {
        noChange
      } else {
        val updateRouterFx = Effect.action {
          routerCtl
            .set( SioPages.Login(m.tab, v0.returnUrl) )
            .runNow()
          DoNothing
        }
        val v2 = MLoginFormOverallS.loginTab.set( m.tab )(v0)
        updated(v2, updateRouterFx)
      }

  }

}
