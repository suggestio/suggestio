package io.suggest.lk.m.color

import com.github.mikbry.materialui.color.Palette_t
import io.suggest.color.MHistogram

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 15:55
  * Description: Внешний контекст для color-picker'ов.
  */
case class MColor2PickerCtx(
                             histogram      : MHistogram        = MHistogram.empty,
                           ) {

  lazy val toMuiColorPalette: Palette_t = {
    js.Dictionary
      .empty[String]
      .addAll {
        histogram
          .colors
          .iterator
          .map { mcd =>
            val hexCode = mcd.hexCode
            hexCode -> hexCode
          }
      }
  }

}
