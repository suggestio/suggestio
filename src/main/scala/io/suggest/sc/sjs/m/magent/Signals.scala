package io.suggest.sc.sjs.m.magent

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.04.16 10:38
  * Description: Стандартные сигналы от юзер-агента.
  */

case class VisibilityChange(event: Event)
  extends IFsmMsg
object VisibilityChange
  extends IFsmEventMsgCompanion
