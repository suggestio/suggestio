package io.suggest.sc.sjs.m.mgeo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 17:47
 * Description: Аддон для моделей, поддерживающих опциональное поле geoLoc.
 */
trait MGeoLocUtil {

  def geoLoc      : Option[MGeoLoc]

  def currGeoMode: IMGeoMode = {
    geoLoc.fold[IMGeoMode](MGeoModeIp)(MGeoModeLoc.apply)
  }

}
