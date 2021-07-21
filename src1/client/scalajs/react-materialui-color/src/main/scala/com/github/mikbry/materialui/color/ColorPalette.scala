package com.github.mikbry.materialui.color

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 9:55
  * Description: @see [[https://github.com/mikbry/material-ui-color/blob/master/src/components/ColorPalette.jsx]]
  */
object ColorPalette {

  val component = JsComponent[Props, Children.None, Null]( Js )


  @js.native
  @JSImport(PACKAGE_NAME, "ColorPalette")
  object Js extends js.Object


  /** @see [[https://github.com/mikbry/material-ui-color/blob/master/src/components/ColorPalette.jsx#L53]] */
  trait Props extends js.Object {
    val borderWidth: js.UndefOr[Int] = js.undefined
    val size: js.UndefOr[Int] = js.undefined
    val palette: js.UndefOr[Palette_t] = js.undefined
    /** function(translatedName, palette[name]) => * */
    val onSelect: js.UndefOr[js.Function2[String, String, Unit]] = js.undefined
    val disableAlpha: js.UndefOr[Boolean] = js.undefined
  }

}
