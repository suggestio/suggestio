package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.map.Point

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.16 12:37
  * Description: API for internal class distance grid.
  */
@js.native
@JSName("L.DistanceGrid")
class DistanceGrid extends js.Object {

  def addObject(obj: js.Any, point: Point): Unit = js.native

  def updateObject(obj: js.Any, point: Point): Unit = js.native

  def removeObject(obj: js.Any, point: Point): Unit = js.native

  def eachObject(f: js.Function1[js.Any,_], context: js.Any): Unit = js.native

  def getNearObject(point: Point): js.Any = js.native

}
