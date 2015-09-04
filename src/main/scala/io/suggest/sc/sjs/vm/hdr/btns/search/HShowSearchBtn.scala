package io.suggest.sc.sjs.vm.hdr.btns.search

import io.suggest.sc.ScConstants.Search.SHOW_PANEL_BTN_ID
import io.suggest.sc.sjs.m.mhdr.ShowSearchClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 16:45
 * Description: ViewModel кнопки раскрытия панели поиска.
 */
object HShowSearchBtn extends FindDiv {

  override type T = HShowSearchBtn

  override def DOM_ID: String = SHOW_PANEL_BTN_ID

}


/** Трейт с логикой экземпляра vm кнопки открытия поисковой панели. */
trait HShowSearchBtnT extends InitOnClickToFsmT {

  override type T = HTMLDivElement

  override protected[this] def _clickMsgModel = ShowSearchClick

}


/** Дефолтовая реализация viewModel'и кнопки раскрытия поисковой панели.
  * @param _underlying корневой div поисковой панели.
  */
case class HShowSearchBtn(
  override val _underlying: HTMLDivElement
)
  extends HShowSearchBtnT
