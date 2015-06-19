package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.sjs.m.mfsm.IFsmMsg
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 16:09
 * Description: Сообщения событий для FSM.
 */

/** Клик по карточке в плитке. */
case class GridBlockClick(e: Event) extends IFsmMsg

/** Событие вертикального скроллинга страницы с плиткой. */
case class VScroll(e: Event) extends IFsmMsg
