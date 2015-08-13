package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.sjs.m.mfsm.{CurrentTargetBackup, IFsmEventMsgCompanion, IFsmMsg}
import io.suggest.sc.sjs.vm.grid.GBlock
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 16:09
 * Description: Сообщения событий для FSM.
 */

/** Клик по карточке в плитке. Нужно бэкапить currentTarget. */
trait IGridBlockClick extends CurrentTargetBackup {
  override type CurrentTarget_t = HTMLDivElement

  def gblock = GBlock(currentTarget)
}

case class GridBlockClick(override val event: Event)
  extends IGridBlockClick with IFsmMsg

object GridBlockClick extends IFsmEventMsgCompanion



/** Событие вертикального скроллинга страницы с плиткой. */
case class GridScroll(e: Event) extends IFsmMsg
