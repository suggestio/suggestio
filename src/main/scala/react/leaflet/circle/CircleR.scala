package react.leaflet.circle

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.map.LatLng
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:06
  * Description: react-leaflet API wrapper for Circle component.
  */

case class CircleR(
  override val props: CirclePropsR
)
  extends JsWrapperR[CirclePropsR, Element]
{

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Circle

}


@ScalaJSDefined
trait CirclePropsR extends js.Object {

  val center  : LatLng
  val radius  : Double

  // sjs-0.6.14 позволяет описывать тут всякие undefined-значения...

}
