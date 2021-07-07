package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


/**
  * @see [[https://next.material-ui.com/ru/api/alert/]]
  */
object MuiAlert {

  val component = JsForwardRefComponent[Props, Children.Varargs, dom.html.Element]( Mui.Alert )

  def apply(props: Props = MuiPropsBaseStatic.empty[Props])(children: VdomNode*) =
    component( props )( children: _* )

  trait Props
    extends MuiPropsBase
    with MuiPropsBaseClasses[Classes]
  {
    val action: js.UndefOr[raw.React.Node] = js.undefined
    val closeText,
        color,
        role: js.UndefOr[String] = js.undefined
    val icon: js.UndefOr[raw.React.Node] = js.undefined
    val iconMapping: js.UndefOr[IconMapping] = js.undefined
    val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
    val severity: js.UndefOr[Severity] = js.undefined
    val variant: js.UndefOr[Variant] = js.undefined
  }


  trait IconMapping extends js.Object {
    val error,
        info,
        success,
        warning: js.UndefOr[raw.React.Node] = js.undefined
  }


  trait Classes extends MuiClassesBase {
    val filled,
        outlined,
        standard,
        standardSuccess,
        standardInfo,
        standardWarning,
        standardError,
        outlinedSuccess,
        outlinedInfo,
        outlinedWarning,
        outlinedError,
        filledSuccess,
        filledInfo,
        filledWarning,
        filledError,
        icon,
        message,
        action
        : js.UndefOr[String] = js.undefined
  }


  type Severity <: String
  object Severity {
    final def ERROR = "error".asInstanceOf[Severity]
    final def INFO = "info".asInstanceOf[Severity]
    final def SUCCESS = "success".asInstanceOf[Severity]
    final def WARNING = "warning".asInstanceOf[Severity]
  }


  type Variant = String
  object Variant {
    final def FILLED: Variant = "filled"
    final def OUTLINED: Variant = "outlined"
    final def STANDARD: Variant = "standard"
  }

}






