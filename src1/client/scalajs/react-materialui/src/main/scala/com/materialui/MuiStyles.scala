package com.materialui

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

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

  /**
    * Link a style sheet with a component.
    * It does not modify the component passed to it;
    * instead, it returns a new component, with a `classes` property.
    * @param styles Стили в виде js-объекта.
    * @return Функция, принимающая компонент и собирающая обёртку с выставленными стилями внутри.
    */
  def withStyles(styles: js.Object): js.Function1[js.Any, js.Any] = js.native

  @JSName("withStyles")
  def withStylesF(stylesCreator: js.Function1[MuiTheme, js.Object]): js.Function1[js.Any, js.Any] = js.native

  // Далее, идёт API для colorManipulator.js:
  // https://github.com/mui-org/material-ui/blob/master/packages/material-ui/src/styles/colorManipulator.js

  // Функции прямо тут, т.к. https://github.com/mui-org/material-ui/blob/master/packages/material-ui/src/styles/index.js
  // содержит строку "export * from './colorManipulator';"

  /** relative brightness of any point in a color space.
    *
    * @param color
    * @return [0..1];
    *         0 = darkest black,
    *         1 = lightest white
    */
  def getLuminance(color: MuiColor): Double = js.native

  def decomposeColor(color: String): MuiColor = js.native
  def fade(color: String | MuiColor, amount: Double): String = js.native
  def lighten(color: String | MuiColor, amount: Double): String = js.native
  def emphasize(color: String | MuiColor, amount: Double = js.native): String = js.native
  def darken(color: String | MuiColor, amount: Double): String = js.native
  def contrastRatio(background: String | MuiColor, foreground: MuiColor): Double = js.native
  def contrastRatioLevel(background: String | MuiColor, foreground: MuiColor): String = js.native
  def recomposeColor(color: MuiColor): String = js.native

  def rgbToHex(color: String | MuiColor): String = js.native
  def hslToRgb(color: String | MuiColor): String = js.native

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
  val mode: js.UndefOr[String] = js.undefined
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
  val components: js.UndefOr[js.Object] = js.undefined
}


trait MuiTheme extends MuiRawTheme


object MuiColorTypes {
  val primary = "primary"
  val secondary = "secondary"
  val inherit = "inherit"
  val default = "default"

  // MuiLink:
  val error = "error"
  val textPrimary = "textPrimary"
  val textSecondary = "textSecondary"
}
