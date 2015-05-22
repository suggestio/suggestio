package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.IGeoPoint

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:48
 * Description: Реализация модели географической точки.
 */

case class MGeoPoint(
  lat: Double,
  lon: Double
)
  extends IGeoPoint
