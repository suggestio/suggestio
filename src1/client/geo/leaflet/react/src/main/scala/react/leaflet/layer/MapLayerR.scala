package react.leaflet.layer

import react.leaflet.Context
import react.leaflet.lmap.MapComponentR

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 17:35
  * Description: Facade API for map layer ES6 class.
  */
@JSImport("react-leaflet", "MapLayer")
@js.native
object MapLayerR extends js.Object {
  val contextTypes: js.Object = js.native
}

@JSImport("react-leaflet", "MapLayer")
@js.native
class MapLayerR[Props <: js.Any](props: Props, context: Context)
  extends MapComponentR[Props](props, context)
