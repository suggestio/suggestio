package io.suggest.sjs.dom2

import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.06.2020 20:41
  * Description: The Network Information API provides information about the system's connection in terms
  * of general connection type (e.g., 'wifi', 'cellular', etc.).
  *
  * Also, relates to cordova-plugin-network-information.
  *
  * @see [[https://developer.mozilla.org/en-US/docs/Web/API/Network_Information_API]]
  * @see [[https://github.com/apache/cordova-plugin-network-information]]
  */
@js.native
trait NetworkInformation extends js.Object {

  val `type`: js.UndefOr[String] = js.native
  val effectiveType: js.UndefOr[String] = js.native

  // in megabits per second = 125 kilobytes per second
  val downlink: js.UndefOr[Double] = js.native
  val downlinkMax: js.UndefOr[Double] = js.native

  var onchange: js.UndefOr[js.Function1[js.Any, Unit]] = js.native
  var ontypechange: js.UndefOr[js.Function1[js.Any, Unit]] = js.native
  val rtt: js.UndefOr[Double] = js.native

  val saveData: js.UndefOr[Boolean] = js.undefined

}


object NetworkInformation {

  /** Т.к. API очень экспериментальное, надо до него стучаться. */
  def safeGet(): Try[Option[NetworkInformation]] = {
    Try {
      val n = dom.window.navigator
      n.connection
        .orElse( n.mozConnection )
        .orElse( n.webkitConnection )
        .toOption
    }
  }


  /**
    * NetworkInformation.type enum,
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation/type]]
    * @see [[https://wicg.github.io/netinfo/#underlying-connection-technology]]
    */
  object Types {

    final def BLUETOOTH = "bluetooth"
    final def WIMAX = "wimax"
    final def OTHER = "other"
    final def UNKNOWN = "unknown"
    final def ETHERNET = "ethernet"
    final def WIFI = "wifi"
    final def CELL = "cellular"
    final def NONE = "none"
    final def MIXED = "mixed"

  }


  /** NetworkInformation.effectiveType enum
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation/effectiveType]]
    * @see [[https://wicg.github.io/netinfo/#effective-connection-types]]
    */
  object EffectiveTypes {

    final def SLOW_2G = "slow-2g"
    final def `2G` = "2g"
    final def `3G` = "3g"
    final def `4G` = "4g"

  }

}


/** Интерфейс для navigator с возможной поддержкой connection-поля. */
@js.native
sealed trait Dom2WndNav_Connection extends js.Object {
  /** @see [[https://developer.mozilla.org/en-US/docs/Web/API/Navigator/connection]] */
  val connection: js.UndefOr[NetworkInformation] = js.native
  val mozConnection: js.UndefOr[NetworkInformation] = js.native
  val webkitConnection: js.UndefOr[NetworkInformation] = js.native
}
