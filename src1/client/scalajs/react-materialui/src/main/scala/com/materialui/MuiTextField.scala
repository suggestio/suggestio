package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiTextField {

  val component = JsComponent[MuiTextFieldProps, Children.Varargs, Null](Mui.TextField)

  def apply(props: MuiTextFieldProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  type Variant <: js.Any
  object Variants {
    def standard = "standard".asInstanceOf[Variant]
    def outlined = "outlined".asInstanceOf[Variant]
    def filled = "filled".asInstanceOf[Variant]
  }

}


trait MuiTextFieldProps extends MuiFormControlProps {
  val autoComplete: js.UndefOr[String] = js.undefined
  val autoFocus: js.UndefOr[Boolean] = js.undefined
  val defaultValue: js.UndefOr[String | Double] = js.undefined
  val FormHelperTextProps: js.UndefOr[js.Object] = js.undefined
  val helperText: js.UndefOr[raw.React.Node] = js.undefined
  val id: js.UndefOr[String] = js.undefined
  val InputLabelProps: js.UndefOr[MuiInputLabelProps] = js.undefined
  val InputProps: js.UndefOr[MuiInputProps] = js.undefined
  val inputProps: js.UndefOr[js.Dictionary[String]] = js.undefined
  val inputRef: js.UndefOr[js.Object | js.Function1[js.Any, Unit]] = js.undefined
  val label: js.UndefOr[raw.React.Node] = js.undefined
  val multiline: js.UndefOr[Boolean] = js.undefined
  val name: js.UndefOr[String] = js.undefined
  val placeholder: js.UndefOr[String] = js.undefined
  val minRows: js.UndefOr[String | Int] = js.undefined
  val maxRows: js.UndefOr[String | Int] = js.undefined
  val select: js.UndefOr[Boolean] = js.undefined
  val SelectProps: js.UndefOr[MuiSelectProps] = js.undefined
  val `type`: js.UndefOr[String] = js.undefined
  val variant: js.UndefOr[MuiTextField.Variant] = js.undefined
  val value: js.UndefOr[MuiInputValue_t | js.Array[MuiInputValue_t]] = js.undefined
  val onOpen: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val native: js.UndefOr[Boolean] = js.undefined
}


@js.native
trait MuiTextFieldM extends js.Object {
  def blur(): Unit = js.native
  def focus(): Unit = js.native
  def getInputNode(): Unit = js.native
  def getValue(): String = js.native
  def select(): Unit = js.native
}
