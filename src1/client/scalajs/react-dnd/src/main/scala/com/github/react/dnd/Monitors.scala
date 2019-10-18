package com.github.react.dnd

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 16:45
  */
@js.native
sealed trait CommonMonitor extends js.Object {
  def getItemType(): DropAccept_t_0 = js.native
  def getItem(): IItem = js.native

  /** Client offset of the pointer at the time when the current drag operation has started.
    * @return null if no item is being dragged. */
  def getInitialClientOffset(): XY = js.native

  /** Client offset of the drag source component's root DOM node at the time when the current drag operation has started
    * @return null if no item is being dragged.
    */
  def getInitialSourceClientOffset(): XY = js.native

  /** Client offset of the pointer while a drag operation is in progress.
    * @return null if no item is being dragged
    */
  def getClientOffset(): XY = js.native

  /** Difference between the last recorded client offset of the pointer and the client offset when current the drag operation has started.
    * @return null if no item is being dragged.
    */
  def getDifferenceFromInitialOffset(): XY = js.native

  /** Client offset of the drag source component's root DOM node,
    * based on its position at the time when the current drag operation has started, and the movement difference.
    * @return null if no item is being dragged.  */
  def getSourceClientOffset(): XY = js.native
}


@js.native
sealed trait CommonSourceTargetMonitor extends CommonMonitor {
  /** Returns null if called outside drop(). */
  def getDropResult(): js.Object = js.native

  /** Returns true if some drop target has handled the drop event, false otherwise.
    * Even if a target did not return a drop result, didDrop() returns true.
    * Use it inside drop() to test whether any nested drop target has already handled the drop.
    * @return false if called outside drop(). */
  def didDrop(): Boolean = js.native
}

@js.native
sealed trait CommonDragMonitor extends js.Object {
  def isDragging(): Boolean = js.native
}

@js.native
trait DragSourceMonitor extends CommonSourceTargetMonitor with CommonDragMonitor {
  def canDrag(): Boolean = js.native
}

@js.native
trait DropTargetMonitor extends CommonSourceTargetMonitor {
  def canDrop(): Boolean = js.native
  def isOver(options: IsOverOptions = js.native): Boolean = js.native
}

@js.native
trait DragLayerMonitor extends CommonMonitor with CommonDragMonitor


object XY {
  def apply(x1: Double, y1: Double): XY = {
    new XY {
      override val x = x1
      override val y = y1
    }
  }
}
trait XY extends js.Object {
  val x: Double
  val y: Double
}

trait IsOverOptions extends js.Object {
  val shallow: js.UndefOr[Boolean] = js.undefined
}
