package io.suggest.sjs.dom2

import io.suggest.sjs.JsApiUtil

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 12:02
  * Description: Abort ctl/signal API for scalajs-dom.
  */
@js.native
@JSGlobal
class AbortController() extends js.Object {

  val signal: AbortSignal = js.native

  def abort(): Unit = js.native

}
@js.native
@JSGlobal
object AbortController extends js.Object
object AbortControllerUtil {
  def isAvailable(): Boolean =
    JsApiUtil.isDefinedSafe( AbortController )
}


@js.native
sealed trait AbortSignal extends js.Object
