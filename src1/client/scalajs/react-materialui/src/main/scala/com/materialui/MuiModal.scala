package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 22:57
  * Description: Mui modal abstract component.
  */
object MuiModal {

  val component = JsForwardRefComponent[MuiModalProps, Children.Varargs, dom.html.Element]( Mui.Modal )

  def apply(props: MuiModalProps)(children: VdomNode*) =
    component( props )(children: _*)

}


trait MuiModalPropsBase extends MuiPropsBase {
  val BackdropComponent: js.UndefOr[Component_t] = js.undefined
  val BackdropProps: js.UndefOr[js.Object] = js.undefined
  val container: js.UndefOr[js.Object | js.Function] = js.undefined
  val disableAutoFocus: js.UndefOr[Boolean] = js.undefined
  // disableBackdropClick: удалено из m-ui v5.
  val disableEnforceFocus: js.UndefOr[Boolean] = js.undefined
  val disableEscapeKeyDown: js.UndefOr[Boolean] = js.undefined
  val disablePortal: js.UndefOr[Boolean] = js.undefined
  val disableRestoreFocus: js.UndefOr[Boolean] = js.undefined
  val hideBackdrop: js.UndefOr[Boolean] = js.undefined
  val keepMounted: js.UndefOr[Boolean] = js.undefined
  val manager: js.UndefOr[js.Object] = js.undefined
  val onBackdropClick: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  @JSName("onClose")
  val onClose2: js.UndefOr[js.Function2[ReactEvent, MuiModalCloseReason, Unit]] = js.undefined
  val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onRendered: js.UndefOr[js.Function0[Unit]] = js.undefined
}

trait MuiModalProps
  extends MuiModalPropsBase
  with MuiPropsBaseClasses[MuiModalClasses]


object MuiModalCloseReason {
  final def backdropClick = "backdropClick".asInstanceOf[MuiModalCloseReason]
  final def escapeKeyDown = "escapeKeyDown".asInstanceOf[MuiModalCloseReason]
}


trait MuiModalClasses extends MuiClassesBase {
  val hidden: js.UndefOr[String] = js.undefined
}

