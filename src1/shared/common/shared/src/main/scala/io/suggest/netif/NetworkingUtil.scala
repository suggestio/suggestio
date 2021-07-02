package io.suggest.netif

import io.suggest.common.html.HtmlConstants
import io.suggest.scalaz.StringValidationNel
import scalaz.Validation

object NetworkingUtil {

  def MAC_ADDR_EXAMPLE = (":00" * 6).tail

  /** MAC-address regexp, used for validation: 00:ff:aa:33:5a:7b */
  def MAC_ADDR_FULL_RE = "^([0-9a-f]{2}:){5}([0-9a-f]{2})$".r

  /** Shortened MAC-address regexp: 00ffaa335a7b */
  def SHORT_MAC_ADDR_RE = "^[0-9a-f]{12}$".r


  /** Validate network interface mac-address.
    *
    * @param mac MAC-address.
    * @return Validation result with normalized MAC-address value.
    */
  def validateMacAddress(mac: String): StringValidationNel[String] = {
    Validation.liftNel(
      mac
        .toLowerCase()
        .replace('-', ':')
    )(
      {macAddr =>
        !(MAC_ADDR_FULL_RE #:: SHORT_MAC_ADDR_RE #:: LazyList.empty)
          .exists( _ matches macAddr )
      },
      fail = "mac-address invalid"
    )
  }


  /** Strip delimiter from network interface mac-address.
    *
    * @param mac MAC-address.
    * @return Minified MAC-address.
    */
  def minifyMacAddress(mac: String): String = {
    mac.replaceAll("[:-]+", "")
  }

  def unminifyMacAddress(macMinified: String): String = {
    macMinified
      .sliding(2, 2)
      .mkString( HtmlConstants.COLON )
  }

}
