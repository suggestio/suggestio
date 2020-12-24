package com.github.mikbry.materialui.color

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 9:40
  * Description: @see [[https://github.com/mikbry/material-ui-color/blob/master/src/components/ColorButton.jsx]]
  */
object ColorButton {

  val component = JsComponent[Props, Children.None, Null]( Js )

  @js.native
  @JSImport("material-ui-color", "ColorButton")
  object Js extends js.Object

  /** @see [[https://github.com/mikbry/material-ui-color/blob/master/src/components/ColorButton.jsx#L86]] */
  trait Props extends js.Object {
    val color: Color_t
    val size: js.UndefOr[Int] = js.undefined
    val disableAlpha: js.UndefOr[Boolean] = js.undefined
    val borderWidth: js.UndefOr[Int] = js.undefined
    val borderColor: js.UndefOr[String] = js.undefined
    val tooltip: js.UndefOr[String] = js.undefined
  }

}
