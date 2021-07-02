package io.suggest.radio.beacon

import io.suggest.radio.{MRadioSignalJs, MRadioSignalType}
import io.suggest.spa.DAction


/** Interface for signals, for beaconer FSM. */
trait IBeaconerAction extends DAction


/** Detected useful radio-signals. */
case class RadioSignalsDetected(
                                 radioType     : MRadioSignalType,
                                 signals       : Seq[MRadioSignalJs],
                               )
  extends IBeaconerAction
