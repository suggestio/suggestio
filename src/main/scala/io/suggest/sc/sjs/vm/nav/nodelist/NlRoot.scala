package io.suggest.sc.sjs.vm.nav.nodelist

import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sc.sjs.vm.util.IInitLayout
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.NavPane.NODE_LIST_ID
import io.suggest.sc.sjs.vm.util.height3.SetHeight3
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 13:20
 * Description: Корневой div списка узлов.
 */
object NlRoot extends FindDiv {
  override type T = NlRoot
  override def DOM_ID = NODE_LIST_ID

  def NL_TOP_OFFSET = 129
}


trait NlRootT extends SetHeight3 {
  override type T = HTMLDivElement

  override protected type SubTagEl_t      = NlWrapper.Dom_t
  override type SubTagVm_t                = NlWrapper.T
  override protected def _subtagCompanion = NlWrapper

  def initLayout(sd: IStData): Unit = {
    for (screen <- sd.screen) {
      val height = screen.height - NlRoot.NL_TOP_OFFSET
      _setHeight3(height)
    }
    // Пробросить инициализацию далее по цепочке.
    wrapper.foreach(IInitLayout.f)
  }

}


case class NlRoot(
  override val _underlying: HTMLDivElement
)
  extends NlRootT
{
  override lazy val wrapper = super.wrapper
}
