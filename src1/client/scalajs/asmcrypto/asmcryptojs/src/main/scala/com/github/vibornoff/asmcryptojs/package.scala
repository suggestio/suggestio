package com.github.vibornoff

import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.17 15:36
  */
package object asmcryptojs {

  type Bytes_t = Uint8Array

  type Data_t = String | ArrayBuffer | Bytes_t

}
