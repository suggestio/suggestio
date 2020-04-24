package cordova.plugins.background.timer

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 9:00
  * Description: Настройки таймера.
  */

trait CordovaBackgroundTimerSettings extends js.Object {

  /** interval between ticks of the timer in milliseconds. [60000] */
  val timerInterval: js.UndefOr[Double] = js.undefined

  /** To start timer after the device was restarted. [false] */
  val startOnBoot: js.UndefOr[Boolean] = js.undefined

  /** true -- force stop timer in case the app is terminated. [true] */
  val stopOnTerminate: js.UndefOr[Boolean] = js.undefined

  /** delay timer to start at certain time. */
  val hours: js.UndefOr[Int] = js.undefined
  /** delay timer to start at certain time. */
  val minutes: js.UndefOr[Int] = js.undefined

}
