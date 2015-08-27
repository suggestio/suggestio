package io.suggest.sjs.common.view.safe.wnd

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.Navigator
import org.scalajs.dom.raw.Geolocation

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.15 18:25
 * Description: Безопасный доступ к полям window.navigator.
 */
case class SafeWndNavigator(_underlying: Navigator) extends ISafe {

  override type T = Navigator

  def stub = NavStub(_underlying)

  def geolocation: Option[Geolocation] = {
    stub.geolocation
      .toOption
  }

}


object NavStub {
  def apply(nav: Navigator): NavStub = {
    nav.asInstanceOf[NavStub]
  }
}

sealed trait NavStub extends js.Object {
  def geolocation: js.UndefOr[Geolocation] = js.native
}
