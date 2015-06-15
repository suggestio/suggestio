package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.focus.FocusedRenderNames._
import scala.scalajs.js
import js.{WrappedDictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:36
 * Description: Wrap-модель одной focused-карточки вместе с метаданными.
 */

case class MFocAd(json: WrappedDictionary[Any]) {

  /** Отрендеренная карточка. */
  def html: String = json(HTML_FN).toString

  /** Режим рендера сервером. */
  val mode: MFocRenderMode = {
    val modeId = json(MODE_FN).toString
    MFocRenderModes.withName(modeId)
  }

}
