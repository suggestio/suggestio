package chandu0101.scalajs.react.components.materialui

import scala.scalajs.js

trait MuiColor extends js.Object {
  val light: js.UndefOr[String] = js.undefined
  val main: String
  val dark: js.UndefOr[String] = js.undefined
  val contrastText: js.UndefOr[String] = js.undefined
}

@js.native
trait MuiStyles extends js.Object {
  /** @see [[https://material-ui.com/customization/themes/#createmuitheme-options-theme]] */
  def createMuiTheme(options: MuiRawTheme): MuiTheme = js.native

  val MuiThemeProvider: js.Dynamic = js.native
}


trait MuiPalette extends js.Object {
  val primary: MuiColor
  val secondary: MuiColor
  val error: js.UndefOr[MuiColor] = js.undefined
  val contrastThreshold: js.UndefOr[Int] = js.undefined
  val tonalOffset: js.UndefOr[Double] = js.undefined

  /** "dark" */
  val `type`: js.UndefOr[String] = js.undefined
}


trait MuiTypography extends js.Object {
  val fontFamily: js.UndefOr[js.Array[String]] = js.undefined
  val fontSize: js.UndefOr[Int] = js.undefined
  val htmlFontSiz: js.UndefOr[Int] = js.undefined
}


trait MuiRawTheme extends js.Object {
  val palette: MuiPalette
  val typography: js.UndefOr[MuiTypography] = js.undefined
}


trait MuiTheme extends MuiRawTheme
