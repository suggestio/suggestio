package react.leaflet.layer

import io.suggest.sjs.leaflet.event.MouseEvent
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 12:03
  * Description: Layer group for react-leaflet wrappers and APIs.
  */
object LayerGroupR {

  val component = JsComponent[LayerGroupPropsR, Children.Varargs, Null]( LLayerGroupJsR )

  def apply(props  : LayerGroupPropsR = new LayerGroupPropsR {})(children: VdomNode*) = {
    component(props)(children: _*)
  }

}


@JSImport("react-leaflet", "LayerGroup")
@js.native
object LLayerGroupJsR extends js.Object


@ScalaJSDefined
trait LayerGroupPropsR extends js.Object {

  val onClick       : UndefOr[js.Function1[MouseEvent, Unit]]         = js.undefined

}
