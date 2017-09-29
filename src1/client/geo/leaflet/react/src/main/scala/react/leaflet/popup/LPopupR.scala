package react.leaflet.popup

import io.suggest.sjs.leaflet.map.LatLng
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:15
  * Description: React-leaflet API wrappers for Popup component.
  */

object LPopupR {

  val component = JsComponent[LPopupPropsR, Children.Varargs, Null]( LPopupJs )

  def apply( props: LPopupPropsR )(ch: VdomElement) = component(props)(ch)

}


@JSImport("react-leaflet", "Popup")
@js.native
object LPopupJs extends js.Object


trait LPopupPropsR extends js.Object {

  val position  : LatLng

  val key       : UndefOr[String]   = js.undefined

}
