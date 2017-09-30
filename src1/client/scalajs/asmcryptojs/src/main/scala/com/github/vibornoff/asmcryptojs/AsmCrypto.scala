package com.github.vibornoff.asmcryptojs

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.17 15:06
  * Description: Top-level api for asmcrypto.js global object.
  */
@js.native
trait IAsmCrypto extends js.Object {

  val SHA1: IHash = js.native

  val SHA256: IHash = js.native

  val HMAC_SHA1: IHmac = js.native

  val HMAC_SHA256: IHmac = js.native

  // TODO all other APIs

}


/** asmCrypto via global scope. */
@js.native
@JSGlobal("asmCrypto")
object AsmCryptoGlobal extends IAsmCrypto


/** asmCrypto via require(). */
@js.native
@JSImport("asmcrypto.js", JSImport.Namespace)
object AsmCrypto extends IAsmCrypto

