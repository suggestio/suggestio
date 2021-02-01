package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 11:49
  * Description:
  * @see [[https://material-ui.com/api/tooltip/]]
  * @see [[https://material-ui.com/demos/tooltips/]]
  */
object MuiToolTip {

  val component = JsForwardRefComponent[MuiToolTipProps, Children.Varargs, dom.html.Element](Mui.Tooltip)

  def apply(p: MuiToolTipProps)(children: VdomElement*) =
    component(p)(children: _*)

}


trait MuiToolTipProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiToolTipClasses] = js.undefined

  val disableFocusListener,
      disableHoverListener,
      disableTouchListener,
      disableInteractive
      : js.UndefOr[Boolean] = js.undefined

  val enterDelaym,
      enterTouchDelay,
      leaveDelay,
      leaveTouchDelay
      : js.UndefOr[Double] = js.undefined

  val id: js.UndefOr[String] = js.undefined
  val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onOpen: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val open: js.UndefOr[Boolean] = js.undefined
  val placement: js.UndefOr[String] = js.undefined
  val PopperProps: js.UndefOr[js.Object] = js.undefined
  val title: raw.React.Node
  val TransitionComponent: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}


trait MuiToolTipClasses extends js.Object {
  val popper: js.UndefOr[String] = js.undefined
  val tooltip: js.UndefOr[String] = js.undefined
  val touch: js.UndefOr[String] = js.undefined
  val tooltipPlacementLeft: js.UndefOr[String] = js.undefined
  val tooltipPlacementRight: js.UndefOr[String] = js.undefined
  val tooltipPlacementTop: js.UndefOr[String] = js.undefined
  val tooltipPlacementBottom: js.UndefOr[String] = js.undefined
}


object MuiToolTipPlacements {
  val BottomEnd = "bottom-end"
  val BottomStart = "bottom-start"
  val Bottom = "bottom"
  val LeftEnd = "left-end"
  val LeftStart = "left-start"
  val Left = "left"
  val RightEnd = "right-end"
  val RightStart = "right-start"
  val Right = "right"
  val TopEnd = "top-end"
  val TopStart = "top-start"
  val Top = "top"
}
