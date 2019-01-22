package io.suggest.sjs.common.vm.wnd.nav

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Navigator
import org.scalajs.dom.raw.Geolocation
import org.scalajs.dom.experimental.permissions._

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.15 18:25
 * Description: Безопасный доступ к полям window.navigator.
 */
case class NavigatorVm(_underlying: Navigator) extends IVm {

  override type T = Navigator

  def stub = NavStub(_underlying)

  def geolocation: Option[Geolocation] = {
    stub
      .geolocation
      .toOption
  }


  def userAgent: Option[String] = {
    stub
      .userAgent
      .toOption
  }

  def standalone: Option[Boolean] = {
    stub
      .standalone
      .toOption
  }

  def permissions: Option[Permissions] = {
    NavigatorPermissionsStub( _underlying )
      .permissionsUndef
      .toOption
  }

}


object NavStub {
  def apply(nav: Navigator): NavStub = {
    nav.asInstanceOf[NavStub]
  }
}

@js.native
sealed trait NavStub extends js.Object {
  def geolocation: js.UndefOr[Geolocation] = js.native
  val userAgent: UndefOr[String] = js.native
  val standalone: UndefOr[Boolean] = js.native
}


@js.native
trait NavigatorPermissionsStub extends js.Object {
  @JSName("permissions")
  val permissionsUndef: js.UndefOr[Permissions] = js.native
}
object NavigatorPermissionsStub {
  def apply(nav: Navigator): NavigatorPermissionsStub =
    nav.asInstanceOf[NavigatorPermissionsStub]
}