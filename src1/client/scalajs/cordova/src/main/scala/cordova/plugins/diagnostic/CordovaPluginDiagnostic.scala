package cordova.plugins.diagnostic

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 16:27
  * Description: API for cordova-plugin-diagnostic.
  * 22.01.19 - implemented: CORE, LOCATION, BLUETOOTH.
  */
@js.native
trait CordovaPluginDiagnostic extends js.Object {

  // Core
  def switchToSettings( onSuccess: js.Function0[_],
                        onError: js.Function1[String, _] ): Unit = js.native

  val permissionStatus: PermissionStatuses = js.native

  val cpuArchitecture: CpuArchitectures = js.native

  def getArchitecture( onSuccess: js.Function1[CpuArch_t, _],
                       onError  : js.Function1[String, _]   ): Unit = js.native

  def enableDebug( onSuccess: js.Function0[_] ): Unit = js.native


  // Location
  def isLocationAvailable( onSuccess: js.Function1[Boolean, _],
                           onError  : js.Function1[String, _]
                         ): Unit = js.native

  def isLocationEnabled( onSuccess: js.Function1[Boolean, _],
                         onError  : js.Function1[String, _]
                       ): Unit = js.native

  def isLocationAuthorized( onSuccess: js.Function1[Boolean, _],
                            onError  : js.Function1[String, _]
                          ): Unit = js.native

  def getLocationAuthorizationStatus( onSuccess: js.Function1[PermissionStatus_t, _],
                                      onError  : js.Function1[String, _]
                                    ): Unit = js.native

  def requestLocationAuthorization( onSuccess: js.Function1[PermissionStatus_t, _],
                                    onError  : js.Function1[String, _],
                                    iosMode: String = js.native,
                                  ): Unit = js.native

  /** On Android, this occurs when the Location Mode is changed.
    * function is passed a single string parameter defined as a constant in [[LocationModes]].
    *
    * On iOS, this occurs when location authorization status is changed.
    * This can be triggered either by the user's response to a location permission authorization dialog,
    * by the user turning on/off Location Services, or by the user changing the Location authorization state specifically for your app.
    * Function is passed a single string parameter indicating the new location authorisation status as a constant in [[PermissionStatuses]].
    *
    * @param callback
    */
  def registerLocationStateChangeHandler( callback: js.Function1[LocationMode_t | PermissionStatus_t, _] = js.native ): Unit = js.native


  // Bluetooth
  val bluetoothState: BluetoothStates = js.native

  def isBluetoothAvailable( onSuccess: js.Function1[Boolean, _],
                            onError  : js.Function1[String, _]
                          ): Unit = js.native

  def getBluetoothState(onSuccess: js.Function1[BluetoothState_t, _],
                        onError  : js.Function1[String, _]
                       ): Unit = js.native

  def registerBluetoothStateChangeHandler( callback: js.Function1[BluetoothState_t, _] = js.native ): Unit = js.native

  @JSName("locationMode")
  val locationModeUndef: js.UndefOr[LocationModes] = js.native

}

object CordovaPluginDiagnostic {
  implicit class CpdOpsExt( val cpd: CordovaPluginDiagnostic ) extends AnyVal {

    /** Расширить API методами, пригодными только для андройда. */
    def androidOnly: CordovaPluginDiagnosticAndroid =
      cpd.asInstanceOf[CordovaPluginDiagnosticAndroid]

    def iosOnly: CordovaPluginDiagnosticsIos =
      cpd.asInstanceOf[CordovaPluginDiagnosticsIos]

  }
}


/** Android-only API. */
@js.native
trait CordovaPluginDiagnosticAndroid extends js.Object {

  // Core

  def switchToWirelessSettings(): Unit = js.native

  def switchToMobileDataSettings(): Unit = js.native

  def getPermissionAuthorizationStatus( onSuccess : js.Function1[PermissionStatus_t, _],
                                        onError   : js.Function1[String, _],
                                        permission: Permission_t
                                      ): Unit = js.native

  def getPermissionsAuthorizationStatus( onSuccess    : js.Function1[js.Dictionary[PermissionStatus_t], _],
                                         onError      : js.Function1[String, _],
                                         permissions  : js.Array[Permission_t]
                                       ): Unit = js.native

  def requestRuntimePermissions( onSuccess    : js.Function1[js.Dictionary[PermissionStatus_t], _],
                                 onError      : js.Function1[String, _],
                                 permissions  : js.Array[Permission_t]
                               ): Unit = js.native

  def isRequestingPermission(): Boolean = js.native

  def registerPermissionRequestCompleteHandler( callback: js.Function1[js.Dictionary[PermissionStatus_t], _] = js.native ): Boolean = js.native

  def isDataRoamingEnabled( onSuccess: js.Function1[Boolean, _],
                            onError  : js.Function1[String, _]
                          ): Unit = js.native

  def isADBModeEnabled( onSuccess: js.Function1[Boolean, _],
                        onError  : js.Function1[String, _]
                      ): Unit = js.native

  def isDeviceRooted( onSuccess: js.Function1[Boolean, _],
                      onError  : js.Function1[String, _]
                    ): Unit = js.native

  def restart(onError : js.Function1[String, _],
              cold    : Boolean = js.native   // [false]
             ): Unit = js.native


  // Location

  val locationMode: LocationModes = js.native

  def isGpsLocationAvailable( onSuccess: js.Function1[Boolean, _],
                              onError  : js.Function1[String, _]
                            ): Unit = js.native

  def isGpsLocationEnabled( onSuccess : js.Function1[Boolean, _],
                            onError   : js.Function1[String, _]
                          ): Unit = js.native

  def isNetworkLocationAvailable( onSuccess: js.Function1[Boolean, _],
                                  onError  : js.Function1[String, _]
                                ): Unit = js.native

  def isNetworkLocationEnabled( onSuccess: js.Function1[Boolean, _],
                                onError  : js.Function1[String, _]
                              ): Unit = js.native

  def getLocationMode( onSuccess: js.Function1[LocationMode_t, _],
                       onError  : js.Function1[String, _]
                     ): Unit = js.native


  def switchToLocationSettings(): Unit = js.native


  // Bluetooth

  def isBluetoothEnabled( onSuccess: js.Function1[Boolean, _],
                          onError  : js.Function1[String, _]
                        ): Unit = js.native

  def hasBluetoothSupport( onSuccess: js.Function1[Boolean, _],
                           onError  : js.Function1[String, _]
                         ): Unit = js.native

  def hasBluetoothLESupport( onSuccess: js.Function1[Boolean, _],
                             onError  : js.Function1[String, _]
                           ): Unit = js.native

  def hasBluetoothLEPeripheralSupport( onSuccess: js.Function1[Boolean, _],
                                       onError  : js.Function1[String, _]
                                     ): Unit = js.native

  def setBluetoothState( onSuccess: js.Function0[_],
                         onError  : js.Function0[_],
                         state    : Boolean
                       ): Unit = js.native

  def switchToBluetoothSettings(): Unit = js.native

}


/** iOS-only API. */
@js.native
trait CordovaPluginDiagnosticsIos extends js.Object {

  def isBackgroundRefreshAuthorized( onSuccess: js.Function1[Boolean, _],
                                     onError  : js.Function1[String, _]
                                   ): Unit = js.native

  def getBackgroundRefreshStatus( onSuccess: js.Function1[PermissionStatus_t, _],
                                  onError  : js.Function1[String, _]
                                ): Unit = js.native


  // Bluetooth

  def requestBluetoothAuthorization( onSuccess: js.Function0[_],
                                     onError  : js.Function1[String, _]
                                   ): Unit = js.native

}

