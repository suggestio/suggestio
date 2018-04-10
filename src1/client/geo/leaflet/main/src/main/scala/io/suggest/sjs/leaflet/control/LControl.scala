package io.suggest.sjs.leaflet.control

import io.suggest.sjs.leaflet.control.locate.{LocateControl, LocateOptions}
import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 12:03
  * Description: API for L.control (aka controls).
  */
@JSImport(LEAFLET_IMPORT, "control")
@js.native
class LControl extends js.Object {

  /** For feature-detection. */
  var locate: UndefOr[_] = js.native

  def locate(options: UndefOr[LocateOptions] = js.native): LocateControl = js.native

}
