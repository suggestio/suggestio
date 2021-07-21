package com.github.mikbry.materialui.color

import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 10:09
  * @see [[https://github.com/mikbry/material-ui-color/blob/master/src/components/ColorInput.jsx]]
  */
object ColorInput {

  val component = JsComponent[Props, Children.None, Null]( Js )


  @js.native
  @JSImport(PACKAGE_NAME, "ColorInput")
  object Js extends js.Object


  /** @see [[https://github.com/mikbry/material-ui-color/blob/master/src/components/ColorInput.jsx]] */
  trait Props extends js.Object {
    val value: js.UndefOr[Color_t] = js.undefined
    val format: js.UndefOr[String] = js.undefined
    val onChange: js.UndefOr[js.Function1[String, Unit]] = js.undefined
    val disableAlpha: js.UndefOr[Boolean] = js.undefined
  }

}
