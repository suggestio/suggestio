package io.suggest.lk.adv.direct.m

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.16 16:16
  * Description: Модель сигнала об изменении состояния привелигерованной галочки adv4free.
  */
case class Adv4FreeChanged(event: Event)
  extends IFsmMsg
object Adv4FreeChanged
  extends IFsmEventMsgCompanion
