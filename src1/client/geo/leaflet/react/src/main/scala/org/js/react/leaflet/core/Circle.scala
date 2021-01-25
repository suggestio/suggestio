package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.LatLngExpression
import io.suggest.sjs.leaflet.path.circle.{CircleMarker, CircleMarkerOptions}
import japgolly.scalajs.react.raw.PropsChildren

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 16:13
  */
trait CircleMarkerProps extends CircleMarkerOptions with PathProps {
  val center: LatLngExpression
  val children: js.UndefOr[PropsChildren] = js.undefined
}


@js.native
@JSImport(PACKAGE_NAME, "updateCircle")
object updateCircle extends js.Function3[CircleMarker, CircleMarkerProps, CircleMarkerProps, Unit] {
  override def apply(layer: CircleMarker,
                     props: CircleMarkerProps,
                     prevProps: CircleMarkerProps
                    ): Unit = js.native
}
