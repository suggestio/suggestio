package com.github.dantrain.react.stonecutter

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 16:08
  * Description: Common components' properties for both [[SpringGrid]] and [[CSSGrid]].
  */

trait PropsCommon extends js.Object {

  /** Number of columns. Required.
    * You can wrap the Grid component in the `makeResponsive` higher-order component to set this dynamically. */
  val columns: Int

  /** Width of a single column, by default in px units. Required. */
  val columnWidth: Int

  /** Width of space between columns. Default: 0. */
  val gutterWidth: js.UndefOr[Int] = js.undefined

  /** Height of vertical space between items. Default: 0. */
  val gutterHeight: js.UndefOr[Int] = js.undefined

  /** Change the HTML tagName of the Grid element, for example to 'ul' or 'ol' for a list. Default: 'div'.
    * @see [[GridComponents]] for valid values.
    */
  val component: js.UndefOr[GridComponent_t] = js.undefined

  /**
    * Use one of the included layouts, or create your own. Defaults to a 'simple' layout with items of fixed height.
    * Included layouts:
    */
  val layout: js.UndefOr[LayoutF_t] = js.undefined

  /** These allow you to change how items animate as they appear and disappear from the grid.
    * Supply functions that return objects with the opacity and transform values for an item's start and end states.
    * By default the item's scale and opacity go from 0 to 1 and back to 0 on exit, like this:
    * {{{
    *   enter={() => ({ scale: 0, opacity: 0 })}
    *   entered={() => ({ scale: 1, opacity: 1 })}
    *   exit={() => ({ scale: 0, opacity: 0 })}
    * }}}
    *
    * The functions are passed three parameters, the item props, grid props and grid state which includes
    * the current height and width of the grid. For example to have disappearing items fall off the bottom of the grid:
    * {{{
    *   exit={(itemProps, gridProps, gridState) => ({ translateY: gridState.gridHeight + 500 })}
    * }}}
    *
    * @see [[EnterExitStyle]]
    */
  val enter, entered, exit: js.UndefOr[EnterExitF_t] = js.undefined


  /**
    * The perspective distance used for 3D transforms.
    * If you are using a transform function like rotateX, use this to strengthen the effect.
    * Default is no perspective applied.
    * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/transform-function/perspective]]
    */
  val perspective: js.UndefOr[Double] = js.undefined

  /** The length unit used throughout.
    * Default: 'px'. Experimental.
    * You could try using 'em' or 'rem' and then adjust the font-size for a fluid layout,
    * but it may not work well with the measureItems and makeResponsive higher-order components.
    * % does not work well due to the way CSS transforms work.
    * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/length]]
    */
  val lengthUnit: js.UndefOr[String] = js.undefined

  /** The angle unit. Affects transform-functions such as rotate.
    * Default: 'deg'.
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/angle]]
    */
  val angleUnit: js.UndefOr[String] = js.undefined

}
