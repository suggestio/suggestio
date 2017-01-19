package com.momentjs

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 10:35
  * Description:
  */
@js.native
class ParsingFlags extends js.Object {

  val overflow: Int = js.native

  val invalidMonth: String = js.native

  val empty: Boolean = js.native

  val nullInput: Boolean = js.native

  val invalidFormat: Boolean = js.native

  val userInvalidated: Boolean = js.native

  val meridiem: String = js.native

  val parsedDateParts: js.Array[js.Any] = js.native

  val unusedTokens: js.Array[String] = js.native

  val unusedInput: js.Array[String] = js.native

}
