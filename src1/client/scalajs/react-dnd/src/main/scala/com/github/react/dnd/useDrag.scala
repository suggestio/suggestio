package com.github.react.dnd

import japgolly.scalajs.react.Ref
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 16:12
  */

@js.native
@JSImport(DND_PACKAGE, "useDrag")
object useDragJs extends js.Function1[DragSpec, js.Array[js.Object]] {
  override def apply(arg1: DragSpec): js.Array[js.Object] = js.native
}


case class UseDragRes(
                       collectedProps   : js.Object,
                       dragSourceRef    : Ref.Simple[html.Element],
                       dragPreviewRef   : Option[Ref.Simple[html.Element]],
                     )

trait DragSpec extends js.Object {
  val item: IItem
  val previewOptions: js.UndefOr[js.Object] = js.undefined
  val options: js.UndefOr[DragDropSpecOptions] = js.undefined
  /** Fired when a drag operation begins. */
  val begin: js.UndefOr[js.Function1[DragSourceMonitor, Unit]] = js.undefined
  val end: js.UndefOr[js.Function2[IItem, DragSourceMonitor, Unit]] = js.undefined
  val canDrag: js.UndefOr[js.Function1[DragSourceMonitor, Boolean]] = js.undefined
  val isDragging: js.UndefOr[js.Function1[DragSourceMonitor, Boolean]] = js.undefined
  val collect: js.UndefOr[js.Function2[DragSourceMonitor, js.Object, js.Object]] = js.undefined
}


trait IItem extends js.Object {
  val `type`: DropAccept_t
}

trait DragDropSpecOptions extends js.Object {
  def arePropsEqual(props: js.Object, otherProps: js.Object): Boolean
}
