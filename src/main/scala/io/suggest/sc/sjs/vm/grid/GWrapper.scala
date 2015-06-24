package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.ChildElOrFind
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:17
 * Description: Модель div-враппера grid'а.
 */
object GridWrapperVm extends FindDiv {

  override type T = GridWrapperVm
  override def DOM_ID = Grid.WRAPPER_DIV_ID

}


/** Логика экземпляра модели. */
trait GridWrapperVmT extends SafeElT with ChildElOrFind {
  override type T = HTMLDivElement

  override type SubTagVm_t = GridContentVm.T
  override protected type SubTagEl_t = GridContentVm.Dom_t
  override protected def _subtagCompanion = GridContentVm

  /** Доступ к grid content div. */
  def content = _findSubtag()

}


/**
 * Реализация экземпляра модели враппера.
 * @param _underlying Соответствующий модели DOM-элемент.
 */
case class GridWrapperVm(override val _underlying: HTMLDivElement)
  extends GridWrapperVmT {

  override lazy val content = super.content

}
