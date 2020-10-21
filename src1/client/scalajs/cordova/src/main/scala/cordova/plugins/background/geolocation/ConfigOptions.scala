package cordova.plugins.background.geolocation

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 16:07
  * Description: Option for .configure().
  */
trait ConfigOptions extends js.Object {

  /** Set location provider. [DISTANCE_FILTER_PROVIDER]
    * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/PROVIDERS.md]]
    */
  val locationProvider: js.UndefOr[LocationProvider_t] = js.undefined

  /** Desired accuracy in meters. [MEDIUM_ACCURACY]
    *
    * Possible values [HIGH_ACCURACY, MEDIUM_ACCURACY, LOW_ACCURACY, PASSIVE_ACCURACY].
    * Accuracy has direct effect on power drain. Lower accuracy = lower power drain.
    */
  val desiredAccuracy: js.UndefOr[Accuracy_t] = js.undefined

  /** Stationary radius in meters. [50]
    * When stopped, the minimum distance the device must move beyond the stationary
    * location for aggressive background-tracking to engage.
    */
  val stationaryRadius: js.UndefOr[Double] = js.undefined

  /** When enabled, the plugin will emit sounds for life-cycle events of background-geolocation! [false]
    * See debugging sounds table.
    */
  val debug: js.UndefOr[Boolean] = js.undefined

  /** The minimum distance (measured in meters) a device must move horizontally before an update event is generated. [500] */
  val distanceFilter: js.UndefOr[Double] = js.undefined

  /**  Enable this in order to force a stop() when the application terminated  [true]
    * (e.g. on iOS, double-tap home button, swipe away the app).
    */
  val stopOnTerminate: js.UndefOr[Boolean] = js.undefined


  // --------------------------------- Android only ------------------------------------

  /** Start background service on device boot. [false] */
  val startOnBoot: js.UndefOr[Boolean] = js.undefined

  /** The minimum time interval between location updates in milliseconds. [60000] */
  val interval: js.UndefOr[Double] = js.undefined

  /** Fastest rate in milliseconds at which your app can handle location updates. [120000] */
  val fastestInterval: js.UndefOr[Double] = js.undefined

  /** Rate in milliseconds at which activity recognition occurs. [10000]
    * Larger values will result in fewer activity detections while improving battery life.
    */
  val activitiesInterval: js.UndefOr[Double] = js.undefined

  /** Stop location updates, when the STILL activity is detected. [true] */
  @deprecated val stopOnStillActivity: js.UndefOr[Boolean] = js.undefined

  /** Enable/disable local notifications when tracking and syncing locations. [true] */
  val notificationsEnabled: js.UndefOr[Boolean] = js.undefined

  /** Allow location sync service to run in foreground state. [false]
    * Foreground state also requires a notification to be presented to the user. */
  val startForeground: js.UndefOr[Boolean] = js.undefined

  /** Custom notification title in the drawer. ["Background tracking"]
    * (goes with startForeground)
    */
  val notificationTitle: js.UndefOr[String] = js.undefined

  /** Custom notification text in the drawer. ["ENABLED"]
    * (goes with startForeground)
    */
  val notificationText: js.UndefOr[String] = js.undefined

  /** The accent color to use for notification. Eg. #4CAF50.
    * (goes with startForeground)
    */
  val notificationIconColor: js.UndefOr[String] = js.undefined

  /** The filename of a custom notification icon. (goes with startForeground)
    * @see Android quirks.
    */
  val notificationIconLarge: js.UndefOr[String] = js.undefined

  /** The filename of a custom notification icon. (goes with startForeground)
    * @see Android quirks.
    */
  val notificationIconSmall: js.UndefOr[String] = js.undefined


  // --------------------------------- iOS only ------------------------------------

  /** Presumably, this affects iOS GPS algorithm. ["OtherNavigation"]
    *
    * [AutomotiveNavigation, OtherNavigation, Fitness, Other]
    */
  val activityType: js.UndefOr[String] = js.undefined

  /** Pauses location updates when app is paused. [false] */
  val pauseLocationUpdates: js.UndefOr[Boolean] = js.undefined

  /** Switch to less accurate significant changes and region monitory when in background. [false] */
  val saveBatteryOnBackground: js.UndefOr[Boolean] = js.undefined


  // --------------------------------- All platforms ------------------------------------

  /** Server url where to send HTTP POST with recorded locations. */
  val url: js.UndefOr[String] = js.undefined

  /** Server url where to send fail to post locations. */
  val syncUrl: js.UndefOr[String] = js.undefined

  /** Specifies how many previously failed locations will be sent to server at once. [100] */
  val syncThreshold: js.UndefOr[Int] = js.undefined

  /** Optional HTTP headers sent along in HTTP request. */
  val httpHeaders: js.UndefOr[js.Dictionary[String]] = js.undefined

  /** Limit maximum number of locations stored into db. [10000] */
  val maxLocations: js.UndefOr[Int] = js.undefined

  /** Customization post template. */
  val postTemplate: js.UndefOr[js.Dictionary[String] | js.Array[String]] = js.undefined

}
