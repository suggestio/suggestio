package cordova.plugins.background.geolocation

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 21:54
  */
object CdvBgGeo {

  /** Проверка доступности API. */
  def isAvailable(): Boolean =
    Try( !js.isUndefined(CdvBackgroundGeolocation) ) getOrElse false

  object Events {

    final def LOCATION = "location"
    final def STATIONARY = "stationary"
    // Android:
    final def ACTIVITY = "activity"
    final def ERROR = "error"
    final def AUTHORIZATION = "authorization"
    final def START = "start"
    final def STOP = "stop"
    // Android:
    final def FOREGROUND = "foreground"
    // Android:
    final def BACKGROUND = "background"
    final def ABORT_REQUESTED = "abort_requested"
    final def HTTP_AUTHORIZATION = "http_authorization"

  }

}
