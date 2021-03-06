package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 15:11
  * Description: Text input field API.
  */
object MuiInput {

  val component = JsForwardRefComponent[MuiInputProps, Children.None, dom.html.Element](Mui.Input)

  final def apply(props: MuiInputProps = MuiPropsBaseStatic.empty) =
    component(props)

}


/** Props for [[MuiInput]]. */
trait MuiInputProps extends MuiPropsBase {
  val autoComplete: js.UndefOr[String] = js.undefined
  val autoFocus: js.UndefOr[Boolean] = js.undefined
  val classes: js.UndefOr[MuiInputClasses] = js.undefined
  val className: js.UndefOr[String] = js.undefined
  val defaultValue: js.UndefOr[String | Double] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disableUnderline: js.UndefOr[Boolean] = js.undefined
  val endAdornment: js.UndefOr[React.Node] = js.undefined
  val error: js.UndefOr[Boolean] = js.undefined
  val fullWidth: js.UndefOr[Boolean] = js.undefined
  val id: js.UndefOr[String] = js.undefined
  val inputComponent: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val inputProps: js.UndefOr[js.Object] = js.undefined
  val inputRef: js.UndefOr[js.Function1[HTMLInputElement, _] | js.Object] = js.undefined
  val margin: js.UndefOr[String] = js.undefined
  val multiline: js.UndefOr[Boolean] = js.undefined
  val name: js.UndefOr[String] = js.undefined
  val placeholder: js.UndefOr[String] = js.undefined
  val readOnly: js.UndefOr[Boolean] = js.undefined
  val required: js.UndefOr[Boolean] = js.undefined
  val rows: js.UndefOr[Int | String] = js.undefined
  val rowsMax: js.UndefOr[Int | String] = js.undefined
  val startAdornment: js.UndefOr[React.Node] = js.undefined
  val `type`: js.UndefOr[String] = js.undefined
  val value: js.UndefOr[MuiInputValue_t | js.Array[MuiInputValue_t]] = js.undefined
}


/** CSS Classes for [[MuiInputProps.classes]]. */
trait MuiInputClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val error: js.UndefOr[String] = js.undefined
  val focused: js.UndefOr[String] = js.undefined
  val formControl: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val underline: js.UndefOr[String] = js.undefined
  val multiline: js.UndefOr[String] = js.undefined
  val fullWidth: js.UndefOr[String] = js.undefined
  val input: js.UndefOr[String] = js.undefined
  val inputMarginDense: js.UndefOr[String] = js.undefined
  val inputMultiline: js.UndefOr[String] = js.undefined
  val inputType: js.UndefOr[String] = js.undefined
  val inputTypeSearch: js.UndefOr[String] = js.undefined
}


/** Enum for values of [[MuiInputProps.margin]]. */
object MuiInputPropsMargins {
  val dense = "dense"
  val none = "none"
  val normal = "normal"
}
