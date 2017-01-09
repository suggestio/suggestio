package io.suggest.sjs.leaflet.control.locate

import io.suggest.sjs.leaflet.control.IControl

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 12:06
  * Description: API for location control.
  */
@js.native
@JSName("L.Control.Locate")
class LocateControl extends IControl {

  def start(): LocateControl = js.native

  def _stopFollowing(): Unit = js.native

}
