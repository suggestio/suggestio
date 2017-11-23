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
@JSImport(REACT_STONECUTTER, "enterExitStyle")
object EnterExitStyle extends js.Object {

  val foldUp: EnterExitStyle = js.native

  val fromBottom: EnterExitStyle = js.native

  val fromCenter: EnterExitStyle = js.native

  val fromLeftToRight: EnterExitStyle = js.native

  val fromTop: EnterExitStyle = js.native

  val newspaper: EnterExitStyle = js.native

  val simple: EnterExitStyle = js.native

  val skew: EnterExitStyle = js.native

}


@js.native
trait EnterExitStyle extends js.Object {

  val enter, entered, exit: EnterExitF_t = js.native

}
