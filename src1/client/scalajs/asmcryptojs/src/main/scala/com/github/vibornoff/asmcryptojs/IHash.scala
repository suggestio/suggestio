package com.github.vibornoff.asmcryptojs

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.17 15:33
  * Description: Common API interface for hashing functions.
  */
@js.native
trait IHash extends js.Object {

  val BLOCK_SIZE: Int = js.native

  val HASH_SIZE: Int = js.native

  def hex(data: Data_t): String = js.native

  def base64(data: Data_t): String = js.native

  def bytes(data: Data_t): Bytes_t = js.native

}
