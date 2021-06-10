package io.suggest.nfc

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import diode.data.Pot
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import monocle.macros.GenLens


/** Diode-controller for NFC-related stuff. */
class NfcAh[M](
                modelRW           : ModelRW[M, MNfcState],
                nfcApiOpt         : () => Option[INfcApi],
                dispatcher        : Dispatcher,
                onNdefFoundF      : NdefMessage => Effect,
              )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Start/stop NFC scanning.
    case m: NfcScan =>
      val v0 = value
      val _nfcApiOpt = nfcApiOpt()

      if (v0.scanning.isPending) {
        logger.warn( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0) )
        noChange

      } else _nfcApiOpt.fold {
        logger.warn( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = (m, v0, _nfcApiOpt) )
        noChange

      } { nfcApi =>
        val v2 = MNfcState.scanning.modify(_.pending())(v0)

        if (m.enabled) {
          // Start NFC sensor
          val fx = Effect.action {
            val props = NfcScanProps(
              onMessage = { ndefMessage =>
                dispatcher.dispatch( NfcFound(ndefMessage) )
              },
              onError = Some { nfcError =>
                dispatcher.dispatch( NfcError(nfcError) )
              },
            )
            NfcSetScanning( v0.scanning.ready(nfcApi.scan(props)) )
          }
          updatedSilent( v2, fx )

        } else (for {
          scanning <- v0.scanning.toOption
          cancelF <- scanning.cancel
        } yield {
          // Stop NFC scanning:
          val fx = Effect.action {
            cancelF()
            NfcSetScanning( Pot.empty )
          }
          updatedSilent( v2, fx )
        }) getOrElse {
          logger.log( ErrorMsgs.CANNOT_CLOSE_SOMETHING, msg = (m, v0.scanning) )
          noChange
        }
      }


    // Update .scanning value
    case m: NfcSetScanning =>
      val v0 = value
      val v2 = (MNfcState.scanning set m.scanning)(v0)
      updatedSilent( v2 )


    // Found NFC NDEF data.
    case m: NfcFound =>
      // TODO iOS: check if continious scanning possible? Or to reset state.scanning if NDEF-data received/found?
      val fx = onNdefFoundF( m.ndefMessage )
      effectOnly( fx )


    // Problems with NFC.
    case m: NfcError =>
      logger.warn( ErrorMsgs.NFC_API_ERROR, msg = m )
      noChange

  }

}


case class MNfcState(
                      scanning          : Pot[NfcPendingState]           = Pot.empty,
                    )
object MNfcState {
  def scanning = GenLens[MNfcState]( _.scanning )
}
