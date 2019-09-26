package com.github.react.dnd

import japgolly.scalajs.react.raw
import japgolly.scalajs.react.vdom.VdomElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.2019 10:14
  */
@js.native
trait DragSourceConnector extends js.Object {

  def dragSource(): DragSourceF[DragSourceFOptions] = js.native
  def dragPreview(): DragSourceF[DragPreviewFOptions] = js.native

}


@js.native
trait DragSourceF[-Options_t <: js.Object] extends js.Function {
  def apply[T <: raw.React.Element](el: T, options: Options_t = js.native): T = js.native
  @JSName("apply")
  def applyNode[T <: raw.React.Node](node: T, options: Options_t = js.native): T = js.native
  @JSName("apply")
  def applyRef[T <: raw.React.Ref](ref: T, options: Options_t = js.native): T = js.native
}
object DragSourceF {
  implicit class DragSourceExt[-T <: js.Object]( val ds: DragSourceF[T] ) extends AnyVal {
    def applyVdomEl( vdomEl: VdomElement ): VdomElement = {
      VdomElement( ds( vdomEl.rawElement ) )
    }
  }
  implicit class DragSourceUndExt[-T <: js.Object]( val dsUnd: js.UndefOr[DragSourceF[T]] ) extends AnyVal {
    def applyVdomEl( vdomEl: VdomElement ): VdomElement = {
      dsUnd.fold( vdomEl )(_.applyVdomEl(vdomEl))
    }
  }
}


trait DragSourceFOptions extends js.Object {
  val dropEffect: js.UndefOr[String] = js.undefined
}
object DropEffects {
  def move = "move"
  def copy = "copy"
}


trait DragPreviewFOptions extends js.Object {
  val captureDraggingState: js.UndefOr[Boolean] = js.undefined
  val anchorX: js.UndefOr[Double] = js.undefined
  val anchorY: js.UndefOr[Double] = js.undefined
  val offsetX: js.UndefOr[Double] = js.undefined
  val offsetY: js.UndefOr[Double] = js.undefined
}