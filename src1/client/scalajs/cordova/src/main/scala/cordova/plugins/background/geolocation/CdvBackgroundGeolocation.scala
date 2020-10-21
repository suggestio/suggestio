package cordova.plugins.background.geolocation

import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import io.suggest.err.ToThrowableJs._

import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 16:04
  * Description: Cordova plugin BackgroundGeolocation JS API.
  */
@js.native
@JSGlobal("BackgroundGeolocation")
object CdvBackgroundGeolocation extends ICdvBackgroundGeolocation


@js.native
/** @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/www/BackgroundGeolocation.d.ts#L458]] */
trait ICdvBackgroundGeolocation extends js.Object {

  val DISTANCE_FILTER_PROVIDER,
      ACTIVITY_PROVIDER,
      RAW_PROVIDER: LocationProvider_t = js.native

  val BACKGROUND_MODE,
      FOREGROUND_MODE: ServiceMode = js.native

  val NOT_AUTHORIZED,
      AUTHORIZED,
      AUTHORIZED_FOREGROUND: AuthorizationStatus = js.native

  val HIGH_ACCURACY,
      MEDIUM_ACCURACY,
      LOW_ACCURACY,
      PASSIVE_ACCURACY: Accuracy_t = js.native

  val LOG_ERROR,
      LOG_WARN,
      LOG_INFO,
      LOG_DEBUG,
      LOG_TRACE: LogLevel = js.native

  val PERMISSION_DENIED,
      LOCATION_UNAVAILABLE,
      TIMEOUT: LocationErrorCode = js.native


  def configure(options: ConfigOptions,
                success: js.Function0[Unit] = js.native,
                failure: js.Function1[js.Any, Unit] = js.native,
               ): Unit = js.native

  def getConfig(success: js.Function1[ConfigOptions, Unit],
                fail: js.Function1[js.Any, Unit] = js.native,
               ): Unit = js.native

  def start(): Unit = js.native

  def stop(): Unit = js.native

  def getCurrentLocation(success: js.Function1[Location, Unit],
                         fail: js.Function1[js.Any, Unit] = js.native,
                         options: js.UndefOr[LocationOptions] = js.undefined,
                        ): Unit = js.native

  def getStationaryLocation(success: js.Function1[StationaryLocation | Null, Unit],
                            fail: js.Function1[BackgroundGeolocationError, Unit],
                           ): Unit = js.native

  @deprecated("Use checkStatus()", "Next major version")
  def isLocationEnabled(success: js.Function1[Boolean, Unit],
                        fail: js.Function1[js.Any, Unit] = js.native,
                       ): Unit = js.native

  def checkStatus(success: js.Function1[ServiceStatus, Unit],
                  fail: js.Function1[BackgroundGeolocationError, Unit] = js.native): Unit = js.native

  def showAppSettings(): Unit = js.native

  /** Android-only. */
  def showLocationSettings(): Unit = js.native

  def getLocations(success: js.Function1[js.Array[Location], Unit],
                   fail: js.Function1[BackgroundGeolocationError, Unit] = js.native): Unit = js.native

  def getValidLocations(success: js.Function1[js.Array[Location], Unit],
                        fail: js.Function1[BackgroundGeolocationError, Unit] = js.native): Unit = js.native

  def deleteLocation(locationId: LocationId_t,
                     success: js.Function0[Unit] = js.native,
                     fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
                    ): Unit = js.native

  def deleteAllLocations(success: js.Function0[Unit] = js.native,
                         fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
                        ): Unit = js.native

  /** iOS */
  def switchMode(modeId: ServiceMode,
                 success: js.Function0[Unit] = js.native,
                 fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
                ): Unit = js.native

  def forceSync( success: js.Function0[Unit] = js.native,
                 fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
               ): Unit = js.native

  def getLogEntries(limit: Int, fromId: Int, minLevel: LogLevel,
                    success: js.Function1[js.Array[LogEntry], Unit],
                    fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
                   ): Unit = js.native

  def removeAllListeners(eventType: String = js.native): Unit = js.native

  def startTask(success: js.Function1[TaskId, Unit],
                fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
               ): Unit = js.native

  def endTask(taskId: TaskId,
              success: js.Function0[Unit] = js.native,
              fail: js.Function1[BackgroundGeolocationError, Unit] = js.native,
             ): Unit = js.native


  def headlessTask(task: HeadlessTaskEvent): Unit = js.native

  def on(eventName: String, callback: js.Function): Unit = js.native

}


object ICdvBackgroundGeolocation {

  implicit final class CdvBgGeoExt( private val cdvBgGeo: ICdvBackgroundGeolocation ) extends AnyVal {

    def Events = CdvBgGeo.Events


    def configureF(options: ConfigOptions): Future[Unit] =
      JsApiUtil.call0ErrFut( cdvBgGeo.configure(options, _, _) )

    def getConfigF(): Future[ConfigOptions] =
      JsApiUtil.call1ErrFut[ConfigOptions, js.Any]( cdvBgGeo.getConfig )

    def getCurrentLocationF(options: js.UndefOr[LocationOptions] = js.undefined): Future[Location] =
      JsApiUtil.call1ErrFut( cdvBgGeo.getCurrentLocation(_, _, options) )

    def getStationaryLocationF(): Future[StationaryLocation | Null] =
      JsApiUtil.call1ErrFut( cdvBgGeo.getStationaryLocation )

    @deprecated("Use checkStatusF()", "Next major version")
    def isLocationEnabledF(): Future[Boolean] =
      JsApiUtil.call1ErrFut( cdvBgGeo.isLocationEnabled )

    def checkStatusF(): Future[ServiceStatus] =
      JsApiUtil.call1ErrFut( cdvBgGeo.checkStatus )

    def getLocationsF(): Future[js.Array[Location]] =
      JsApiUtil.call1ErrFut( cdvBgGeo.getLocations )

    def getValidLocationsF(): Future[js.Array[Location]] =
      JsApiUtil.call1ErrFut( cdvBgGeo.getValidLocations )

    def deleteLocationF(locationId: LocationId_t): Future[Unit] =
      JsApiUtil.call0ErrFut( cdvBgGeo.deleteLocation(locationId, _, _) )

    def deleteAllLocationsF(): Future[Unit] =
      JsApiUtil.call0ErrFut( cdvBgGeo.deleteAllLocations )

    def switchModeF(modeId: ServiceMode): Future[Unit] =
      JsApiUtil.call0ErrFut( cdvBgGeo.switchMode(modeId, _, _) )

    def forceSyncF(): Future[Unit] =
      JsApiUtil.call0ErrFut( cdvBgGeo.forceSync )

    def getLogEntriesF(limit: Int, fromId: Int, minLevel: LogLevel): Future[js.Array[LogEntry]] =
      JsApiUtil.call1ErrFut( cdvBgGeo.getLogEntries(limit, fromId, minLevel, _, _) )

    def startTaskF(): Future[TaskId] =
      JsApiUtil.call1ErrFut( cdvBgGeo.startTask )

    def endTaskF(taskId: TaskId): Future[Unit] =
      JsApiUtil.call0ErrFut( cdvBgGeo.endTask(taskId, _, _) )

    def onLocation(callback: Location => Unit): Unit =
      cdvBgGeo.on( Events.LOCATION, callback )

    def onStationary(callback: StationaryLocation => Unit): Unit =
      cdvBgGeo.on( Events.STATIONARY, callback )

    def onActivity(callback: Activity => Unit): Unit =
      cdvBgGeo.on( Events.ACTIVITY, callback )

    def onStart(callback: () => Unit): Unit =
      cdvBgGeo.on( Events.START, callback )

    def onStop(callback: () => Unit): Unit =
      cdvBgGeo.on( Events.STOP, callback )

    def onError(callback: BackgroundGeolocationError => Unit): Unit =
      cdvBgGeo.on( Events.ERROR, callback )

    def onAuthorization(callback: AuthorizationStatus => Unit): Unit =
      cdvBgGeo.on( Events.AUTHORIZATION, callback )

    def onForeground(callback: () => Unit): Unit =
      cdvBgGeo.on( Events.FOREGROUND, callback )

    def onBackground(callback: () => Unit): Unit =
      cdvBgGeo.on( Events.BACKGROUND, callback )

    def onAbortRequested(callback: () => Unit): Unit =
      cdvBgGeo.on( Events.ABORT_REQUESTED, callback )

    def onHttpAuthorization(callback: () => Unit): Unit =
      cdvBgGeo.on( Events.HTTP_AUTHORIZATION, callback )

  }

}
