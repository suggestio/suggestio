package io.suggest.sjs.leaflet.map

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 16:27
 * Description: API for pixel bounds.
 */
@JSImport("leaflet", "Bounds")
@js.native
class Bounds extends js.Object {

  var min: Point = js.native

  var max: Point = js.native


  def extend(point: Point): Unit = js.native

  def getCenter(): Point = js.native

  def contains(bounds: Bounds): Boolean = js.native

  def contains(point: Point): Boolean = js.native

  def intersects(bounds: Bounds): Boolean = js.native

  def isValid(): Boolean = js.native

  def getSize(): Point = js.native

}
