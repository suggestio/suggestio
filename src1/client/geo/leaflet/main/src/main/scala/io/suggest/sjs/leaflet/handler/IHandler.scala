package io.suggest.sjs.leaflet.handler

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 16:10
 * Description: API for interaction handlers.
 */
@js.native
trait IHandler extends js.Object {

  def enable(): Unit = js.native

  def disable(): Unit = js.native

  def enabled(): Boolean = js.native

}
