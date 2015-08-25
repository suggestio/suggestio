package io.suggest.sc.sjs.m.mwc

import io.suggest.sc.sjs.m.mfsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 15:29
 * Description: Сигналы в ScFsm от welcome-карточки.
 */

trait IWcStepSignal extends IFsmMsg


/** Сигналы от таймеров сокрытия welcome объеденены в этот объект. */
case object WcTimeout
  extends IWcStepSignal


/** Клик по карточке приветствия для педалирования её сокрытия. */
case class WcClick(event: Event)
  extends IWcStepSignal
object WcClick
  extends IFsmEventMsgCompanion

