package io.suggest.sjs.common.vm.wnd

import io.suggest.sjs.common.vm.wnd.compstyle.GetComputedStyleT
import io.suggest.sjs.common.vm.wnd.dpr.DevPxRatioT
import io.suggest.sjs.common.vm.wnd.nav.NavigatorVm
import io.suggest.sjs.dom2.{DomExt, DomWindowExt}
import org.scalajs.dom.{MediaQueryList, Window}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:10
 * Description: Безопасный доступ к необязательным полям window, таким как devicePixelRatio.
 * @see [[http://habrahabr.ru/post/159419/ Расовое авторитетное мнение Мицгола о devicePixelRatio, например.]]
 */
case class WindowVm()
  extends DevPxRatioT
  with GetComputedStyleT
{
  override type T = Window

  override def _underlying = DomExt.windowOpt getOrElse {
    // Crunch for graalVM environment: emulate window object presence for some scala calls.
    js.Object().asInstanceOf[Window]
  }

  def stub = DomWindowExt(_underlying)

  def documentOpt = stub.documentU.toOption

  /** Безопасный доступ к навигатору. */
  def navigator: Option[NavigatorVm] = {
    stub
      .navigator
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
