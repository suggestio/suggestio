package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.ChildElOrFind
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Grid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:08
 * Description: ViewModel корневого DIV'а выдачи-сетки карточек.
 *
 * Цепочка из root-wrapper-content используется для решения возможных проблем со скроллингом и
 * скроллбаром в некоторых браузерах.
 */
object GridRootVm extends FindDiv {

  override def DOM_ID = Grid.ROOT_DIV_ID
  override type T = GridRootVm

}


/** Логика и интерфейс экземпляра модели. */
trait GridRootVmT extends SafeElT with ChildElOrFind {

  override type T = HTMLDivElement

  override type SubTagVm_t = GridWrapperVm.T
  override protected type SubTagEl_t = GridWrapperVm.Dom_t
  override protected def _subtagCompanion = GridWrapperVm

  def wrapper = _findSubtag()

}


/**
 * Экземпляр модели; дефолтовая реализация [[GridRootVmT]].
 * @param _underlying Соответствующий этой модели DOM div.
 */
case class GridRootVm(
  override val _underlying: HTMLDivElement
)
  extends GridRootVmT {

  override lazy val wrapper = super.wrapper

}
