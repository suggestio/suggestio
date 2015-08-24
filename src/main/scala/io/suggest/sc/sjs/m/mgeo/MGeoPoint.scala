package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.IGeoPoint
import org.scalajs.dom.Coordinates

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:48
 * Description: Реализация модели географической точки.
 */

object MGeoPoint {

  def apply(domCoords: Coordinates): MGeoPoint = {
    apply(
      lat = domCoords.latitude,
      lon = domCoords.longitude
    )
  }

}

case class MGeoPoint(
  override val lat: Double,
  override val lon: Double
)
  extends IGeoPoint
