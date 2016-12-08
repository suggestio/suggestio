package react.leaflet.popup

import io.suggest.sjs.leaflet.map.LatLng
import org.scalajs.dom.raw.HTMLElement
import react.leaflet.Wrapper1R

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:15
  * Description: React-leaflet API wrappers for Popup component.
  */

object PopupR {

  def apply(position: LatLng): PopupR = {
    val p = js.Dynamic.literal().asInstanceOf[PopupPropsR]
    p.position = position
    PopupR(p)
  }

}

case class PopupR(
  override val props: PopupPropsR
)
  extends Wrapper1R[PopupPropsR, HTMLElement]
{
  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Popup
}


@js.native
trait PopupPropsR extends js.Object {
  var position: LatLng = js.native
}
