package io.suggest.sc.sjs.vm.nav

import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.vm.util.GridOffsetCalc
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.NavPane.ROOT_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 12:28
 * Description: VM для Nav root, т.е. корневого div старой панели гео-навигации по сети suggest.io.
 */
object NRoot extends FindDiv {
  override type T = NRoot
  override def DOM_ID = ROOT_ID
}


trait NRootT extends VmT with ShowHideDisplayT with GridOffsetCalc {

  override type T = HTMLDivElement

  // Поддержка калькулятора пересчета сетки для этой панели.
  override protected def gridOffsetMinWidthPx: Int = 280

  override def saveNewOffsetIntoGridState(mgs0: MGridState, newOff: Int): MGridState = {
    mgs0.copy(
      leftOffset = newOff
    )
  }

}


case class NRoot(
  override val _underlying: HTMLDivElement
)
  extends NRootT
