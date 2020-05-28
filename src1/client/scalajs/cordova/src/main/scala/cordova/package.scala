import cordova.plugins.background.fetch.CordovaBackgroundFetch
import cordova.plugins.background.timer.CordovaBackgroundTimer
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.05.2020 11:23
  */
package object cordova {

  /** Название cordova в Global scope.
    * final val обязательно, т.к. используется в аннотациях.
    */
  final val CORDOVA = "cordova"

  /** Поддержка dom.window.BackgroundTimer. */
  implicit def window_CdvExt(window: Window): DomWindow_CordovaExt =
    window.asInstanceOf[DomWindow_CordovaExt]

}

/** API для window для поддержки BackgroundTimer. */
@js.native
sealed trait DomWindow_CordovaExt extends js.Object {

  val BackgroundTimer: CordovaBackgroundTimer = js.native

  val BackgroundFetch: CordovaBackgroundFetch
}
