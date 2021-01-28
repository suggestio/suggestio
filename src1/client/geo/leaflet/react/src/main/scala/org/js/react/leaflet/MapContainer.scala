package org.js.react.leaflet

import io.suggest.sjs.leaflet.LatLngBoundsExpression
import io.suggest.sjs.leaflet.map.{FitBoundsOptions, LMap, MapOptions}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import org.js.react.leaflet.core.EventedProps
import org.scalajs.dom
import org.scalajs.dom.html.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 16:01
  */
object MapContainer {

  val component = JsComponent[MapContainerProps, Children.Varargs, Null]( Js )

  def apply( props: MapContainerProps = new MapContainerProps {} )(children: VdomNode*) =
    component(props)(children: _*)

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "MapContainer")
  object Js extends js.Function

}


trait MapContainerProps extends MapOptions with EventedProps {
  val bounds: js.UndefOr[LatLngBoundsExpression] = js.undefined
  val boundsOptions: js.UndefOr[FitBoundsOptions] = js.undefined
  val className: js.UndefOr[String] = js.undefined
  val id: js.UndefOr[String] = js.undefined
  val placeholder: js.UndefOr[raw.React.Node] = js.undefined
  val style: js.UndefOr[js.Object /* CSSProperties */] = js.undefined
  val whenCreated: js.UndefOr[js.Function1[LMap, Unit]] = js.undefined
  val whenReady: js.UndefOr[js.Function0[Unit]] = js.undefined
}


@js.native
@JSImport(REACT_LEAFLET_PACKAGE, "useMapElement")
object useMapElement extends js.Function2[raw.React.RefHandle[dom.html.Element], MapContainerProps, LMap | Null] {
  override def apply(mapRef: React.RefHandle[Element], props: MapContainerProps): LMap | Null = js.native
}