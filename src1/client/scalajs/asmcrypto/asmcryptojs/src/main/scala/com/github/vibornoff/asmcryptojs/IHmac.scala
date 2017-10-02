package com.github.vibornoff.asmcryptojs

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.17 15:38
  * Description: Common interface for hmac functions.
  */
@js.native
trait IHmac extends js.Object {

  val BLOCK_SIZE: Int = js.native

  val HMAC_SIZE: Int = js.native

  def hex(data: Data_t, password: Data_t): String = js.native

  def base64(data: Data_t, password: Data_t): String = js.native

  def bytes(data: Data_t, password: Data_t): Bytes_t = js.native

}
