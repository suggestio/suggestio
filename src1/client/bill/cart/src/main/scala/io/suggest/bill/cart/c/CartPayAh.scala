package io.suggest.bill.cart.c

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.bill.cart.{MCartConf, MCartSubmitArgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.bill.cart.m.{CartSubmit, MCartPayS, PaySystemJsInit}
import io.suggest.i18n.MMessageException
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

import scala.util.Success

final class CartPayAh[M](
                          modelRW          : ModelRW[M, MCartPayS],
                          lkCartApi        : => ILkCartApi,
                          confRO           : ModelRO[MCartConf],
                        )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // User clicked
    case m: CartSubmit =>
      val v0 = value

      if (m.state ===* Pot.empty) {
        val fx = Effect {
          lkCartApi
            .cartSubmit(
              args = MCartSubmitArgs(
                onNodeId = confRO.value.onNodeId,
              ),
            )
            .transform { tryRes =>
              val action = m.copy(
                state = m.state withTry tryRes,
              )
              Success( action )
            }
        }
        val v2 = MCartPayS.cartSubmit
          .modify(_.pending( m.timestampMs ))(v0)
        updated( v2, fx )

      } else if (v0.cartSubmit isPendingWithStartTime m.timestampMs) {
        var modsAcc = (MCartPayS.cartSubmit set m.state)

        // Also, if ready and cart is need to pay, lets start paySystem-widget initialization:
        for {
          cartSubmitResult <- m.state
          paySystem <- cartSubmitResult.paySystem
          if paySystem.payWidgetJsScriptUrl.nonEmpty
        } {
          modsAcc = modsAcc andThen MCartPayS.paySystemInit.modify( _.ready(paySystem).pending() )
        }

        val v2 = modsAcc(v0)
        updated(v2)

      } else {
        logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (v0.cartSubmit, m) )
        noChange
      }


    // Client-side payment system's widget initialization flow:
    case m: PaySystemJsInit =>
      val v0 = value
      val v2 = MCartPayS.paySystemInit.modify(
        m.scriptLoadResult.fold(
          {errorObj =>
            _.fail( new MMessageException( errorObj ) )
          },
          {_ =>
            _.unPending
          }
        )
      )(v0)
      updated(v2)

  }

}
