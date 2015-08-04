package io.suggest.sc.sjs.vm.hdr.btns.nav

import io.suggest.sc.ScConstants.NavPane.SHOW_PANEL_BTN_ID
import io.suggest.sc.sjs.m.mhdr.ShowNavClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:44
 * Description: ViewModel кнопки отображения панели навигации.
 */
object HShowNavBtn extends FindDiv {

  override type T = HShowNavBtn

  override def DOM_ID: String = SHOW_PANEL_BTN_ID

}


/** Трейт с логикой экземпляра ViewModel'и кнопки отображения панели навигации. */
trait HShowNavBtnT extends InitOnClickToFsmT {

  override type T = HTMLDivElement

  override protected[this] def _msgModel = ShowNavClick

}


/** Дефолтовая реализация модели ViewModel'и кнопки отображения панели навигации. */
case class HShowNavBtn(
  override val _underlying: HTMLDivElement
)
  extends HShowNavBtnT
