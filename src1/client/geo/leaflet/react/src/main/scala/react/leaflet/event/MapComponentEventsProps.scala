package react.leaflet.event

import io.suggest.sjs.leaflet.event.MouseEvent

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 22:25
  * Description: Common events for most react-leaflet MapComponent's impls.
  */
trait MapComponentEventsProps extends js.Object {

  val onClick       : UndefOr[js.Function1[MouseEvent, Unit]]         = js.undefined

}
