package react.leaflet.popup

import io.suggest.react.JsWrapper1R
import io.suggest.sjs.leaflet.map.LatLng
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:15
  * Description: React-leaflet API wrappers for Popup component.
  */

object PopupR {

  def apply(position: LatLng,
            key: UndefOr[String] = js.undefined ): PopupR = {
    val p = js.Dynamic.literal().asInstanceOf[PopupPropsR]

    p.position = position
    key.foreach( p.key = _ )

    PopupR(p)
  }

}

case class PopupR(
  override val props: PopupPropsR
)
  extends JsWrapper1R[PopupPropsR, HTMLElement]
{
  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Popup
}


@js.native
trait PopupPropsR extends js.Object {

  var position: LatLng = js.native

  var key: String = js.native

}
