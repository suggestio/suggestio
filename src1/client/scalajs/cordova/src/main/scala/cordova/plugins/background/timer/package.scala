package cordova.plugins.background

import cordova.plugins.background.timer.CordovaBackgroundTimer
import org.scalajs.dom.Window

import scala.language.implicitConversions
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.04.2020 22:20
  */
package object timer {

  /** Поддержка dom.window.BackgroundTimer. */
  implicit def window_CdvBgTimer(window: Window): Window_CdvBgTimer =
    window.asInstanceOf[Window_CdvBgTimer]

}


/** API для window для поддержки BackgroundTimer. */
@js.native
sealed trait Window_CdvBgTimer extends js.Object {

  val BackgroundTimer: CordovaBackgroundTimer = js.native

}
