package react.leaflet.circle

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.event.MouseEvent
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.path.PathOptions
import japgolly.scalajs.react.{JsComponentType, TopNode}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:06
  * Description: react-leaflet API wrapper for Circle component.
  *
  * scala-js 0.6.14+ required.
  */

case class CircleR(
  override val props: CirclePropsR
)
  extends JsWrapperR[CirclePropsR, Element]
{
  override protected def _rawComponent = js.constructorOf[Circle]
}

@JSImport("react-leaflet", "Circle")
@js.native
sealed class Circle extends JsComponentType[CirclePropsR, js.Object, TopNode]


@ScalaJSDefined
trait CirclePropsR extends PathOptions {

  /** Circle center. */
  val center  : LatLng

  /** Circle radius. */
  val radius  : Double

  /** Реакция на клики по кружочку. */
  val onClick       : UndefOr[js.Function1[MouseEvent, Unit]]         = js.undefined

}
