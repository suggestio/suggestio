package io.suggest.sjs.leaflet.control

import io.suggest.sjs.leaflet.control.locate.{LocateControl, LocateOptions}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 12:03
  * Description: API for L.controls.
  */
@js.native
@JSName("L.Control")
class LControl extends js.Object {

  /** For feature-detection. */
  def locate: UndefOr[_] = js.native

  def locate(options: LocateOptions = js.native): LocateControl = js.native

}
