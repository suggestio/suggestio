package io.suggest.sjs.leaflet.control.locate

import io.suggest.sjs.leaflet.control.IControl

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 12:06
  * Description: API for location control.
  */
//@JSImport("string-replace-loader?search=L&replace=LLLLLLLLL!./node_modules/leaflet.locatecontrol/src/L.Control.Locate.js", JSImport.Namespace)
@JSImport("leaflet.locatecontrol", JSImport.Namespace)
@js.native
class LocateControl(options: LocateOptions) extends IControl {

  def start(): LocateControl = js.native

  def _stopFollowing(): Unit = js.native

}
