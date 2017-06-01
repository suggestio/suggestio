package react.leaflet.layer

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.event.MouseEvent
import japgolly.scalajs.react.{JsComponentType, TopNode}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 12:03
  * Description: Layer group for react-leaflet wrappers and APIs.
  */
case class LayerGroupR(
                        override val props  : LayerGroupPropsR = new LayerGroupPropsR {}
                      )
  extends JsWrapperR[LayerGroupPropsR, TopNode]
{

  override protected def _rawComponent = js.constructorOf[LayerGroup]

}


@JSImport("react-leaflet", "LayerGroup")
@js.native
sealed class LayerGroup extends JsComponentType[LayerGroupPropsR, js.Object, TopNode]


@ScalaJSDefined
trait LayerGroupPropsR extends js.Object {

  val onClick       : UndefOr[js.Function1[MouseEvent, Unit]]         = js.undefined

}
