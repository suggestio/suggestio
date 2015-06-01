package io.suggest.sjs.common.view.safe.nav.ua

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.NavigatorID

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:23
 * Description: Аккуратный доступ к dom.navigator.userAgent.
 */
trait SafeNavUa extends ISafe {

  override type T <: NavigatorID

  def userAgent: Option[String] = {
    SafeNavUaStub(_underlying)
      .userAgent
      .toOption
  }

}


sealed trait SafeNavUaStub extends js.Object {
  val userAgent: UndefOr[String] = js.native
}

object SafeNavUaStub {
  def apply(nav: NavigatorID): SafeNavUaStub = {
    nav.asInstanceOf[SafeNavUaStub]
  }
}

