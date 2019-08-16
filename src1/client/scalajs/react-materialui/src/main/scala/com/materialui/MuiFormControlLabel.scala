package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 16:24
  */
object MuiFormControlLabel {

  val component = JsComponent[MuiFormControlLabelProps, Children.None, Null](Mui.FormControlLabel)

  def apply(props: MuiFormControlLabelProps = MuiFormControlLabelProps.empty) =
    component(props)

}


trait MuiFormControlLabelProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiFormControlLabelClasses]
  with MuiPropsBaseComponent
{
  val control: js.UndefOr[React.Element] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val inputRef: js.UndefOr[js.Function1[HTMLInputElement, _] | js.Object] = js.undefined
  val label: js.UndefOr[React.Node] = js.undefined
  val labelPlacement: js.UndefOr[String] = js.undefined
  val name: js.UndefOr[String] = js.undefined
  @JSName("onChange")
  val onChange2: js.UndefOr[js.Function2[ReactEventFromInput, Boolean, _]] = js.undefined
  val value: js.UndefOr[String] = js.undefined
}
object MuiFormControlLabelProps extends MuiPropsBaseStatic[MuiFormControlLabelProps]


trait MuiFormControlLabelClasses extends MuiClassesBase {
  val labelPlacementStart: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
}

object MuiLabelPlacements {
  val start = "start"
  val end = "end"
  val top = "top"
  val bottom = "bottom"
}
