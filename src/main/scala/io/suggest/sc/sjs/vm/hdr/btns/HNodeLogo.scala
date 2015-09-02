package io.suggest.sc.sjs.vm.hdr.btns

import io.suggest.sc.sjs.m.mhdr.LogoClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById
import io.suggest.sc.sjs.vm.util.domvm.FindElT
import io.suggest.sc.ScConstants.Logo.LOGO_CONT_ID
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLSpanElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.15 14:58
 * Description: vm'ка контейнера логотипа.
 */
object HNodeLogo extends FindElT with GetDivById {
  override def DOM_ID: String = LOGO_CONT_ID
  override type Dom_t = HTMLSpanElement
  override type T = HNodeLogo
}


trait HNodeLogoT extends SafeElT with InitOnClickToFsmT {

  override type T = HTMLSpanElement

  override protected[this] def _clickMsgModel = LogoClick

}


case class HNodeLogo(
  override val _underlying: HTMLSpanElement
)
  extends HNodeLogoT
