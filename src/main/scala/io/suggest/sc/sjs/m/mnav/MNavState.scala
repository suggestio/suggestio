package io.suggest.sc.sjs.m.mnav

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:50
 * Description: Набор переменных состояния панели навигации.
 */
case class MNavState(
  panelOpened     : Boolean       = false,
  currGlayIndex   : Option[Int]   = None
)



/** Событие клика внутри списка узлов. */
case class NodeListClick(e: Event)
  extends IFsmMsg
object NodeListClick
  extends IFsmEventMsgCompanion
