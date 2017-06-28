package react.leaflet.control

import io.suggest.sjs.leaflet.control.ControlOptions
import japgolly.scalajs.react.raw.ReactElement
import react.leaflet.Context
import react.leaflet.lmap.MapComponentR

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 12:41
  * Description: Facade API for react MapControl class.
  */
@JSImport("react-leaflet", "MapControl")
@js.native
object MapControlR extends js.Object {

  val contextTypes: js.Object = js.native

}


@JSImport("react-leaflet", "MapControl")
@js.native
class MapControlR[Props <: ControlOptions](props: Props, context: Context)
  extends MapComponentR[Props](props, context) {

  def render(): ReactElement = js.native    // returns null by default.

}

