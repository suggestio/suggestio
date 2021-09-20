package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|


object MuiFade {

  val component = JsForwardRefComponent[MuiFadeProps, Children.Varargs, dom.html.Element]( Mui.Fade )

  // TODO child: make can-hold-ref type instead of VdomElement.
  def apply(props: MuiFadeProps)(child: VdomElement) =
    component(props)(child)

}


trait MuiFadeProps extends MuiPropsBase {
  val appear,
      in: js.UndefOr[Boolean] = js.undefined
  val easing: js.UndefOr[MuiEasingDuration | String] = js.undefined
  val timeout: js.UndefOr[Double | MuiTransitionDuration] = js.undefined
}


trait MuiEasingDuration extends js.Object {
  val enter,
      exit: js.UndefOr[String] = js.undefined
}


