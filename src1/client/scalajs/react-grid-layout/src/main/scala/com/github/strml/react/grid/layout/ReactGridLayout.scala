package com.github.strml.react.grid.layout

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 17:15
  * Description: Scala.js API for ReactGridLayout component.
  */
object ReactGridLayout {

  val component = JsComponent[ReactGridLayoutProps, Children.Varargs, js.Object]( ReactGridLayoutJs )

  def apply(rglProps: ReactGridLayoutProps)(children: VdomNode*)  = component( rglProps )(children: _*)

}


@js.native
@JSImport("react-grid-layout", JSImport.Namespace)
object ReactGridLayoutJs extends js.Object


/** Props for instantiating RGL. */
trait ReactGridLayoutProps extends RglPropsCommon {

  /** Number of columns in this layout. */
  val cols                : js.UndefOr[Int]                       = js.undefined

  /** Layout is an array of object with the format:
    * {x: number, y: number, w: number, h: number}
    * The index into the layout must match the key used on each item component.
    * If you choose to use custom keys, you can specify that key in the layout
    * array objects like so:
    * {i: string, x: number, y: number, w: number, h: number}
    */
  val layout              : js.UndefOr[js.Array[Layout]]          = js.undefined

  /** Callback so you can save the layout.
    * Calls back with (currentLayout) after every drag or resize stop. */
  val onLayoutChange      : js.UndefOr[js.Function1[Layout, _]]   = js.undefined

}
