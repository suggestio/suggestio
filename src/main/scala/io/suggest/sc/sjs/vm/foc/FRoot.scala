package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Focused.ROOT_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 15:07
 * Description: ViewModel для взаимодейстия с корневым контейнером для выдачи focused ads.
 */

object FRoot extends FindDiv {
  
  override def DOM_ID: String = ROOT_ID
  override type T = FRoot

}



trait FRootT extends SafeElT {
  override type T = HTMLDivElement

  def replaceCarousel(car: FCarouselT): Unit = {
    VUtil.removeAllChildren(_underlying)
    _underlying.appendChild( car._underlying )
  }
}


case class FRoot(_underlying: HTMLDivElement)
  extends FRootT

