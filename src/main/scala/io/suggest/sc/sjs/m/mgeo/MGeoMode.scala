package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.GeoConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 16:08
 * Description: Режимы геолокации для передачи на сервер suggest.io.
 */
object IMGeoMode {

  def apply(mglOpt: Option[MGeoLoc]): IMGeoMode = {
    mglOpt
      .fold[IMGeoMode](MGeoModeIp)(MGeoModeLoc.apply)
  }

}


trait IMGeoMode {
  def toQsStr: String
}

/** Геолокация по IP. */
case object MGeoModeIp extends IMGeoMode {
  override def toQsStr: String = {
    GeoConstants.GEO_MODE_IP
  }
}


/** Режим геолокации по геокоординатам. */
case class MGeoModeLoc(gl: MGeoLoc) extends IMGeoMode {
  override def toQsStr: String = {
    gl.point.toQsStr + "," + gl.accuracyM
  }
}
