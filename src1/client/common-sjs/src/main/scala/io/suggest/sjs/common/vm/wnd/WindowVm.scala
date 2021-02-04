package io.suggest.sjs.common.vm.wnd

import io.suggest.sjs.common.vm.wnd.compstyle.GetComputedStyleT
import io.suggest.sjs.common.vm.wnd.dpr.DevPxRatioT
import io.suggest.sjs.common.vm.wnd.nav.NavigatorVm
import org.scalajs.dom
import org.scalajs.dom.{MediaQueryList, Navigator, Window}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:10
 * Description: Безопасный доступ к необязательным полям window, таким как devicePixelRatio.
 * @see [[http://habrahabr.ru/post/159419/ Расовое авторитетное мнение Мицгола о devicePixelRatio, например.]]
 */
trait WindowVmT
  extends DevPxRatioT
  with GetComputedStyleT
{
  override type T <: Window

  def stub = WindowStub(_underlying)

  /** Безопасный доступ к навигатору. */
  def navigator: Option[NavigatorVm] = {
    stub.navigator
      .toOption
      .map { NavigatorVm.apply }
  }

  /** Безопасный упрощённый доступ к navigator.geolocation. */
  def geolocation = navigator.flatMap(_.geolocation)

  def matchMedia(mediaQuery: String): Option[MediaQueryList] = {
    stub
      .matchMedia(mediaQuery)
      .toOption
  }

}


/** Дефолтовая реализация [[WindowVmT]]. */
case class WindowVm(_underlying: Window = dom.window) extends WindowVmT {
  override type T = Window
}


object WindowStub {
  implicit def apply(wnd: Window): WindowStub =
    wnd.asInstanceOf[WindowStub]
}
@js.native
sealed trait WindowStub extends js.Object {
  def navigator: js.UndefOr[Navigator] = js.native
  def matchMedia(mediaQuery: String): UndefOr[MediaQueryList] = js.native

  var ontouchstart: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
  var ontouchmove: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
  var ontouchend: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
  var ontouchcancel: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
}
