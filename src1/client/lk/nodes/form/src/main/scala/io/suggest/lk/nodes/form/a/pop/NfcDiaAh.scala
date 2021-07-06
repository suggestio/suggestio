package io.suggest.lk.nodes.form.a.pop

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.nodes.form.m.{MNfcDiaS, NfcOpenDia, NfcWrite, NodesDiConf}
import io.suggest.nfc.NfcWriteOptions
import japgolly.univeq._

/** Controller for dialog of NFC writing. */
final class NfcDiaAh[M](
                         nodesDiConf    : NodesDiConf,
                         modelRW        : ModelRW[M, Option[MNfcDiaS]],
                       )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Open/close dialog.
    case m: NfcOpenDia =>
      val v0 = value

      if (m.isOpen ==* v0.isDefined) {
        noChange
      } else {
        val v2 = Option.when( m.isOpen )( MNfcDiaS() )
        updated(v2)
      }


    // NFC start writing.
    case NfcWrite =>
      (for {
        v0 <- value
      } yield {
        val fx = Effect {
          val nfcApi = nodesDiConf.nfcApi.get

          nfcApi.write()
        }

        ???
      })
        .getOrElse( noChange )

  }


}
