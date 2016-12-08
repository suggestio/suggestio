package react.leaflet.circle

import io.suggest.sjs.leaflet.map.LatLng
import org.scalajs.dom.Element
import react.leaflet.WrapperR

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:06
  * Description: react-leaflet API wrapper for Circle component.
  */
object CircleR {

  def apply(
    center  : LatLng,
    radius  : Double
  ): CircleR = {
    val p = js.Dynamic.literal().asInstanceOf[CirclePropsR]
    p.center = center
    p.radius = radius
    CircleR(p)
  }

}

case class CircleR(
  override val props: CirclePropsR
)
  extends WrapperR[CirclePropsR, Element]
{

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Circle

}


@js.native
trait CirclePropsR extends js.Object {
  var center  : LatLng  = js.native
  var radius  : Double  = js.native
}
