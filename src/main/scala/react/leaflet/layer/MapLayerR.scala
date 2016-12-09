package react.leaflet.layer

import react.leaflet.Context
import react.leaflet.lmap.MapComponentR

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 17:35
  * Description: Facade API for map layer ES6 class.
  */
@js.native
@JSName("ReactLeaflet.MapLayer")
object MapLayerR extends js.Object {
  val contextTypes: js.Object = js.native
}

@js.native
@JSName("ReactLeaflet.MapLayer")
class MapLayerR[Props <: js.Any](props: Props, context: Context)
  extends MapComponentR[Props](props, context)
