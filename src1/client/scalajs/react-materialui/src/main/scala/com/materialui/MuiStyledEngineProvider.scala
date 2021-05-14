package com.materialui

import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 15:01
  * Description: Component for control over styles.
  * Used during migration v4 => v5
  * @see [[https://github.com/mui-org/material-ui/blob/next/CHANGELOG.md#material-uicorev500-alpha17]]
  */
object MuiStyledEngineProvider {

  val component = JsComponent[Props, Children.Varargs, Null]( Mui.StyledEngineProvider )


  trait Props extends js.Object {
    val injectFirst: js.UndefOr[Boolean] = js.undefined
  }

}
