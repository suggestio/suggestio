package com.github.react.dnd

import japgolly.scalajs.react.Ref
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 16:12
  * @see [[http://react-dnd.github.io/react-dnd/docs/api/use-drop]]
  */

@js.native
@JSImport(DND_PACKAGE, "useDrop")
object useDropJs extends js.Function1[DropSpec, js.Array[js.Object]] {
  override def apply(arg1: DropSpec): js.Array[js.Object] = js.native
}


case class UseDropRes(
                       collectedProps   : js.Object,
                       dropTargetRef    : Ref.Simple[html.Element],
                     )


trait DropSpec extends js.Object {
  val accept: DropAccept_t
  val options: js.UndefOr[DragDropSpecOptions] = js.undefined
  /** drop(item, monitor) - Called when a compatible item is dropped on the target. */
  val drop: js.UndefOr[js.Function2[ItemType_t, DropTargetMonitor, Unit]] = js.undefined
  /** Fired when a drag operation begins */
  val hover: js.UndefOr[js.Function2[ItemType_t, DropTargetMonitor, Unit]] = js.undefined
  val canDrop: js.UndefOr[js.Function2[ItemType_t, DropTargetMonitor, Unit]] = js.undefined
  /** The collecting function. It should return a plain object of the props to return for injection into your component.
    * It receives two parameters, monitor and props. */
  val collect: js.UndefOr[js.Function2[DropTargetMonitor, js.Object, js.Object]] = js.undefined
}

