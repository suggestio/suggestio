package io.suggest.sc.v.styl

import com.materialui.{Mui, MuiColor, MuiPalette, MuiPaletteAction, MuiPaletteBackground, MuiPaletteText, MuiPaletteTypes, MuiRawTheme, MuiTheme, MuiThemeTypoGraphy}
import io.suggest.color.{MColorData, MColorTypes, MColors}
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.06.2020 16:20
  * Description: Утиль для тем выдачи.
  */
final class ScThemes {

  def muiDefault(mcolors: MColors): MuiTheme = {
    val bgHex = mcolors.bg.getOrElse( MColorData(MColorTypes.Bg.scDefaultHex) ).hexCode
    val fgHex = mcolors.fg.getOrElse( MColorData(MColorTypes.Fg.scDefaultHex) ).hexCode
    val primaryColor = new MuiColor {
      override val main = bgHex
      override val contrastText = fgHex
    }
    val secondaryColor = new MuiColor {
      override val main = fgHex
      override val contrastText = bgHex
    }
    val paletteText = new MuiPaletteText {
      override val primary = fgHex
      override val secondary = bgHex
    }
    val palletteBg = new MuiPaletteBackground {
      override val paper = bgHex
      override val default = bgHex
    }
    val btnsPalette = new MuiPaletteAction {
      override val active = fgHex
    }
    val paletteRaw = new MuiPalette {
      override val primary      = primaryColor
      override val secondary    = secondaryColor
      // TODO Нужно портировать getLuminance() и выбирать dark/light на основе формулы luma >= 0.5. https://github.com/mui-org/material-ui/blob/355317fb479dc234c6b1e374428578717b91bdc0/packages/material-ui/src/styles/colorManipulator.js#L151
      override val mode       = MuiPaletteTypes.dark
      override val text         = paletteText
      override val background   = palletteBg
      override val action       = btnsPalette
    }

    val themeRaw = new MuiRawTheme {
      override val palette = paletteRaw
      override val typography = typoGraphyProps
      //override val shadows = js.Array("none")
    }

    Mui.Styles.createTheme( themeRaw )
  }


  private def typoGraphyProps: MuiThemeTypoGraphy = {
    new MuiThemeTypoGraphy {
      override val useNextVariants = true
    }
  }

}
