package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mgrid.{MGridParams, GridScroll}
import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.{WrapperChildContent, ChildElOrFind}
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:17
 * Description: Модель div-враппера grid'а.
 */
object GWrapper extends FindDiv {

  override type T = GWrapper
  override def DOM_ID = Grid.WRAPPER_DIV_ID

}


/** Логика экземпляра модели. */
trait GWrapperT extends SafeElT with WrapperChildContent {
  override type T = HTMLDivElement

  override type SubTagVm_t = GContent.T
  override protected type SubTagEl_t = GContent.Dom_t
  override protected def _subtagCompanion = GContent

  /** Раняя инициализация враппера. */
  def initLayout(stData: IStData): Unit = {
    // Повесить событие
    for (c <- content; scr <- stData.screen) {
      // Передаем найденные элементы внутрь функции, т.к. при пересоздании layout событие будет повешено повторно.
      addEventListener("scroll") { (e: Event) =>
        val wrappedScrollTop = _underlying.scrollTop
        val contentHeight    = c._underlying.offsetHeight
        // Пнуть контроллер, чтобы подгрузил ещё карточек, когда пора.
        val scrollPxToGo = contentHeight - scr.height - wrappedScrollTop
        if (scrollPxToGo < MGridParams.LOAD_MORE_SCROLL_DELTA_PX) {
          ScFsm ! GridScroll(e)
        }
      }
    }
  }

}


/**
 * Реализация экземпляра модели враппера.
 * @param _underlying Соответствующий модели DOM-элемент.
 */
case class GWrapper(override val _underlying: HTMLDivElement)
  extends GWrapperT {

  override lazy val content = super.content

}
