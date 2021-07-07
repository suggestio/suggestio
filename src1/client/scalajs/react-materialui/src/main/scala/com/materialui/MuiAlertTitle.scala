package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

/**
  * @see [[https://next.material-ui.com/ru/api/alert-title/]]
  */
object MuiAlertTitle {

  val component = JsForwardRefComponent[Props, Children.Varargs, dom.html.Element]( Mui.AlertTitle )

  def apply(props: Props = MuiPropsBaseStatic.empty[Props])(children: VdomNode*) =
    component(props)( children: _* )


  trait Props
    extends MuiPropsBase
    with MuiPropsBaseClasses[Classes]


  trait Classes extends MuiClassesBase

}
