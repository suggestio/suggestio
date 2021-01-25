package org.js.react

import io.suggest.sjs.leaflet.control.attribution.{Attribution, AttributionOptions}
import io.suggest.sjs.leaflet.control.zoom.{Zoom, ZoomOptions}
import io.suggest.sjs.leaflet.path.circle.{Circle, CircleMarker}
import japgolly.scalajs.react.raw
import org.js.react.leaflet.core.{CircleMarkerProps, PathProps}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:31
  */
package object leaflet {

  final val REACT_LEAFLET_PACKAGE = "react-leaflet"

  type AttributionControlProps = AttributionOptions

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "AttributionControl")
  val AttributionControl: raw.React.ForwardRefComponent[AttributionControlProps, Attribution] = js.native

  type CircleProps = CircleMarkerProps

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "Circle")
  val Circle: raw.React.ForwardRefComponent[CircleProps, Circle] = js.native

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "CircleMarker")
  val CircleMarker: raw.React.ForwardRefComponent[CircleMarkerProps, CircleMarker] = js.native

  type ZoomControlProps = ZoomOptions

  @js.native
  @JSImport(REACT_LEAFLET_PACKAGE, "ZoomControl")
  val ZoomControl: raw.React.ForwardRefComponent[ZoomControlProps, Zoom] = js.native

  

}
