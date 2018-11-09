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
  val `type`: js.UndefOr[String] = js.undefined
}

/** Значения для [[MuiPalette]].type */
object MuiPaletteTypes {
  val dark = "dark"
  val light = "light"
}


trait MuiThemeTypoGraphy extends js.Object {
  val fontFamily: js.UndefOr[js.Array[String]] = js.undefined
  val fontSize: js.UndefOr[Int] = js.undefined
  val htmlFontSiz: js.UndefOr[Int] = js.undefined
  // https://material-ui.com/style/typography/#migration-to-typography-v2
  val useNextVariants: js.UndefOr[Boolean] = js.undefined
}


trait MuiRawTheme extends js.Object {
  val palette: MuiPalette
  val typography: js.UndefOr[MuiThemeTypoGraphy] = js.undefined
}


trait MuiTheme extends MuiRawTheme


object MuiColorTypes {
  val primary = "primary"
  val secondary = "secondary"
  val inherit = "inherit"
  val default = "default"
}
