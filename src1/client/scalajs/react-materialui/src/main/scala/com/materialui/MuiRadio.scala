package com.materialui

import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2020 8:34
  * Description: API for MUI radio-button.
  * @see [[https://material-ui.com/ru/api/radio/]]
  */
object MuiRadio {

  val component = JsComponent[MuiRadioProps, Children.None, Null]( Mui.Radio )

  def apply( p: MuiRadioProps = MuiPropsBaseStatic.empty ) =
    component(p)

}


trait MuiRadioProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiRadioClasses]
{

  val checked, disabled, disableRipple, required: js.UndefOr[Boolean] = js.undefined
  val checkedIcon, icon: js.UndefOr[raw.React.Node] = js.undefined
  val color, id, name, size, value: js.UndefOr[String] = js.undefined
  val inputProps: js.UndefOr[js.Object] = js.undefined
  val inputRef: js.UndefOr[raw.React.Ref] = js.undefined

}


trait MuiRadioClasses extends MuiClassesBase {
  val checked, disabled, colorPrimary, colorSecondary: js.UndefOr[String] = js.undefined
}

object MuiRadioSizes {
  final def MEDIUM = "medium"
  final def SMALL = "small"
}
