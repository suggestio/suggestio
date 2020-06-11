package cordova.plugins.statusbar

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.2020 11:29
  */
@js.native
@JSGlobal("StatusBar")
object CdvStatusBar extends js.Object {

  def overlaysWebView(overlaying: Boolean): Unit = js.native

  /** dark text, for light backgrounds */
  def styleDefault(): Unit = js.native

  /** light text, for dark backgrounds */
  def styleLightContent(): Unit = js.native

  @deprecated("Use styleLightContent() instead", "")
  def styleBlackTranslucent(): Unit = js.native

  @deprecated("Use styleLightContent() instead", "")
  def styleBlackOpaque(): Unit = js.native

  def backgroundColorByName(name: String): Unit = js.native

  def backgroundColorByHexString(colorHex: String): Unit = js.native

  def hide(): Unit = js.native

  def show(): Unit = js.native

  val isVisible: Boolean = js.native

}
