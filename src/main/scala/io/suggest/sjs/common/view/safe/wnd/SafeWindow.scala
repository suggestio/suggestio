package io.suggest.sjs.common.view.safe.wnd

import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import io.suggest.sjs.common.view.safe.scroll.ScrollTop
import io.suggest.sjs.common.view.safe.wnd.cs.SafeGetComputedStyleT
import io.suggest.sjs.common.view.safe.wnd.dpr.SafeWndDpr
import io.suggest.sjs.common.view.safe.wnd.hist.SafeHistoryApiT
import org.scalajs.dom
import org.scalajs.dom.{Navigator, Window}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:10
 * Description: Безопасный доступ к необязательным полям window, таким как devicePixelRatio.
 * @see [[http://habrahabr.ru/post/159419/ Расовое авторитетное мнение Мицгола о devicePixelRatio, например.]]
 */
trait SafeWindowT
  extends SafeWndDpr
  with SafeGetComputedStyleT
  with SafeHistoryApiT
  with SafeEventTargetT
  with ScrollTop
{
  override type T <: Window

  def stub = WindowStub(_underlying)

  /** Безопасный доступ к навигатору. */
  def navigator: Option[SafeWndNavigator] = {
    stub.navigator
      .toOption
      .map { SafeWndNavigator.apply }
  }

}


/** Дефолтовая реализация [[SafeWindowT]]. */
case class SafeWindow(_underlying: Window = dom.window) extends SafeWindowT {
  override type T = Window
}


object WindowStub {
  def apply(wnd: Window): WindowStub = {
    wnd.asInstanceOf[WindowStub]
  }
}
@js.native
sealed trait WindowStub extends js.Object {
  def navigator: js.UndefOr[Navigator] = js.native
}
