package com.materialui

import com.materialui.MuiTransitionDuration.TransitionDuration_t
import japgolly.scalajs.react.{Children, JsForwardRefComponent}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

object MuiCollapse {

  val component = JsForwardRefComponent[Props, Children.Varargs, dom.html.Element]( Mui.Collapse )

  trait Props
    extends MuiPropsBase
    with MuiPropsBaseClasses[Classes]
    with MuiPropsBaseComponent
  {
    val addEndListener: js.UndefOr[js.Function0[_]] = js.undefined
    val collapsedSize: js.UndefOr[Int | String] = js.undefined
    val easing: js.UndefOr[MuiEasingDuration | String] = js.undefined
    val in: js.UndefOr[Boolean] = js.undefined
    val orientation: js.UndefOr[Orientation] = js.undefined
    val timeout: js.UndefOr[TransitionDuration_t] = js.undefined
  }


  trait Classes extends MuiClassesBase {
    val
      horizontal,
      entered,
      hidden,
      wrapper,
      wrapperInner
      : js.UndefOr[String] = js.undefined
  }


  type Orientation <: String
  object Orientation {
    final def HORIZONTAL = "horizontal".asInstanceOf[Orientation]
    final def VERTICAL = "vertical".asInstanceOf[Orientation]
  }

}
