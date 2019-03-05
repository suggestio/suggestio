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
  //val colorManipulator: MuiStylesColorManipulator = js.native // TODO not exported.
}

trait MuiPaletteCommon extends js.Object {
  val black: js.UndefOr[String]
  val white: js.UndefOr[String]
}

trait MuiPaletteText extends js.Object {
  val primary: js.UndefOr[String] = js.undefined
  val secondary: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val hint: js.UndefOr[String] = js.undefined
}

trait MuiPaletteBackground extends js.Object {
  val paper: js.UndefOr[String] = js.undefined
  val default: js.UndefOr[String] = js.undefined
}

trait MuiPaletteAction extends js.Object {
  val active: js.UndefOr[String] = js.undefined
  val hover: js.UndefOr[String] = js.undefined
  val hoverOpacity: js.UndefOr[Double] = js.undefined
  val selected: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val disabledBackground: js.UndefOr[String] = js.undefined
}

trait MuiPalette extends js.Object {
  val common: js.UndefOr[MuiPaletteCommon] = js.undefined
  val primary: MuiColor
  val secondary: MuiColor
  val text: js.UndefOr[MuiPaletteText] = js.undefined
  val error: js.UndefOr[MuiColor] = js.undefined
  val contrastThreshold: js.UndefOr[Int] = js.undefined
  val tonalOffset: js.UndefOr[Double] = js.undefined
  val `type`: js.UndefOr[String] = js.undefined
  val divider: js.UndefOr[String] = js.undefined
  val background: js.UndefOr[MuiPaletteBackground] = js.undefined
  val action: js.UndefOr[MuiPaletteAction] = js.undefined
}

/** Значения для [[MuiPalette]].type */
object MuiPaletteTypes {
  val dark = "dark"
  val light = "light"
}


trait MuiThemeTypoGraphy extends js.Object {
  val fontFamily: js.UndefOr[js.Array[String]] = js.undefined
  val fontSize: js.UndefOr[Int] = js.undefined
  val htmlFontSize: js.UndefOr[Int] = js.undefined
  // https://material-ui.com/style/typography/#migration-to-typography-v2
  val useNextVariants: js.UndefOr[Boolean] = js.undefined
}


trait MuiRawTheme extends js.Object {
  val breakpoints: js.UndefOr[js.Object] = js.undefined
  val palette: MuiPalette
  val overrides: js.UndefOr[js.Object] = js.undefined
  val mixins: js.UndefOr[js.Object] = js.undefined
  val direction: js.UndefOr[String] = js.undefined
  val props: js.UndefOr[js.Object] = js.undefined
  val shadows: js.UndefOr[js.Array[String]] = js.undefined
  val typography: js.UndefOr[MuiThemeTypoGraphy] = js.undefined
}


trait MuiTheme extends MuiRawTheme


object MuiColorTypes {
  val primary = "primary"
  val secondary = "secondary"
  val inherit = "inherit"
  val default = "default"
}
