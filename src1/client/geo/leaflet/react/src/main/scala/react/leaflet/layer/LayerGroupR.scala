package react.leaflet.layer

import io.suggest.react.JsWrapperNoPropsR
import japgolly.scalajs.react.{JsComponentType, TopNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 12:03
  * Description: Layer group for react-leaflet wrappers and APIs.
  */
case class LayerGroupR() extends JsWrapperNoPropsR[TopNode] {
  override protected def _rawComponent = js.constructorOf[LayerGroup]
}

@JSImport("react-leaflet", "LayerGroup")
@js.native
sealed class LayerGroup extends JsComponentType[js.Object, js.Object, TopNode]
