package io.suggest.cordova

import io.suggest.sjs.JsApiUtil

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.16 12:37
  * Description: Constants of cordova.
  */
object CordovaConstants {

  private lazy val _isCordova: Boolean = {
    JsApiUtil.isDefinedSafe(
      js.Dynamic.global
        .selectDynamic( cordova.CORDOVA )
    )
  }


  /** Является ли данная платформа - Cordova? */
  def isCordovaPlatform(): Boolean =
    _isCordova


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
