package io.suggest.sjs.common.view.safe.wnd

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:10
 * Description: Безопасный доступ к необязательным полям window, таким как devicePixelRatio.
 * @see [[http://habrahabr.ru/post/159419/ Расовое авторитетное мнение Мицгола о devicePixelRatio, например.]]
 */
trait SafeWindowT extends ISafe {

  override type T <: Window

  /**
   * Вернуть devicePixelRatio по мнению браузера.
   * @return Some() если браузер поддерживает определение. Иначе None.
   */
  def devicePixelRatio: Option[Double] = {
    WindowDrpStub(dom.window)
      .devicePixelRatio
      .toOption
  }
}


/** Дефолтовая реализация [[SafeWindowT]]. */
case class SafeWindow(_underlying: Window) extends SafeWindowT {
  override type T = Window
}


/** Интерфейс для аккуратного доступа к возможному значению window.devicePixelRatio. */
sealed class WindowDrpStub extends js.Object {

  /** @return undefined | 1.0123123 */
  def devicePixelRatio: UndefOr[Double] = js.native
}

object WindowDrpStub {
  def apply(wnd: Window): WindowDrpStub = {
    wnd.asInstanceOf[WindowDrpStub]
  }
}

