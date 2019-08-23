package com.github.react.dnd

import japgolly.scalajs.react.raw
import japgolly.scalajs.react.vdom.VdomElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.2019 17:09
  * @see [[http://react-dnd.github.io/react-dnd/docs/api/drop-target-connector]]
  */
@js.native
trait DropTargetConnector extends js.Object {
  def dropTarget(): DropTargetF = js.native
}


@js.native
trait DropTargetF extends js.Function {
  def apply[T <: raw.React.Element](el: T): T = js.native
  @JSName("apply")
  def applyNode[T <: raw.React.Node](node: T): T = js.native
  @JSName("apply")
  def applyRef[T <: raw.React.Ref](ref: T): T = js.native
}
object DropTargetF {
  implicit class DropTargetExt( val ds: DropTargetF ) extends AnyVal {
    def applyVdomEl( vdomEl: VdomElement ): VdomElement = {
      VdomElement( ds( vdomEl.rawElement ) )
    }
  }
  implicit class DropTargetUndExt( val dsUnd: js.UndefOr[DropTargetF] ) extends AnyVal {
    def applyVdomEl( vdomEl: VdomElement ): VdomElement = {
      dsUnd.fold( vdomEl )(_.applyVdomEl(vdomEl))
    }
  }
}