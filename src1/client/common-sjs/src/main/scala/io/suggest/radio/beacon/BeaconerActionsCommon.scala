package io.suggest.radio.beacon

import io.suggest.radio.{MRadioSignalJs, MRadioSignalType}
import io.suggest.spa.DAction


/** Interface for signals, for beaconer FSM. */
trait IBeaconerAction extends DAction


/** Start/stop or alter options of bluetooth BeaconerAh.
  *
  * @param isEnabled New state (on/off)
  *                  None means stay in current state, possibly altering beaconer options.
  * @param opts Options of beaconer.
  */
case class BtOnOff(isEnabled: Option[Boolean],
                   opts: MBeaconerOpts = MBeaconerOpts.default) extends IBeaconerAction


/** Detected useful radio-signals. */
case class RadioSignalsDetected(
                                 radioType     : MRadioSignalType,
                                 signals       : Seq[MRadioSignalJs],
                               )
  extends IBeaconerAction
