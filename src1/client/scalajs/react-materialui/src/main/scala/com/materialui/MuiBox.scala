package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

/** The Box component serves as a wrapper component for most of the CSS utility needs.
  * @see [[https://mui.com/components/box/]]
  */
object MuiBox {

  val component = JsForwardRefComponent[Props, Children.Varargs, dom.html.Element]( Mui.Box )

  def apply(props: Props = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  /** Properties for Box. */
  trait Props
    extends MuiPropsBase
    with MuiPropsBaseComponent
    //with MuiSx  // [Commented] "As a CSS utility component, the Box also supports all system properties. You can use them as prop directly on the component."

}
