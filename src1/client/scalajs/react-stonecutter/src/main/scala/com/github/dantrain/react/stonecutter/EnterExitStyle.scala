package com.github.dantrain.react.stonecutter

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 18:20
  * Description: enterExitStyle object interface.
  */

@js.native
@JSImport("react-stonecutter", "enterExitStyle")
object EnterExitStyle extends js.Object {

  val foldUp: EnterExitStyle = js.native

}


@js.native
trait EnterExitStyle extends js.Object {

  val enter, entered, exit: EnterExitF_t = js.native

}
