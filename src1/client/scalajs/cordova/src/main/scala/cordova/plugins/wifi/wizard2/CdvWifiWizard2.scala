package cordova.plugins.wifi.wizard2

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.|

@js.native
@JSGlobal("WifiWizard2")
object CdvWifiWizard2 extends js.Object {

  // Android, iOS:

  def getConnectedSSID(): js.Promise[String] = js.native
  def getConnectedBSSID(): js.Promise[String] = js.native

  def timeout(delayMs: Double): js.Promise[_] = js.native


  // iOS:

  /** @return ssid */
  def iOSConnectNetwork(ssid: String, ssidPassword: String = js.native): js.Promise[String] = js.native

  /** @return ssid */
  def iOSDisconnectNetwork(ssid: String): js.Promise[String] = js.native


  // Android:

  /** @return "NETWORK_CONNECTION_COMPLETED" */
  def connect(ssid: String,
              bindAll: Boolean = js.native,
              password: String = js.native,
              algorithm: String = js.native,
              isHiddenSSID: Boolean = js.native
             ): js.Promise[String] = js.native

  def disconnect(ssid: String = js.native): js.Promise[String] = js.native

  def formatWifiConfig(ssid: String,
                       password: String = js.native,
                       algorithm: String = js.native,
                       isHiddenSSID: Boolean = js.native
                      ): js.Promise[WifiConfig] = js.native
  def formatWPAConfig(ssid: String,
                      password: String = js.native,
                      isHiddenSSID: Boolean = js.native): js.Promise[WifiConfig] = js.native

  def add(wifiConfig: WifiConfig): js.Promise[AndroidNetworkIdOrStub_t] = js.native
  def remove(ssidOrNetId: String | Int): js.Promise[String] = js.native

  /** @return [SSID] */
  def listNetworks(): js.Promise[js.Array[String]] = js.native
  def scan(options: js.Array[ScanOptions] = js.native): js.Promise[js.Array[WifiScanResult]] = js.native
  def getScanResults(): js.Promise[js.Array[WifiScanResult]] = js.native
  def startScan(): js.Promise[Unit] = js.native
  def isWifiEnabled(): js.Promise[Boolean] = js.native
  def setWifiEnabled(enabled: Boolean): js.Promise[Unit] = js.native
  def getConnectedNetworkID(): js.Promise[AndroidNetworkId_t] = js.native

  // v3.1.1+
  def resetBindAll(): js.Promise[String] = js.native
  def setBindAll(): js.Promise[String] = js.native
  def canConnectToInternet(): js.Promise[Boolean] = js.native
  def canConnectToRouter(): js.Promise[Boolean] = js.native

  // v3.0.0+
  def isConnectedToInternet(): js.Promise[Boolean] = js.native
  def canPingWifiRouter(): js.Promise[Boolean] = js.native
  def enableWifi(): js.Promise[Unit] = js.native
  def disableWifi(): js.Promise[Unit] = js.native
  def getWifiIP(): js.Promise[String] = js.native
  def getWifiRouterIP(): js.Promise[String] = js.native
  def getWifiIPInfo(): js.Promise[WifiIpInfo] = js.native
  def reconnect(): js.Promise[String] = js.native
  def reassociate(): js.Promise[String] = js.native
  def getSSIDNetworkID(ssid: String): js.Promise[AndroidNetworkId_t] = js.native
  def disable(ssidOrNetId: String | AndroidNetworkId_t): js.Promise[String] = js.native
  def requestPermission(): js.Promise[String] = js.native
  def enable(ssid: String | AndroidNetworkId_t,
             bindAll: Boolean = js.native,
             waitForConnection: Boolean = js.native): js.Promise[String] = js.native

}


trait WifiConfig extends js.Object {
  val SSID: String
  val isHiddenSSID: Boolean
  val auth: WifiAuth
}

trait WifiAuth extends js.Object {
  val algorithm: String
  val password: js.UndefOr[String] = js.undefined
}


trait WifiScanResult extends js.Object {
  /** @return -80 (in dBm; also known as the RSSI) */
  val level: Int
  /** @return 2437 (in MHz) */
  val frequency: Int
  val SSID: String
  /** @return "ab:44:cd:55:6f:6f" */
  val BSSID: String
  /** @return "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS][WPS]" */
  val capabilities: String
  /** @return 1997694028251 (ms) */
  val timestamp: Double
  /** Wrap into Option() to use the value. */
  val channelWidth, centerFreq0, centerFreq1: Int | Null
}


trait ScanOptions extends js.Object {
  val numLevels: js.UndefOr[Int | Boolean] = js.undefined
}


trait WifiIpInfo extends js.Object {
  /** "192.168.1.2" */
  val ip: String
  /** "255.255.255.0" */
  val subnet: String
}