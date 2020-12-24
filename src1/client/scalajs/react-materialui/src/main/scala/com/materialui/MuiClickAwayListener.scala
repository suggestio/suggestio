package com.materialui

import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 21:30
  * @see [[https://material-ui.com/ru/api/click-away-listener/]]
  */
object MuiClickAwayListener {

  val component = JsComponent[Props, Children.Varargs, Null]( Mui.ClickAwayListener )


  trait Props extends js.Object {
    val disableReactTree: js.UndefOr[Boolean] = js.undefined
    val mouseEvent: js.UndefOr[MouseEvent] = js.undefined
    val onClickAway: js.Function0[Unit]
    val touchEvent: js.UndefOr[TouchEvent] = js.undefined
  }


  type MouseEvent <: js.Any
  object MouseEvent {
    final def ON_CLICK = "onClick".asInstanceOf[MouseEvent]
    final def ON_MOUSE_DOWN = "onMouseDown".asInstanceOf[MouseEvent]
    final def ON_MOUSE_UP = "onMouseUp".asInstanceOf[MouseEvent]
    final def DISABLE = false.asInstanceOf[MouseEvent]
  }


  type TouchEvent <: js.Any
  object TouchEvent {
    final def ON_TOUCH_END = "onTouchEnd".asInstanceOf[TouchEvent]
    final def ON_TOUCH_START = "onTouchStart".asInstanceOf[TouchEvent]
    final def DISABLE = false.asInstanceOf[TouchEvent]
  }

}

