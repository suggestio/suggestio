package com.github.mikbry.materialui

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 8:28
  */
package object color {

  // For mui v5.alpha to v5.beta migration, we use some crunches: See https://github.com/mikbry/material-ui-color/issues/166
  final val PACKAGE_NAME = "string-replace-loader?search=core/styles&replace=styles!./node_modules/material-ui-color/index.js"
  //final val PACKAGE_NAME = "material-ui-color"

  /** @see [[https://github.com/mikbry/material-ui-color/blob/master/src/helpers/commonTypes.js#L10]] */
  type Color_t = Color | String | Double

  /** Палитра заданных цветов вида:
    * {{{
    *   {
    *     red: '#ff0000',
    *     blue: '#0000ff',
    *     green: '#00ff00',
    *     yellow: 'yellow',
    *     cyan: 'cyan',
    *     lime: 'lime',
    *     ...
    *   }
    * }}}
    *
    * @see [[https://github.com/mikbry/material-ui-color#colorpalette-]]
    */
  type Palette_t = js.Dictionary[String]

  type InputFormats_t = js.Array[String]

}
