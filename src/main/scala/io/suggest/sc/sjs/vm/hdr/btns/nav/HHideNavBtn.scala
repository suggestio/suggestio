package io.suggest.sc.sjs.vm.hdr.btns.nav

import io.suggest.sc.sjs.m.mhdr.HideNavClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.NavPane.HIDE_PANEL_BTN_ID
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 18:12
 * Description: ViewModel кнопки сокрытия панели навигации.
 */
object HHideNavBtn extends FindDiv {

  override type T = HHideNavBtn

  override def DOM_ID = HIDE_PANEL_BTN_ID

}


/** Логика экземпляра vm кнопки сокрытия. */
trait HHideNavBtnT extends InitOnClickToFsmT {

  override type T = HTMLDivElement

  override protected[this] def _msgModel = HideNavClick

}


/** Дефолтовая реализация vm кнопки сокрытия навигации. */
case class HHideNavBtn(
  override val _underlying: HTMLDivElement
)
  extends HHideNavBtnT
