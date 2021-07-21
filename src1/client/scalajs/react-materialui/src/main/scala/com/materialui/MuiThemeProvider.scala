package com.materialui

import japgolly.scalajs.react._

import scala.scalajs.js


object MuiThemeProvider {

  val component = JsComponent[Props, Children.Varargs, Null](Mui.ThemeProvider)

  trait Props extends js.Object {
    val theme:    MuiTheme    // TODO MuiTheme | js.FunctionX

    val disableStylesGeneration: js.UndefOr[Boolean]       = js.undefined
    val sheetsManager: js.UndefOr[js.Object]               = js.undefined

    val key:      js.UndefOr[String]                       = js.undefined
    val ref:      js.UndefOr[RefFunction => Unit]    = js.undefined
  }


  @js.native
  trait RefFunction extends js.Object {
    def getChildContext(): MuiTheme = js.native
  }

}

