package com.github.react.dnd.backend.touch

import org.scalajs.dom.html

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2019 15:09
  * Description:
  * {{{
  *   import TouchBackend from 'react-dnd-touch-backend'
  *
  *   <DndProvider backend={TouchBackend} options={touchBackedOptions}>
  *     ...
  *   </DndProvider>
  * }}}
  * @see [[http://react-dnd.github.io/react-dnd/docs/backends/touch]]
  */
trait TouchBackendOptions extends js.Object {

  /** default: true */
  val enableTouchEvents: js.UndefOr[Boolean] = js.undefined

  /** default: false
    *
    * A flag indicating whether to enable click-based event processing.
    * NOTE: This is BUGGY due to the difference in touchstart/touchend event propagation compared to mousedown/mouseup/click.
    */
  val enableMouseEvents: js.UndefOr[Boolean] = js.undefined

  val enableKeyboardEvents: js.UndefOr[Boolean] = js.undefined

  /** The amount in ms to delay processing for all events. */
  val delay: js.UndefOr[Int] = js.undefined
  val delayTouchStart: js.UndefOr[Int] = js.undefined
  val delayMouseStart: js.UndefOr[Int] = js.undefined
  val touchSlop: js.UndefOr[Double] = js.undefined

  /** default: false
    * If true, prevents the contextmenu event from canceling a drag.
    */
  val ignoreContextMenu: js.UndefOr[Boolean] = js.undefined

  val scrollAngleRanges: js.UndefOr[js.Array[ScrollAngleRange]] = js.undefined

  /** Continue dragging of currently dragged element when pointer leaves DropTarget area. */
  val enableHoverOutsideTarget: js.UndefOr[Boolean] = js.undefined

  /** function getDropTargetElementsAtPoint(x, y, dropTargets) => Boolean
    * uses document.elementsFromPoint or polyfill
    * Specify a custom function to find drop target elements at the given point.
    * Useful for improving performance in environments (iOS Safari) where document.elementsFromPoint is not available.
    */
  val getDropTargetElementsAtPoint: js.UndefOr[js.Function3[Double, Double, js.Array[html.Element], Boolean]] = js.undefined

}


trait ScrollAngleRange extends js.Object {
  val start: js.UndefOr[Int] = js.undefined
  val end: js.UndefOr[Int] = js.undefined
}
