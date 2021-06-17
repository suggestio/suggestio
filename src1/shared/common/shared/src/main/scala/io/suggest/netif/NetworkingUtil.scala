package io.suggest.netif

import io.suggest.scalaz.StringValidationNel
import scalaz.Validation

object NetworkingUtil {

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
      !SHORT_MAC_ADDR_RE.matches(_),
      fail = "mac-address invalid"
    )
  }

}
