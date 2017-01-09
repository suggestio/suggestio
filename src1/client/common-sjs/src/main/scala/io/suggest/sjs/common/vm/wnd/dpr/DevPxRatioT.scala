package io.suggest.sjs.common.view.safe.wnd.dpr

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:44
 * Description: Безопасный доступ к window.devicePixelRatio.
 */
trait DevPxRatioT extends IVm {

  override type T = Window

  /**
   * Вернуть devicePixelRatio по мнению браузера.
   * @return Some() если браузер поддерживает определение. Иначе None.
   */
  def devicePixelRatio: Option[Double] = {
    WindowDrpStub(_underlying)
      .devicePixelRatio
      .toOption
  }

}


/** Интерфейс для аккуратного доступа к возможному значению window.devicePixelRatio. */
@js.native
sealed class WindowDrpStub extends js.Object {

  /** @return undefined | 1.0123123 */
  def devicePixelRatio: UndefOr[Double] = js.native
}

object WindowDrpStub {
  def apply(wnd: Window): WindowDrpStub = {
    wnd.asInstanceOf[WindowDrpStub]
  }
}
