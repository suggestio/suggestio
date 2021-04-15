package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.2020 18:20
  * Description: Sjs API for Mui Popover.
  * @see [[https://material-ui.com/api/popover/]] JS API.
  * @see [[https://material-ui.com/components/popover/]] Demos.
  */
object MuiPopOver {

  val component = JsForwardRefComponent[MuiPopOverProps, Children.Varargs, dom.html.Element]( Mui.Popover )

  final def apply(props: MuiPopOverProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiPopOverPropsBase
  extends MuiPropsBase
{
  val action: js.UndefOr[raw.React.Ref] = js.undefined
  val anchorEl: js.UndefOr[js.Function | dom.html.Element] = js.undefined
  val anchorOrigin: js.UndefOr[MuiAnchorOrigin] = js.undefined
  val anchorPosition: js.UndefOr[MuiTopLeft] = js.undefined
  val anchorReference: js.UndefOr[MuiAnchorReferences.AnchorReference_t] = js.undefined
  val container: js.UndefOr[dom.html.Element | raw.React.ComponentUntyped | js.Function] = js.undefined
  val elevation: js.UndefOr[Int] = js.undefined
  val getContentAnchorEl: js.UndefOr[js.Function] = js.undefined
  val marginThreshold: js.UndefOr[Int] = js.undefined

  val onClose: js.UndefOr[js.Function1[ReactEvent, _]] = js.undefined

  val PaperProps: js.UndefOr[MuiPaperProps] = js.undefined
  val transformOrigin: js.UndefOr[MuiAnchorOrigin] = js.undefined
  val TransitionComponent: js.UndefOr[js.Object] = js.undefined
  val transitionDuration: js.UndefOr[MuiTransitionDuration.TransitionDuration_t] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}

trait MuiPopOverPropsBaseOpen
  extends MuiPopOverPropsBase
{
  val open: Boolean
}


trait MuiPopOverProps
  extends MuiPopOverPropsBaseOpen
  with MuiPropsBaseClasses[MuiPopOverClasses]



trait MuiPopOverClasses extends MuiClassesBase {
  val paper: js.UndefOr[String] = js.undefined
}
