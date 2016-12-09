package react.leaflet

import io.suggest.sjs.leaflet.map.LMap

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 12:48
  * Description: React context facade API for react-leaflet.
  */
@js.native
trait Context extends js.Object {

  val map: LMap = js.native

}
