package cordova.plugins.background.fetch

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.05.2020 11:59
  * Description: Интерфейс для конфигурации фонового режима.
  * [[https://github.com/transistorsoft/cordova-plugin-background-fetch#config]]
  */
trait CbfConfig extends js.Object {

  /** "com.transistorsoft.customtask". Игнорируется в configure(). */
  val taskId: js.UndefOr[String] = js.undefined

  /** milliseconds. */
  val delay: js.UndefOr[Int] = js.undefined

  /** [false] */
  val periodic: js.UndefOr[Boolean] = js.undefined

  /** minutes [15]. */
  val minimumFetchInterval: js.UndefOr[Int] = js.undefined

  // Android-only
  /** Set `false` to continue background-fetch events after user terminates the app. [true] */
  val stopOnTerminate: js.UndefOr[Boolean] = js.undefined

  /** Set `true` to initiate background-fetch events when the device is rebooted. [false]
    * requires stopOnTerminate: false
    */
  val startOnBoot: js.UndefOr[Boolean] = js.undefined

  /** `true` will bypass JobScheduler to use Android's older AlarmManager API,
    * resulting in more accurate task-execution at the cost of higher battery usage. [false]
    */
  val forceAlarmManager: js.UndefOr[Boolean] = js.undefined

  val requiredNetworkType: js.UndefOr[CbfNetworkType_t] = js.undefined

  val requiresBatteryNotLow: js.UndefOr[Boolean] = js.undefined

  val requiresStorageNotLow: js.UndefOr[Boolean] = js.undefined

  val requiresCharging: js.UndefOr[Boolean] = js.undefined

  val requiresDeviceIdle: js.UndefOr[Boolean] = js.undefined

  /** https://github.com/transistorsoft/cordova-plugin-background-fetch#config-boolean-enableheadless-false */
  val enableHeadless: js.UndefOr[Boolean] = js.undefined

}
