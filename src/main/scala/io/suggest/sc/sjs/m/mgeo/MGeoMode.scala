package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.{IGeoPoint, GeoConstants}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 16:08
 * Description: Режимы геолокации.
 */
trait IMGeoMode {
  def toQsStr: String
}

/** Геолокация по IP. */
object MGeoModeIp extends IMGeoMode {
  override def toQsStr: String = {
    GeoConstants.GEO_MODE_IP
  }

}


/** Дефолтовая реализации модели гео-точки. */
case class MGeoPoint(lat: Double, lon: Double) extends IGeoPoint

/** Геолокация по геокоординатам. */
case class MGeoModeLoc(point: MGeoPoint, accuracy: Double) extends IMGeoMode {
  override def toQsStr: String = {
    point.toQsStr + "," + accuracy
  }
}
