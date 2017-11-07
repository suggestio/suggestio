package com.github.dantrain.react.stonecutter

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 21:25
  * Description:
  * {{{
  *   const Grid = measureItems(SpringGrid, { measureImages: true })
  * }}}
  */

trait MeasureItemsOptions extends js.Object {

  /**
    * If set to true, waits for images to load before measuring items and adding them to the Grid.
    * This may be necessary if you don't know the height of your images ahead of time.
    * @see [[https://github.com/desandro/imagesloaded imagesLoaded]]
    */
  val measureImages: js.UndefOr[Boolean] = js.undefined

  /** This option is passed through to the imagesLoaded library.
    * It allows you to wait for background images to load, in addition to <img> tags.
    * @see [[https://github.com/desandro/imagesloaded]]
    */
  val background: js.UndefOr[Boolean | String] = js.undefined

}
