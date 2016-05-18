package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.mgrid.{GridScroll, MGridParams}
import io.suggest.sc.sjs.m.msc.IScSd
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.child.{SubTagFind, WrapperChildContent}
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:17
 * Description: Модель div-враппера grid'а.
 */
object GWrapper extends FindDiv {
  override type T     = GWrapper
  override def DOM_ID = Grid.WRAPPER_DIV_ID
}


import GWrapper.Dom_t


/** Логика экземпляра модели. */
trait GWrapperT extends VmT with SubTagFind with WrapperChildContent {

  override type T = Dom_t

  override protected type SubtagCompanion_t = GContent.type
  override type SubTagVm_t                  = GContent.T
  override protected type SubTagEl_t        = GContent.Dom_t
  override protected def _subtagCompanion   = GContent

  /** Раняя инициализация враппера. */
  def initLayout(stData: IScSd): Unit = {
    // Повесить событие
    for (c <- content; scr <- stData.screen) {
      // Передаем найденные элементы внутрь функции, т.к. при пересоздании layout событие будет повешено повторно.
      addEventListener("scroll") { (e: Event) =>
        val wrappedScrollTop = _underlying.scrollTop
        val contentHeight    = c._underlying.offsetHeight
        // Пнуть контроллер, чтобы подгрузил ещё карточек, когда пора.
        val scrollPxToGo = contentHeight - scr.height - wrappedScrollTop
        if (scrollPxToGo < MGridParams.LOAD_MORE_SCROLL_DELTA_PX) {
          ScFsm !! GridScroll(e)
        }
      }
    }
  }

}


/**
 * Реализация экземпляра модели враппера.
  *
  * @param _underlying Соответствующий модели DOM-элемент.
 */
case class GWrapper(override val _underlying: Dom_t)
  extends GWrapperT {

  override lazy val content = super.content

}
