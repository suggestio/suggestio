package com.materialui

import japgolly.scalajs.react.raw
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:34
  * @see [[https://material-ui.com/api/step-content/]]
  */
object MuiStepButton {

  val component = JsComponent[MuiStepButtonProps, Children.Varargs, Null]( Mui.StepButton )

  def apply(props: MuiStepButtonProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiStepButtonProps
  extends MuiButtonBaseCommonProps
  with MuiPropsBaseClasses[MuiStepButtonClasses]
{
  val icon: js.UndefOr[raw.React.Node] = js.undefined
  val optional: js.UndefOr[raw.React.Node] = js.undefined
}


trait MuiStepButtonClasses extends MuiButtonBaseClasses {
  val horizontal: js.UndefOr[String] = js.undefined
  val vertical: js.UndefOr[String] = js.undefined
  val touchRipple: js.UndefOr[String] = js.undefined
}
