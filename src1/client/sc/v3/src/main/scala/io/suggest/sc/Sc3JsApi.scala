package io.suggest.sc

import io.suggest.sc.m.UpdateUnsafeScreenOffsetBy

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.2020 13:29
  * Description: Plain JS api for debugging from js-console.
  */
@JSExportTopLevel("___Sio___Sc___")
object Sc3JsApi {

  @JSExport
  def unsafeOffsetAdd(incDecBy: Int): Unit =
    Sc3Module.sc3Circuit.dispatch( UpdateUnsafeScreenOffsetBy(incDecBy) )

}
