package com.github.mikbry.materialui.color

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 7:57
  * Description: ColorPicker component API.
  * @see [[https://github.com/mikbry/material-ui-color#colorpicker]]
  */
object ColorPicker {

  val component = JsComponent[Props, Children.None, Null]( Js )

  @js.native
  @JSImport(PACKAGE_NAME, "ColorPicker")
  object Js extends js.Object


  trait Props extends js.Object {
    val defaultValue: js.UndefOr[Color_t] = js.undefined
    val value: js.UndefOr[Color_t] = js.undefined
    val disableTextfield: js.UndefOr[Boolean] = js.undefined
    val deferred: js.UndefOr[Boolean] = js.undefined
    val palette: js.UndefOr[Palette_t] = js.undefined
    val inputFormats: js.UndefOr[InputFormats_t] = js.undefined
    val onChange: js.UndefOr[js.Function1[Color, Unit]] = js.undefined
    val onOpen: js.UndefOr[js.Function1[Boolean, Unit]] = js.undefined
    val openAtStart: js.UndefOr[Boolean] = js.undefined
    /** Использовать другой компонент вместо Popover. */
    val doPopup: js.UndefOr[js.Function1[raw.React.Element, raw.React.Element]] = js.undefined
    val disableAlpha: js.UndefOr[Boolean] = js.undefined
    val hideTextfield: js.UndefOr[Boolean] = js.undefined
  }

}
