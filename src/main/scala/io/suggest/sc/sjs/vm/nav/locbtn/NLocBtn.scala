package io.suggest.sc.sjs.vm.nav.locbtn

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.NavPane.FIND_ME_BTN_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 13:34
 * Description: vm div'а, который представляется кнопкой
 */
object NLocBtn extends FindDiv {
  override type T = NLocBtn
  override def DOM_ID: String = FIND_ME_BTN_ID
}


trait NLocBtnT extends SafeElT {
  override type T = HTMLDivElement
}


case class NLocBtn(
  override val _underlying: HTMLDivElement
)
  extends NLocBtnT
