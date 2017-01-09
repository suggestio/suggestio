package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.GeoConstants
import io.suggest.sjs.common.model.loc.MGeoLoc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 16:08
 * Description: Режимы геолокации для передачи на сервер suggest.io.
 */
@deprecated("use loc env instead", "2016.sep.16")
object IMGeoMode {

  def apply(mglOpt: Option[MGeoLoc]): IMGeoMode = {
    mglOpt
      .fold[IMGeoMode](MGeoModeIp)(MGeoModeLoc.apply)
  }

}


@deprecated("use loc env instead", "2016.sep.16")
trait IMGeoMode {
  def toQsStr: String
}

/** Геолокация по IP. */
@deprecated("use loc env option geo None instead", "2016.sep.16")
case object MGeoModeIp extends IMGeoMode {
  override def toQsStr: String = {
    GeoConstants.GEO_MODE_IP
  }
}


/** Режим геолокации по геокоординатам. */
@deprecated("use loc env option geo Some instead", "2016.sep.16")
case class MGeoModeLoc(gl: MGeoLoc) extends IMGeoMode {
  override def toQsStr: String = {
    gl.point.toQsStr + "," + gl.accuracyM
  }
}
