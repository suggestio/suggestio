package io.suggest.sc.controller.showcase

import cordova.plugins.appminimize.CdvAppMinimize
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.sc.model.MScRoot
import io.suggest.sc.util.ScRoutingUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.{DoNothing, HwBackBtn}
import org.scalajs.dom

/** Showcase hardware buttons hi-level controller. */
final class ScHwButtonsAh[M](
                              modelRW: ModelRW[M, MScRoot],
                            )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Hardrware "Back" button pressed action.
    case m @ HwBackBtn =>
      val v0 = value

      // TODO dia.nodes.circuit: Need to optimize out this crunch, so js-router should process such situation.
      //      Need to add support LkNodes in js-router in same way as login-form (but more complex).
      v0.dialogs.nodes.circuit
        .map { nodesCircuit =>
          val fx = Effect.action {
            nodesCircuit.dispatch( m.asInstanceOf[HwBackBtn.type] )
            DoNothing
          }
          effectOnly(fx)
        }
        .getOrElse {
          // cordova (android): When pressed BACK, hide opened panels or dialogs, or minize application into background.
          val plat = v0.dev.platform
          if (
            plat.isCordova &&
              // TODO m.RouteTo.isBack - нажата кнопка Back?
              ScRoutingUtil.root_internals_info_currRoute_LENS
                .get(v0)
                .exists { m => !m.isSomeThingOpened }
          ) {
            if (plat.isUsingNow && plat.isReady) {
              val minimizeFx = Effect.action {
                CdvAppMinimize.minimize()
                DoNothing
              }
              effectOnly( minimizeFx )

            } else {
              noChange
            }

          } else {
            val goBackFx = Effect.action {
              dom.window.history.back()
              DoNothing
            }
            effectOnly( goBackFx )
          }
        }

  }

}
