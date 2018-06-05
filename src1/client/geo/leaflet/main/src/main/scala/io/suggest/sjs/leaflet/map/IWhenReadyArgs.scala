package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 12:42
  * Description: Iterface of args in f(args) for L.whenReady(f)
  * Imitation of leaflet Event, without any other fields.
  * @see [[https://github.com/Leaflet/Leaflet/blob/00cc6ff11972dcc011a2374f175727f03b0f9aa4/src/map/Map.js#L1411]]
  */
@js.native
trait IWhenReadyArgs extends js.Object {

  val target: LMap

}
