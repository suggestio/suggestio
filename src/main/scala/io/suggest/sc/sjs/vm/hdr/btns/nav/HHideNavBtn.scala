package io.suggest.sc.sjs.vm.hdr.btns.nav

import io.suggest.sc.sjs.m.mhdr.HideNavClick
import io.suggest.sc.sjs.vm.util.InitOnClickToScFsmT
import io.suggest.sc.ScConstants.NavPane.HIDE_PANEL_BTN_ID
import io.suggest.sjs.common.vm.find.FindDiv
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
trait HHideNavBtnT extends InitOnClickToScFsmT {

  override type T = HTMLDivElement

  override protected[this] def _clickMsgModel = HideNavClick

}


/** Дефолтовая реализация vm кнопки сокрытия навигации. */
case class HHideNavBtn(
  override val _underlying: HTMLDivElement
)
  extends HHideNavBtnT
