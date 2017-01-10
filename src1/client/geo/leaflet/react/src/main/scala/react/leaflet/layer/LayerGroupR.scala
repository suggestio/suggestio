package react.leaflet.layer

import io.suggest.react.JsWrapperNoPropsR
import japgolly.scalajs.react.TopNode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 12:03
  * Description: Layer group for react-leaflet wrappers and APIs.
  */
case class LayerGroupR() extends JsWrapperNoPropsR[TopNode] {

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.LayerGroup

}
