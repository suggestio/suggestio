package react.leaflet.control

import io.suggest.sjs.leaflet.control.ControlOptions
import japgolly.scalajs.react.ReactElement
import react.leaflet.Context
import react.leaflet.lmap.MapComponentR

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 12:41
  * Description: Facade API for react MapControl class.
  */

@js.native
@JSName("ReactLeaflet.MapControl")
object MapControlR extends js.Object {

  val contextTypes: js.Object = js.native

}


@js.native
@JSName("ReactLeaflet.MapControl")
class MapControlR[Props <: ControlOptions](props: Props, context: Context)
  extends MapComponentR[Props](props, context) {

  def render(): ReactElement = js.native    // returns null by default.

}

