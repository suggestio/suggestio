package io.suggest.sc.c.android

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import cordova.plugins.intent.{CdvIntentShim, Intent}
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.android.AndroidConst
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.sc.m.{MScRoot, RouteTo}
import io.suggest.spa.DiodeUtil.Implicits.EffectsOps
import io.suggest.spa.{DAction, SioPagesUtilJs}

import java.net.URI
import scala.scalajs.js
import scala.scalajs.js.JSON

/** Controller for Android Intents processing.
  * Primarily, for NFC-intents receiving.
  */
final class ScIntentsAh[M](
                            modelRW: ModelRW[M, MScRoot],
                          )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: HandleIntent =>
      var fxAcc = List.empty[Effect]

      if (m.intent.action contains[String] AndroidConst.Intent.Action.NDEF_DISCOVERED) {
        // NDEF discovered, related to intent-filter in cordova config.xml. URL should be attached here in .data field.
        for {
          url <- m.intent.data.toOption
          if url.nonEmpty && (url startsWith HttpConst.Proto.HTTP)
          qs <- Option( URI.create(url).getRawQuery )
          if qs.nonEmpty
        } {
          val parsed = SioPagesUtilJs.parseSc3FromQs( qs )
          fxAcc ::= RouteTo( parsed ).toEffectPure
        }

      } else if (m.intent.action contains[String] AndroidConst.Intent.Action.MAIN) {
        // Do nothing

      } else {
        logger.log( ErrorMsgs.INTENT_ACTION_UNKNOWN, msg = JSON.stringify(m.intent) )
      }

      fxAcc
        .mergeEffects
        .fold {
          //logger.log( ErrorMsgs.UNKNOWN_INTENT, msg = (m, JSON.stringify(m.intent)) )
          noChange
        }( effectOnly )

  }

}

object ScIntentsAh extends Log {

  /** For Android only, two things here:
    * 1. Make onIntent() subscription.
    * 2. Call getIntent() for very-first intent from Activity launch.
    */
  def subscribeIntentsFx(dispatcher: Dispatcher) = Effect {
    // Subscribe for new intents via onIntent():
    try {
      CdvIntentShim.onIntent { intent: Intent =>
        dispatcher.dispatch( HandleIntent(intent) )
      }
    } catch { case ex: Throwable =>
      logger.error( ErrorMsgs.NATIVE_API_ERROR, ex, (CdvIntentShim, HandleIntent) )
    }

    // Return very first intent, used for launching of MainActivity:
    CdvIntentShim
      .getIntentF()
      .map( HandleIntent )
  }

}


sealed trait IIntentAction extends DAction

/** Intent received. Action to process one intent. */
case class HandleIntent( intent: Intent ) extends IIntentAction {
  override def toString =
    productPrefix + "(" + JSON.stringify(intent, null: js.Array[js.Any], space = 2) + ")"
}
