package io.suggest.radio

import japgolly.univeq._

import java.time.Instant

object MRadioSignalJs {

  @inline implicit def univEq: UnivEq[MRadioSignalJs] = UnivEq.derive


  implicit final class RadioSignalJsExt( private val signalJs: MRadioSignalJs ) extends AnyVal {

    /** Is radio-signal should be guessed as "visible" now?
      *
      * @param now Now time.
      * @return true, if signal guessed as still visible (signal is still here).
      */
    def isStillVisibleNow( now: Instant = Instant.now() ): Boolean =
      (now.getEpochSecond - signalJs.seenAt.getEpochSecond) <= signalJs.signal.typ.goneAwayAfterSeconds

  }

}


/** Client-side container for adding some client-only fields around [[MRadioSignal]].
  *
  * @param signal Cross-platform radio signal information.
  * @param seenAt Time, when radio-signal has been detected.
  */
final case class MRadioSignalJs(
                                 signal        : MRadioSignal,
                                 seenAt        : Instant           = Instant.now(),
                               )
