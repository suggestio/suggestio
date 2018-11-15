package cordova

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.16 12:37
  * Description: Constants of cordova.
  */
object CordovaConstants {

  /** Является ли данная платформа - Cordova? */
  def isCordovaPlatform(): Boolean = !js.isUndefined( Cordova )


  object Events {

    def DEVICE_READY      = "deviceready"

    def PAUSE             = "pause"

    def RESUME            = "resume"

    def BACK_BUTTON       = "backbutton"

    def MENU_BUTTON       = "menubutton"

    def SEARCH_BUTTON     = "searchbutton"

    def VOL_DOWN_BUTTON   = "volumedownbutton"

    def VOL_UP_BUTTON     = "volumedownbutton"

  }

}
