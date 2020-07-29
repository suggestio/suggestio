package io.suggest.sc

import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.{SetDebug, UpdateUnsafeScreenOffsetBy}
import io.suggest.sc.m.inx.{GetIndex, MScSwitchCtx, UnIndex}
import io.suggest.spa.DAction

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.2020 13:29
  * Description: Plain JS api for debugging from js-console.
  *
  * Используется длинное неповторимое глобальное имя, т.к. короткое "Sc" приводило к name clash после js-минификации.
  */
@JSExportTopLevel("___Sio___Sc___")
object Sc3JsApi {

  private def _d( action: DAction ) =
    Sc3Module.sc3Circuit.dispatch( action )

  @JSExport
  def unsafeOffsetAdd(incDecBy: Int): Unit =
    _d( UpdateUnsafeScreenOffsetBy(incDecBy) )

  @JSExport
  def unIndex(): Unit =
    _d( UnIndex )

  @JSExport
  def reIndex(): Unit = {
    val a = GetIndex(MScSwitchCtx(
      indexQsArgs = MScIndexArgs(
        geoIntoRcvr = true,
      ),
      showWelcome = false,
    ))
    _d( a )
  }

  @JSExport
  def debug(isDebug: Boolean): Unit =
    _d( SetDebug( isDebug ) )

}
