package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.Layout
import io.suggest.sc.sjs.vm.util.domget.GetDivById
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 9:58
 * Description: Модель доступа к div sioMartLayout.
 */
object LayContentVm extends GetDivById {

  /** Создать модель из DOM, если возможно. */
  def find(): Option[LayRootVm] = {
    getDivById(Layout.LAYOUT_ID)
      .map(LayRootVm.apply)
  }

}


/** Абстрактная реализация модели. */
trait LayContentVmT extends SafeElT {
  override type T = HTMLDivElement
}


case class LayContentVm(
  override val _underlying: HTMLDivElement
) extends LayContentVmT
