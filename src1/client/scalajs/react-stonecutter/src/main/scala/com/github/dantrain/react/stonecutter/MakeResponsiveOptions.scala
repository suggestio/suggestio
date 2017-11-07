package com.github.dantrain.react.stonecutter

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 21:29
  * Description:
  * {{{
  *   const Grid = makeResponsive(SpringGrid, { maxWidth: 1920 })
  * }}}
  */
trait MakeResponsiveOptions extends js.Object {

  /** Maximum width for the Grid in px. */
  val maxWidth: js.UndefOr[Int] = js.undefined

  /** Minimum horizontal length between the edge of the Grid and the edge of the viewport in px. Default: 0. */
  val minPadding: js.UndefOr[Int] = js.undefined

  /** Default number of columns before the breakpoints kick in.
    * May be useful when rendering server-side in a universal app.
    * Default: 4.
    */
  val defaultColumns: js.UndefOr[Int] = js.undefined

}
