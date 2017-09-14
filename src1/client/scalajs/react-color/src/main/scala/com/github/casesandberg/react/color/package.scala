package com.github.casesandberg.react

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 12:13
  * Description:
  */
package object color {

  type Color_t = String | Rgb | Hsl

  type PresetColor_t = String | NamedColor

  type PresetColors_t = js.Array[PresetColor_t]

}
