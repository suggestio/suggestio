package com.materialui

import japgolly.scalajs.react._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.10.2019 14:48
  * @see [[https://material-ui.com/api/slider/]]
  */
object MuiSlider {

  val component = JsComponent[MuiSliderProps, Children.None, Null](Mui.Slider)

  def apply(props: MuiSliderProps = MuiSliderProps.empty) = component(props)

}


trait MuiSliderProps
  extends MuiPropsBase
  with MuiPropsBaseComponent
  with MuiPropsBaseClasses[MuiSliderClasses]
{
  import MuiSliderProps._

  val `aria-label`: js.UndefOr[String] = js.undefined
  val `aria-labelledby`: js.UndefOr[String] = js.undefined
  val `aria-valuetext`: js.UndefOr[String] = js.undefined
  /** primary | secondary */
  val color: js.UndefOr[String] = js.undefined
  val defaultValue: js.UndefOr[Value_t | js.Array[Value_t]] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val getAriaLabel: js.UndefOr[js.Function1[Index_t, String]] = js.undefined
  /** $1 value: The thumb label's value to format.
    * $2 index: The thumb label's index to format.
    */
  val getAriaValueText: js.UndefOr[js.Function2[Value_t, Index_t, String]] = js.undefined
  val marks: js.UndefOr[Boolean | js.Array[MuiSliderMark]] = js.undefined
  val max: js.UndefOr[Value_t] = js.undefined
  val min: js.UndefOr[Value_t] = js.undefined
  val name: js.UndefOr[String] = js.undefined
  @JSName("onChange")
  val onChange2: js.UndefOr[js.Function2[ReactEventFromHtml, Value_t, Unit]] = js.undefined
  val onChangeCommitted: js.UndefOr[js.Function2[ReactEventFromHtml, Value_t, Unit]] = js.undefined
  /** horizontal | vertical */
  val orientation: js.UndefOr[String] = js.undefined
  val step: js.UndefOr[Value_t] = js.undefined
  val ThumbComponent: js.UndefOr[Component_t] = js.undefined
  val track: js.UndefOr[MuiSliderTrackTypes.Track_t] = js.undefined
  val value: js.UndefOr[Value_t | js.Array[Value_t]] = js.undefined
  val ValueLabelComponent: js.UndefOr[Component_t] = js.undefined
  val valueLabelDisplay: js.UndefOr[MuiSliderValueLabelDisplay.Display_t] = js.undefined
  val valueLabelFormat: js.UndefOr[String | js.Function2[Value_t, Index_t, String]] = js.undefined
}
object MuiSliderProps extends MuiPropsBaseStatic[MuiSliderProps] {
  type Index_t = Int
  type Value_t = Index_t
}


trait MuiSliderMark extends js.Object {
  val value: MuiSliderProps.Value_t
  val label: js.UndefOr[String] = js.undefined
}


trait MuiSliderClasses extends MuiClassesBase {
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val marked: js.UndefOr[String] = js.undefined
  val vertical: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val rail: js.UndefOr[String] = js.undefined
  val track: js.UndefOr[String] = js.undefined
  val trackFalse: js.UndefOr[String] = js.undefined
  val trackInverted: js.UndefOr[String] = js.undefined
  val thumb: js.UndefOr[String] = js.undefined
  val thumbColorPrimary: js.UndefOr[String] = js.undefined
  val thumbColorSecondary: js.UndefOr[String] = js.undefined
  val active: js.UndefOr[String] = js.undefined
  val focusVisible: js.UndefOr[String] = js.undefined
  val valueLabel: js.UndefOr[String] = js.undefined
  val mark: js.UndefOr[String] = js.undefined
  val markActive: js.UndefOr[String] = js.undefined
  val markLabel: js.UndefOr[String] = js.undefined
  val markLabelActive: js.UndefOr[String] = js.undefined
}


object MuiSliderTrackTypes {
  final type Track_t = js.Any
  final def Normal    = "normal".asInstanceOf[Track_t]
  final def False     = false.asInstanceOf[Track_t]
  final def Inverted  = "inverted".asInstanceOf[Track_t]
}


object MuiSliderValueLabelDisplay {
  final type Display_t = js.Any
  final def On      = "on".asInstanceOf[Display_t]
  final def Off     = "off".asInstanceOf[Display_t]
  final def Auto    = "auto".asInstanceOf[Display_t]
}
