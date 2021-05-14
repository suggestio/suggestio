package com.materialui

import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 15:01
  * Description: Component for styles control.
  * Useful during migration v4 => v5
  * @see [[https://github.com/mui-org/material-ui/blob/next/CHANGELOG.md#material-uicorev500-alpha17]]
  */
object MuiStylesProvider {

  val component = JsComponent[Props, Children.Varargs, Null]( Mui.StylesProvider )


  trait Props extends js.Object {
    val disableGeneration: js.UndefOr[Boolean] = js.undefined
    val generateClassName: js.UndefOr[js.Function] = js.undefined
    val injectFirst: js.UndefOr[Boolean] = js.undefined
    val jss: js.UndefOr[js.Object] = js.undefined
    val serverGenerateClassName: js.UndefOr[js.Function] = js.undefined
    val sheetsCache: js.UndefOr[js.Object] = js.undefined
    val sheetsManager: js.UndefOr[js.Object] = js.undefined
    val sheetsRegistry: js.UndefOr[js.Object] = js.undefined
  }

}
