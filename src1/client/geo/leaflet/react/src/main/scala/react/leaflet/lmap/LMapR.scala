package react.leaflet.lmap

import io.suggest.sjs.leaflet.event.{DragEndEvent, Event, LocationEvent, PopupEvent}
import io.suggest.sjs.leaflet.map.{LMap, LatLngBounds, MapOptions}
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}
import react.leaflet.Context

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: UndefOr[Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: UndefOr[08.12.16 12:20
  * Description: UndefOr[React leaflet map wrapper по упрощенным технологиям.
  * @see [[https://github.com/chandu0101/scalajs-react-components/blob/master/doc/InteropWithThirdParty.md]]
  */

object LMapR {

  val component = JsComponent[LMapPropsR, Children.Varargs, Null]( LMapJsR )
    .addFacade[LMapJsR]

  def apply(props: LMapPropsR)(children: VdomNode*) = component(props)(children: _*)

}


@JSImport("react-leaflet", "Map")
@js.native
sealed class LMapJsR(
                       props   : LMapPropsR,
                       context : Context
                     )
  extends MapComponentR(props, context)
{
  override type El_t = LMap
}


@JSImport("react-leaflet", "Map")
@js.native
object LMapJsR extends js.Object


trait LMapPropsR extends MapOptions {

  val animate         : UndefOr[Boolean]        = js.undefined
  val bounds          : UndefOr[LatLngBounds]   = js.undefined
  val boundsUndefOrs  : UndefOr[js.Object]      = js.undefined
  val className       : UndefOr[String]         = js.undefined
  val style           : UndefOr[String]         = js.undefined
  val id              : UndefOr[String]         = js.undefined
  val useFlyTo        : UndefOr[Boolean]        = js.undefined

  /**
    * Optional reaction about detected location (L.control.locate).
    * Handled automatically inside MapComponent.bindLeafletEvents().
    */
  val onLocationFound: UndefOr[js.Function1[LocationEvent, Unit]] = js.undefined

  val onPopupClose: UndefOr[js.Function1[PopupEvent, Unit]] = js.undefined

  val onZoomStart: UndefOr[js.Function1[Event, Unit]] = js.undefined
  val onZoomEnd: UndefOr[js.Function1[Event, Unit]] = js.undefined

  val onMoveStart: UndefOr[js.Function1[Event, Unit]] = js.undefined
  val onMoveEnd: UndefOr[js.Function1[Event, Unit]] = js.undefined

  val onDragStart: UndefOr[js.Function1[Event, Unit]] = js.undefined
  val onDragEnd: UndefOr[js.Function1[DragEndEvent, Unit]] = js.undefined

}
