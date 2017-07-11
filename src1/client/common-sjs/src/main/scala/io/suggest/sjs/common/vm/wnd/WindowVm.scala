package io.suggest.sjs.common.vm.wnd

import io.suggest.sjs.common.vm.evtg.EventTargetVmT
import io.suggest.sjs.common.vm.scroll.ScrollT
import io.suggest.sjs.common.vm.wnd.compstyle.GetComputedStyleT
import io.suggest.sjs.common.vm.wnd.dpr.DevPxRatioT
import io.suggest.sjs.common.vm.wnd.hist.HistoryApiT
import io.suggest.sjs.common.vm.wnd.nav.NavigatorVm
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
trait WindowVmT
  extends DevPxRatioT
  with GetComputedStyleT
  with HistoryApiT
  with EventTargetVmT
  with ScrollT
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

}


/** Дефолтовая реализация [[WindowVmT]]. */
case class WindowVm(_underlying: Window = dom.window) extends WindowVmT {
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
